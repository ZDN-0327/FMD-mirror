package de.nulide.findmydevice.database

import android.content.Context
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import de.nulide.findmydevice.commands.FmdPermission
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = "plugin",
    indices = [
        Index(value = ["app_id"], unique = true),
    ],
)
data class Plugin(
    @PrimaryKey(autoGenerate = true) val rowId: Int = 0,

    @ColumnInfo("app_name") val label: String,

    @ColumnInfo("app_id") val appId: String,

    @ColumnInfo("permissions") val permission: Long = FmdPermission.DEFAULT,
) : AccessItem, Parcelable {

    override fun getItemPermission(): Long {
        return permission
    }

    override fun toDisplayLabel(context: Context): String {
        return label
    }
}
