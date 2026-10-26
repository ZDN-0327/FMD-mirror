package de.nulide.findmydevice.transports

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import de.nulide.findmydevice.R
import de.nulide.findmydevice.commands.AccessResponse
import de.nulide.findmydevice.commands.ParserResult
import de.nulide.findmydevice.data.FmdLocation
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.data.SettingsRepository
import de.nulide.findmydevice.net.NextcloudApiRepository
import de.nulide.findmydevice.net.NextcloudSsoHelper
import de.nulide.findmydevice.permissions.Permission
import de.nulide.findmydevice.ui.settings.NextcloudActivity
import de.nulide.findmydevice.utils.log

/**
 * Transport for the Nextcloud `fmd` app.
 *
 * Authentication: Nextcloud Android SSO only (Nextcloud Files app).
 * No app passwords, no Basic auth, no FMD Protocol v2 keys.
 */
class NextcloudTransport(
    private val context: Context,
) : Transport<Unit>(Unit) {

    private val settings = SettingsRepository.getInstance(context)
    private val api = NextcloudApiRepository.getInstance(context)

    @get:DrawableRes
    override val icon = R.drawable.ic_database

    @get:StringRes
    override val title = R.string.transport_nextcloud_title

    override val description: String =
        context.getString(R.string.transport_nextcloud_description)

    override val descriptionAuth: String =
        context.getString(R.string.transport_nextcloud_description_auth)

    override val descriptionNote: String =
        context.getString(R.string.transport_nextcloud_description_note)

    override val requiredPermissions: List<Permission> = emptyList()

    override val actions = listOf(
        TransportAction(R.string.Settings_Settings) { activity ->
            activity.startActivity(Intent(context, NextcloudActivity::class.java))
        },
    )

    override fun getDestinationString(): String {
        val account = settings.get(Settings.SET_NEXTCLOUD_SSO_ACCOUNT) as? String ?: ""
        val deviceId = settings.get(Settings.SET_NEXTCLOUD_DEVICE_ID) as? String ?: ""
        return "nextcloud-sso:$account/$deviceId"
    }

    override suspend fun isAllowed(parsed: ParserResult.Success): AccessResponse {
        // Operator is authenticated via Nextcloud session on the server side.
        return AccessResponse.ALLOWED
    }

    override fun send(context: Context, msg: String) {
        super.send(context, msg)
        // Upload text replies (e.g. stats) so the Nextcloud web UI can display them
        if (!accountConfigured()) {
            context.log().w(TAG, "Cannot upload message: Nextcloud not configured")
            return
        }
        val payload = mapOf(
            "text" to msg,
            "time" to (System.currentTimeMillis() / 1000),
        )
        api.postData(
            "message",
            payload,
            { context.log().i(TAG, "Message uploaded via SSO (${msg.length} chars)") },
            { e -> context.log().e(TAG, "Message upload failed: ${e.message}") },
        )
    }

    override fun sendNewLocation(context: Context, location: FmdLocation) {
        if (!accountConfigured()) {
            context.log().w(TAG, "Cannot upload location: Nextcloud not configured")
            return
        }
        val payload = mapOf(
            "lat" to location.lat,
            "lon" to location.lon,
            "accuracy" to location.accuracy,
            "altitude" to location.altitude,
            "provider" to location.provider,
            "bat" to location.batteryLevel,
            "time" to (location.timeMillis / 1000),
        )
        api.postData(
            "location",
            payload,
            { context.log().i(TAG, "Location uploaded via SSO") },
            { e -> context.log().e(TAG, "Location upload failed: ${e.message}") },
        )
    }

    fun uploadPicture(imageBase64: String, facing: String) {
        if (!accountConfigured()) return
        val payload = mapOf(
            "imageBase64" to imageBase64,
            "facing" to facing,
            "time" to (System.currentTimeMillis() / 1000),
        )
        api.postData(
            "picture",
            payload,
            { context.log().i(TAG, "Picture uploaded via SSO") },
            { e -> context.log().e(TAG, "Picture upload failed: ${e.message}") },
        )
    }

    fun accountConfigured(): Boolean {
        val deviceId = settings.get(Settings.SET_NEXTCLOUD_DEVICE_ID) as? String ?: ""
        val enabled = settings.get(Settings.SET_NEXTCLOUD_ENABLED) as? Boolean ?: false
        return enabled && deviceId.isNotBlank() && NextcloudSsoHelper.hasAccount(context)
    }

    companion object {
        private val TAG = NextcloudTransport::class.simpleName
    }
}
