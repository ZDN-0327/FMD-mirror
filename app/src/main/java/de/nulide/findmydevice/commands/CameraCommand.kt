package de.nulide.findmydevice.commands

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import de.nulide.findmydevice.R
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.permissions.CameraPermission
import de.nulide.findmydevice.permissions.OverlayPermission
import de.nulide.findmydevice.transports.Transport
import de.nulide.findmydevice.ui.DummyCameraxActivity
import de.nulide.findmydevice.utils.log


class CameraCommand(context: Context) : Command(context) {
    companion object {
        private val TAG = CameraCommand::class.simpleName
    }

    override val keyword = "camera"
    override val usage = "camera [front | back] [flash]"

    override val permission = FmdPermission.CAMERA

    @get:DrawableRes
    override val icon = R.drawable.ic_camera

    @get:StringRes
    override val shortDescription = R.string.cmd_camera_description_short

    override val longDescription = R.string.cmd_camera_description_long

    override val requiredPermissions = listOf(CameraPermission(), OverlayPermission())

    override suspend fun <T> executeInternal(
        args: List<String>,
        transport: Transport<T>,
    ) {
        val ncEnabled = settings.get(Settings.SET_NEXTCLOUD_ENABLED) as? Boolean ?: false
        if (!settings.serverAccountExists() && !ncEnabled) {
            context.log().w(TAG, "Cannot take picture: no FMD Server or Nextcloud account")
            transport.send(context, context.getString(R.string.cmd_camera_response_no_fmd_server))
            return
        }

        val dummyCameraActivity = Intent(context, DummyCameraxActivity::class.java)
        dummyCameraActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        if (args.contains("front")) {
            dummyCameraActivity.putExtra(
                DummyCameraxActivity.EXTRA_CAMERA,
                DummyCameraxActivity.CAMERA_FRONT
            )
        } else {
            dummyCameraActivity.putExtra(
                DummyCameraxActivity.EXTRA_CAMERA,
                DummyCameraxActivity.CAMERA_BACK
            )
        }
        if (args.contains("flash")) {
            dummyCameraActivity.putExtra(DummyCameraxActivity.EXTRA_FLASH, true)
        }
        context.log().d(TAG, "Starting camera activity")
        context.startActivity(dummyCameraActivity)

        val serverUrl = when {
            settings.serverAccountExists() -> settings.get(Settings.SET_FMDSERVER_URL) as String
            ncEnabled -> settings.get(Settings.SET_NEXTCLOUD_URL) as? String ?: "Nextcloud"
            else -> ""
        }
        transport.send(context, context.getString(R.string.cmd_camera_response_success, serverUrl))
    }
}
