import SwiftUI

/// RunvealRadius(자동 생성)를 SwiftUI Shape로. `radius/full`은 [full] = Capsule().
enum RunvealShapes {
    static let radius8 = AnyShape(RoundedRectangle(cornerRadius: RunvealRadius.radius8))
    static let radius16 = AnyShape(RoundedRectangle(cornerRadius: RunvealRadius.radius16))
    static let radius24 = AnyShape(RoundedRectangle(cornerRadius: RunvealRadius.radius24))
    /// Home Bottom Panel처럼 안에 탭바를 품는 큰 패널
    static let radius32 = AnyShape(RoundedRectangle(cornerRadius: RunvealRadius.radius32))
    /// 완전한 원형(pill)
    static let full = AnyShape(Capsule())
}
