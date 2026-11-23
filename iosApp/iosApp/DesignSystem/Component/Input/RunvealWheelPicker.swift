import SwiftUI

/// 숫자 휠 선택(거리 등). 드래그하는 동안은 인덱스를 바꾸지 않고 손가락을 그대로 따라 계속 움직이다가,
/// 손을 뗐을 때 가장 가까운 값 하나로 확정하고 그 자리로 스냅 애니메이션한다. 보이는 줄을 바로 눌러도
/// 고를 수 있다.
struct RunvealWheelPicker: View {
    private static let rowHeight: CGFloat = 44
    private static let visibleRows = 5
    /// 센터 기준 위아래로 더 그려두는 줄 수 — 드래그 중엔 인덱스를 바꾸지 않고 화면 밖 값까지
    /// 끊김 없이 보여준다.
    private static let rowBuffer = 10

    private let values: [String]
    private let selectedIndex: Int
    private let unit: String
    private let onSelectedIndexChange: (Int) -> Void

    @State private var offset: CGFloat = 0

    init(
        values: [String],
        selectedIndex: Int,
        unit: String,
        onSelectedIndexChange: @escaping (Int) -> Void
    ) {
        self.values = values
        self.selectedIndex = selectedIndex
        self.unit = unit
        self.onSelectedIndexChange = onSelectedIndexChange
    }

    /// 보이는 창(5줄) 안에 들어와 있는 줄인가.
    ///
    /// 드래그 중 끊기지 않게 창 위아래로 줄을 더 그려 두는데, 그 줄들도 **버튼**이다.
    /// `clipped()`는 그리는 것만 자르고 터치는 그대로 남기므로, 그냥 두면 창 밖의 보이지 않는
    /// 줄이 위에 있는 칩·버튼을 덮는다 — 그쪽을 눌러도 휠 값만 바뀌는 증상이 그것이다.
    private func isWithinWindow(_ rowOffset: Int) -> Bool {
        let rowCenter = CGFloat(rowOffset) * Self.rowHeight + offset
        return abs(rowCenter) <= Self.rowHeight * CGFloat(Self.visibleRows) / 2
    }

    /// 드래그 중 실제로 센터 줄에 와 있는 값 — 인덱스 커밋과는 별개로 스타일만 따라간다.
    private var centerRowOffset: Int {
        let raw = Int((-offset / Self.rowHeight).rounded())
        return min(max(raw, -Self.rowBuffer), Self.rowBuffer)
    }

    var body: some View {
        ZStack {
            centerGuide
            rows
        }
        .frame(maxWidth: .infinity)
        .frame(height: Self.rowHeight * CGFloat(Self.visibleRows))
        .clipped()
        // 터치도 보이는 창 안으로 제한한다 — `clipped()`는 **그리는 것만** 자른다.
        .contentShape(Rectangle())
        .gesture(dragGesture)
    }

    private var centerGuide: some View {
        let colors = RunvealTheme.colors
        return VStack(spacing: 0) {
            Rectangle().fill(colors.border.default).frame(height: 1)
            Spacer()
            Rectangle().fill(colors.border.default).frame(height: 1)
        }
        .frame(height: Self.rowHeight)
    }

    private var rows: some View {
        VStack(spacing: 0) {
            ForEach(-Self.rowBuffer...Self.rowBuffer, id: \.self) { rowOffset in
                row(rowOffset: rowOffset)
            }
        }
        .offset(y: offset)
    }

    @ViewBuilder
    private func row(rowOffset: Int) -> some View {
        let index = selectedIndex + rowOffset
        let value = values.indices.contains(index) ? values[index] : nil

        Button {
            if value != nil { onSelectedIndexChange(index) }
        } label: {
            ZStack {
                if let value {
                    rowContent(value: value, distanceFromCenter: rowOffset - centerRowOffset)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: Self.rowHeight)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(value == nil)
        .allowsHitTesting(isWithinWindow(rowOffset))
    }

    @ViewBuilder
    private func rowContent(value: String, distanceFromCenter: Int) -> some View {
        let colors = RunvealTheme.colors
        if distanceFromCenter == 0 {
            HStack(alignment: .lastTextBaseline, spacing: RunvealSpacing.space4) {
                Text(value)
                    .runvealTextStyle(RunvealTheme.typography.numL)
                    .foregroundStyle(colors.text.primary)
                Text(unit)
                    .runvealTextStyle(RunvealTheme.typography.unitL)
                    .foregroundStyle(colors.text.secondary)
            }
        } else if abs(distanceFromCenter) <= Self.visibleRows / 2 {
            Text(value)
                .runvealTextStyle(RunvealTheme.typography.numM)
                .foregroundStyle(colors.text.tertiary)
                .opacity(abs(distanceFromCenter) == 1 ? 0.6 : 0.3)
        } else {
            // 보이는 5줄 밖(드래그 중 화면 밖으로 넘어갈 수 있는 값)은 흐려진 숫자로만 채운다.
            Text(value)
                .runvealTextStyle(RunvealTheme.typography.numM)
                .foregroundStyle(colors.text.tertiary)
                .opacity(0.15)
        }
    }

    private var dragGesture: some Gesture {
        DragGesture()
            .onChanged { gesture in
                offset = gesture.translation.height
            }
            .onEnded { _ in
                let steps = Int((-offset / Self.rowHeight).rounded())
                let newIndex = min(max(selectedIndex + steps, 0), max(values.count - 1, 0))
                // 경계에 걸려 steps만큼 다 못 움직였으면 그만큼만 보정해서 시각 위치를 이어 붙인다.
                let start = offset + CGFloat(newIndex - selectedIndex) * Self.rowHeight
                if newIndex != selectedIndex { onSelectedIndexChange(newIndex) }
                offset = start
                // 같은 프레임에서 두 번 대입하면 SwiftUI가 합쳐 버려서 스냅 애니메이션이 안 보인다.
                DispatchQueue.main.async {
                    withAnimation(.spring()) { offset = 0 }
                }
            }
    }
}

#Preview {
    @Previewable @State var index = 4
    RunvealWheelPicker(
        values: (1...20).map(String.init),
        selectedIndex: index,
        unit: "km"
    ) { index = $0 }
        .padding()
        .background(RunvealTheme.colors.bg.canvas)
}
