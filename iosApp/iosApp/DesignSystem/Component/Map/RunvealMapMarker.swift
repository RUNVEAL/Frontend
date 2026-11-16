import SwiftUI

/// 현위치는 항상 같은 파란 점, 위계는 출발·도착 마커로 잡는다. Pin은 출발지·도착지 선택 모드에서만.
enum RunvealMapMarkerType { case current, start, end, currentStart }

enum RunvealMapMarkerStyle { case dot, pin }

/// Small은 코스 썸네일 안 출발·도착 점.
enum RunvealMapMarkerSize { case `default`, small }

/// 지도 마커. 지도 SDK 마커 이미지로 사용한다.
struct RunvealMapMarker: View {
    private let type: RunvealMapMarkerType
    private let style: RunvealMapMarkerStyle
    private let size: RunvealMapMarkerSize
    private let showLabel: Bool
    private let label: String?

    init(
        type: RunvealMapMarkerType,
        style: RunvealMapMarkerStyle = .dot,
        size: RunvealMapMarkerSize = .default,
        showLabel: Bool = true,
        label: String? = nil
    ) {
        self.type = type
        self.style = style
        self.size = size
        self.showLabel = showLabel
        self.label = label
    }

    var body: some View {
        let colors = RunvealTheme.colors

        if type == .current {
            RunvealShapes.full
                .fill(colors.map.location)
                .overlay { RunvealShapes.full.stroke(colors.icon.primary, lineWidth: 2) }
                .frame(width: 18, height: 18)
        } else if type == .currentStart {
            ZStack {
                RunvealShapes.full
                    .fill(colors.map.location.opacity(0.3))
                    .frame(width: 40, height: 40)
                DotMarker(
                    fill: colors.accent.glow,
                    borderColor: colors.icon.onAccent,
                    dotSize: 18,
                    borderWidth: 2
                )
            }
            .frame(width: 40, height: 40)
        } else if style == .pin {
            PinMarker(type: type, showLabel: showLabel, label: label)
        } else {
            let dotSize: CGFloat = size == .default ? 18 : 8
            let borderWidth: CGFloat = size == .default ? 2 : 1
            switch type {
            case .start:
                DotMarker(
                    fill: colors.accent.glow,
                    borderColor: colors.icon.onAccent,
                    dotSize: dotSize,
                    borderWidth: borderWidth
                )
            case .end:
                DotMarker(
                    fill: colors.icon.primary,
                    borderColor: colors.icon.onAccent,
                    dotSize: dotSize,
                    borderWidth: borderWidth
                )
            case .current, .currentStart:
                // 위에서 이미 걸러진다.
                EmptyView()
            }
        }
    }
}

private struct DotMarker: View {
    let fill: Color
    let borderColor: Color
    let dotSize: CGFloat
    let borderWidth: CGFloat

    var body: some View {
        RunvealShapes.full
            .fill(fill)
            .overlay { RunvealShapes.full.stroke(borderColor, lineWidth: borderWidth) }
            .frame(width: dotSize, height: dotSize)
    }
}

private struct PinMarker: View {
    let type: RunvealMapMarkerType
    let showLabel: Bool
    let label: String?

    var body: some View {
        let colors = RunvealTheme.colors
        let accentColor = type == .start ? colors.accent.glow : colors.icon.primary
        let resolvedLabel = label ?? (
            type == .start
                ? RunvealDesignSystemStrings.mapMarkerStartLabel
                : RunvealDesignSystemStrings.mapMarkerEndLabel
        )

        VStack(spacing: 0) {
            if showLabel {
                Text(resolvedLabel)
                    .runvealTextStyle(RunvealTheme.typography.bodyMSemibold)
                    .foregroundStyle(colors.text.onAccent)
                    .padding(.vertical, RunvealSpacing.space2)
                    .padding(.horizontal, RunvealSpacing.space8)
                    .background(accentColor, in: RunvealShapes.radius8)
            }
            Rectangle()
                .fill(accentColor)
                .frame(width: 1, height: 10)
            RunvealShapes.full
                .fill(accentColor)
                .frame(width: 8, height: 8)
                .runvealDropShadow(RunvealEffects.glowGoldStrong)
        }
    }
}

#Preview {
    VStack(spacing: 16) {
        RunvealMapMarker(type: .current)
        RunvealMapMarker(type: .currentStart)
        RunvealMapMarker(type: .start, size: .small)
        RunvealMapMarker(type: .end, size: .small)
        RunvealMapMarker(type: .start, style: .pin)
        RunvealMapMarker(type: .end, style: .pin)
    }
    .padding()
    .background(RunvealTheme.colors.bg.canvas)
}
