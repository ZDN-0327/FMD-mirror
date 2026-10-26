package de.nulide.findmydevice.net

import android.content.Context
import com.google.gson.GsonBuilder
import com.nextcloud.android.sso.AccountImporter
import com.nextcloud.android.sso.api.NextcloudAPI
import com.nextcloud.android.sso.helper.SingleAccountHelper
import com.nextcloud.android.sso.model.SingleSignOnAccount
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.data.SettingsRepository
import de.nulide.findmydevice.utils.log
import retrofit2.NextcloudRetrofitApiBuilder

/**
 * Wraps Nextcloud Android SSO (account from Nextcloud Files app).
 * No passwords are stored in FMD — auth is delegated entirely to the Files app.
 */
object NextcloudSsoHelper {

    private val TAG = NextcloudSsoHelper::class.simpleName
    private const val API_ENDPOINT = "/index.php/apps/fmd/"

    fun storedAccountName(context: Context): String {
        val settings = SettingsRepository.getInstance(context)
        return settings.get(Settings.SET_NEXTCLOUD_SSO_ACCOUNT) as? String ?: ""
    }

    fun setStoredAccountName(context: Context, name: String) {
        SettingsRepository.getInstance(context).set(Settings.SET_NEXTCLOUD_SSO_ACCOUNT, name)
    }

    fun hasAccount(context: Context): Boolean {
        return try {
            getAccount(context)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun getAccount(context: Context): SingleSignOnAccount {
        val preferred = storedAccountName(context)
        return if (preferred.isNotBlank()) {
            try {
                AccountImporter.getSingleSignOnAccount(context, preferred)
            } catch (_: Exception) {
                SingleAccountHelper.getCurrentSingleSignOnAccount(context)
            }
        } else {
            SingleAccountHelper.getCurrentSingleSignOnAccount(context)
        }
    }

    fun createApi(
        context: Context,
        onError: (Exception) -> Unit = { },
    ): Pair<NextcloudAPI, FmdNextcloudApi> {
        val account = getAccount(context)
        val gson = GsonBuilder().create()
        val nextcloudAPI = NextcloudAPI(
            context.applicationContext,
            account,
            gson,
            object : NextcloudAPI.ApiConnectedListener {
                override fun onConnected() {
                    context.log().d(TAG, "SSO API connected for ${account.name}")
                }

                override fun onError(e: Exception) {
                    context.log().e(TAG, "SSO API error: ${e.message}")
                    onError(e)
                }
            },
        )
        val api = NextcloudRetrofitApiBuilder(nextcloudAPI, API_ENDPOINT)
            .create(FmdNextcloudApi::class.java)
        return nextcloudAPI to api
    }

    fun syncAccountMetaToSettings(context: Context, account: SingleSignOnAccount) {
        val settings = SettingsRepository.getInstance(context)
        settings.set(Settings.SET_NEXTCLOUD_SSO_ACCOUNT, account.name)
        settings.set(Settings.SET_NEXTCLOUD_URL, account.url ?: "")
        settings.set(Settings.SET_NEXTCLOUD_USER, account.userId ?: "")
    }

    fun clearAccount(context: Context) {
        setStoredAccountName(context, "")
        val settings = SettingsRepository.getInstance(context)
        settings.set(Settings.SET_NEXTCLOUD_ENABLED, false)
        settings.set(Settings.SET_NEXTCLOUD_DEVICE_ID, "")
    }
}
