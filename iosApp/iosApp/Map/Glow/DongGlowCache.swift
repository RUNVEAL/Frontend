import CryptoKit
import Foundation
import SharedLogic
import UIKit

/// 바이트 기준 메모리 캐시(32MB로 시작) — Android `DongGlowMemoryCache`(LruCache)에 해당.
final class DongGlowMemoryCache {
    private let cache = NSCache<NSString, DongGlowAsset>()

    init(maxBytes: Int = 32 * 1024 * 1024) {
        cache.totalCostLimit = maxBytes
    }

    func asset(admCd2: String, level: Int) -> DongGlowAsset? {
        cache.object(forKey: key(admCd2, level))
    }

    func put(admCd2: String, level: Int, asset: DongGlowAsset) {
        cache.setObject(asset, forKey: key(admCd2, level), cost: asset.approximateByteCount)
    }

    /// 경계 출처가 바뀌었을 때 — 옛 경계로 만든 이미지를 더 쓰면 안 된다.
    func clear() {
        cache.removeAllObjects()
    }

    private func key(_ admCd2: String, _ level: Int) -> NSString {
        "\(admCd2):\(level)" as NSString
    }
}

/// `Caches/dong_glow/{cacheKey}/{adm_cd2}_{level}.png` + 같은 이름의 `.json`(corners) —
/// Android `DongGlowDiskCache`(webp + json)에 해당한다. iOS는 WebP 인코딩이 기본 API에 없어서 PNG로 쓴다.
///
/// 모든 함수는 블로킹 I/O다 — 호출하는 쪽에서 백그라운드로 돌려야 한다.
enum DongGlowDiskCache {
    private static let rootDirName = "dong_glow"

    /// [cacheKey] 폴더가 없으면 만들고, 다른 키로 남아있던 이전 폴더는 지운다.
    static func ensureCacheDir(cacheKey: String) -> URL? {
        guard let caches = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first else {
            return nil
        }
        let root = caches.appendingPathComponent(rootDirName, isDirectory: true)
        let dir = root.appendingPathComponent(cacheKey, isDirectory: true)
        if FileManager.default.fileExists(atPath: dir.path) { return dir }

        let stale = (try? FileManager.default.contentsOfDirectory(at: root, includingPropertiesForKeys: nil)) ?? []
        for entry in stale where entry.lastPathComponent != cacheKey {
            try? FileManager.default.removeItem(at: entry)
        }
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir
    }

    static func read(dir: URL, admCd2: String, level: Int) -> DongGlowAsset? {
        let base = fileBase(admCd2, level)
        let imageURL = dir.appendingPathComponent("\(base).png")
        let sidecarURL = dir.appendingPathComponent("\(base).json")
        guard let imageData = try? Data(contentsOf: imageURL),
              let image = UIImage(data: imageData),
              let sidecarData = try? Data(contentsOf: sidecarURL),
              let corners = decodeCorners(sidecarData) else { return nil }
        return DongGlowAsset(image: image, corners: corners)
    }

    static func write(dir: URL, admCd2: String, level: Int, asset: DongGlowAsset) {
        let base = fileBase(admCd2, level)
        guard let pngData = asset.image.pngData() else { return }
        try? pngData.write(to: dir.appendingPathComponent("\(base).png"))

        let corners = asset.corners.map { [$0.lon, $0.lat] }
        if let sidecar = try? JSONSerialization.data(withJSONObject: ["corners": corners]) {
            try? sidecar.write(to: dir.appendingPathComponent("\(base).json"))
        }
    }

    private static func decodeCorners(_ data: Data) -> [LatLon]? {
        guard let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let raw = object["corners"] as? [[Double]] else { return nil }
        return raw.compactMap { pair in
            guard pair.count == 2 else { return nil }
            return LatLon(lat: pair[1], lon: pair[0])
        }
    }

    private static func fileBase(_ admCd2: String, _ level: Int) -> String {
        "\(admCd2)_\(level)"
    }
}

/// 디스크 캐시 폴더 이름 — 효과·테두리 두께 토큰과 경계 출처(`"asset"` / `"server:<지도 버전>"`)가
/// 하나라도 바뀌면 다른 키가 나와 이전 캐시 폴더를 자연히 무효화한다.
///
/// 출처가 키에 꼭 들어가야 한다: 서버가 동 경계를 고쳐 새 지도 버전으로 배포하면 테두리 선은 새
/// 경계로 바뀌는데, 빛 이미지는 옛 경계로 만든 캐시가 남아 둘이 어긋난 채로 보인다.
enum DongGlowCacheKey {
    static func compute(boundarySourceKey: String) -> String {
        let input = "tokens=\(DongGlowEffectTokens.tokenFingerprint)|source=\(boundarySourceKey)"
        let digest = SHA256.hash(data: Data(input.utf8))
        return digest.map { String(format: "%02x", $0) }.joined().prefix(16).description
    }
}
