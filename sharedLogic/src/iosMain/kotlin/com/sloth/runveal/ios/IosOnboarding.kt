package com.sloth.runveal.ios

import com.sloth.runveal.data.local.RunvealDatabaseProvider
import com.sloth.runveal.onboarding.OnboardingStore
import kotlinx.coroutines.Dispatchers
// Kotlin/Native에서 `Dispatchers.IO`는 객체의 멤버가 아니라 **확장 프로퍼티**다(멤버 쪽은 internal).
// 이 import가 없으면 "Cannot access 'val IO': it is internal"로 깨진다 — 지우지 않는다.
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * 온보딩을 봤는지 여부를 Swift에서 쓰기 위한 창구 — Android `OnboardingViewModel`과
 * `splashGraph.resolveStartDestination()`이 [OnboardingStore]를 부르는 자리에 대응한다.
 *
 * **왜 감싸는가.** [OnboardingStore]는 생성자에 데이터베이스를 받고 메서드가 suspend가 아니다 —
 * Swift에서 바로 부르면 DB 조회가 **부른 쪽 스레드에서 그대로** 돌아, 호출부가 메인 액터면 그만큼
 * 화면이 멈춘다. Android가 `withContext(Dispatchers.IO)`로 감싸는 것과 같은 이유로 여기서 옮긴다.
 *
 * [Throws]가 없으면 DB에서 난 예외가 Swift `catch`로 가지 않고 앱을 종료시킨다
 * ([IosMeta.fetchAndPublish] 주석 참고) — 온보딩을 못 읽어도 앱은 떠야 한다.
 */
object IosOnboarding {

    private val store by lazy { OnboardingStore(RunvealDatabaseProvider.database) }

    /** 이미 봤으면 true — 스플래시가 온보딩을 건너뛰고 홈으로 보낸다. */
    @Throws(Exception::class)
    suspend fun isCompleted(): Boolean = withContext(Dispatchers.IO) { store.isCompleted() }

    /** 끝까지 봤든 건너뛰었든 "봤다"로 적는다. */
    @Throws(Exception::class)
    suspend fun markCompleted() {
        withContext(Dispatchers.IO) { store.markCompleted() }
    }
}
