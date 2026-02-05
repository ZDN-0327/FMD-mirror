package de.nulide.findmydevice.utils

import android.os.Build
import java.io.BufferedReader
import java.io.InputStreamReader

fun isInBuildProps(needle: String): Boolean {
    // https://stackoverflow.com/questions/78714450/how-to-get-all-system-properties-from-device
    val process = Runtime.getRuntime().exec("getprop")
    return BufferedReader(InputStreamReader(process.inputStream))
        .lineSequence()
        .any { it.contains(needle) }
}

fun isOxygenOs(): Boolean {
    // adb shell getprop | grep ro.product.manufacturer
    if (Build.MANUFACTURER.equals("OnePlus")) {
        // Since spawning a process is relatively expensive, only run this on OnePlus devices.
        // We still need this check, since people may be running Custom ROMs.
        return isInBuildProps("ro.oxygen.version")
    }
    return false
}
