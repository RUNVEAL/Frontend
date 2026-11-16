package com.swyp.runveal.external

import com.swyp.runveal.db.RunvealDatabase

/**
 * 외부 기록 가져오기 재개 지점(`docs/api/API.md` §3.7 "진행·재개 지점은 앱이 관리", §4 `settings`).
 * 마지막으로 성공한 세션의 종료 시각을 적어 두고, 다음 가져오기는 그 뒤부터만 훑는다.
 *
 * 중복 방지 자체는 `track.external_id` UNIQUE 제약이 보장한다 — 이 체크포인트는 "이미 다 가져온
 * 구간을 매번 다시 조회하지 않기" 위한 것이라, 값이 틀어지거나 사라져도 데이터가 중복되진 않는다.
 */
class ImportCheckpointStore(private val database: RunvealDatabase) {

    fun importedUntilEpochMs(key: String): Long? =
        database.appSettingQueries.selectSetting(key).executeAsOneOrNull()?.toLongOrNull()

    fun setImportedUntilEpochMs(key: String, epochMs: Long) {
        database.appSettingQueries.upsertSetting(key = key, value_ = epochMs.toString())
    }

    fun clear(key: String) {
        database.appSettingQueries.deleteSetting(key)
    }

    companion object {
        /** Health Connect 가져오기 체크포인트 키. */
        const val KEY_HEALTH_CONNECT = "import.health_connect.imported_until_epoch_ms"
    }
}
