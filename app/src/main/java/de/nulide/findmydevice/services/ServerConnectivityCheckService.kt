package de.nulide.findmydevice.services

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build
import de.nulide.findmydevice.R
import de.nulide.findmydevice.data.SettingsRepository
import de.nulide.findmydevice.net.FmdServerRepository
import de.nulide.findmydevice.ui.settings.FMDServerActivity
import de.nulide.findmydevice.utils.Notifications
import de.nulide.findmydevice.utils.log
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


/**
 * Checks the connectivity to FMD Server at regular intervals in the background.
 */
class ServerConnectivityCheckService : FmdJobService() {

    override val TAG = ServerConnectivityCheckService::class.simpleName

    companion object {
        const val JOB_ID: Int = 112

        val TAG = ServerConnectivityCheckService::class.simpleName

        @JvmStatic
        fun scheduleJob(context: Context) {
            val settingsRepo = SettingsRepository.getInstance(context)
            val interval = settingsRepo.settings.serverConnCheckIntervalHours
            scheduleJob(context, interval)
        }

        @JvmStatic
        fun scheduleJob(context: Context, intervalHours: Int) {
            if (intervalHours == 0) {
                context.log().i(TAG, "Not scheduling connectivity check service")
                return
            }

            val intervalMillis = intervalHours * 60 * 60 * 1000L

            val serviceComponent =
                ComponentName(context, ServerConnectivityCheckService::class.java)
            val builder = JobInfo.Builder(JOB_ID, serviceComponent)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                // The period must be >= 15 minutes, due to Android requirements
                .setPeriodic(intervalMillis)
                .setPersisted(true)

            val jobScheduler = context.getSystemService(JobScheduler::class.java)
            jobScheduler.schedule(builder.build())
        }

        @JvmStatic
        fun cancelJob(context: Context) {
            val jobScheduler = context.getSystemService(JobScheduler::class.java)
            jobScheduler.cancel(JOB_ID)
        }

        fun shouldNudgeAboutConnectivityCheck(context: Context): Boolean {
            val settingsRepo = SettingsRepository.getInstance(context)
            val interval = settingsRepo.settings.serverConnCheckIntervalHours
            return settingsRepo.serverAccountExists() && interval <= 0L
        }

        /**
         * Nudge the user to enable the server connectivity check,
         * by showing a notification telling the user that this feature exists.
         * This should be used sparingly, to not annoy the user.
         */
        fun notifyAboutConnectivityCheck(context: Context) {
            if (shouldNudgeAboutConnectivityCheck(context)) {
                val title = context.getString(R.string.server_connectivity_check_not_enabled_title)
                val text = context.getString(R.string.server_connectivity_check_not_enabled_text)

                Notifications.notify(
                    context,
                    title,
                    text,
                    Notifications.CHANNEL_SERVER,
                    cls = FMDServerActivity::class.java,
                )
            }
        }
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        super.onStartJob(params)
        doWork()
        return true
    }

    private fun doWork() {
        val context = this

        val settingsRepo = SettingsRepository.getInstance(context)
        val fmdServerRepo = FmdServerRepository(context).getApiService()
        val now: Long = System.currentTimeMillis()

        fmdServerRepo.checkConnection(
            { _ ->
                context.log().i(TAG, "Successfully connected to FMD Server")
                settingsRepo.update { copy(serverConnCheckLastSeenMillis = now) }
                jobFinished()
            },
            { error ->
                context.log().e(TAG, "Failed to connect to FMD Server: ${error.message}")

                if (error.statusCode == 404) {
                    val userName = settingsRepo.settings.serverUsername
                    Notifications.notify(
                        context,
                        getString(R.string.server_connectivity_lost_title),
                        getString(R.string.server_apparently_deleted_text, userName),
                        Notifications.CHANNEL_FAILED
                    )
                    jobFinished()
                    return@checkConnection
                }

                val lastSuccessMillis = settingsRepo.settings.serverConnCheckLastSeenMillis

                val notifyAfterHours = settingsRepo.settings.serverConnCheckNotifyAfterHours
                val notifyAfterMillis = notifyAfterHours * 60 * 60 * 1000

                // Notify the user if the last successful connection is too long ago
                if (lastSuccessMillis + notifyAfterMillis < now) {
                    val lastSuccessString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Instant.ofEpochMilli(lastSuccessMillis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime()
                            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    } else {
                        lastSuccessMillis.toString()
                    }

                    val baseUrl = settingsRepo.settings.serverUrl
                    Notifications.notify(
                        context,
                        context.getString(R.string.server_connectivity_lost_title),
                        context.getString(
                            R.string.server_connectivity_lost_text,
                            baseUrl,
                            lastSuccessString
                        ),
                        Notifications.CHANNEL_FAILED
                    )
                }

                jobFinished()
            }
        )
    }
}
