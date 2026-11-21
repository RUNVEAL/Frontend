package com.sloth.runveal.courserecommend.contract

import com.sloth.runveal.ui.mvi.UiIntent

sealed interface CourseDetailIntent : UiIntent {
    /** 화면이 뜰 때 한 번 — [source]가 추천 결과(메모리)인지 저장한 코스(로컬 DB)인지 가른다. */
    data class Load(val source: CourseDetailSource) : CourseDetailIntent

    /** "코스 저장" 버튼 — 지금 보고 있는 코스를 로컬 `course` 테이블에 저장한다. */
    data object SaveCourse : CourseDetailIntent
}
