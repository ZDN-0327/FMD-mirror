package de.nulide.findmydevice.ui.settings

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.android.sso.AccountImporter
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppNotInstalledException
import com.nextcloud.android.sso.exceptions.SSOException
import com.nextcloud.android.sso.helper.SingleAccountHelper
import com.nextcloud.android.sso.model.SingleSignOnAccount
import com.nextcloud.android.sso.ui.UiExceptionManager
import de.nulide.findmydevice.R
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.data.SettingsRepository
import de.nulide.findmydevice.databinding.ActivityNextcloudBinding
import de.nulide.findmydevice.net.NextcloudApiRepository
import de.nulide.findmydevice.net.NextcloudSsoHelper
import de.nulide.findmydevice.services.isRegisteredWithUnifiedPush
import de.nulide.findmydevice.services.unregisterWithUnifiedPush
import de.nulide.findmydevice.ui.FmdActivity
import de.nulide.findmydevice.ui.UiUtil.Companion.setupEdgeToEdgeAppBar
import de.nulide.findmydevice.ui.UiUtil.Companion.setupEdgeToEdgeScrollView
import de.nulide.findmydevice.utils.log
import de.nulide.findmydevice.warnings.showDialogMissingUnifiedPush
import de.nulide.findmydevice.warnings.showDialogMultipleUnifiedPushDistributorApps
import org.unifiedpush.android.connector.INSTANCE_DEFAULT
import org.unifiedpush.android.connector.data.ResolvedDistributor
import org.unifiedpush.android.connector.UnifiedPush

/**
 * Settings UI for the Nextcloud SSO transport:
 * choose Files-app account, register/unregister device, and UnifiedPush.
 */
class NextcloudActivity : FmdActivity() {

    private lateinit var binding: ActivityNextcloudBinding
    private lateinit var settings: SettingsRepository
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = SettingsRepository.getInstance(this)
        binding = ActivityNextcloudBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupEdgeToEdgeAppBar(binding.appBar)
        setupEdgeToEdgeScrollView(binding.scrollView)

        binding.buttonChooseAccount.setOnClickListener { chooseAccount() }
        binding.buttonRegister.setOnClickListener { registerDevice() }
        binding.buttonUnregister.setOnClickListener { confirmUnregister() }
        binding.buttonRegisterPush.setOnClickListener { onRegisterPushClicked() }

