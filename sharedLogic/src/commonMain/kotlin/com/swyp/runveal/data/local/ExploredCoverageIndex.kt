package com.swyp.runveal.data.local

import com.swyp.runveal.db.RunvealDatabase
import com.swyp.runveal.dong.CoverageMask

/**
 * "이 구간의 **이 자리**를 이미 가봤나"를 묻는 `explored_segment` 캐시 조회기.
 *
 * 구간을 쪼개 적립하면서([CoverageMask]) 구간 ID만으로는 답이 안 나온다 — 300m 도로의 앞 50m만
 * 달렸다면 나머지 250m는 아직 처음 가는 길이라 러닝 중에도 금색이어야 한다.
 *
 * 매 GPS 점마다 DB를 다시 읽을 수는 없어서 한 번 읽어 들고 있는다. 적립은 러닝이 끝날 때 일어나므로
 * **러닝을 시작할 때마다** [refresh]를 불러야 한다(앱을 켠 채 두 번째 러닝을 하면 직전 러닝에서
 * 적립된 길이 또 새 길로 보인다).
 */
class ExploredCoverageIndex(private val database: RunvealDatabase) {
    private var masksBySegment: Map<Long, String>? = null
    private val decoded = mutableMapOf<Long, CoverageMask>()

    fun refresh() {
        masksBySegment = load()
        decoded.clear()
    }

    fun isEmpty(): Boolean = masks().isEmpty()

    /** [segmentId]의 [sampleIndex]번째 샘플을 이미 지나갔는지. 적립 기록이 없으면 false. */
    fun isCovered(segmentId: Long, sampleIndex: Int, sampleCount: Int): Boolean {
        val encoded = masks()[segmentId] ?: return false
        val mask = decoded.getOrPut(segmentId) { CoverageMask.decode(encoded).resizedTo(sampleCount) }
        return mask[sampleIndex]
    }

    private fun masks(): Map<Long, String> = masksBySegment ?: load().also { masksBySegment = it }

    private fun load(): Map<Long, String> =
        database.exploredSegmentQueries.selectCoverageMasks().executeAsList()
            .associate { it.segment_id to it.covered_mask }
}
