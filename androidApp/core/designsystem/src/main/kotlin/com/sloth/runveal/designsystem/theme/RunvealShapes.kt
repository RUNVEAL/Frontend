package com.sloth.runveal.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.sloth.runveal.designsystem.RunvealRadius

/** RunvealRadius(자동 생성)를 Compose Shape로. `radius/full`은 [full] = CircleShape. */
object RunvealShapes {
    val radius8 = RoundedCornerShape(RunvealRadius.radius8)
    val radius16 = RoundedCornerShape(RunvealRadius.radius16)
    val radius24 = RoundedCornerShape(RunvealRadius.radius24)
    /** Home Bottom Panel처럼 안에 탭바를 품는 큰 패널 */
    val radius32 = RoundedCornerShape(RunvealRadius.radius32)
    /** 완전한 원형(pill) */
    val full = CircleShape
}
