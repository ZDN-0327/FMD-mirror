package de.nulide.findmydevice.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.data.SettingsRepository
import de.nulide.findmydevice.utils.log
import de.nulide.findmydevice.workers.CommandExecutionWorker
import io.rebble.pebblekit2.common.SendDataCallback
import io.rebble.pebblekit2.common.UniversalRequestResponse

/**
 * PebbleKit 2 companion service.
 *
 * Lets watchapps on Pebble watches run FMD commands, delivered by the Pebble
 * mobile app via the public PebbleKit 2 binder contract
 * (io.rebble.pebblekit2.RECEIVE_DATA_FROM_WATCH). The watchapp's pbw must
 * declare this app's package in its "companionApp" list.
 *
 * Opt-in: disabled unless Settings.SET_PK2_ENABLED is set. When disabled,
 * onBind() returns null - the bind simply fails, nothing runs, no process is
 * kept alive. The service only exists while the Pebble app holds a binding
 * (i.e. while a companion watchapp is open on a connected watch).
 *
 * Security model: the binder caller must be a Pebble mobile app (checked via
 * the calling UID's packages). Command-level permissions are enforced by the
 * transport (PebbleKit2Transport) against Settings.SET_PK2_PERMISSIONS.
 */
class PebbleKit2Service : Service() {

    companion object {
        private val TAG = PebbleKit2Service::class.simpleName

        // PebbleKit 2 bundle keys (public wire contract)
        private const val KEY_ACTION = "ACTION"
        private const val KEY_WATCHAPP_UUID = "WATCHAPP_UUID"
        private const val KEY_DATA_DICTIONARY = "DATA_DICTIONARY"
        private const val KEY_RESULTS = "TRANSMISSION_RESULTS"
        private const val KEY_TYPE = "TYPE"
        private const val ACTION_RECEIVE_DATA_FROM_WATCH = "ACTION_RECEIVE_DATA_FROM_WATCH"
    }

    private val binder = object : UniversalRequestResponse.Stub() {

        override fun request(data: Bundle?, callback: SendDataCallback?) {
            val reply = Bundle()
            try {
                val ok = handleRequest(data)
                val result = Bundle()
                result.putString(KEY_TYPE, if (ok) "ACK" else "NACK")
                reply.putBundle(KEY_RESULTS, result)
            } catch (e: Exception) {
                log().e(TAG, "PK2 request failed: ${e.stackTraceToString()}")
            }
            callback?.onResult(reply)
        }

        private fun handleRequest(data: Bundle?): Boolean {
            if (data == null) {
                return false
            }
            if (!callerIsPebbleApp()) {
                log().w(TAG, "Rejecting PK2 request from non-Pebble caller")
                return false
            }

            val action = data.getString(KEY_ACTION)
            if (action != ACTION_RECEIVE_DATA_FROM_WATCH) {
                log().w(TAG, "Ignoring PK2 action: $action")
                return false
            }

            val watchappUuid = data.getString(KEY_WATCHAPP_UUID)
                ?: run {
                    log().w(TAG, "PK2 request without watchapp UUID")
                    return false
                }

            val commandSuffix = parseCommandSuffix(data.getBundle(KEY_DATA_DICTIONARY))
            if (commandSuffix == null) {
                log().w(TAG, "PK2 request without a command")
                return false
            }

            val settings = SettingsRepository.getInstance(applicationContext)
            val triggerWord = settings.get(Settings.SET_FMD_COMMAND) as String
            val command = "$triggerWord $commandSuffix"

            log().i(TAG, "Executing command from watchapp $watchappUuid: $command")

            val inputData = workDataOf(
                CommandExecutionWorker.KEY_COMMAND to command,
                CommandExecutionWorker.KEY_TRANSPORT_TYPE to CommandExecutionWorker.TRANS_PK2,
                CommandExecutionWorker.KEY_DESTINATION to watchappUuid,
            )
            WorkManager.getInstance(applicationContext)
                .enqueue(OneTimeWorkRequestBuilder<CommandExecutionWorker>().setInputData(inputData).build())
            return true
        }

        /**
         * The watchapp sends one String dictionary entry with the FMD command
         * suffix (e.g. "ring", "locate cell"). Dictionary entries are stored
         * as "<idx>_TYPE" / "<idx>_VALUE" pairs; scan instead of hardcoding
         * indices. Sanitized to [A-Za-z0-9 ].
         */
        private fun parseCommandSuffix(dict: Bundle?): String? {
            if (dict == null) {
                return null
            }
            var cmd: String? = null
            for (key in dict.keySet()) {
                if (!key.endsWith("_TYPE")) {
                    continue
                }
                if (dict.getString(key) != "String") {
                    continue
                }
                val value = dict.get(key.removeSuffix("_TYPE") + "_VALUE") as? String
                if (!value.isNullOrEmpty()) {
                    cmd = value
                }
            }
            if (cmd == null) {
                return null
            }
            val clean = cmd.filter { it.isLetterOrDigit() || it == ' ' }.trim()
            return if (clean.isEmpty()) null else clean
        }

        private fun callerIsPebbleApp(): Boolean {
            val packages = packageManager.getPackagesForUid(Binder.getCallingUid())
                ?: return false
            return packages.any {
                it.lowercase().contains("pebble") || it.lowercase().contains("rebble")
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        val settings = SettingsRepository.getInstance(this)
        val enabled = settings.get(Settings.SET_PK2_ENABLED) as Boolean
        if (!enabled) {
            log().i(TAG, "PK2 companion is disabled; refusing bind")
            return null
        }
        log().d(TAG, "PK2 companion bound: $intent")
        return binder
    }
}