        val storedName = settings.get(Settings.SET_NEXTCLOUD_DEVICE_NAME) as? String ?: ""
        if (storedName.isNotBlank()) {
            binding.editDeviceName.setText(storedName)
        } else {
            binding.editDeviceName.setText(Build.MODEL)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    private fun refreshUi() {
        val accountName = settings.get(Settings.SET_NEXTCLOUD_SSO_ACCOUNT) as? String ?: ""
        val url = settings.get(Settings.SET_NEXTCLOUD_URL) as? String ?: ""
        val user = settings.get(Settings.SET_NEXTCLOUD_USER) as? String ?: ""
        val deviceId = settings.get(Settings.SET_NEXTCLOUD_DEVICE_ID) as? String ?: ""
        val enabled = settings.get(Settings.SET_NEXTCLOUD_ENABLED) as? Boolean ?: false

        if (accountName.isBlank() && !NextcloudSsoHelper.hasAccount(this)) {
            binding.textViewAccount.text = getString(R.string.Settings_Nextcloud_NoAccount)
            binding.textViewServer.text = ""
        } else {
            val display = when {
                user.isNotBlank() && url.isNotBlank() -> "$user@$url"
                accountName.isNotBlank() -> accountName
                else -> getString(R.string.Settings_Nextcloud_Account, "—")
            }
            binding.textViewAccount.text = getString(R.string.Settings_Nextcloud_Account, display)
            binding.textViewServer.text = url
        }

        if (deviceId.isNotBlank()) {
            binding.textViewDeviceId.text = getString(R.string.Settings_Nextcloud_Registered, deviceId)
        } else {
            binding.textViewDeviceId.text = getString(R.string.Settings_Nextcloud_NotRegistered)
        }

        binding.textViewStatus.text = if (enabled && deviceId.isNotBlank()) {
            getString(R.string.Settings_Nextcloud_Status_Active)
        } else {
            getString(R.string.Settings_Nextcloud_Status_Inactive)
        }

        binding.buttonRegister.isEnabled =
            NextcloudSsoHelper.hasAccount(this) || accountName.isNotBlank()
        binding.buttonUnregister.isEnabled = deviceId.isNotBlank() || enabled

        updatePushSection()
    }

    private fun updatePushSection() {
        val distributor = UnifiedPush.getAckDistributor(this)
        val pushUrl = (settings.get(Settings.SET_NEXTCLOUD_PUSH_URL) as? String)
            ?.ifBlank { null }
            ?: (settings.get(Settings.SET_FMDSERVER_PUSH_URL) as? String)?.ifBlank { null }
            ?: ""

        if (!distributor.isNullOrEmpty()) {
            binding.textViewPushDistributor.text =
                getString(R.string.Settings_FMDServer_Push_Distributor, distributor)
        } else {
            binding.textViewPushDistributor.text = getString(R.string.Settings_Nextcloud_Push_None)
        }

        if (pushUrl.isNotBlank()) {
            binding.textViewPushUrl.text =
                getString(R.string.Settings_FMDServer_Push_Url, pushUrl)
        } else {
            binding.textViewPushUrl.text = getString(R.string.Settings_Nextcloud_Push_None)
        }

        binding.buttonRegisterPush.setText(
            if (isRegisteredWithUnifiedPush(this) && pushUrl.isNotBlank()) {
                R.string.Settings_FMDServer_Push_Register_Again
            } else {
                R.string.Settings_FMDServer_Push_Register
            },
        )
    }

    private fun chooseAccount() {
        try {
            AccountImporter.pickNewAccount(this)
        } catch (e: NextcloudFilesAppNotInstalledException) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.Settings_Nextcloud)
                .setMessage(R.string.Settings_Nextcloud_Requires_Files_App)
                .setPositiveButton(R.string.Ok, null)
                .show()
        } catch (e: SSOException) {
            UiExceptionManager.showDialogForException(this, e)
        } catch (e: Exception) {
            log().e(TAG, "Account picker failed: ${e.message}")
            Toast.makeText(this, e.message ?: "SSO error", Toast.LENGTH_LONG).show()
        }
    }

    private fun onAccountImported(account: SingleSignOnAccount) {
        try {
            SingleAccountHelper.commitCurrentAccount(this, account.name)
        } catch (e: Exception) {
            log().w(TAG, "commitCurrentAccount: ${e.message}")
        }
        NextcloudSsoHelper.syncAccountMetaToSettings(this, account)
        log().i(TAG, "SSO account selected: ${account.name}")
        refreshUi()
        Toast.makeText(
            this,
            getString(R.string.Settings_Nextcloud_Account, account.name),
            Toast.LENGTH_SHORT,
        ).show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            AccountImporter.onActivityResult(requestCode, resultCode, data, this) { account ->
                onAccountImported(account)
            }
        } catch (e: SSOException) {
            UiExceptionManager.showDialogForException(this, e)
        } catch (e: Exception) {
            log().e(TAG, "onActivityResult SSO: ${e.message}")
        }
    }

    private fun registerDevice() {
        if (!NextcloudSsoHelper.hasAccount(this)) {
            Toast.makeText(this, R.string.Settings_Nextcloud_NoAccount, Toast.LENGTH_LONG).show()
            return
        }
        val name = binding.editDeviceName.text?.toString()?.trim().orEmpty().ifBlank { Build.MODEL }
        settings.set(Settings.SET_NEXTCLOUD_DEVICE_NAME, name)
        val pushUrl = (settings.get(Settings.SET_NEXTCLOUD_PUSH_URL) as? String)
            ?.ifBlank { null }
            ?: (settings.get(Settings.SET_FMDSERVER_PUSH_URL) as? String)?.ifBlank { null }

        binding.buttonRegister.isEnabled = false
        Toast.makeText(this, R.string.Settings_Nextcloud_Registering, Toast.LENGTH_SHORT).show()

        NextcloudApiRepository.getInstance(this).registerDevice(
            name = name,
            pushUrl = pushUrl,
            onSuccess = {
                runOnUiThread {
                    binding.buttonRegister.isEnabled = true
                    refreshUi()
                    val id = settings.get(Settings.SET_NEXTCLOUD_DEVICE_ID) as? String ?: ""
                    Toast.makeText(
                        this,
                        getString(R.string.Settings_Nextcloud_Registered, id),
                        Toast.LENGTH_LONG,
                    ).show()
                    // Ensure push is registered so the Nextcloud app can wake the device
                    registerWithUnifiedPush()
                }
            },
            onError = { e ->
                runOnUiThread {
                    binding.buttonRegister.isEnabled = true
                    log().e(TAG, "Register failed: ${e.message}")
                    Toast.makeText(
                        this,
                        getString(R.string.Settings_Nextcloud_Register_Failed, e.message ?: ""),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            },
        )
    }

    private fun confirmUnregister() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.Settings_Nextcloud_Unregister)
            .setMessage(R.string.Settings_Nextcloud_Unregister_Confirm)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.Settings_Nextcloud_Unregister) { _, _ ->
                unregisterDevice()
            }
            .show()
    }

    private fun unregisterDevice() {
        binding.buttonUnregister.isEnabled = false
        NextcloudApiRepository.getInstance(this).unregister(
            onSuccess = {
                runOnUiThread {
                    binding.buttonUnregister.isEnabled = true
                    settings.set(Settings.SET_NEXTCLOUD_PUSH_URL, "")
                    refreshUi()
                    Toast.makeText(this, R.string.Settings_Nextcloud_Unregistered, Toast.LENGTH_SHORT)
                        .show()
                }
            },
            onError = { e ->
                runOnUiThread {
                    binding.buttonUnregister.isEnabled = true
                    settings.set(Settings.SET_NEXTCLOUD_DEVICE_ID, "")
                    settings.set(Settings.SET_NEXTCLOUD_ENABLED, false)
                    settings.set(Settings.SET_NEXTCLOUD_PUSH_URL, "")
                    refreshUi()
                    Toast.makeText(
                        this,
                        getString(R.string.Settings_Nextcloud_Register_Failed, e.message ?: ""),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            },
        )
    }

    private fun onRegisterPushClicked() {
        // Force re-registration to refresh the push URL and re-upload to Nextcloud
        unregisterWithUnifiedPush(this)
        // Keep Nextcloud enabled flag so UnifiedPushService.onNewEndpoint still updates NC
        registerWithUnifiedPush()
    }

    /**
     * Same flow as [FMDServerActivity.registerWithUnifiedPush].
     * When the distributor delivers a new endpoint, [de.nulide.findmydevice.services.UnifiedPushService]
     * stores it and (if Nextcloud is enabled) calls updatePushUrl on the fmd app.
     */
    private fun registerWithUnifiedPush() {
        when (val res = UnifiedPush.resolveDefaultDistributor(this)) {
            is ResolvedDistributor.Found -> {
                UnifiedPush.saveDistributor(this, res.packageName)
                UnifiedPush.register(this, INSTANCE_DEFAULT, null, null)
                handler.postDelayed({ updatePushSection() }, 1500L)
            }

            is ResolvedDistributor.ToSelect -> {
                showDialogMultipleUnifiedPushDistributorApps(this) {
                    registerWithUnifiedPushMultipleDistributors()
                }
            }

            is ResolvedDistributor.NoneAvailable -> {
                log().w(TAG, "No UnifiedPush distributor app found")
                showDialogMissingUnifiedPush(this) {
                    // User can install a distributor then tap Register again
                }
            }

            else -> {
                log().e(TAG, "Unknown ResolvedDistributor: $res")
            }
        }
    }

    private fun registerWithUnifiedPushMultipleDistributors() {
        UnifiedPush.tryPickDistributor(this) { success ->
            if (success) {
                UnifiedPush.register(this, INSTANCE_DEFAULT, null, null)
                handler.postDelayed({ updatePushSection() }, 1500L)
            }
        }
    }

    companion object {
        private val TAG = NextcloudActivity::class.simpleName
    }
}
