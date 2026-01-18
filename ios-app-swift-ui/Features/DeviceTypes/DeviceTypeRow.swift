import SwiftUI
import shared

struct DeviceTypeRow: View {
    let deviceType: DeviceType
    
    var body: some View {
        VStack(alignment: .leading) {
            Text(deviceType.name)
                .font(.headline)
            if !deviceType.batteryType.isEmpty {
                Text(deviceType.batteryType)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}
