package com.sloth.runveal.devicestate

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * 기기의 인터넷 연결 상태. 와이파이·모바일 데이터가 **둘 다 없는 상태**를 화면에서 가려내는 데
 * 쓴다 — 서버가 죽었거나 특정 API가 503인 경우(`docs/api/API.md` §5.7)는 여기서 다루지 않는다.
 * 그건 호출한 화면이 응답을 보고 따로 처리할 몫이다.
 */
@Stable
class NetworkState internal constructor(
    val isConnected: Boolean,
    private val onRefresh: () -> Unit,
) {
    /**
     * 지금 연결 상태를 다시 읽어 [isConnected]에 반영한다. "다시 시도" 버튼이 쓴다.
     *
     * 연결이 돌아오면 [rememberNetworkState]의 콜백이 알아서 갱신하므로 꼭 필요한 건 아니지만,
     * 버튼을 누른 사람에게 즉시 결과를 보여주는 쪽이 낫다 — 특히 방금 붙은 네트워크가 시스템
     * 검증(VALIDATED)을 기다리는 동안엔 화면이 아직 "연결 없음"으로 남아 있다.
     */
    fun refresh() = onRefresh()
}

/**
 * 연결 상태를 구독해 바뀔 때마다 갱신한다. `ConnectivityManager.NetworkCallback`을 쓰므로 비행기
 * 모드를 켜거나 와이파이를 끄면 **화면을 떠나지 않아도** 즉시 반영된다
 * ([com.sloth.runveal.permission.rememberLocationPermissionState]가 위치 서비스 꺼짐을
 * `PROVIDERS_CHANGED` 브로드캐스트로 잡는 것과 같은 방식).
 *
 * "연결됨"의 기준은 [NetworkCapabilities.NET_CAPABILITY_VALIDATED]까지 포함한다 — 와이파이에
 * 붙어 있어도 실제로 인터넷이 안 되는 경우(로그인이 필요한 공공 와이파이 등)를 "연결됨"으로 보면
 * 화면은 정상인데 요청이 전부 실패하는 상태가 된다. 안내 문구도 "인터넷 연결을 확인해 주세요"다.
 *
 * 다만 이 기준에는 대가가 있다: 방금 붙은 네트워크는 시스템 검증이 끝나기까지 잠깐 VALIDATED가
 * 아니라서 그 사이엔 "연결 없음"으로 보인다. 검증이 끝나면 콜백이 바로 갱신하므로 잠깐 스쳐
 * 지나가는 정도다.
 */
@Composable
fun rememberNetworkState(): NetworkState {
    val context = LocalContext.current
    var connected by remember { mutableStateOf(context.hasValidatedInternet()) }

    DisposableEffect(context) {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        if (manager == null) {
            // ConnectivityManager가 없는 환경(일부 테스트·프리뷰)에서는 구독 없이 둔다 — 초기값이
            // false라 "연결 없음"으로 보이지만, 그 환경에서 네트워크를 쓸 일도 없다.
            return@DisposableEffect onDispose { }
        }

        // 세 콜백 모두 같은 계산을 다시 돌린다. onLost는 "그 네트워크가 끊겼다"일 뿐 다른 네트워크가
        // 남아 있을 수 있고(와이파이 → 데이터), onCapabilitiesChanged는 VALIDATED가 붙는 순간을
        // 잡아야 하므로, 개별 이벤트를 해석하지 않고 현재 상태를 통째로 다시 읽는 게 정확하다.
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                connected = context.hasValidatedInternet()
            }

            override fun onLost(network: Network) {
                connected = context.hasValidatedInternet()
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                connected = context.hasValidatedInternet()
            }
        }
        manager.registerDefaultNetworkCallback(callback)
        onDispose { manager.unregisterNetworkCallback(callback) }
    }

    return remember(connected, context) {
        NetworkState(
            isConnected = connected,
            onRefresh = { connected = context.hasValidatedInternet() },
        )
    }
}

private fun Context.hasValidatedInternet(): Boolean {
    val manager = getSystemService(ConnectivityManager::class.java) ?: return false
    val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
