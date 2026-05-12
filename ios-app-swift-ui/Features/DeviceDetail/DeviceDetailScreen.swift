import SwiftUI
import shared

struct DeviceDetailScreen: View {
    @StateObject private var wrapper: DeviceDetailViewModelWrapper
    private let component: NativeComponent
    private let deviceId: String
    @State private var showingEditDevice = false

    init(component: NativeComponent, deviceId: String) {
        self.deviceId = deviceId
        self.component = component
        let viewModel = component.deviceDetailViewModelFactory.create(deviceId: deviceId)
        _wrapper = StateObject(wrappedValue: DeviceDetailViewModelWrapper(viewModel))
    }

    var body: some View {
        DeviceDetailContentView(
            state: wrapper.state,
            onRecordReplacement: {
                wrapper.recordReplacement()
            },
            eventDestination: { eventId in
                EventDetailScreen(eventId: eventId, component: component)
            }
        )
        .navigationTitle("device_detail.title")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button("common.edit") {
                    showingEditDevice = true
                }
            }
        }
        .sheet(isPresented: $showingEditDevice) {
            EditDeviceScreen(deviceId: deviceId, component: component)
        }
    }
}

struct DeviceDetailContentView<Destination: View>: View {
    let state: DeviceDetailScreenState
    let onRecordReplacement: () -> Void
    let eventDestination: (String) -> Destination

    var body: some View {
        Group {
            if let success = state as? DeviceDetailScreenStateSuccess {
                DeviceDetailSuccessView(
                    success: success,
                    onRecordReplacement: onRecordReplacement,
                    eventDestination: eventDestination
                )
            } else if state is DeviceDetailScreenStateNotFound {
                Text("device_detail.not_found")
            } else {
                ProgressView()
                    .accessibilityLabel("device_detail.accessibility.loading")
            }
        }
    }
}

// MARK: - Success State

private struct DeviceDetailSuccessView<Destination: View>: View {
    let success: DeviceDetailScreenStateSuccess
    let onRecordReplacement: () -> Void
    let eventDestination: (String) -> Destination

    var body: some View {
        let sfSymbol = SFSymbolMapper.sfSymbolName(for: success.deviceType?.defaultIcon)

        ScrollView {
            VStack(alignment: .leading, spacing: ButlerSpacing.standard) {
                // Profile Header
                profileHeader(sfSymbol: sfSymbol)

                // Stat Cards
                statCards

                // Record Replacement Button
                replacementButton

                Divider()

                // History Section
                historySection
            }
            .padding()
        }
    }

    // MARK: - Profile Header

    @ViewBuilder
    private func profileHeader(sfSymbol: String) -> some View {
        VStack(spacing: ButlerSpacing.small) {
            // Circular icon
            ZStack {
                Circle()
                    .fill(Color.butlerPrimaryContainer)
                    .frame(width: 88, height: 88)
                Image(systemName: sfSymbol)
                    .font(.system(size: 40))
                    .foregroundStyle(Color.butlerOnPrimaryContainer)
            }
            .padding(.bottom, ButlerSpacing.small)

            // Device name
            Text(success.device.name)
                .font(.title)
                .bold()

            // Device type
            HStack(spacing: ButlerSpacing.extraSmall) {
                Image(systemName: "desktopcomputer")
                    .font(.system(size: ButlerIconSize.extraSmall))
                    .foregroundStyle(Color.butlerOnSurfaceVariant)
                Text(success.deviceType?.name ?? String(localized: "common.unknown"))
                    .font(.headline)
                    .foregroundStyle(Color.butlerOnSurfaceVariant)
            }

            // Location
            if let location = success.device.location,
               !location.isEmpty {
                HStack(spacing: ButlerSpacing.extraSmall) {
                    Image(systemName: "location.fill")
                        .font(.system(size: ButlerIconSize.extraSmall))
                        .foregroundStyle(Color.butlerOnSurfaceVariant)
                    Text(location)
                        .font(.subheadline)
                        .foregroundStyle(Color.butlerOnSurfaceVariant)
                }
            }
        }
        .frame(maxWidth: .infinity)
        .padding(ButlerSpacing.standard)
        .background(Color.butlerSurface)
        .clipShape(RoundedRectangle(cornerRadius: ButlerCornerRadius.large))
        .overlay(
            RoundedRectangle(cornerRadius: ButlerCornerRadius.large)
                .stroke(Color.butlerOutline.opacity(0.5), lineWidth: 1)
        )
    }

    // MARK: - Stat Cards

