package de.nulide.findmydevice.services

import android.content.Context
import de.nulide.findmydevice.R
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.data.SettingsRepository
import de.nulide.findmydevice.net.FmdServerRepository
import de.nulide.findmydevice.net.NextcloudApiRepository
import de.nulide.findmydevice.net.NextcloudCommandDownloader
import de.nulide.findmydevice.net.ServerCommandDownloader
import de.nulide.findmydevice.net.ServerError
import de.nulide.findmydevice.ui.settings.FMDServerActivity
import de.nulide.findmydevice.ui.settings.NextcloudActivity
import de.nulide.findmydevice.utils.Notifications
import de.nulide.findmydevice.utils.log
import org.unifiedpush.android.connector.FailedReason
import org.unifiedpush.android.connector.PushService
import org.unifiedpush.android.connector.UnifiedPush
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage


class UnifiedPushService : PushService() {
    companion object {
        val TAG: String = UnifiedPushService::class.java.getSimpleName()
    }

    override fun onMessage(message: PushMessage, instance: String) {
        log().i(TAG, "Received push message")
        ServerCommandDownloader(this).download()
        NextcloudCommandDownloader(this).download()
    }

    override fun onNewEndpoint(endpoint: PushEndpoint, instance: String) {
        log().i(TAG, "Received new push endpoint")

        val settings = SettingsRepository.getInstance(this)

        // Always remember the local endpoint URL for both transports
        settings.set(Settings.SET_FMDSERVER_PUSH_URL, endpoint.url)

        // FMD Server: only upload when a server account exists.
        // Without an account, registerPushEndpoint fails and would show a
        // misleading "push registration failed" notification (e.g. Nextcloud-only users).
        if (settings.serverAccountExists()) {
            val repo = FmdServerRepository(this).getApiService()
            repo.registerPushEndpoint(
                endpoint.url,
                listener = {
                    Notifications.dismiss(this, Notifications.ID_SERVER_PUSH_MISSING)
                    Notifications.dismiss(this, Notifications.ID_SERVER_PUSH_REGISTRATION_FAILED)
                },
                errorListener = { _: ServerError ->
                    val context = this
                    Notifications.notify(
                        context,
                        title = context.getString(R.string.push_registration_failed_title),
                        text = context.getString(R.string.push_registration_upload_failed_text),
                        channelID = Notifications.CHANNEL_SERVER,
                        notificationId = Notifications.ID_SERVER_PUSH_REGISTRATION_FAILED,
                        cls = FMDServerActivity::class.java,
                    )
                },
            )
        } else {
            log().i(TAG, "Skipping FMD Server push upload (no server account)")
        }

        // Nextcloud fmd app: upload when that transport is enabled and registered
        val ncEnabled = settings.get(Settings.SET_NEXTCLOUD_ENABLED) as? Boolean ?: false
        if (ncEnabled) {
            settings.set(Settings.SET_NEXTCLOUD_PUSH_URL, endpoint.url)
            NextcloudApiRepository.getInstance(this).updatePushUrl(
                endpoint.url,
                onSuccess = { log().i(TAG, "Updated Nextcloud push URL") },
                onError = { e ->
                    log().e(TAG, "Failed to update Nextcloud push URL: ${e.message}")
                    Notifications.notify(
                        this,
                        title = getString(R.string.push_registration_failed_title),
                        text = getString(R.string.Settings_Nextcloud_Push_Upload_Failed),
                        channelID = Notifications.CHANNEL_SERVER,
                        cls = NextcloudActivity::class.java,
                    )
                },
            )
        } else {
            log().i(TAG, "Skipping Nextcloud push upload (transport not enabled)")
        }
    }

    override fun onRegistrationFailed(reason: FailedReason, instance: String) {
        log().e(TAG, "Push registration failed, reason=$reason")
    }

    override fun onUnregistered(instance: String) {
        clearPushState(this)
    }
}

fun isUnifiedPushAvailable(context: Context): Boolean {
    return UnifiedPush.getDistributors(context).isNotEmpty()
}

fun isRegisteredWithUnifiedPush(context: Context): Boolean {
    return UnifiedPush.getAckDistributor(context).isNullOrEmpty().not()
}

// fun registerWithUnifiedPush: see FMDServerActivity / NextcloudActivity

fun unregisterWithUnifiedPush(context: Context) {
    if (isRegisteredWithUnifiedPush(context)) {
        UnifiedPush.unregister(context)
    }
    clearPushState(context)
}

private fun clearPushState(context: Context) {
    val settings: SettingsRepository = SettingsRepository.getInstance(context)
    settings.set(Settings.SET_FMDSERVER_PUSH_URL, "")
    settings.set(Settings.SET_NEXTCLOUD_PUSH_URL, "")

    // Either we have triggered the push deregistration (after server account deletion),
    // or someone else (e.g., the distributor itself) has triggered the deregistration.
    // In the latter case, inform the server.
    if (settings.serverAccountExists()) {
        val repo = FmdServerRepository(context).getApiService()
        repo.registerPushEndpoint("", {}, { _ -> /* noop */ })
    }

    val ncEnabled = settings.get(Settings.SET_NEXTCLOUD_ENABLED) as? Boolean ?: false
    if (ncEnabled) {
        NextcloudApiRepository.getInstance(context).updatePushUrl(
            "",
            onSuccess = { },
            onError = { },
        )
    }
}
