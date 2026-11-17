package com.sloth.runveal.ios

import com.sloth.runveal.map.LatLon
import com.sloth.runveal.recording.CourseProgressTracker

/**
 * [CourseProgressTracker]를 기본 설정으로 만들어 주는 팩토리.
 *
 * Kotlin/Native는 **기본 인자(default argument)를 Objective-C/Swift로 내보내지 않는다** — Swift에서
 * `CourseProgressTracker(coursePath:)`만 쓰려면 생략된 두 인자(근접 반경, 전방 탐색 거리)까지 다
 * 넘겨야 해서, 기본값이 Kotlin 한 곳에만 남도록 여기서 감싼다.
 */
object IosCourseProgress {
    fun tracker(coursePath: List<LatLon>): CourseProgressTracker = CourseProgressTracker(coursePath)
}
