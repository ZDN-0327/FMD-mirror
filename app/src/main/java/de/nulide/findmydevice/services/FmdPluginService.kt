package de.nulide.findmydevice.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import de.nulide.findmydevice.IFmdPlugin
import de.nulide.findmydevice.IFmdPluginCallback
import de.nulide.findmydevice.data.AccessRepository
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.data.SettingsRepository
import de.nulide.findmydevice.database.Plugin
import de.nulide.findmydevice.workers.CommandExecutionWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FmdPluginService: Service() {

    companion object {
        const val ACTION_REPLY = "de.nulide.findmydevice.PLUGIN_REPLY"
        const val EXTRA_CALLER = "caller"
        const val EXTRA_MSG = "msg"
    }

    private val PLUGIN_PERMISSION = "de.nulide.findmydevice.permission.CONTROL_FMD"

    private var pluginCallbacks = mutableMapOf<String, IFmdPluginCallback>()

    private val settings: SettingsRepository = SettingsRepository.getInstance(this)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val binder = object : IFmdPlugin.Stub() {
        override fun executeCommand(cmd: String?) {
            enforceCallingPermission(PLUGIN_PERMISSION, "Missing Permission")
            val caller = getCaller()
            if(caller == null || cmd == null){
                return
            }
            val fullCmd = (settings.get(Settings.SET_FMD_COMMAND) as String) + " " + cmd
            val inputData = workDataOf(
                CommandExecutionWorker.KEY_COMMAND to fullCmd,
                CommandExecutionWorker.KEY_TRANSPORT_TYPE to CommandExecutionWorker.TRANS_PLUGIN,
                CommandExecutionWorker.KEY_DESTINATION to caller,
            )
            val workRequest = OneTimeWorkRequestBuilder<CommandExecutionWorker>()
                .setInputData(inputData)
                .build()
            WorkManager.getInstance(baseContext).enqueue(workRequest)
        }

        override fun requestPluginAccess() {
            val caller = getCaller()
            if (!caller.isNullOrEmpty()) {
                scope.launch {
                    val accessRepo = AccessRepository.getInstance(this@FmdPluginService)
                    accessRepo.insertPlugin(Plugin(0,caller,caller))
                    pluginCallbacks[caller]?.accessGranted()
                }
            }
        }

        override fun registerCallBack(callback: IFmdPluginCallback?) {
            val caller = getCaller()
            if (caller != null && callback != null) {
                pluginCallbacks[caller] = callback
            }
        }
    }

    fun getCaller(): String?{
        val callingUuid = Binder.getCallingUid()
        val callingPackages = packageManager.getPackagesForUid(callingUuid)
        if(!callingPackages.isNullOrEmpty()){
            return callingPackages[0]
        }
        return null
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action){
            ACTION_REPLY -> {
                val caller = intent.getStringExtra(EXTRA_CALLER)
                val msg = intent.getStringExtra(EXTRA_MSG)
                if (!caller.isNullOrEmpty() && msg != null) {
                    pluginCallbacks[caller]?.onResponse(msg)
                }
            }
        }

        return START_STICKY
    }

}