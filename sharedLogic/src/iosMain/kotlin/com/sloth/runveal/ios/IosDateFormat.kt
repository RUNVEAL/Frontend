package com.sloth.runveal.ios

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitWeekday
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970

/**
 * 기록 목록·요약이 쓰는 날짜·숫자 포맷. Android는 `java.time` + `String.format`을 쓰는데 Kotlin/Native
 * 에는 둘 다 없어서 `NSCalendar`와 직접 만든 소수점 포맷으로 같은 결과를 낸다.
 *
 * 같은 포맷을 쓰는 Swift 쪽(`RunFormat`)과 **공식이 어긋나면 안 된다** — 결과 화면에서 6'05″로 보이는
 * 페이스가 목록에서 다르면 안 되니, 기록 목록처럼 Kotlin에서 라벨까지 만드는 화면은 이 파일을 쓴다.
 */
internal object IosDateFormat {

    private val calendar: NSCalendar get() = NSCalendar.currentCalendar

    private fun components(epochMs: Long): NSDateComponents {
        val date = NSDate.dateWithTimeIntervalSince1970(epochMs / 1000.0)
        return calendar.components(
            NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or
                NSCalendarUnitHour or NSCalendarUnitWeekday,
            fromDate = date,
        )
    }

    /** "11월 3일 화요일" */
    fun monthDayWeekday(epochMs: Long): String {
        val parts = components(epochMs)
        return "${parts.month}월 ${parts.day}일 ${weekdayLabel(parts.weekday.toInt())}"
    }

    /** "아침"/"점심"/"오후"/"저녁"/"밤" — 결과 화면과 같은 구간 기준. */
    fun timeOfDayLabel(epochMs: Long): String = when (components(epochMs).hour.toInt()) {
        in 5..10 -> "아침"
        in 11..13 -> "점심"
        in 14..17 -> "오후"
        in 18..21 -> "저녁"
        else -> "밤"
    }

    /** NSCalendar의 weekday는 일요일 = 1. */
    private fun weekdayLabel(weekday: Int): String = when (weekday) {
        1 -> "일요일"
        2 -> "월요일"
        3 -> "화요일"
        4 -> "수요일"
        5 -> "목요일"
        6 -> "금요일"
        else -> "토요일"
    }

    /** 이번 달의 시작(포함)과 다음 달의 시작(제외) epoch ms. */
    fun thisMonthRange(): Pair<Long, Long> {
        val now = NSDate()
        val parts = calendar.components(NSCalendarUnitYear or NSCalendarUnitMonth, fromDate = now)
        val startComponents = NSDateComponents().apply {
            year = parts.year
            month = parts.month
            day = 1
        }
        val start = calendar.dateFromComponents(startComponents) ?: now
        val nextComponents = NSDateComponents().apply {
            year = if (parts.month.toInt() == 12) parts.year + 1 else parts.year
            month = if (parts.month.toInt() == 12) 1 else parts.month + 1
            day = 1
        }
        val next = calendar.dateFromComponents(nextComponents) ?: now
        return (start.timeIntervalSince1970 * 1000).toLong() to (next.timeIntervalSince1970 * 1000).toLong()
    }

    /** "2026년 11월" */
    fun currentMonthLabel(): String {
        val parts = calendar.components(NSCalendarUnitYear or NSCalendarUnitMonth, fromDate = NSDate())
        return "${parts.year}년 ${parts.month}월"
    }

    /** 결과 화면과 달리 한 시간을 넘긴 기록도 들어오므로 1시간 이상이면 `h:mm:ss`로 늘린다. */
    fun elapsedLabel(elapsedMs: Long): String {
        val totalSeconds = elapsedMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "$hours:${pad2(minutes)}:${pad2(seconds)}"
        } else {
            "${pad2(minutes)}:${pad2(seconds)}"
        }
    }

    /** "6'05″" 형태 — km당 분'초″. */
    fun paceLabel(elapsedMs: Long, distanceMeters: Double): String {
        if (distanceMeters <= 0.0) return "0'00″"
        val paceSecondsPerKm = (elapsedMs / 1000.0) / (distanceMeters / 1000.0)
        val minutes = (paceSecondsPerKm / 60).toInt()
        val seconds = (paceSecondsPerKm % 60).toInt()
        return "$minutes'${pad2(seconds.toLong())}″"
    }

    private fun pad2(value: Long): String = if (value < 10) "0$value" else "$value"
}

/** `String.format("%.2f")` 대체 — Kotlin/Native에는 `String.format`이 없다. */
internal fun formatKilometers(meters: Double, fractionDigits: Int): String =
    formatDecimal(meters / 1000.0, fractionDigits)

internal fun formatDecimal(value: Double, fractionDigits: Int): String {
    var factor = 1L
    repeat(fractionDigits) { factor *= 10 }
    val scaled = kotlin.math.round(value * factor).toLong()
    val negative = scaled < 0
    val absolute = if (negative) -scaled else scaled
    val whole = absolute / factor
    val fraction = absolute % factor
    if (fractionDigits == 0) return (if (negative) "-" else "") + "$whole"
    val fractionText = "$fraction".padStart(fractionDigits, '0')
    return (if (negative) "-" else "") + "$whole.$fractionText"
}
