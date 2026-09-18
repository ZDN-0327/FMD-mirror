package de.nulide.findmydevice.database

import android.content.Context

interface AccessItem {
    fun getItemPermission(): Long

    fun toDisplayLabel(context: Context): String
}
