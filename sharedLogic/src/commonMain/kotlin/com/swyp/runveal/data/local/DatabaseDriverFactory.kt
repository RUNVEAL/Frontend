package com.swyp.runveal.data.local

import app.cash.sqldelight.db.SqlDriver

/**
 * 플랫폼별 SQLite 드라이버 생성. SQLite는 기본으로 외래키를 강제하지 않아서, actual 구현이 커넥션을
 * 열 때마다 `PRAGMA foreign_keys = ON`을 실행해 둔다 — 안 하면 `track_edge`/`course`/`track_note`의
 * `ON DELETE CASCADE`/`SET NULL`이 전부 무시된다 (docs/local-db/local-db-schema-design.md §3.5).
 */
expect class DatabaseDriverFactory() {
    fun createDriver(): SqlDriver
}
