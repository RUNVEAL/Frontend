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

    /// 줄은 **그리기만** 한다 — 누르기·끌기는 휠 전체가 하나의 제스처로 받는다([dragGesture]).
    ///
    /// 줄마다 버튼을 두면 그 버튼이 터치를 먼저 가져가서 부모의 드래그와 싸운다. 손가락을 따라
    /// 움직이지 못하고 손을 뗀 자리에서 한 번에 튀는 게 그 증상이다. Android도 드래그를 컨테이너
    /// (`pointerInput` + `change.consume()`)에서 받는다.
    @ViewBuilder
    private func row(rowOffset: Int) -> some View {
        let index = selectedIndex + rowOffset
        let value = values.indices.contains(index) ? values[index] : nil

        ZStack {
            if let value {
                rowContent(value: value, distanceFromCenter: rowOffset - centerRowOffset)
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: Self.rowHeight)
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

    /// 거의 움직이지 않았으면 "누른 것"으로 본다 — 그 자리의 줄을 고른다.
    private static let tapSlop: CGFloat = 8

    /// 누르기와 끌기를 **하나의 제스처로** 받는다.
    ///
    /// `minimumDistance: 0`이라 손이 닿는 순간부터 받아서, 중간에 다른 뷰에 터치를 뺏기지 않고
    /// 손가락을 그대로 따라간다. 끝에서 움직인 거리를 보고 누른 것인지 끈 것인지 가른다.
    private var dragGesture: some Gesture {
        DragGesture(minimumDistance: 0)
            .onChanged { gesture in
                offset = gesture.translation.height
            }
            .onEnded { gesture in
                if abs(gesture.translation.height) < Self.tapSlop {
                    offset = 0
                    selectRow(atY: gesture.startLocation.y)
                } else {
                    commitDrag()
                }
            }
    }

    /// 누른 자리의 줄을 고른다. 줄 `rowOffset`은 휠 한가운데에서 `rowOffset * rowHeight`만큼
    /// 떨어진 자리에 그려지므로, 거꾸로 환산하면 어떤 줄을 눌렀는지 나온다.
    private func selectRow(atY y: CGFloat) {
        let center = Self.rowHeight * CGFloat(Self.visibleRows) / 2
        let rowOffset = Int(((y - center) / Self.rowHeight).rounded())
        let index = selectedIndex + rowOffset
        guard values.indices.contains(index), index != selectedIndex else { return }
        onSelectedIndexChange(index)
    }

    /// 손을 뗐을 때 가장 가까운 값으로 확정하고 그 자리로 스냅한다(Android `onDragEnd`와 같다).
    private func commitDrag() {
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
