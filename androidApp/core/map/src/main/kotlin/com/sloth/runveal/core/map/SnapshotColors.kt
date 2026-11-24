package com.sloth.runveal.core.map

/**
 * 지도 스냅샷에 쓰는 색 묶음. `RunvealTheme.colors`는 Composable 안에서만 읽을 수 있어서, SDK 레이어
 * (`MapSnapshotter` 스타일)에 넘기기 전에 ARGB `Int`로 미리 바꿔 둔다.
 *
 * 색이 하나라도 다르면 **같은 캐시 파일을 다른 그림으로 덮어쓰게 된다** — 미리 렌더링하는 쪽
 * (`CourseDetailViewModel`)과 화면에서 그리는 쪽([CourseSnapshotImage])이 같은 값을 써야 한다.
 */
data class SnapshotColors(
    /** 경로 선(코스). 러닝 기록은 [todayRoad] 위에 [newRoad]를 덧그린다. */
    val route: Int,
    /** 코스 썸네일의 출발지 점 — 디자인시스템 출발 마커와 같은 금색 점 + 어두운 테두리. */
    val start: Int,
    val startStroke: Int,
    /** 코스 썸네일의 도착지 점 — 디자인시스템 도착 마커와 같은 흰 점 + 어두운 테두리. */
    val end: Int,
    val endStroke: Int,
    /** 러닝 결과 카드의 출발·도착 점(두 점이 같은 색) — 코스 썸네일과 색 체계가 다르다. */
    val resultEndpoint: Int,
    val resultEndpointStroke: Int,
    /** 러닝 기록에서 처음 간 구간(금색)과 이미 가본 구간(흰색). */
    val newRoad: Int,
    val todayRoad: Int,
    /** 동 썸네일에서 동 **밖**을 덮는 색 — 프레임 배경과 같아야 잘려 보인다(`bg.map`). */
    val dongMask: Int,
    /** 동 썸네일의 동 경계선 색. */
    val dongOutline: Int,
) {
    companion object {
        /**
         * Compose 밖(ViewModel)에서 쓰는 한 벌. `RunvealTheme.colors`를 읽을 수 없어 같은 디자인 토큰을
         * ARGB로 옮겨 적었다 — gold200 / neutral900 / neutral50 / blue500. [CourseSnapshotImage]가
         * 테마에서 뽑는 값과 **같아야** 같은 캐시 파일을 함께 쓴다(토큰이 바뀌면 둘 다 고칠 것).
         */
        val DesignTokens = SnapshotColors(
            route = 0xFFF2E096.toInt(),
            start = 0xFFF2E096.toInt(),
            startStroke = 0xFF0E0E0E.toInt(),
            end = 0xFFF4EEEA.toInt(),
            endStroke = 0xFF0E0E0E.toInt(),
            resultEndpoint = 0xFF4286E0.toInt(),
            resultEndpointStroke = 0xFFF4EEEA.toInt(),
            // 코스 썸네일은 러닝이 아니라서(isRunRoute=false) 아래 두 색은 실제로 쓰이지 않는다.
            newRoad = 0xFFF2E096.toInt(),
            todayRoad = 0xFFF2E096.toInt(),
            // bg/map(neutral900)과 border/default(neutral700) — 프레임 배경과 같은 색이어야
            // 동 모양으로 잘린 것처럼 보인다.
            dongMask = 0xFF0E0E0E.toInt(),
            dongOutline = 0xFF443F3D.toInt(),
        )
    }
}
