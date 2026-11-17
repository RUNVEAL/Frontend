package com.sloth.runveal.recording

/**
 * "이 러닝을 기록으로 남길지" 판단 기준. 버튼을 잘못 눌렀거나 신호만 받아보려고 몇 초 켰다 끈
 * 기록까지 목록/월간 요약에 쌓이면 통계가 망가지므로, 너무 짧은 러닝은 저장하지 않고 버린다.
 *
 * 경과 시간은 **일시정지를 뺀** 시간([TrackRecorder.elapsedMs])을 넘겨야 한다 — 러닝 화면에 찍히는
 * 숫자와 같은 기준이라야 "1분도 안 뛰었는데 저장됐다"는 어긋남이 생기지 않는다.
 */
object RecordingSavePolicy {
    const val MIN_DISTANCE_METERS = 100.0
    const val MIN_ELAPSED_MS = 60_000L

    fun isSavable(distanceMeters: Double, elapsedMs: Long): Boolean =
        distanceMeters >= MIN_DISTANCE_METERS && elapsedMs >= MIN_ELAPSED_MS
}
