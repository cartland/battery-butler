import SwiftUI
import shared

struct DeviceRow: View {
    let device: Device
    
    var body: some View {
        HStack {
            Image(systemName: "cpu") // Placeholder icon
                .font(.title2)
                .foregroundColor(.blue)
            
            VStack(alignment: .leading) {
                Text(device.name)
                    .font(.headline)
                Text(device.typeId) // Display type ID
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            // Battery indicator could go here
            Image(systemName: "battery.100")
                .foregroundColor(.green)
        }
        .padding(.vertical, 4)
    }
}
