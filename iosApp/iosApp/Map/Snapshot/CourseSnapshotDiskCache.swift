import Foundation
import SharedLogic
import UIKit

/// `Documents/course_snapshots/{variant}_{ownerId}_{STYLE_VERSION}_{w}x{h}@{ratio}.png` —
/// Android `CourseSnapshotDiskCache`(webp)와 같은 파일 캐시 패턴. iOS는 WebP 인코딩이 기본 API에
/// 없어서 PNG로 쓴다.
///
/// 모든 함수는 블로킹 I/O다.
enum CourseSnapshotDiskCache {
    private static let rootDirName = "course_snapshots"

    /// **iOS 렌더링 판 번호** — 합성 방식이 바뀌면 올린다. 올리면 예전에 구워 둔 이미지를 더 쓰지
    /// 않고 다시 굽는다. 공용 `STYLE_VERSION`을 건드리면 Android 캐시까지 버리게 돼서 따로 둔다.
    ///
    /// 2 — 스냅샷 하단의 출처 표기 띠를 잘라 내기 시작([CourseSnapshotRenderer]).
    private static let iosRenderVersion = 2

    static func fileName(
        request: SnapshotRequest,
        widthPx: Int,
        heightPx: Int,
        pixelRatio: CGFloat
    ) -> String {
        let version = SnapshotConfig.shared.STYLE_VERSION
        return "\(request.cacheKeyBase())_\(version)r\(iosRenderVersion)_\(widthPx)x\(heightPx)@\(pixelRatio).png"
    }

    static func existingFile(
        request: SnapshotRequest,
        widthPx: Int,
        heightPx: Int,
        pixelRatio: CGFloat
    ) -> URL? {
        guard let root = rootDir() else { return nil }
        let url = root.appendingPathComponent(
            fileName(request: request, widthPx: widthPx, heightPx: heightPx, pixelRatio: pixelRatio)
        )
        return FileManager.default.fileExists(atPath: url.path) ? url : nil
    }

    /// 임시 파일에 쓴 뒤 옮겨서 원자적으로 저장하고, 같은 소유자·variant의 이전 버전(크기·배율·
    /// STYLE_VERSION이 달랐던) 파일은 지운다.
    static func write(
        request: SnapshotRequest,
        image: UIImage,
        widthPx: Int,
        heightPx: Int,
        pixelRatio: CGFloat
    ) -> URL? {
        guard let root = rootDir(), let data = image.pngData() else { return nil }
        try? FileManager.default.createDirectory(at: root, withIntermediateDirectories: true)

        let name = fileName(request: request, widthPx: widthPx, heightPx: heightPx, pixelRatio: pixelRatio)
        let target = root.appendingPathComponent(name)
        let temp = root.appendingPathComponent("\(name).tmp")
        do {
            try data.write(to: temp)
            if FileManager.default.fileExists(atPath: target.path) {
                try FileManager.default.removeItem(at: target)
            }
            try FileManager.default.moveItem(at: temp, to: target)
        } catch {
            try? FileManager.default.removeItem(at: temp)
            return nil
        }
        deleteStaleVersions(root: root, cacheKeyBase: request.cacheKeyBase(), keepFileName: name)
        return target
    }

    static func deleteAll(ownerId: String) {
        guard let root = rootDir() else { return }
        let prefixes = ["thumbnail_\(ownerId)_", "result_card_\(ownerId)_"]
        let entries = (try? FileManager.default.contentsOfDirectory(at: root, includingPropertiesForKeys: nil)) ?? []
        for entry in entries where prefixes.contains(where: { entry.lastPathComponent.hasPrefix($0) }) {
            try? FileManager.default.removeItem(at: entry)
        }
    }

    private static func deleteStaleVersions(root: URL, cacheKeyBase: String, keepFileName: String) {
        let prefix = "\(cacheKeyBase)_"
        let entries = (try? FileManager.default.contentsOfDirectory(at: root, includingPropertiesForKeys: nil)) ?? []
        for entry in entries {
            let name = entry.lastPathComponent
            if name.hasPrefix(prefix), name != keepFileName {
                try? FileManager.default.removeItem(at: entry)
            }
        }
    }

    private static func rootDir() -> URL? {
        FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first?
            .appendingPathComponent(rootDirName, isDirectory: true)
    }
}
