package de.nulide.findmydevice.services

import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import de.nulide.findmydevice.IFmdPlugin
import de.nulide.findmydevice.IFmdPluginCallback
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.data.SettingsRepository
import de.nulide.findmydevice.ui.access.AccessControlActivity
import de.nulide.findmydevice.workers.CommandExecutionWorker

class FmdPluginService : Service() {

    companion object {
        const val ACTION_REPLY = "de.nulide.findmydevice.PLUGIN_REPLY"
        const val ACTION_GRANTED = "de.nulide.findmydevice.PLUGIN_GRANTED"
        const val EXTRA_APP_ID = "app_id"
        const val EXTRA_MSG = "msg"
    }

    private val PLUGIN_PERMISSION = "de.nulide.findmydevice.permission.CONTROL_FMD"

    private var pluginCallbacks = mutableMapOf<String, IFmdPluginCallback>()

    private val settings: SettingsRepository = SettingsRepository.getInstance(this)

    private val binder = object : IFmdPlugin.Stub() {
        override fun executeCommand(cmd: String?) {
            enforceCallingPermission(PLUGIN_PERMISSION, "Missing Permission")
            val appId = getAppId()
            if (appId == null || cmd == null) {
                return
            }
            val fullCmd = (settings.get(Settings.SET_FMD_COMMAND) as String) + " " + cmd
            val inputData = workDataOf(
                CommandExecutionWorker.KEY_COMMAND to fullCmd,
                CommandExecutionWorker.KEY_TRANSPORT_TYPE to CommandExecutionWorker.TRANS_PLUGIN,
                CommandExecutionWorker.KEY_DESTINATION to appId,
            )
            val workRequest = OneTimeWorkRequestBuilder<CommandExecutionWorker>()
                .setInputData(inputData)
                .build()
            WorkManager.getInstance(baseContext).enqueue(workRequest)
        }

        override fun requestPluginAccess() {
            val appId = getAppId()
            val appName = getAppName(appId ?: "")
            if (!appId.isNullOrEmpty() && !appName.isNullOrEmpty()) {
                val intent = Intent(this@FmdPluginService, AccessControlActivity::class.java)
                intent.putExtra(AccessControlActivity.EXTRA_PAGE, AccessControlActivity.PAGE_PLUGIN)
                intent.putExtra(AccessControlActivity.EXTRA_ADD_PLUGIN_NAME, appName)
                intent.putExtra(AccessControlActivity.EXTRA_ADD_PLUGIN_ID, appId)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        }

        override fun registerCallBack(callback: IFmdPluginCallback?) {
            val appId = getAppId()
            if (appId != null && callback != null) {
                pluginCallbacks[appId] = callback
            }
        }
    }

    fun getAppId(): String? {
        val callingUuid = Binder.getCallingUid()
        val packages = packageManager.getPackagesForUid(callingUuid)
        if (!packages.isNullOrEmpty()) {
            return packages[0]
        }
        return null
    }

    fun getAppName(appId: String): String? {
        return try {
            val info = packageManager.getApplicationInfo(appId, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_REPLY -> {
                val appId = intent.getStringExtra(EXTRA_APP_ID)
                val msg = intent.getStringExtra(EXTRA_MSG)
                if (!appId.isNullOrEmpty() && msg != null) {
                    pluginCallbacks[appId]?.onResponse(msg)
                }
            }

            ACTION_GRANTED -> {
                val appId = intent.getStringExtra(EXTRA_APP_ID)
                if (!appId.isNullOrEmpty()) {
                    pluginCallbacks[appId]?.accessGranted()
                }
            }
        }

        return START_STICKY
    }

}