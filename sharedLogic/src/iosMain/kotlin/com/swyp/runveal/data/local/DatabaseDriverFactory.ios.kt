package com.swyp.runveal.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.swyp.runveal.db.RunvealDatabase

private const val DATABASE_NAME = "runveal.db"

actual class DatabaseDriverFactory actual constructor() {
    actual fun createDriver(): SqlDriver {
        val driver = NativeSqliteDriver(RunvealDatabase.Schema, DATABASE_NAME)
        driver.execute(null, "PRAGMA foreign_keys = ON;", 0)
        return driver
    }
}
