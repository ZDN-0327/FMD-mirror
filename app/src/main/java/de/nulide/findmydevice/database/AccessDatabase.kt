package de.nulide.findmydevice.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RenameColumn
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec

const val ACCESS_DB_FILENAME = "access.db"

@Database(
    entities = [
        PhoneNumber::class,
        SmsPassword::class,
        TempPhoneNumber::class,
        NotificationPassword::class,
        Plugin::class,
    ],
    version = 4,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3, spec = AccessDatabase.Migration2to3::class),
        AutoMigration(from = 3, to = 4, spec = AccessDatabase.Migration3to4::class),
    ]
)
abstract class AccessDatabase : RoomDatabase() {
    abstract fun phoneNumberDao(): PhoneNumberDao

    abstract fun smsPasswordDao(): SmsPasswordDao

    abstract fun tempPhoneNumberDao(): TempPhoneNumberDao

    abstract fun notificationPasswordDao(): NotificationPasswordDao

    abstract fun pluginDao(): PluginDao

    // ------- Migrations -------
    // https://developer.android.com/training/data-storage/room/migrating-db-versions

    @RenameColumn.Entries(
        RenameColumn(
            tableName = "notification_password",
            fromColumnName = "password",
            toColumnName = "password_hash"
        ),
        RenameColumn(
            tableName = "sms_password", fromColumnName = "password", toColumnName = "password_hash"
        )
    )
    class Migration2to3 : AutoMigrationSpec

    class Migration3to4 : AutoMigrationSpec

}
