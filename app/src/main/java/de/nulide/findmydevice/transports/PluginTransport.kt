package de.nulide.findmydevice.transports

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import de.nulide.findmydevice.R
import de.nulide.findmydevice.commands.AccessResponse
import de.nulide.findmydevice.commands.ParserResult
import de.nulide.findmydevice.data.SettingsRepository
import de.nulide.findmydevice.permissions.Permission
import de.nulide.findmydevice.services.FmdPluginService
import de.nulide.findmydevice.ui.access.AccessControlActivity

class PluginTransport(
    val context: Context,
    private val caller: String?,
    ) : Transport<Unit>(Unit) {

    constructor(context: Context) : this(context, "FMD Plugin")

    companion object {
        private val TAG = PluginTransport::class.simpleName
    }

    private val settings = SettingsRepository.getInstance(context)

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
        activity.startActivity(Intent(context, AccessControlActivity::class.java))
    })

    override fun getDestinationString(): String {
        return caller ?: ""
    }

    override suspend fun isAllowed(parsed: ParserResult.Success): AccessResponse {
        return AccessResponse.ALLOWED
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