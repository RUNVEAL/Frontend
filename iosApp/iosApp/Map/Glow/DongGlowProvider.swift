import Foundation
import SharedLogic

/// 동 하나·단계 하나의 빛 이미지를 메모리 → 디스크 → (없으면) 생성 큐 순서로 구해준다 —
/// Android `DongGlowProvider`에 해당한다.
///
/// 화면에 보이는 동 중 **단계 ≥ 1인 동만** [asset]을 부르는 건 호출 쪽 책임 — 0단계는 빛이 없다.
/// 생성은 동시 2개까지만 돌고, "화면 중심에 가까운 동부터"는 호출 쪽이 한 번의 새로고침 안에서
/// 거리 가까운 순으로 부르는 것으로 맞춘다.
@MainActor
final class DongGlowProvider {
    /// 빛 이미지 한 장이 새로 만들어질 때마다 호출된다. 생성은 비동기라, 이 신호가 없으면 호출 쪽이
    /// 다음 카메라 idle까지 이미지를 못 붙인다 — 캐시가 빈 첫 실행에서 "지도를 한 번 움직여야 빛이
    /// 뜨는" 현상의 원인이다.
    private let onAssetReady: () -> Void

    private let memoryCache = DongGlowMemoryCache()
    private var runningKeys: Set<String> = []
    private var runningTasks: [String: Task<Void, Never>] = [:]
    private var cacheDir: URL?
    private var cacheDirSourceKey: String?

    private static let maxConcurrentRenders = 2

    init(onAssetReady: @escaping () -> Void) {
        self.onAssetReady = onAssetReady
    }

    static func jobKey(admCd2: String, level: Int) -> String {
        "\(admCd2):\(level)"
    }

    /// 메모리나 디스크에 이미 있으면 바로 돌려준다. 없으면 생성 큐에 넣고 `nil`을 돌려준다 — 생성이
    /// 끝나면 메모리 캐시에 올라가므로, 호출 쪽은 다음 새로고침(카메라 idle 또는 [onAssetReady])에서
    /// 다시 부르면 된다. 준비 전에는 호출 쪽이 이전 이미지를 그대로 둔다.
    func asset(
        admCd2: String,
        level: Int,
        parts: [DongPolygonPart],
        boundarySourceKey: String
    ) async -> DongGlowAsset? {
        if let cached = memoryCache.asset(admCd2: admCd2, level: level) { return cached }

        guard let dir = ensureCacheDir(boundarySourceKey: boundarySourceKey) else { return nil }

        if let fromDisk = await Task.detached(priority: .utility) {
            DongGlowDiskCache.read(dir: dir, admCd2: admCd2, level: level)
        }.value {
            memoryCache.put(admCd2: admCd2, level: level, asset: fromDisk)
            return fromDisk
        }

        enqueueRender(dir: dir, admCd2: admCd2, level: level, parts: parts)
        return nil
    }

    /// [stillWantedKeys]에 없는 대기/진행 중인 생성 작업을 취소한다(화면을 벗어난 동).
    func cancelExcept(stillWantedKeys: Set<String>) {
        for key in runningKeys.subtracting(stillWantedKeys) {
            runningTasks.removeValue(forKey: key)?.cancel()
            runningKeys.remove(key)
        }
    }

    func dispose() {
        runningTasks.values.forEach { $0.cancel() }
        runningTasks.removeAll()
        runningKeys.removeAll()
    }

    private func enqueueRender(dir: URL, admCd2: String, level: Int, parts: [DongPolygonPart]) {
        let key = Self.jobKey(admCd2: admCd2, level: level)
        guard !runningKeys.contains(key) else { return }
        guard runningKeys.count < Self.maxConcurrentRenders else { return }
        guard DongGlowEffectTokens.effectsByLevel.indices.contains(level) else { return }

        let effects = DongGlowEffectTokens.effectsByLevel[level]
        let strokeWidth = Float(DongGlowEffectTokens.strokeWidthByLevel[level])
        guard !effects.isEmpty else { return }

        runningKeys.insert(key)
        let task = Task.detached(priority: .utility) { () -> DongGlowAsset? in
            let glow = IosDongGlowRenderer.shared.render(
                parts: parts,
                strokeWidthPx: strokeWidth,
                effects: effects
            )
            guard let asset = DongGlowAsset.make(from: glow) else { return nil }
            DongGlowDiskCache.write(dir: dir, admCd2: admCd2, level: level, asset: asset)
            return asset
        }

        runningTasks[key] = Task { [weak self] in
            let asset = await task.value
            guard let self, !Task.isCancelled else { return }
            self.runningKeys.remove(key)
            self.runningTasks.removeValue(forKey: key)
            guard let asset else { return }
            self.memoryCache.put(admCd2: admCd2, level: level, asset: asset)
            self.onAssetReady()
        }
    }

    /// 경계 출처가 바뀌면(번들 ↔ 서버, 또는 서버 지도 버전 업) 폴더를 다시 잡는다 — 옛 경계로 만든
    /// 이미지가 새 경계 위에 그대로 올라오는 걸 막는다. 메모리 캐시도 같이 비운다.
    private func ensureCacheDir(boundarySourceKey: String) -> URL? {
        if cacheDirSourceKey == boundarySourceKey, let cacheDir { return cacheDir }
        if cacheDirSourceKey != nil { memoryCache.clear() }
        let dir = DongGlowDiskCache.ensureCacheDir(
            cacheKey: DongGlowCacheKey.compute(boundarySourceKey: boundarySourceKey)
        )
        cacheDir = dir
        cacheDirSourceKey = boundarySourceKey
        return dir
    }
}
