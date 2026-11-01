package com.swyp.runveal.designsystem

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.swyp.runveal.feature.main.R

/** Pretendard - 한글, UI 글자. Regular / SemiBold만 사용 */
val pretendardFontFamily: FontFamily = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
)

/** Geist - 강조 숫자 전용. ExtraLight / Light만 사용 */
val geistFontFamily: FontFamily = FontFamily(
    Font(R.font.geist_extralight, FontWeight.ExtraLight),
    Font(R.font.geist_light, FontWeight.Light),
)

/**
 * RunvealTypography(자동 생성)의 생성자 파라미터는 tokens JSON의 font.family 키를 따른다.
 * 폰트를 추가하면 res/font에 파일을 넣고 여기에 FontFamily를 추가한다.
 */
val runvealTypography: RunvealTypography = RunvealTypography(pretendard = pretendardFontFamily, geist = geistFontFamily)
