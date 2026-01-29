import SwiftUI
import shared

struct DeviceDetailScreen: View {
    @StateObject private var wrapper: DeviceDetailViewModelWrapper
    
    init(factory: DeviceDetailViewModelFactory, deviceId: String) {
        let viewModel = factory.create(deviceId: deviceId)
        _wrapper = StateObject(wrappedValue: DeviceDetailViewModelWrapper(viewModel))
    }
    
    var body: some View {
        Group {
            if let success = wrapper.state as? DeviceDetailUiStateSuccess {
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        // Header
                        HStack {
                            Image(systemName: "cpu")
                                .font(.largeTitle)
                                .foregroundColor(.accentColor)
                                .accessibilityLabel("Device icon")
                            
                            VStack(alignment: .leading) {
                                Text(success.device.name)
                                    .font(.title)
                                    .bold()
                                Text(success.deviceType?.name ?? success.device.typeId)
                                    .font(.headline)
                                    .foregroundColor(.secondary)
                            }
                            Spacer()
                        }
                        .padding()
                        .background(Color(.systemBackground))
                        .cornerRadius(12)
                        .shadow(radius: 2)
                        
                        // Actions
                        Button(action: {
                            wrapper.recordReplacement()
                        }) {
                            Text("Replaced Battery")
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color.accentColor)
                                .foregroundColor(.white)
                                .cornerRadius(10)
                        }
                        .accessibilityHint("Records a battery replacement for today")
                        
                        Divider()
                        
                        // History Section
                        Text("Battery History")
                            .font(.title2)
                            .bold()
                        
                        if success.events.isEmpty {
                            Text("No history recorded")
                                .italic()
                                .foregroundColor(.secondary)
                        } else {
                            ForEach(success.events, id: \.self) { event in
                                HStack {
                                    Text(event.date.description) // Format
                                    Spacer()
                                    Text("Replaced")
                                }
                                .padding(.vertical, 4)
                            }
                        }
                    }
                    .padding()
                }
            } else if wrapper.state is DeviceDetailUiStateNotFound {
                Text("Device not found")
            } else {
                ProgressView()
                    .accessibilityLabel("Loading device details")
            }
        }
        .navigationTitle("Device Details")
    }
}
