import SwiftUI
import shared

struct DeviceRow: View {
    let device: Device
    let deviceType: DeviceType?

    var body: some View {
        let batteryDays = BatteryAgeHelper.daysSince(epochMilliseconds: device.batteryLastReplaced.toEpochMilliseconds())
        let sfSymbol = SFSymbolMapper.sfSymbolName(for: deviceType?.defaultIcon)

        HStack(spacing: ButlerSpacing.standard) {
            ButlerIconBox(systemName: sfSymbol)
                .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: ButlerSpacing.extraSmall) {
                Text(device.name)
                    .font(.headline)
                    .fontWeight(.semibold)
                    .lineLimit(1)

                let typeName = deviceType?.name ?? device.typeId
                let subtitle = [typeName, device.location].compactMap { $0 }.joined(separator: " \u{2022} ")
                if !subtitle.isEmpty {
                    Text(subtitle)
                        .font(.subheadline)
                        .foregroundStyle(Color.butlerOnSurfaceVariant)
                        .lineLimit(1)
                }
            }

            Spacer()

            VStack(spacing: ButlerSpacing.extraSmall) {
                Image(systemName: "battery.100")
                    .font(.system(size: ButlerIconSize.small))
                    .foregroundStyle(BatteryAgeHelper.batteryAgeColor(days: batteryDays))
                    .accessibilityHidden(true)
                if let days = batteryDays {
                    Text("\(days)d")
                        .font(.caption)
                        .foregroundStyle(BatteryAgeHelper.batteryAgeColor(days: batteryDays))
                        .fontWeight(BatteryAgeHelper.batteryAgeFontWeight(days: batteryDays))
                }
            }
            .frame(width: 60)
        }
        .padding(.vertical, ButlerSpacing.small)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(device.name), \(deviceType?.name ?? device.typeId)")
    }
}
