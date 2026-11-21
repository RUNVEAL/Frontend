package com.sloth.runveal.courserecommend.contract

/**
 * 코스 상세가 보여줄 코스를 **어디서 읽을지**. 같은 화면([com.sloth.runveal.courserecommend.CourseDetailScreen])을
 * 두 경로에서 쓰는데, 들고 있는 데이터가 서로 달라서 id만으로는 구분할 수 없다.
 */
sealed interface CourseDetailSource {
    val id: String

    /**
     * 코스 추천 결과에서 카드를 눌러 들어온 코스 — 아직 저장 전일 수 있다.
     * [com.sloth.runveal.courserecommend.CourseRecommendResultHolder]가 메모리에 들고 있는 응답에서
     * 이 routeId로 다시 찾는다(네비게이션 인자로 전체 응답을 못 넘겨서).
     */
    data class Recommended(override val id: String) : CourseDetailSource

    /** "내 코스"에서 눌러 들어온, 이미 저장된 코스 — 로컬 `course` 테이블에서 읽는다. */
    data class Saved(override val id: String) : CourseDetailSource
}
