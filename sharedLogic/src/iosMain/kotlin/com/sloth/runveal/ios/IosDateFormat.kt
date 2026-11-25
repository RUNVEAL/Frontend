package com.sloth.runveal.ios

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
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

    /**
     * 쓰는 항목을 **빠짐없이** 넣어야 한다 — 요청하지 않은 항목은 `NSDateComponents`가
     * `NSDateComponentUndefined`(= Long.MAX_VALUE)를 돌려줘서, 기록 상세의 분이
     * "10:9223372036854775807"로 찍히던 적이 있다.
     */
    private fun components(epochMs: Long): NSDateComponents {
        val date = NSDate.dateWithTimeIntervalSince1970(epochMs / 1000.0)
        return calendar.components(
            NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or
                NSCalendarUnitHour or NSCalendarUnitMinute or NSCalendarUnitWeekday,
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

    /** 지금 연·월 — 월 선택의 기본값이자 "이번 달" 판정 기준. */
    fun currentYear(): Int =
        calendar.components(NSCalendarUnitYear, fromDate = NSDate()).year.toInt()

    fun currentMonth(): Int =
        calendar.components(NSCalendarUnitMonth, fromDate = NSDate()).month.toInt()

    /** 이번 달의 시작(포함)과 다음 달의 시작(제외) epoch ms. */
    fun thisMonthRange(): Pair<Long, Long> = monthRange(currentYear(), currentMonth())

    /** [year]년 [month]월의 시작(포함)과 다음 달의 시작(제외) epoch ms. */
    fun monthRange(year: Int, month: Int): Pair<Long, Long> {
        val now = NSDate()
        val startComponents = NSDateComponents().apply {
            this.year = year.toLong()
            this.month = month.toLong()
            day = 1
        }
        val start = calendar.dateFromComponents(startComponents) ?: now
        val nextComponents = NSDateComponents().apply {
            this.year = (if (month == 12) year + 1 else year).toLong()
            this.month = (if (month == 12) 1 else month + 1).toLong()
            day = 1
        }
        val next = calendar.dateFromComponents(nextComponents) ?: now
        return (start.timeIntervalSince1970 * 1000).toLong() to (next.timeIntervalSince1970 * 1000).toLong()
    }

    /** "2026년 11월" */
    fun currentMonthLabel(): String = monthLabel(currentYear(), currentMonth())

    /** "2026년 11월" — 앱바 옆 라벨. */
    fun monthLabel(year: Int, month: Int): String = "${year}년 ${month}월"

    /**
     * 요약 줄 앞머리 — 이번 달이면 "이번 달", 다른 달이면 "10월"처럼 그 달을 가리킨다.
     * 해가 다르면 달만 적으면 헷갈려서 연도까지 붙인다(Android `RecordViewModel.periodLabel`).
     */
    fun periodLabel(year: Int, month: Int): String {
        val nowYear = currentYear()
        val nowMonth = currentMonth()
        return when {
            year == nowYear && month == nowMonth -> "이번 달"
            year == nowYear -> "${month}월"
            else -> "${year}년 ${month}월"
        }
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
        return paceSecondsLabel((elapsedMs / 1000.0) / (distanceMeters / 1000.0))
    }

    /** "6'05″" 형태 — km당 분'초″. 구간 기록은 이미 초/km로 계산돼 있어 이쪽을 바로 쓴다. */
    fun paceSecondsLabel(paceSecondsPerKm: Double): String {
        val minutes = (paceSecondsPerKm / 60).toInt()
        val seconds = (paceSecondsPerKm % 60).toInt()
        return "$minutes'${pad2(seconds.toLong())}″"
    }

    /** "2026.11.20 12:34" — 기록 상세 상단. Android `yyyy.MM.dd HH:mm`과 같다. */
    fun dateTimeLabel(epochMs: Long): String {
        val parts = components(epochMs)
        return "${parts.year}." + pad2(parts.month) + "." + pad2(parts.day) +
            " " + pad2(parts.hour) + ":" + pad2(parts.minute)
    }

    /** 지금 epoch ms — 이름 수정·삭제가 기록에 남기는 시각. */
    fun nowEpochMs(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

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
