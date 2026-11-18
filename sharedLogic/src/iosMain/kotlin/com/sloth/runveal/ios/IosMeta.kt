package com.sloth.runveal.ios

import com.sloth.runveal.network.meta.MetaApi
import com.sloth.runveal.network.meta.MetaHolder
import com.sloth.runveal.network.meta.MetaResponse

/**
 * `GET /api/v1/meta` — 앱 실행 시 한 번(`docs/api/API.md` §3.1/§9).
 *
 * **왜 감싸는가.** [MetaApi]의 생성자는 인자에 기본값이 있는데, Kotlin/Native는 기본값을 Swift로
 * 내보내지 못해 `init()`을 사용 불가로 표시한다. Swift에서 `MetaApi()`를 쓰면
 * `'init()' is unavailable`로 깨진다. 여기서 한 번 만들어 두고 호출만 내보낸다.
 */
object IosMeta {

    private val api = MetaApi()

    /**
     * 받아서 [MetaHolder]에 둔다 — 지도 버전·제한값을 쓰는 쪽은 전부 홀더를 본다.
     * 실패하면 예외가 그대로 올라간다(Swift에서는 throws).
     *
     * **[Throws]가 없으면 Swift의 `catch`가 안 돌고 앱이 그대로 죽는다.** Kotlin/Native는
     * `@Throws`에 적힌 클래스(와 그 하위)만 `NSError`로 바꿔 넘기고, 나머지 예외는 "처리되지 않은
     * 예외"로 보고 **프로세스를 종료**한다(suspend 함수의 기본값은 `CancellationException` 하나뿐).
     * 서버가 꺼져 있거나 응답 형식이 바뀌면 스플래시에서 `try await`를 감싼 `catch`로 들어오지 않고
     * 앱이 튕긴다 — 그래서 `Exception` 전체를 넘긴다.
     */
    @Throws(Exception::class)
    suspend fun fetchAndPublish(): MetaResponse =
        api.fetchMeta().also { MetaHolder.publish(it) }
}
