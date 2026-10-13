package de.nulide.findmydevice.transports

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import de.nulide.findmydevice.R
import de.nulide.findmydevice.commands.AccessResponse
import de.nulide.findmydevice.commands.ParserResult
import de.nulide.findmydevice.commands.hasPermission
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.data.SettingsRepository
import de.nulide.findmydevice.net.PebbleKit2Sender
import de.nulide.findmydevice.permissions.Permission
import de.nulide.findmydevice.utils.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Transport for commands received via the PebbleKit 2 companion service
 * (PebbleKit2Service). The destination is the watchapp UUID that sent the
 * command; command results are pushed back to that watchapp.
 *
 * Permissions: the bitmask in Settings.SET_PK2_PERMISSIONS (defaults to
 * FmdPermission.DEFAULT, same as new passwords).
 */
class PebbleKit2Transport(
    context: Context,
    private val watchappUuid: String,
) : Transport<String>(watchappUuid) {

    private val settings = SettingsRepository.getInstance(context)

    @get:DrawableRes
    override val icon = R.drawable.ic_phone

    @get:StringRes
    override val title = R.string.transport_pk2_title

    override val description = context.getString(R.string.transport_pk2_description)

    override val requiredPermissions: List<Permission> = emptyList()

    override fun getDestinationString(): String = watchappUuid

    override suspend fun isAllowed(parsed: ParserResult.Success): AccessResponse {
        val grantedPerms = (settings.get(Settings.SET_PK2_PERMISSIONS) as Number).toLong()
        val allowed = grantedPerms.hasPermission(parsed.command.permission)
        return if (allowed) AccessResponse.ALLOWED else AccessResponse.DENIED_EXISTS
    }

    override fun send(context: Context, msg: String) {
        super.send(context, msg)

        // Results (e.g. location data) are pushed to the watch asynchronously;
        // never block command execution on the watch connection.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                PebbleKit2Sender(context).send(watchappUuid, msg)
            } catch (e: Exception) {
                context.log().w(TAG, "Could not deliver result to watchapp $watchappUuid: ${e.message}")
            }
        }
    }

    companion object {
        private val TAG = PebbleKit2Transport::class.simpleName
    }
}
