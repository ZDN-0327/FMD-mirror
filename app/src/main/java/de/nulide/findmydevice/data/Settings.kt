package de.nulide.findmydevice.data

import de.nulide.findmydevice.BuildConfig
import de.nulide.findmydevice.commands.FmdPermission
import de.nulide.findmydevice.utils.RingerUtils
import kotlinx.serialization.Serializable

@Serializable
data class Settings(

    val version: Int = 0,

    // General
    val commandKeyword: String = BuildConfig.DEFAULT_FMD_COMMAND,
    val ringerTone: String = RingerUtils.getDefaultRingtoneAsString(),
    val lockScreenMessage: String = "",
    val deleteEnabled: Boolean = false,

    // FMD Server
    val serverUsername: String = "",
    val serverUrl: String = "",
    val serverPushUrl: String = "",
    val serverHashedAuthPw: String = "",
    val serverEncPrivKeyV1: String = "",
    val serverPubKeyV1: String = "",
    val serverPermissions: Long = FmdPermission.ALL,
    val serverConnCheckIntervalHours: Int = 0,
    val serverConnCheckNotifyAfterHours: Int = 36,
    val serverBackgroundUploadIntervalMins: Int = 60,
    val serverBackgroundLocationType: Int = BackgroundLocationType.EMPTY,
    val serverLowBatUpload: Boolean = true,

    // FMD Server state
    val serverConnCheckLastSeenMillis: Long = 0,
    val serverLastCmdMillis: Long = 0,
    val serverLastLocUpload: Long = 0,

    // Last known location
    val lastLocTimeMillis: Long = 0,
    val lastLocLat: Double = Double.NaN,
    val lastLocLon: Double = Double.NaN,
    val lastLocAcc: Float = Float.NaN,
    val lastLocAlt: Double = Double.NaN,
    val lastLocBearing: Float = Float.NaN,
    val lastLocSpeed: Float = Float.NaN,

    // OpenCelliD
    val openCellIdApiToken: String = "",

    // Appearance
    val themeMode: String = THEME_FOLLOW_SYSTEM,
    val themeDynamicColors: Boolean = false,

    // Tutorials
    val tutorialFmdEdge: Boolean = false,
    val tutorialFirstContact: Boolean = false,

    // Varia
    val crashLogEntry: Boolean = false,
) {
    companion object {
        const val CURRENT_VERSION = 4

        const val THEME_FOLLOW_SYSTEM = "follow_system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
    }
}
