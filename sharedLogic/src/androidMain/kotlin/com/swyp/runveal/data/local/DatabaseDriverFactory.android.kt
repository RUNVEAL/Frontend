package com.swyp.runveal.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.swyp.runveal.auth.AndroidAppContextHolder
import com.swyp.runveal.db.RunvealDatabase

private const val DATABASE_NAME = "runveal.db"

actual class DatabaseDriverFactory actual constructor() {
    actual fun createDriver(): SqlDriver = AndroidSqliteDriver(
        schema = RunvealDatabase.Schema,
        context = AndroidAppContextHolder.context,
        name = DATABASE_NAME,
        callback = object : AndroidSqliteDriver.Callback(RunvealDatabase.Schema) {
            override fun onOpen(db: SupportSQLiteDatabase) {
                db.setForeignKeyConstraintsEnabled(true)
            }
        },
    )
}