    private var statCards: some View {
        HStack(spacing: ButlerSpacing.standard) {
            StatCardView(
                systemName: "battery.100",
                label: String(localized: "device_detail.stat.type"),
                value: success.deviceType?.batteryType.isEmpty == false
                    ? (success.deviceType?.batteryType ?? String(localized: "device_detail.stat.not_available"))
                    : String(localized: "device_detail.stat.not_available")
            )
            StatCardView(
                systemName: "number",
                label: String(localized: "device_detail.stat.quantity"),
                value: "\(success.deviceType?.batteryQuantity ?? 0)"
            )
        }
    }

    // MARK: - Replacement Button

    private var replacementButton: some View {
        Button(action: onRecordReplacement) {
            HStack {
                ZStack {
                    RoundedRectangle(cornerRadius: ButlerCornerRadius.small)
                        .fill(Color.white.opacity(0.2))
                        .frame(width: 40, height: 40)
                    Image(systemName: "plus.circle.fill")
                        .font(.system(size: ButlerIconSize.medium))
                        .foregroundStyle(.white)
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text("device_detail.replaced_battery")
                        .font(.headline)
                        .bold()
                    Text("device_detail.replaced_battery_description")
                        .font(.caption)
                        .opacity(0.8)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: ButlerIconSize.small))
            }
            .padding(ButlerSpacing.standard)
            .frame(maxWidth: .infinity, minHeight: 72)
            .background(Color.butlerPrimary)
            .foregroundStyle(.white)
            .clipShape(RoundedRectangle(cornerRadius: ButlerCornerRadius.large))
        }
        .accessibilityHint("device_detail.accessibility.replaced_battery_hint")
    }

    // MARK: - History Section

    private var historySection: some View {
        VStack(alignment: .leading, spacing: ButlerSpacing.small) {
            HStack {
                Text("device_detail.battery_history")
                    .font(.title2)
                    .bold()
                Spacer()
                Text("device_detail.view_all")
                    .font(.subheadline)
                    .foregroundStyle(Color.butlerPrimary)
            }

            if success.events.isEmpty {
                Text("device_detail.no_history")
                    .italic()
                    .foregroundStyle(Color.butlerOnSurfaceVariant)
            } else {
                ForEach(success.events, id: \.self) { event in
                    NavigationLink(destination: eventDestination(event.id)) {
                        historyRow(event: event)
                    }
                }
            }
        }
    }

    private func historyRow(event: BatteryEvent) -> some View {
        let epochMs = event.date.toEpochMilliseconds()
        let days = BatteryAgeHelper.daysSince(epochMilliseconds: epochMs)
        let ageColor = BatteryAgeHelper.batteryAgeColor(days: days)
        let fontWeight = BatteryAgeHelper.batteryAgeFontWeight(days: days)
        let dateString = Date(
            timeIntervalSince1970: TimeInterval(epochMs) / 1000.0
        ).formatted(date: .abbreviated, time: .shortened)

        return HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(dateString)
                    .foregroundStyle(Color.butlerOnSurface)
                if let days = days {
                    Text("\(days)d ago")
                        .font(.caption)
                        .fontWeight(fontWeight)
                        .foregroundStyle(ageColor)
                }
            }
            Spacer()
            Image(systemName: "chevron.right")
                .font(.caption)
                .foregroundStyle(Color.butlerOnSurfaceVariant)
        }
        .padding(ButlerSpacing.standard)
        .background(Color.butlerSurface)
        .clipShape(RoundedRectangle(cornerRadius: ButlerCornerRadius.medium))
        .overlay(
            RoundedRectangle(cornerRadius: ButlerCornerRadius.medium)
                .stroke(Color.butlerOutline.opacity(0.5), lineWidth: 1)
        )
    }
}

// MARK: - Stat Card Component

struct StatCardView: View {
    let systemName: String
    let label: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: ButlerSpacing.extraSmall) {
            HStack(spacing: ButlerSpacing.small) {
                ZStack {
                    RoundedRectangle(cornerRadius: 6)
                        .fill(Color.butlerPrimary.opacity(0.1))
                        .frame(width: 28, height: 28)
                    Image(systemName: systemName)
                        .font(.system(size: ButlerIconSize.small))
                        .foregroundStyle(Color.butlerPrimary)
                }
                Text(label)
                    .font(.caption2)
                    .fontWeight(.semibold)
                    .foregroundStyle(Color.butlerOnSurfaceVariant)
            }
            Text(value)
                .font(.title2)
                .bold()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(ButlerSpacing.standard)
        .background(Color.butlerSurface)
        .clipShape(RoundedRectangle(cornerRadius: ButlerCornerRadius.large))
        .overlay(
            RoundedRectangle(cornerRadius: ButlerCornerRadius.large)
                .stroke(Color.butlerOutline.opacity(0.5), lineWidth: 1)
        )
    }
}
