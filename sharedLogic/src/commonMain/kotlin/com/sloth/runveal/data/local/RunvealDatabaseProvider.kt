package com.sloth.runveal.data.local

import com.sloth.runveal.db.RunvealDatabase

/**
 * 앱 전체가 공유하는 [RunvealDatabase] 하나. 화면·서비스마다 `RunvealDatabase(DatabaseDriverFactory()
 * .createDriver())`를 새로 만들면 **같은 파일에 드라이버(= 커넥션)가 여러 개** 생긴다.
 *
 * 그러면 **앱을 처음 설치했을 때 앱이 뜨자마자 죽는다**: 스키마가 아직 없는 상태에서 드라이버 A가
 * 테이블을 만드는 동안 드라이버 B가 같은 파일을 열어 질의하면
 * `SQLiteException: no such table: explored_segment`가 난다. 기존 DB가 있는 기기에서는 생성 단계
 * 자체가 없어서 드러나지 않아, 재설치 없이 테스트하는 동안은 오래 숨어 있었다.
 *
 * SQLDelight 권장대로 드라이버는 프로세스당 하나만 만들고 이 값을 공유한다. `by lazy`라 처음 쓰는
 * 쪽에서 한 번만 초기화되고, 그 사이 다른 스레드는 생성이 끝날 때까지 기다린다.
 */
object RunvealDatabaseProvider {
    val database: RunvealDatabase by lazy { RunvealDatabase(DatabaseDriverFactory().createDriver()) }
}
