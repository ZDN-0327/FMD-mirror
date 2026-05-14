package de.nulide.findmydevice.data

import android.content.Context
import androidx.core.net.toUri
import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy
import de.nulide.findmydevice.BuildConfig
import de.nulide.findmydevice.R
import de.nulide.findmydevice.database.NotificationPassword
import de.nulide.findmydevice.database.SmsPassword
import de.nulide.findmydevice.utils.CypherUtils
import de.nulide.findmydevice.utils.SingletonHolder
import de.nulide.findmydevice.utils.Utils
import de.nulide.findmydevice.utils.log
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File
import java.io.FileReader
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.spec.EncodedKeySpec
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec


const val SETTINGS_FILENAME_V1 = "settings.json"
const val SETTINGS_FILENAME_V2 = "settings_v2.json"

/**
 * Settings should be accessed through this repository.
 * Only having a single Settings instance prevents race conditions on the file.
 */
class SettingsRepository private constructor(private val context: Context) {

    companion object :
        SingletonHolder<SettingsRepository, Context>(::SettingsRepository) {

        val TAG = SettingsRepository::class.simpleName
    }

    private val gson = GsonBuilder()
        // Force Gson to parse numbers as either Long or Double.
        // When needed, we can cast Longs down to Ints.
        .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
        .serializeSpecialFloatingPointValues() // to allow NaN
        .create()

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = true
        allowSpecialFloatingPointValues = true
    }

    // Should only be accessed via the getters/setters in this repository
    var settings: Settings
        private set

    init {
        settings = loadNoSet()
    }

    fun load() {
        settings = loadNoSet()
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun loadNoSet(): Settings {
        val file = File(context.filesDir, SETTINGS_FILENAME_V2)
        if (!file.exists()) {
            file.createNewFile()
        }

        // Better crash with a SerializationException than silently resetting the settings (they are important!).
        // If a user is affected by a crash due to an invalid settings JSON, they can manually fix this
        // by clearing the entire app storage.
        return file.inputStream().use {
            json.decodeFromStream<Settings>(it)
        }
    }

    // https://kotlinlang.org/docs/lambdas.html#function-literals-with-receiver
    fun update(transform: Settings.() -> Settings) {
        settings = settings.transform()
        saveSettings()
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun saveSettings() {
        val file = File(context.filesDir, SETTINGS_FILENAME_V2)
        file.outputStream().use {
            json.encodeToStream(settings, it)
        }
    }

    private fun migrateV1ToV2() {
        val file = File(context.filesDir, SETTINGS_FILENAME_V1)
        if (!file.exists()) {
            return
        }
        context.log().i(TAG, "Migrating $SETTINGS_FILENAME_V1 to $SETTINGS_FILENAME_V2")

        val old: SettingsOld = FileReader(file).use { reader ->
            gson.fromJson(reader, SettingsOld::class.java)
        }

        val migrated = Settings(
            version = (old.get(SettingsOld.SET_SET_VERSION) as Number).toInt(),

            commandKeyword = old.get(SettingsOld.SET_FMD_COMMAND) as String,
            ringerTone = old.get(SettingsOld.SET_RINGER_TONE) as String,
            lockScreenMessage = old.get(SettingsOld.SET_SET_VERSION) as String,
            deleteEnabled = old.get(SettingsOld.SET_WIPE_ENABLED) as Boolean,

            serverUsername = old.get(SettingsOld.SET_FMDSERVER_ID) as String,
            serverUrl = old.get(SettingsOld.SET_FMDSERVER_URL) as String,
            serverPushUrl = old.get(SettingsOld.SET_FMDSERVER_PUSH_URL) as String,
            serverHashedAuthPw = old.get(SettingsOld.SET_FMD_CRYPT_HPW) as String,
            serverEncPrivKeyV1 = old.get(SettingsOld.SET_FMD_CRYPT_PRIVKEY) as String,
            serverPubKeyV1 = old.get(SettingsOld.SET_FMD_CRYPT_PUBKEY) as String,
            serverPermissions = (old.get(SettingsOld.SET_FMDSERVER_PERMISSIONS) as Number).toLong(),
            serverConnCheckIntervalHours = (old.get(SettingsOld.SET_FMD_SERVER_CONNECTIVITY_CHECK_INTERVAL_HOURS) as Number).toInt(),
            serverConnCheckNotifyAfterHours = (old.get(SettingsOld.SET_FMD_SERVER_CONNECTIVITY_CHECK_NOTIFY_AFTER_HOURS) as Number).toInt(),
            serverBackgroundUploadIntervalMins = (old.get(SettingsOld.SET_FMDSERVER_UPDATE_TIME) as Number).toInt(),
            serverBackgroundLocationType = (old.get(SettingsOld.SET_FMDSERVER_LOCATION_TYPE) as Number).toInt(),
            serverLowBatUpload = old.get(SettingsOld.SET_FMD_LOW_BAT_SEND) as Boolean,

            serverConnCheckLastSeenMillis = (old.get(SettingsOld.SET_FMD_SERVER_LAST_CONNECTIVITY_UNIX_TIME) as Number).toLong(),
            serverLastCmdMillis = (old.get(SettingsOld.SET_FMDSERVER_LAST_CMD_MILLIS) as Number).toLong(),
            serverLastLocUpload = (old.get(SettingsOld.SET_FMDSERVER_LAST_LOCATION_UPLOAD_TIME) as Number).toLong(),

            lastLocTimeMillis = (old.get(SettingsOld.SET_LAST_KNOWN_LOCATION_TIME) as Number).toLong(),
            lastLocLat = (old.get(SettingsOld.SET_LAST_KNOWN_LOCATION_LAT) as String).toDouble(),
            lastLocLon = (old.get(SettingsOld.SET_LAST_KNOWN_LOCATION_LON) as String).toDouble(),
            lastLocAcc = (old.get(SettingsOld.SET_LAST_KNOWN_LOCATION_ACCURACY) as Number).toFloat(),
            lastLocAlt = (old.get(SettingsOld.SET_LAST_KNOWN_LOCATION_ALTITUDE) as Number).toDouble(),
            lastLocBearing = (old.get(SettingsOld.SET_LAST_KNOWN_LOCATION_BEARING) as Number).toFloat(),
            lastLocSpeed = (old.get(SettingsOld.SET_LAST_KNOWN_LOCATION_SPEED) as Number).toFloat(),

            openCellIdApiToken = old.get(SettingsOld.SET_OPENCELLID_API_KEY) as String,

            themeMode = old.get(SettingsOld.SET_THEME) as String,
            themeDynamicColors = old.get(SettingsOld.SET_DYNAMIC_COLORS) as Boolean,

            tutorialFmdEdge = old.get(SettingsOld.SET_FMD_EDGE_INFO_SHOWN) as Boolean,
            tutorialFirstContact = old.get(SettingsOld.SET_FIRST_TIME_CONTACT_ADDED) as Boolean,

            crashLogEntry = (old.get(SettingsOld.SET_APP_CRASHED_LOG_ENTRY) as Number) == 1,
        )

        // If the v1 file exists, it takes priority, and overwrite everything that might already be there.
        settings = migrated
        saveSettings()
        file.delete()
    }

    suspend fun migrateSettings() {
        // First migrate the storage format
        migrateV1ToV2()

        // Then migrate the content
        val currentVersion = settings.version

        migrateServerUrl()
        if (currentVersion < 3) {
            migrateDeletePassword()
        }
        migrateBackgroundLocationType()
        if (currentVersion < 4) {
            migrateFmdPinToDb()
        }

        update { copy(version = Settings.CURRENT_VERSION) }
    }

    private fun migrateServerUrl() {
        val oldUrl = settings.serverUrl
        val uri = oldUrl.toUri()
        if (uri.host == "fmd.nulide.de") {
            val msg = "Updating server URL: old=$oldUrl new=${BuildConfig.DEFAULT_FMD_SERVER_URL}"
            context.log().i(TAG, msg)
            update { copy(serverUrl = BuildConfig.DEFAULT_FMD_SERVER_URL) }
        }
    }

    private fun migrateDeletePassword() {
        // For users that upgrade, initialize the new delete password with the existing FMD PIN
        context.log().i(TAG, "Migrating to separate delete password")
        val encSettings = EncryptedSettingsRepository.getInstance(context)
        val pin = encSettings.getFmdPin()
        encSettings.setDeletePassword(pin)
    }

    private fun migrateBackgroundLocationType() {
        val oldType = settings.serverBackgroundLocationType
        if (oldType < BackgroundLocationType.BASE) {
            val newType = BackgroundLocationType.fromOldEncoding(oldType)
            update { copy(serverBackgroundLocationType = newType.encode()) }
        }
    }

    suspend fun migrateFmdPinToDb() {
        val encSettings = EncryptedSettingsRepository.getInstance(context)
        val pin = encSettings.getFmdPin()
        if (pin.isNotBlank()) {
            context.log().i(TAG, "Migrating FMD PIN to database")
            val accessRepo = AccessRepository.getInstance(context)
            accessRepo.insertSmsPassword(SmsPassword(0, label = "FMD PIN", password = pin))
            accessRepo.insertNotificationPassword(
                NotificationPassword(0, label = "FMD PIN", password = pin)
            )
        }
    }

// ---------- Convenience helpers ----------

    fun serverAccountExists(): Boolean {
        // The SET_FMDSERVER_ID is remembered during logout.
        // Therefore, check both (to be sure).
        return settings.serverUsername.isNotEmpty() && settings.serverHashedAuthPw.isNotEmpty()
    }

    fun setKeys(keys: FmdKeyPair) {
        update { copy(serverEncPrivKeyV1 = keys.encryptedPrivateKey) }
        update { copy(serverPubKeyV1 = CypherUtils.encodeBase64(keys.publicKey.encoded)) }
    }

    fun getKeys(): FmdKeyPair? {
        if (settings.serverPubKeyV1 == "") {
            return null
        }

        val pubKeySpec: EncodedKeySpec = X509EncodedKeySpec(
            CypherUtils.decodeBase64(settings.serverPubKeyV1)
        )
        var publicKey: PublicKey? = null
        try {
            val keyFactory = KeyFactory.getInstance("RSA")
            publicKey = keyFactory.generatePublic(pubKeySpec)
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: InvalidKeySpecException) {
            e.printStackTrace()
        }

        return if (publicKey != null) {
            FmdKeyPair(publicKey, settings.serverEncPrivKeyV1)
        } else {
            null
        }
    }

    fun removeServerAccount(full: Boolean = true) {
        // If not full removal, remember the user ID for autofill convenience.
        if (full) {
            update { copy(serverUsername = "") }
        }

        update { copy(serverHashedAuthPw = "") }
        update { copy(serverEncPrivKeyV1 = "") }
        update { copy(serverPubKeyV1 = "") }
    }

    fun storeLastKnownLocation(loc: FmdLocation) {
        update {
            copy(
                lastLocLat = loc.lat,
                lastLocLon = loc.lon,
                lastLocAcc = loc.accuracy ?: Float.NaN,
                lastLocAlt = loc.altitude ?: Double.NaN,
                lastLocBearing = loc.bearing ?: Float.NaN,
                lastLocSpeed = loc.speed ?: Float.NaN,
                lastLocTimeMillis = loc.timeMillis,
            )
        }
    }

    /**
     * Return the last known location as cached by the settings.
     */
    fun getLastKnownLocation(): FmdLocation? {
        return try {
            val loc = getLastKnownLocationInt()
            loc
        } catch (e: NumberFormatException) {
            null
        } catch (e: ClassCastException) {
            // https://gitlab.com/fmd-foss/fmd-android/-/issues/379
            // java.lang.ClassCastException: java.lang.String cannot be cast to java.lang.Number
            null
        }
    }

    private fun getLastKnownLocationInt(): FmdLocation {
        return FmdLocation(
            lat = settings.lastLocLat,
            lon = settings.lastLocLon,
            accuracy = settings.lastLocAcc.nullIfNaN(),
            altitude = settings.lastLocAlt.nullIfNaN(),
            bearing = settings.lastLocBearing.nullIfNaN(),
            speed = settings.lastLocSpeed.nullIfNaN(),
            provider = context.getString(R.string.cmd_locate_last_known_location_text),
            batteryLevel = Utils.getBatteryLevel(context),
            timeMillis = settings.lastLocTimeMillis,
        )
    }

    // ------- Help functions for easier usage from Java -------

    @Deprecated("Migrate to Kotlin")
    fun setCrashLogShown() {
        update { copy(crashLogEntry = true) }
    }

    @Deprecated("Migrate to Kotlin")
    fun setEdgeInfoShown() {
        update { copy(tutorialFmdEdge = true) }
    }
}

fun Float.nullIfNaN(): Float? {
    return if (isNaN()) null else this
}

fun Double.nullIfNaN(): Double? {
    return if (isNaN()) null else this
}
