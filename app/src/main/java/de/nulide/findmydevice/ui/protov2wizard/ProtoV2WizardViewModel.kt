package de.nulide.findmydevice.ui.protov2wizard


import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.nulide.findmydevice.FmdApplication
import de.nulide.findmydevice.R
import de.nulide.findmydevice.crypto.CryptoV2
import de.nulide.findmydevice.crypto.LongTermKeys
import de.nulide.findmydevice.data.EncryptedSettingsRepository
import de.nulide.findmydevice.data.EncryptedSettingsRepository.Companion.KEY_FMDSERVER_V2_MASTER_KEY
import de.nulide.findmydevice.data.EncryptedSettingsRepository.Companion.KEY_FMDSERVER_V2_PASSWORD_KEY
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.data.SettingsRepository
import de.nulide.findmydevice.net.FMD_SERVER_PROTO_V2
import de.nulide.findmydevice.net.FmdServerApiV1RepoSpec
import de.nulide.findmydevice.net.FmdServerApiV1Repository
import de.nulide.findmydevice.net.FmdServerApiV2RepoSpec
import de.nulide.findmydevice.net.FmdServerApiV2Repository
import de.nulide.findmydevice.utils.CypherUtils
import de.nulide.findmydevice.utils.encodeBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.KeyPair

class ProtoV2WizardViewModel(
    private val context: Application,
    private val settings: SettingsRepository,
    private val encryptedSettings: EncryptedSettingsRepository,
    private val apiV1: FmdServerApiV1Repository,
    private val apiV2: FmdServerApiV2Repository,
) : ViewModel() {

    companion object {
        // https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-factories
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[APPLICATION_KEY] as FmdApplication)
                val settings = SettingsRepository.getInstance(app)
                val encryptedSettings = EncryptedSettingsRepository.getInstance(app)
                val apiV1 = FmdServerApiV1Repository.getInstance(FmdServerApiV1RepoSpec(app))
                val apiV2 = FmdServerApiV2Repository.getInstance(FmdServerApiV2RepoSpec(app))
                ProtoV2WizardViewModel(app, settings, encryptedSettings, apiV1, apiV2)
            }
        }
    }

    private val _wizardState = MutableLiveData<WizardState>(WizardState.STEP_1)
    val wizardState: LiveData<WizardState> = _wizardState

    fun advanceFromStep1(password: String) = viewModelScope.launch(Dispatchers.IO) {
        _wizardState.postValue(WizardState.STEP_1_LOADING)

        // Hash password and decrypt v1 private key
        // Same as FmdServerApiV1Repository::changePassword
        val encryptedPrivKey = settings.get(Settings.SET_FMD_CRYPT_PRIVKEY) as String
        var keyPair: KeyPair? = null
        try {
            keyPair = CypherUtils.decryptPrivateKeyWithPassword(encryptedPrivKey, password)
        } catch (e: Exception) {
            _wizardState.postValue(WizardState.ERROR(e.stackTraceToString()))
            return@launch
        }

        if (keyPair == null) {
            val msg = context.getString(R.string.pw_change_wrong_password)
            _wizardState.postValue(WizardState.ERROR(msg))
            return@launch
        }

        // Generate v2 keys
        // Same as FmdServerApiV2Repository::register
        val username = settings.get(Settings.SET_FMDSERVER_ID) as String
        val hashResult = CryptoV2.hashPassword(username, password)
        val ltk = LongTermKeys.generate(username)
        encryptedSettings.putString(KEY_FMDSERVER_V2_MASTER_KEY, ltk.masterKey.encodeBase64())
        encryptedSettings.putString(
            KEY_FMDSERVER_V2_PASSWORD_KEY,
            hashResult.passwordKey.encodeBase64()
        )
        apiV2.initServicePub() // reload v2 keys from encryptedSettings

        // Upload v2 keys (change password)
        apiV2.changePassword(password, password, {
            // Critical part complete, v2 keys uploaded
            settings.set(Settings.SET_FMD_CRYPT_PROTO, FMD_SERVER_PROTO_V2)

            // Get old v1 location/picture sizes for Step 2
            apiV1.getLocationSize({ numLocations ->
                apiV1.getPictureSize(
                    { numPictures ->
                        // Step 1 complete, proceed to either step 2 or 3
                        if (numLocations == 0 && numPictures == 0) {
                            _wizardState.postValue(WizardState.STEP_3)
                        } else {
                            _wizardState.postValue(
                                WizardState.STEP_2(numLocations, numPictures, keyPair)
                            )
                        }
                    },
                    { _wizardState.postValue(WizardState.ERROR(it.message)) },
                )
            }, {
                _wizardState.postValue(WizardState.ERROR(it.message))
            })

        }, {
            _wizardState.postValue(WizardState.ERROR(it.message))
        })

    }

    fun advanceFromStep2DeleteData() {
        _wizardState.postValue(WizardState.STEP_2_LOADING)

        apiV1.deleteAllLocations({
            apiV1.deleteAllPictures(
                {
                    // Wizard complete
                    _wizardState.postValue(WizardState.STEP_3)
                },
                { _wizardState.postValue(WizardState.ERROR(it.message)) },
            )
        }, {
            _wizardState.postValue(WizardState.ERROR(it.message))
        })
    }

    fun advanceFromStep2MigrateData(keyPair: KeyPair) {
        _wizardState.postValue(WizardState.STEP_2_LOADING)

        // Download the data via APIv1 and re-upload it via APIv2
        apiV1.getAllLocations(
            keyPair.private,
            { locations ->
                apiV2.sendLocations(locations)

                apiV1.getAllPictures(
                    keyPair.private,
                    { pictures ->
                        apiV2.sendPictures(pictures)

                        // Delete the data
                        advanceFromStep2DeleteData()
                    },
                    { _wizardState.postValue(WizardState.ERROR(it.message)) },
                )
            },
            { _wizardState.postValue(WizardState.ERROR(it.message)) },
        )
    }

    fun advanceFromStep3() {
        // Remove the crypto v1 data
        settings.remove(Settings.SET_FMD_CRYPT_PUBKEY)
        settings.remove(Settings.SET_FMD_CRYPT_PRIVKEY)
        settings.remove(Settings.SET_FMD_CRYPT_HPW)

        _wizardState.postValue(WizardState.FINISH)
    }
}
