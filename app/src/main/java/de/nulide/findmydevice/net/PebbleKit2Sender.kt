package de.nulide.findmydevice.net

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.data.SettingsRepository
import de.nulide.findmydevice.utils.log
import io.rebble.pebblekit2.common.SendDataCallback
import io.rebble.pebblekit2.common.UniversalRequestResponse
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Sends an AppMessage (a result string) to a watchapp via the Pebble mobile
 * app's PebbleKit 2 sender service (io.rebble.pebblekit2.SEND_DATA_TO_WATCH),
 * the client side of the same binder PebbleKit2Service serves.
 *
 * The dictionary key used for the result string is configurable
 * (Settings.SET_PK2_RESULT_KEY) because message key indices belong to the
 * watchapp. The default (10001) matches a watchapp declaring
 * "messageKeys": ["Cmd", "Result"] - keys are auto-assigned from 10000.
 */
class PebbleKit2Sender(
    private val context: Context,
) {

    companion object {
        private val TAG = PebbleKit2Sender::class.simpleName
        private const val ACTION_SEND_DATA_TO_WATCH = "io.rebble.pebblekit2.SEND_DATA_TO_WATCH"
        private const val KEY_ACTION = "ACTION"
        private const val KEY_WATCHAPP_UUID = "WATCHAPP_UUID"
        private const val KEY_DATA_DICTIONARY = "DATA_DICTIONARY"
        private const val ACTION_SEND_DATA = "SEND_DATA_TO_WATCH"
        private const val KEY_TYPE = "TYPE"
        private const val KEY_VALUE = "_VALUE"
        private const val KEY_TYPE_SUFFIX = "_TYPE"
        private const val SEND_TIMEOUT_MS = 8000L
    }

    suspend fun send(watchappUuid: String, message: String) {
        val settings = SettingsRepository.getInstance(context)
        val resultKey = (settings.get(Settings.SET_PK2_RESULT_KEY) as Number).toInt()

        val pebblePackage = findPebbleApp() ?: run {
            context.log().w(TAG, "No Pebble app with PK2 sender service installed")
            return
        }

        val intent = Intent(ACTION_SEND_DATA_TO_WATCH).setPackage(pebblePackage)

        withTimeoutOrNull(SEND_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val connection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName, service: IBinder) {
                        try {
                            val api = UniversalRequestResponse.Stub.asInterface(service)

                            val dict = Bundle()
                            dict.putString(resultKey.toString() + KEY_TYPE_SUFFIX, "String")
                            dict.putString(resultKey.toString() + KEY_VALUE, message)

                            val data = Bundle()
                            data.putString(KEY_ACTION, ACTION_SEND_DATA)
                            data.putString(KEY_WATCHAPP_UUID, watchappUuid)
                            data.putBundle(KEY_DATA_DICTIONARY, dict)

                            api.request(data, object : SendDataCallback.Stub() {
                                override fun onResult(bundle: Bundle) {
                                    unbind(name)
                                    if (continuation.isActive) {
                                        continuation.resume(Unit)
                                    }
                                }
                            })
                        } catch (e: Exception) {
                            unbind(name)
                            if (continuation.isActive) {
                                continuation.resume(Unit)
                            }
                        }
                    }

                    override fun onServiceDisconnected(name: ComponentName) {
                        // binding dropped; nothing else to do
                    }

                    private fun unbind(name: ComponentName) {
                        try {
                            context.unbindService(this)
                        } catch (e: IllegalArgumentException) {
                            // already unbound
                        }
                    }
                }

                if (!context.bindService(intent, connection, Context.BIND_AUTO_CREATE)) {
                    context.log().w(TAG, "bindService failed for $pebblePackage")
                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }

                continuation.invokeOnCancellation {
                    try {
                        context.unbindService(connection)
                    } catch (e: IllegalArgumentException) {
                        // already unbound
                    }
                }
            }
        } ?: context.log().w(TAG, "Timed out sending to watchapp $watchappUuid")
    }

    private fun findPebbleApp(): String? {
        // Any app exposing the PK2 sender service is a Pebble mobile app.
        // (Do NOT filter by package name - coredevices.coreapp would be missed.)
        val services = context.packageManager.queryIntentServices(Intent(ACTION_SEND_DATA_TO_WATCH), 0)
        return services.firstOrNull()?.serviceInfo?.packageName
    }
}
