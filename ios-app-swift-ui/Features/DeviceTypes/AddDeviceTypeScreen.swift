import SwiftUI
import shared

struct AddDeviceTypeScreen: View {
    @StateObject var viewModelWrapper: AddDeviceTypeViewModelWrapper
    @Environment(\.dismiss) private var dismiss

    init(viewModel: AddDeviceTypeViewModel) {
        _viewModelWrapper = StateObject(wrappedValue: AddDeviceTypeViewModelWrapper(viewModel))
    }

    var body: some View {
        AddDeviceTypeContentView(
            state: viewModelWrapper.state,
            onUpdateName: { viewModelWrapper.updateName(name: $0) },
            onUpdateBatteryType: { viewModelWrapper.updateBatteryType(type: $0) },
            onSelectIcon: { viewModelWrapper.selectIcon(icon: $0) },
            onIncrementQuantity: { viewModelWrapper.incrementBatteryQuantity() },
            onDecrementQuantity: { viewModelWrapper.decrementBatteryQuantity() },
            onSave: { viewModelWrapper.save() },
            onCancel: { dismiss() }
        )
        .onChange(of: viewModelWrapper.state.isSaved) { _, isSaved in
            if isSaved {
                viewModelWrapper.consumeSaveSuccess()
                dismiss()
            }
        }
    }
}

struct AddDeviceTypeContentView: View {
    let state: AddDeviceTypeState
    let onUpdateName: (String) -> Void
    let onUpdateBatteryType: (String) -> Void
    let onSelectIcon: (String) -> Void
    let onIncrementQuantity: () -> Void
    let onDecrementQuantity: () -> Void
    let onSave: () -> Void
    let onCancel: () -> Void

