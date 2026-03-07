import SwiftUI
import shared

struct DeviceTypeRow: View {
    let deviceType: DeviceType

    var body: some View {
        let sfSymbol = SFSymbolMapper.sfSymbolName(for: deviceType.defaultIcon)

        HStack(spacing: ButlerSpacing.standard) {
            ButlerIconBox(systemName: sfSymbol)
                .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: ButlerSpacing.extraSmall) {
                Text(deviceType.name)
                    .font(.headline)
                    .fontWeight(.semibold)
                    .lineLimit(1)

                if !deviceType.batteryType.isEmpty {
                    Text("\(deviceType.batteryQuantity) \u{00D7} \(deviceType.batteryType)")
                        .font(.subheadline)
                        .foregroundStyle(Color.butlerOnSurfaceVariant)
                        .lineLimit(1)
                }
            }
        }
        .padding(.vertical, ButlerSpacing.small)
        .accessibilityElement(children: .combine)
        .accessibilityLabel(deviceType.batteryType.isEmpty
            ? deviceType.name
            : "\(deviceType.name), uses \(deviceType.batteryType) battery")
    }
}
