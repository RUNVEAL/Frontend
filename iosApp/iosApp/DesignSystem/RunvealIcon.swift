import SwiftUI

/// 아이콘 레이어 하나(SVG의 `<path>` 또는 `<circle>` 한 개). RunvealIcons(자동 생성)의 각 아이콘이
/// 이 타입의 배열이다. 좌표는 전부 24x24 viewBox 기준이고, 실제 크기는 [RunvealIcon]이 맞춰 준다.
struct RunvealIconLayer {
    /// `<circle cx cy r>` — 호(A) 명령을 파싱하지 않아도 되도록 생성기가 원을 패스로 바꾸지 않고
    /// 그대로 내려준다(Android 생성기는 호로 바꿔 Vector Drawable에 넣는다).
    struct Circle {
        let cx: CGFloat
        let cy: CGFloat
        let r: CGFloat
    }

    enum Geometry {
        case path(String)
        case circle(Circle)
    }

    let geometry: Geometry
    var fill: Color?
    var stroke: Color?
    var strokeWidth: CGFloat
    var lineCap: CGLineCap
    var lineJoin: CGLineJoin

    init(
        path: String,
        fill: Color? = nil,
        stroke: Color? = nil,
        strokeWidth: CGFloat = 0,
        lineCap: CGLineCap = .butt,
        lineJoin: CGLineJoin = .miter
    ) {
        self.geometry = .path(path)
        self.fill = fill
        self.stroke = stroke
        self.strokeWidth = strokeWidth
        self.lineCap = lineCap
        self.lineJoin = lineJoin
    }

    init(
        circle: Circle,
        fill: Color? = nil,
        stroke: Color? = nil,
        strokeWidth: CGFloat = 0,
        lineCap: CGLineCap = .butt,
        lineJoin: CGLineJoin = .miter
    ) {
        self.geometry = .circle(circle)
        self.fill = fill
        self.stroke = stroke
        self.strokeWidth = strokeWidth
        self.lineCap = lineCap
        self.lineJoin = lineJoin
    }
}

/// 아이콘 하나. design/icons/*.svg에서 빌드 때 자동 생성된다
/// (gradle/runveal-icons-swift.gradle.kts → RunvealIcons.swift).
struct RunvealIconSpec {
    /// 원본 SVG의 viewBox — design/icons의 모든 아이콘이 24x24다.
    static let viewBox: CGFloat = 24

    let layers: [RunvealIconLayer]
}

/// 아이콘 레이어 하나를 그리는 Shape. 24x24 좌표를 받은 rect 크기로 비례 확대한다.
private struct RunvealIconLayerShape: Shape {
    let layer: RunvealIconLayer

    func path(in rect: CGRect) -> Path {
        let scale = min(rect.width, rect.height) / RunvealIconSpec.viewBox
        let raw: Path
        switch layer.geometry {
        case .path(let d):
            raw = RunvealSvgPath.path(from: d)
        case .circle(let circle):
            raw = Path(
                ellipseIn: CGRect(
                    x: circle.cx - circle.r,
                    y: circle.cy - circle.r,
                    width: circle.r * 2,
                    height: circle.r * 2
                )
            )
        }
        return raw.applying(CGAffineTransform(scaleX: scale, y: scale))
    }
}

/// 아이콘 뷰. Android `Icon(imageVector, tint = ...)`과 같은 의미로 쓴다.
///
/// [tint]가 `nil`이면 아이콘 원본 색을 그대로 쓴다 — Android의 `tint = Color.Unspecified`와 같다.
/// location-off처럼 전경(F4EEEA) + 배경 knockout(272626) 두 색으로 만들어진 아이콘은 반드시
/// `nil`로 둬야 모양이 유지된다.
///
/// 사용: `RunvealIcon(RunvealIcons.home, tint: RunvealTheme.colors.icon.primary)`
struct RunvealIcon: View {
    private let spec: RunvealIconSpec
    private let tint: Color?
    private let size: CGFloat

    init(_ spec: RunvealIconSpec, tint: Color? = nil, size: CGFloat = RunvealLayout.sizeIcon) {
        self.spec = spec
        self.tint = tint
        self.size = size
    }

    var body: some View {
        // stroke 두께는 Shape 안에서 안 늘어나므로(선 굵기는 뷰 좌표 기준) 여기서 같은 비율로 키운다.
        let scale = size / RunvealIconSpec.viewBox
        ZStack {
            ForEach(spec.layers.indices, id: \.self) { index in
                let layer = spec.layers[index]
                let shape = RunvealIconLayerShape(layer: layer)
                if let fill = tint ?? layer.fill {
                    shape.fill(fill)
                }
                if let stroke = tint ?? layer.stroke {
                    shape.stroke(
                        stroke,
                        style: StrokeStyle(
                            lineWidth: layer.strokeWidth * scale,
                            lineCap: layer.lineCap,
                            lineJoin: layer.lineJoin
                        )
                    )
                }
            }
        }
        .frame(width: size, height: size)
    }
}

