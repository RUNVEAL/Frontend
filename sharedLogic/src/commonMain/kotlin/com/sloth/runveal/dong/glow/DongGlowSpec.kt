package com.sloth.runveal.dong.glow

/**
 * `tools/dong-glow/gen_dong_glow.py`와 같은 값을 쓰는 상수. [REF_INRADIUS_PX], [REF_EFFECT_SCALE]은
 * 피그마 시안과 픽셀 단위로 맞춘 보정값이라 바꾸지 않는다. [QUALITY]는 화면 확인 후 사용자와 정한다.
 * [VERSION]은 알고리즘·상수가 바뀌면 올려서 디스크 캐시를 무효화하는 데 쓴다.
 */
object DongGlowSpec {
    const val REF_INRADIUS_PX = 38.8f
    const val REF_EFFECT_SCALE = 0.7f
    const val QUALITY = 1.0f
    const val MAX_SIDE = 1024
    /** 2 = 빛 이미지 입력 폴리곤을 테두리 선과 같은 좌표로 맞춘 버전.
     *  3 = 그 좌표에서 인접 동 정점 스냅(15m) 보정을 걷어낸 버전 — 서버 지도가 공유 경계를 같은
     *      좌표로 내려주면서 보정이 필요 없어졌고, 보정이 멀쩡한 정점까지 합칠 수 있었다. */
    const val VERSION = 3
}
