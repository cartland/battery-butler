import SwiftUI
import shared

struct DeviceRow: View {
    let device: Device
    
    var body: some View {
        HStack {
            Image(systemName: "cpu")
                .font(.title2)
                .foregroundColor(.blue)
                .accessibilityHidden(true)

            VStack(alignment: .leading) {
                Text(device.name)
                    .font(.headline)
                Text(device.typeId)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }

            Spacer()

            Image(systemName: "battery.100")
                .foregroundColor(.green)
                .accessibilityHidden(true)
        }
        .padding(.vertical, 4)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(device.name), \(device.typeId)")
    }
}
