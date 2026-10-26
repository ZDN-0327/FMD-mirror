package de.nulide.findmydevice.net

import android.content.Context
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.nextcloud.android.sso.api.NextcloudAPI
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.data.SettingsRepository
import de.nulide.findmydevice.utils.log
import java.util.concurrent.Executors

/**
 * Client for the Nextcloud `fmd` app API using **only** Nextcloud Android SSO.
 * Credentials never leave the Nextcloud Files app.
 */
class NextcloudApiRepository private constructor(
    private val context: Context,
) {
    private val settings = SettingsRepository.getInstance(context)
    private val executor = Executors.newSingleThreadExecutor()

    private fun deviceId(): String =
        settings.get(Settings.SET_NEXTCLOUD_DEVICE_ID) as? String ?: ""

    private fun withApi(block: (FmdNextcloudApi) -> Unit, onError: (Exception) -> Unit) {
        executor.execute {
            var ncApi: NextcloudAPI? = null
            try {
                if (!NextcloudSsoHelper.hasAccount(context)) {
                    onError(IllegalStateException("No Nextcloud SSO account selected"))
                    return@execute
                }
                val (apiBridge, retrofit) = NextcloudSsoHelper.createApi(context, onError)
                ncApi = apiBridge
                block(retrofit)
            } catch (e: Exception) {
                context.log().e(TAG, "SSO request failed: ${e.message}")
                onError(e)
            } finally {
                try {
                    ncApi?.close()
                } catch (_: Exception) {
                }
            }
        }
    }

    fun registerDevice(
        name: String,
        pushUrl: String?,
        onSuccess: (JsonObject) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        withApi({ api ->
            val body = JsonObject()
            body.addProperty("name", name)
            val existingId = deviceId()
            if (existingId.isNotBlank()) {
                body.addProperty("deviceId", existingId)
            }
            if (!pushUrl.isNullOrBlank()) {
                body.addProperty("pushUrl", pushUrl)
            }
            val response = api.registerDevice(body).execute()
            if (!response.isSuccessful) {
                onError(Exception("HTTP ${response.code()}: ${response.errorBody()?.string()}"))
                return@withApi
            }
            val json = response.body() ?: JsonObject()
            val id = json.get("deviceId")?.asString ?: ""
            if (id.isNotBlank()) {
                settings.set(Settings.SET_NEXTCLOUD_DEVICE_ID, id)
            }
            settings.set(Settings.SET_NEXTCLOUD_ENABLED, true)
            onSuccess(json)
        }, onError)
    }

    fun updatePushUrl(
        pushUrl: String,
        onSuccess: (JsonObject) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val id = deviceId()
        if (id.isBlank()) {
            onError(IllegalStateException("No device id"))
            return
        }
        withApi({ api ->
            val body = JsonObject().apply { addProperty("pushUrl", pushUrl) }
            val response = api.updatePush(id, body).execute()
            if (!response.isSuccessful) {
                onError(Exception("HTTP ${response.code()}"))
                return@withApi
            }
            settings.set(Settings.SET_NEXTCLOUD_PUSH_URL, pushUrl)
            onSuccess(response.body() ?: JsonObject())
        }, onError)
    }

    fun postData(
        type: String,
        payload: Map<String, Any?>,
        onSuccess: (JsonObject) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val id = deviceId()
        if (id.isBlank()) {
            onError(IllegalStateException("No device id"))
            return
        }
        withApi({ api ->
            val data = JsonObject()
            for ((k, v) in payload) {
                when (v) {
                    null -> data.add(k, null)
                    is Number -> data.addProperty(k, v)
                    is Boolean -> data.addProperty(k, v)
                    else -> data.addProperty(k, v.toString())
                }
            }
            val body = JsonObject().apply { add("data", data) }
            val response = api.postData(id, type, body).execute()
            if (!response.isSuccessful) {
                onError(Exception("HTTP ${response.code()}"))
                return@withApi
            }
            onSuccess(response.body() ?: JsonObject())
        }, onError)
    }

    fun getPendingCommands(
        onSuccess: (JsonArray) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val id = deviceId()
        if (id.isBlank()) {
            onError(IllegalStateException("No device id"))
            return
        }
        withApi({ api ->
            val response = api.pendingCommands(id).execute()
            if (!response.isSuccessful) {
                onError(Exception("HTTP ${response.code()}"))
                return@withApi
            }
            val arr = response.body()?.getAsJsonArray("commands") ?: JsonArray()
            onSuccess(arr)
        }, onError)
    }

    fun ackCommand(
        commandId: Long,
        onSuccess: (JsonObject) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val id = deviceId()
        if (id.isBlank()) {
            onError(IllegalStateException("No device id"))
            return
        }
        withApi({ api ->
            val response = api.ackCommand(id, commandId).execute()
            if (!response.isSuccessful) {
                onError(Exception("HTTP ${response.code()}"))
                return@withApi
            }
            onSuccess(response.body() ?: JsonObject())
        }, onError)
    }

    fun unregister(
        onSuccess: (JsonObject) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val id = deviceId()
        if (id.isBlank()) {
            NextcloudSsoHelper.clearAccount(context)
            onSuccess(JsonObject())
            return
        }
        withApi({ api ->
            val response = api.deleteDevice(id).execute()
            settings.set(Settings.SET_NEXTCLOUD_DEVICE_ID, "")
            settings.set(Settings.SET_NEXTCLOUD_ENABLED, false)
            if (!response.isSuccessful && response.code() != 404) {
                onError(Exception("HTTP ${response.code()}"))
                return@withApi
            }
            onSuccess(response.body() ?: JsonObject())
        }, onError)
    }

    companion object {
        private val TAG = NextcloudApiRepository::class.simpleName

        @Volatile
        private var instance: NextcloudApiRepository? = null

        fun getInstance(context: Context): NextcloudApiRepository {
            return instance ?: synchronized(this) {
                instance ?: NextcloudApiRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