    private var sortedIcons: [String] {
        let usedSet = Set(state.usedIcons)
        return SFSymbolMapper.availableIcons.sorted { first, second in
            let firstUsed = usedSet.contains(first)
            let secondUsed = usedSet.contains(second)
            if firstUsed != secondUsed {
                return firstUsed
            }
            return false
        }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: ButlerSpacing.large) {
                    // Icon picker section
                    iconPickerSection

                    Divider()

                    // Device details section
                    deviceDetailsSection

                    if let error = state.saveError {
                        Text(error)
                            .font(.body)
                            .foregroundStyle(Color.butlerError)
                    }
                }
                .padding(.horizontal, ButlerSpacing.standard)
                .padding(.vertical, ButlerSpacing.standard)
            }
            .background(Color.butlerBackground)
            .navigationTitle("add_device_type.title")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("common.cancel") {
                        onCancel()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("common.save") {
                        onSave()
                    }
                    .disabled(state.isSaving || state.name.isEmpty)
                    .fontWeight(.bold)
                }
            }
            .disabled(state.isSaving)
            .overlay {
                if state.isSaving {
                    ProgressView()
                }
            }
        }
    }

    // MARK: - Icon Picker

    private var iconPickerSection: some View {
        VStack(alignment: .leading, spacing: ButlerSpacing.standard) {
            Text("add_device_type.section.icon")
                .font(.title3)
                .fontWeight(.bold)

            LazyVGrid(
                columns: Array(
                    repeating: GridItem(.flexible(), spacing: ButlerSpacing.medium),
                    count: 4
                ),
                spacing: ButlerSpacing.medium
            ) {
                ForEach(sortedIcons, id: \.self) { iconName in
                    iconCell(iconName: iconName)
                }
            }
            .frame(maxHeight: 160)
            .clipped()
        }
    }

    private func iconCell(iconName: String) -> some View {
        let isSelected = state.selectedIcon == iconName
        let sfSymbol = SFSymbolMapper.sfSymbolName(for: iconName)

        return Button {
            onSelectIcon(iconName)
        } label: {
            VStack(spacing: ButlerSpacing.extraSmall) {
                ZStack {
                    Circle()
                        .fill(isSelected ? Color.butlerPrimary : Color.butlerPrimaryContainer)
                        .frame(width: 52, height: 52)

                    if isSelected {
                        Circle()
                            .strokeBorder(Color.butlerPrimary, lineWidth: 2)
                            .frame(width: 52, height: 52)
                    }

                    Image(systemName: sfSymbol)
                        .font(.system(size: ButlerIconSize.medium))
                        .foregroundStyle(
                            isSelected ? Color.butlerOnPrimary : Color.butlerOnPrimaryContainer
                        )
                }

                Text(SFSymbolMapper.displayName(for: iconName))
                    .font(.caption2)
                    .foregroundStyle(Color.butlerOnSurface)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
            }
        }
        .buttonStyle(.plain)
        .accessibilityLabel(SFSymbolMapper.displayName(for: iconName))
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }

    // MARK: - Device Details

    private var deviceDetailsSection: some View {
        VStack(alignment: .leading, spacing: ButlerSpacing.standard) {
            Text("add_device_type.section.details")
                .font(.title3)
                .fontWeight(.bold)

            // Name field with AI suggestion
            nameField

            // Battery type field
            batteryTypeField

            // Battery quantity controls
            batteryQuantityControl
        }
    }

    private var nameField: some View {
        VStack(alignment: .leading, spacing: ButlerSpacing.small) {
            Text("add_device_type.field.name_label")
                .font(.subheadline)
                .fontWeight(.medium)

            HStack {
                TextField("add_device_type.field.name", text: Binding(
                    get: { state.name },
                    set: { onUpdateName($0) }
                ))

                if !state.name.isEmpty {
                    if state.isSuggestingIcon {
                        ProgressView()
                            .controlSize(.small)
                    } else {
                        Image(systemName: "sparkles")
                            .foregroundStyle(Color.butlerPrimary)
                            .font(.system(size: ButlerIconSize.medium))
                    }
                }
            }
            .padding(ButlerSpacing.medium)
            .background(Color.butlerSurface)
            .clipShape(RoundedRectangle(cornerRadius: ButlerCornerRadius.medium))
            .overlay(
                RoundedRectangle(cornerRadius: ButlerCornerRadius.medium)
                    .strokeBorder(Color.butlerOutline, lineWidth: 1)
            )
        }
    }

    private var batteryTypeField: some View {
        VStack(alignment: .leading, spacing: ButlerSpacing.small) {
            Text("add_device_type.field.battery_type_label")
                .font(.subheadline)
                .fontWeight(.medium)

            TextField("add_device_type.field.battery_type", text: Binding(
                get: { state.batteryType },
                set: { onUpdateBatteryType($0) }
            ))
            .padding(ButlerSpacing.medium)
            .background(Color.butlerSurface)
            .clipShape(RoundedRectangle(cornerRadius: ButlerCornerRadius.medium))
            .overlay(
                RoundedRectangle(cornerRadius: ButlerCornerRadius.medium)
                    .strokeBorder(Color.butlerOutline, lineWidth: 1)
            )
        }
    }

    private var batteryQuantityControl: some View {
        VStack(alignment: .leading, spacing: ButlerSpacing.small) {
            Text("add_device_type.field.battery_quantity_label")
                .font(.subheadline)
                .fontWeight(.medium)

            HStack {
                // Battery icon and label
                HStack(spacing: ButlerSpacing.medium) {
                    ZStack {
                        RoundedRectangle(cornerRadius: ButlerCornerRadius.small)
                            .fill(Color.butlerSurfaceVariant)
                            .frame(width: 40, height: 40)

                        Image(systemName: "battery.100")
                            .foregroundStyle(Color.butlerOnSurfaceVariant)
                    }

                    Text("add_device_type.field.batteries_needed")
                        .font(.body)
                        .foregroundStyle(Color.butlerOnSurfaceVariant)
                }

                Spacer()

                // Stepper controls
                HStack(spacing: ButlerSpacing.standard) {
                    Button {
                        onDecrementQuantity()
                    } label: {
                        Image(systemName: "minus")
                            .font(.system(size: ButlerIconSize.small, weight: .bold))
                            .foregroundStyle(Color.butlerOnSurfaceVariant)
                            .frame(width: 40, height: 40)
                            .background(Color.butlerSurfaceVariant)
                            .clipShape(RoundedRectangle(cornerRadius: ButlerCornerRadius.small))
                    }
                    .disabled(state.batteryQuantity <= 1)

                    Text("\(state.batteryQuantity)")
                        .font(.title2)
                        .fontWeight(.bold)
                        .frame(minWidth: 24)

                    Button {
                        onIncrementQuantity()
                    } label: {
                        Image(systemName: "plus")
                            .font(.system(size: ButlerIconSize.small, weight: .bold))
                            .foregroundStyle(Color.butlerOnPrimary)
                            .frame(width: 40, height: 40)
                            .background(Color.butlerPrimary)
                            .clipShape(RoundedRectangle(cornerRadius: ButlerCornerRadius.small))
                    }
                }
            }
            .padding(.horizontal, ButlerSpacing.standard)
            .padding(.vertical, ButlerSpacing.medium)
            .overlay(
                RoundedRectangle(cornerRadius: ButlerCornerRadius.medium)
                    .strokeBorder(Color.butlerOutline, lineWidth: 1)
            )
        }
    }
}
