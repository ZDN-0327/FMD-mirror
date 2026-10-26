package de.nulide.findmydevice.net

import android.content.Context
import de.nulide.findmydevice.commands.CommandHandler
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.data.SettingsRepository
import de.nulide.findmydevice.transports.NextcloudTransport
import de.nulide.findmydevice.utils.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Downloads pending commands from the Nextcloud fmd app (SSO) and executes them.
 * Invoked from UnifiedPushService on push, similar to [ServerCommandDownloader].
 *
 * Each command runs in its own coroutine so a long-running locate (GPS) does not
 * block ring/lock/stats/camera or other commands that arrive while GPS is settling.
 */
class NextcloudCommandDownloader(
    private val context: Context,
) {
    companion object {
        private val TAG = NextcloudCommandDownloader::class.simpleName
    }

    private val settings = SettingsRepository.getInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun download() {
        val enabled = settings.get(Settings.SET_NEXTCLOUD_ENABLED) as? Boolean ?: false
        if (!enabled || !NextcloudSsoHelper.hasAccount(context)) {
            return
        }
        val deviceId = settings.get(Settings.SET_NEXTCLOUD_DEVICE_ID) as? String ?: ""
        if (deviceId.isBlank()) {
            return
        }

        context.log().i(TAG, "Fetching pending Nextcloud commands via SSO")
        val api = NextcloudApiRepository.getInstance(context)
        api.getPendingCommands(
            onSuccess = { commands ->
                val trigger = settings.get(Settings.SET_FMD_COMMAND) as? String ?: "fmd"
                for (i in 0 until commands.size()) {
                    val item = commands.get(i).asJsonObject
                    val cmdId = item.get("id")?.asLong ?: -1L
                    val command = item.get("command")?.asString ?: ""
                    if (command.isBlank()) continue
                    val raw = if (command.trim().startsWith(trigger, ignoreCase = true)) {
                        command
                    } else {
                        "$trigger $command"
                    }

                    // Launch each command independently so GPS locate does not
                    // serialize the whole queue behind requestLocationUpdates.
                    scope.launch {
                        context.log().i(TAG, "Executing: $raw (id=$cmdId)")
                        try {
                            // Fresh transport/handler per job (not shared across concurrent cmds)
                            val transport = NextcloudTransport(context)
                            val handler = CommandHandler(transport, showUsageNotification = false)
                            handler.execute(context, raw)
                        } catch (e: Exception) {
                            context.log().e(TAG, "Command failed: ${e.message}")
                        } finally {
                            if (cmdId >= 0) {
                                api.ackCommand(cmdId, { }, { e ->
                                    context.log().w(TAG, "Ack failed: ${e.message}")
                                })
                            }
                        }
                    }
                }
            },
            onError = { e ->
                context.log().e(TAG, "Pending fetch failed: ${e.message}")
            },
        )
    }
}
