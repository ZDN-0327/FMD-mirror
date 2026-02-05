package de.nulide.findmydevice.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build

object RingerUtils {
    @JvmStatic
    fun getRingtone(context: Context, ringtone: String): Ringtone {
        val r = RingtoneManager.getRingtone(context, Uri.parse(ringtone))
        val aa = AudioAttributes.Builder()
            .setUsage(usageType())
            .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
            .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
            .build()
        r.setAudioAttributes(aa)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            r.setLooping(true)
        }
        return r
    }

    @JvmStatic
    val defaultRingtoneAsString: String
        get() = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString()

    @JvmStatic
    fun streamType(): Int {
        // https://gitlab.com/fmd-foss/fmd-android/-/work_items/384
        // https://gitlab.com/fmd-foss/fmd-android/-/issues/357
        return if (isOxygenOs()) {
            AudioManager.STREAM_MUSIC
        } else {
            AudioManager.STREAM_ALARM
        }
    }

    @JvmStatic
    fun usageType(): Int {
        return if (isOxygenOs()) {
            AudioAttributes.USAGE_MEDIA
        } else {
            AudioAttributes.USAGE_ALARM
        }
    }
}
