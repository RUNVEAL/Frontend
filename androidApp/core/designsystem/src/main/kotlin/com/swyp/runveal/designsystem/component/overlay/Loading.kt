package com.swyp.runveal.designsystem.component.overlay

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.swyp.runveal.designsystem.R
import com.swyp.runveal.designsystem.theme.RunvealTheme

/** JSON 원본 크기(80x80)와 맞춘 기본 크기 — 호출하는 쪽이 modifier로 따로 안 주면 이 크기로 보인다.
 * 없으면 Box 안에서 크기 제약이 없어 0dp로 그려져 안 보인다. */
private val DEFAULT_LOADING_SIZE = 80.dp

/** 디자이너가 전달한 공용 로딩 모션(`res/raw/runveal_loading_common.json`, 80x80 기준). 계속 반복
 * 재생만 한다 — 배경 딤 처리나 탭 막기는 호출하는 쪽이 맡는다. */
@Composable
fun RunvealLoading(modifier: Modifier = Modifier.size(DEFAULT_LOADING_SIZE)) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.runveal_loading_common))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier,
    )
}

@Preview
@Composable
private fun RunvealLoadingPreview() {
    RunvealTheme {
        RunvealLoading()
    }
}

/** JSON 원본 크기(100x100)와 맞춘 기본 크기. [DEFAULT_LOADING_SIZE]와 같은 이유로 둔다. */
private val DEFAULT_IMPORT_RECORDS_LOADING_SIZE = 100.dp

/** 기록 불러오기 전용 로딩 모션(`res/raw/runveal_loading_import_records.json`, 100x100 / 4.5초).
 * 공용 모션([RunvealLoading])과 달리 한 번 재생이 길어서, 끝날 때까지 기다리는 화면에만 쓴다. */
@Composable
fun RunvealImportRecordsLoading(
    modifier: Modifier = Modifier.size(DEFAULT_IMPORT_RECORDS_LOADING_SIZE),
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.runveal_loading_import_records),
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier,
    )
}

@Preview
@Composable
private fun RunvealImportRecordsLoadingPreview() {
    RunvealTheme {
        RunvealImportRecordsLoading()
    }
}
