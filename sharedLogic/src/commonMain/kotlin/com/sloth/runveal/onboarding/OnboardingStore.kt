package com.sloth.runveal.onboarding

import com.sloth.runveal.db.RunvealDatabase

/**
 * 온보딩을 이미 봤는지 기억한다(`app_setting` 키-값, [ImportCheckpointStore][com.sloth.runveal.external.ImportCheckpointStore]와 같은 표).
 * 끝까지 봤든 건너뛰었든 "봤다"로 적고, 그 뒤로는 스플래시가 바로 홈으로 보낸다.
 *
 * 값이 지워져도(앱 데이터 삭제) 온보딩을 한 번 더 보는 것뿐이라 복구 수단은 두지 않는다.
 */
class OnboardingStore(private val database: RunvealDatabase) {

    fun isCompleted(): Boolean =
        database.appSettingQueries.selectSetting(KEY_COMPLETED).executeAsOneOrNull() == VALUE_COMPLETED

    fun markCompleted() {
        database.appSettingQueries.upsertSetting(key = KEY_COMPLETED, value_ = VALUE_COMPLETED)
    }

    private companion object {
        const val KEY_COMPLETED = "onboarding.completed"
        const val VALUE_COMPLETED = "true"
    }
}
