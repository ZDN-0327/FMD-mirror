package de.nulide.findmydevice.transports

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import de.nulide.findmydevice.R
import de.nulide.findmydevice.commands.AccessResponse
import de.nulide.findmydevice.commands.ParserResult
import de.nulide.findmydevice.commands.hasPermission
import de.nulide.findmydevice.data.AccessRepository
import de.nulide.findmydevice.permissions.Permission
import de.nulide.findmydevice.services.FmdPluginService
import de.nulide.findmydevice.ui.access.AccessControlActivity
import de.nulide.findmydevice.utils.log

class PluginTransport(
    val context: Context,
    private val caller: String?,
) : Transport<Unit>(Unit) {

    constructor(context: Context) : this(context, "FMD Plugin")

    companion object {
        private val TAG = PluginTransport::class.simpleName
    }

    private val accessRepo = AccessRepository.getInstance(context)

    @get:DrawableRes
    override val icon = R.drawable.ic_plugin

    @get:StringRes
    override val title = R.string.transport_plugin_title

    override val description = context.getString(R.string.transport_plugin_description)

    override val descriptionAuth = context.getString(R.string.transport_plugin_description_auth)

    override val requiredPermissions = emptyList<Permission>()

    override val actions = listOf(TransportAction(R.string.Settings_Access_Control) { activity ->
        val intent = Intent(context, AccessControlActivity::class.java)
        intent.putExtra(AccessControlActivity.EXTRA_PAGE, AccessControlActivity.PAGE_PLUGIN)
        activity.startActivity(intent)
    })

    override fun getDestinationString(): String {
        return caller ?: ""
    }

    override suspend fun isAllowed(parsed: ParserResult.Success): AccessResponse {
        if (caller.isNullOrEmpty()) {
            return AccessResponse.DENIED_UNKNOWN
        }
        var isKnownButDenied = false
        val storedPlugin = accessRepo.getPlugin(caller)
        if (storedPlugin != null) {
            val hasPermission = storedPlugin.permission.hasPermission(parsed.command.permission)
            if (hasPermission) {
                context.log().i(TAG, "$caller used FMD")
                return AccessResponse.ALLOWED
            } else {
                context.log().i(
                    TAG,
                    "$caller denied access to ${parsed.command.keyword}."
                )
                isKnownButDenied = true
            }
        } else {
            context.log().i(
                TAG,
                "$caller has no access"
            )
        }
        return if (isKnownButDenied) AccessResponse.DENIED_EXISTS else AccessResponse.DENIED_UNKNOWN
    }

    @SuppressLint("MissingSuperCall")
    override fun send(context: Context, msg: String) {
        val intent = Intent(context, FmdPluginService::class.java).apply {
            action = FmdPluginService.ACTION_REPLY
            putExtra(FmdPluginService.EXTRA_CALLER, caller)
            putExtra(FmdPluginService.EXTRA_MSG, msg)
        }
        context.startService(intent)
    }

}