/// SVG path `d` 문자열 → SwiftUI `Path`.
///
/// design/icons/*.svg가 실제로 쓰는 명령만 지원한다(M/L/H/V/C/Z, 소문자 상대 좌표 포함).
/// 그 밖의 명령(특히 호 `A`)이 들어오면 **빌드가 실패**한다 — 생성기
/// (gradle/runveal-icons-swift.gradle.kts)가 SVG를 읽을 때 미리 검사해서, 런타임에 아이콘이
/// 조용히 깨지는 대신 아이콘을 추가한 사람이 바로 알게 한다.
enum RunvealSvgPath {
    static func path(from d: String) -> Path {
        var path = Path()
        var lexer = Lexer(d)
        var current = CGPoint.zero
        var subpathStart = CGPoint.zero

        while let command = lexer.nextCommand() {
            let relative = command.isLowercase
            switch Character(command.lowercased()) {
            case "m":
                var first = true
                while let x = lexer.nextNumber(), let y = lexer.nextNumber() {
                    let point = resolve(x: x, y: y, from: current, relative: relative)
                    if first {
                        path.move(to: point)
                        subpathStart = point
                        first = false
                    } else {
                        // SVG 규칙: moveto에 좌표가 더 붙어 있으면 그 뒤는 lineto로 이어진다.
                        path.addLine(to: point)
                    }
                    current = point
                }
            case "l":
                while let x = lexer.nextNumber(), let y = lexer.nextNumber() {
                    let point = resolve(x: x, y: y, from: current, relative: relative)
                    path.addLine(to: point)
                    current = point
                }
            case "h":
                while let x = lexer.nextNumber() {
                    let point = CGPoint(x: relative ? current.x + x : x, y: current.y)
                    path.addLine(to: point)
                    current = point
                }
            case "v":
                while let y = lexer.nextNumber() {
                    let point = CGPoint(x: current.x, y: relative ? current.y + y : y)
                    path.addLine(to: point)
                    current = point
                }
            case "c":
                while let x1 = lexer.nextNumber(), let y1 = lexer.nextNumber(),
                      let x2 = lexer.nextNumber(), let y2 = lexer.nextNumber(),
                      let x = lexer.nextNumber(), let y = lexer.nextNumber() {
                    let control1 = resolve(x: x1, y: y1, from: current, relative: relative)
                    let control2 = resolve(x: x2, y: y2, from: current, relative: relative)
                    let point = resolve(x: x, y: y, from: current, relative: relative)
                    path.addCurve(to: point, control1: control1, control2: control2)
                    current = point
                }
            case "z":
                path.closeSubpath()
                current = subpathStart
            default:
                // 생성기가 미리 걸러 주므로 여기까지 오지 않는다. 혹시 들어오면 그 명령만 버린다.
                lexer.skipNumbers()
            }
        }
        return path
    }

    private static func resolve(x: CGFloat, y: CGFloat, from current: CGPoint, relative: Bool) -> CGPoint {
        relative ? CGPoint(x: current.x + x, y: current.y + y) : CGPoint(x: x, y: y)
    }

    private struct Lexer {
        private let chars: [Character]
        private var index = 0

        init(_ text: String) {
            chars = Array(text)
        }

        mutating func nextCommand() -> Character? {
            skipSeparators()
            guard index < chars.count, chars[index].isLetter else { return nil }
            defer { index += 1 }
            return chars[index]
        }

        mutating func nextNumber() -> CGFloat? {
            skipSeparators()
            guard index < chars.count, isNumberStart(chars[index]) else { return nil }

            var text = ""
            if chars[index] == "-" || chars[index] == "+" {
                text.append(chars[index])
                index += 1
            }
            var seenDot = false
            var seenExponent = false
            while index < chars.count {
                let char = chars[index]
                if char.isNumber {
                    text.append(char)
                    index += 1
                } else if char == ".", !seenDot, !seenExponent {
                    seenDot = true
                    text.append(char)
                    index += 1
                } else if char == "e" || char == "E", !seenExponent {
                    seenExponent = true
                    text.append(char)
                    index += 1
                    if index < chars.count, chars[index] == "-" || chars[index] == "+" {
                        text.append(chars[index])
                        index += 1
                    }
                } else {
                    break
                }
            }
            guard let value = Double(text) else { return nil }
            return CGFloat(value)
        }

        mutating func skipNumbers() {
            while nextNumber() != nil {}
        }

        mutating func skipSeparators() {
            while index < chars.count, chars[index] == " " || chars[index] == ","
                || chars[index] == "\n" || chars[index] == "\r" || chars[index] == "\t" {
                index += 1
            }
        }

        private func isNumberStart(_ char: Character) -> Bool {
            char.isNumber || char == "-" || char == "+" || char == "."
        }
    }
}
