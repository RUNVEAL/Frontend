package com.sloth.runveal.dong

/**
 * 구간(segment) 하나를 [SegmentCoverageMatcher.SAMPLE_STEP_METERS] 간격으로 샘플링했을 때 **어느
 * 샘플까지 지나갔는지**를 담는 비트맵. "구간을 쪼개서 적립"하는 최소 단위다.
 *
 * 이게 없으면 적립 단위가 구간 전체여서, 300m 구간을 20m만 달려도 300m를 다 탐험한 것으로 쳤다.
 * 반대로 한 번에 다 못 달린 구간은 영원히 미완으로 남는 문제도 생기는데, 러닝마다 비트를 OR로
 * 합치면([or]) 오늘 앞쪽 절반·내일 뒤쪽 절반을 달려도 합쳐서 전체가 된다.
 *
 * [size]가 샘플 수라 길이 환산은 `coveredCount / size * 구간길이`로 한다 — 구간 길이는
 * `road_segment.length_m`(서버 값)를 쓰고 비율만 여기서 본다.
 */
class CoverageMask private constructor(
    /** 샘플 수 — 비트 개수. */
    val size: Int,
    private val bytes: ByteArray,
) {
    val coveredCount: Int get() = bytes.sumOf { byte -> (byte.toInt() and 0xFF).countOneBits() }

    /** 덮인 비율(0.0~1.0). 샘플이 없으면 0. */
    val coveredRatio: Double get() = if (size == 0) 0.0 else coveredCount.toDouble() / size

    fun isEmpty(): Boolean = coveredCount == 0

    fun isFull(): Boolean = size > 0 && coveredCount == size

    operator fun get(index: Int): Boolean =
        index in 0 until size && (bytes[index / 8].toInt() shr (index % 8)) and 1 == 1

    fun set(index: Int) {
        if (index !in 0 until size) return
        bytes[index / 8] = (bytes[index / 8].toInt() or (1 shl (index % 8))).toByte()
    }

    /** 합집합 — 여러 러닝의 커버리지를 누적할 때. [other]의 해상도가 다르면 이쪽에 맞춰 환산한다. */
    fun or(other: CoverageMask): CoverageMask {
        val aligned = other.resizedTo(size)
        val merged = empty(size)
        for (i in bytes.indices) {
            merged.bytes[i] = (bytes[i].toInt() or aligned.bytes[i].toInt()).toByte()
        }
        return merged
    }

    /** 차집합 — "이번 러닝에서 **새로** 덮인 부분"을 구할 때(`이번 mask andNot 지금까지의 mask`). */
    fun andNot(other: CoverageMask): CoverageMask {
        val aligned = other.resizedTo(size)
        val remaining = empty(size)
        for (i in bytes.indices) {
            remaining.bytes[i] = (bytes[i].toInt() and aligned.bytes[i].toInt().inv()).toByte()
        }
        return remaining
    }

    /**
     * 해상도를 [newSize] 샘플로 바꾼다 — 서버 지도 버전이 바뀌어 구간 지오메트리(=샘플 수)가 달라져도
     * 예전에 적립한 커버리지를 버리지 않기 위한 보정이다. 전체를 덮은 mask는 해상도와 무관하게 전체로
     * 남긴다(옛 전체 적립분을 일부로 깎지 않는다).
     */
    fun resizedTo(newSize: Int): CoverageMask {
        if (newSize == size) return this
        if (isFull()) return full(newSize)
        val resized = empty(newSize)
        if (size == 0 || newSize == 0) return resized
        // 비트 하나가 뜻하는 구간(샘플 i ~ i+1)을 새 해상도의 구간으로 그대로 옮긴다 — 한 칸씩만
        // 찍으면 해상도를 올릴 때 덮인 비율이 줄어든다.
        for (i in 0 until size) {
            if (!this[i]) continue
            val from = i * newSize / size
            val until = maxOf(from + 1, (i + 1) * newSize / size)
            for (j in from until minOf(until, newSize)) resized.set(j)
        }
        return resized
    }

    /** `"<샘플 수>:<hex>"` — DB 컬럼(`covered_mask`)에 그대로 넣는 형태. */
    fun encode(): String = buildString {
        append(size)
        append(':')
        bytes.forEach { byte ->
            val value = byte.toInt() and 0xFF
            append(HEX[value shr 4])
            append(HEX[value and 0x0F])
        }
    }

    companion object {
        private const val HEX = "0123456789abcdef"

        fun empty(size: Int): CoverageMask =
            CoverageMask(size.coerceAtLeast(0), ByteArray((size.coerceAtLeast(0) + 7) / 8))

        fun full(size: Int): CoverageMask = empty(size).also { mask ->
            for (i in 0 until mask.size) mask.set(i)
        }

        /**
         * [encode] 결과를 되읽는다. **빈 문자열은 "구간 전체"** 로 본다 — 쪼개 적립하기 전에 쌓인
         * 행(구간 전체 적립)이 그런 모양이라, 0으로 읽으면 이미 달린 길이 갑자기 안 간 길이 된다.
         * 형식이 깨졌으면 그것도 전체로 본다(적립을 잃는 쪽보다 안전하다).
         */
        fun decode(text: String): CoverageMask {
            if (text.isBlank()) return full(1)
            val separator = text.indexOf(':')
            val size = if (separator > 0) text.substring(0, separator).toIntOrNull() else null
            if (size == null || size < 0) return full(1)
            val hex = text.substring(separator + 1)
            val mask = empty(size)
            for (i in 0 until minOf(mask.bytes.size, hex.length / 2)) {
                val value = hex.substring(i * 2, i * 2 + 2).toIntOrNull(radix = 16) ?: return full(size)
                mask.bytes[i] = value.toByte()
            }
            return mask
        }
    }
}
