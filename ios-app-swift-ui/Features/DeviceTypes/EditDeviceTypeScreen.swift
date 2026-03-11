import SwiftUI
import shared

struct EditDeviceTypeScreen: View {
    @StateObject var viewModelWrapper: EditDeviceTypeViewModelWrapper
    @Environment(\.dismiss) private var dismiss
    @State private var showDeleteConfirmation = false

    init(factory: EditDeviceTypeViewModelFactory, typeId: String) {
        _viewModelWrapper = StateObject(wrappedValue: EditDeviceTypeViewModelWrapper(factory.create(typeId: typeId)))
    }

    var body: some View {
        EditDeviceTypeContentView(
            state: viewModelWrapper.state,
            onUpdateName: { viewModelWrapper.updateName(name: $0) },
            onUpdateBatteryType: { viewModelWrapper.updateBatteryType(type: $0) },
            onSelectIcon: { viewModelWrapper.selectIcon(icon: $0) },
            onIncrementQuantity: { viewModelWrapper.incrementBatteryQuantity() },
            onDecrementQuantity: { viewModelWrapper.decrementBatteryQuantity() },
            onSave: { viewModelWrapper.save() },
            onDelete: {
                viewModelWrapper.delete()
                dismiss()
            }
        )
        .onChange(of: viewModelWrapper.state.isSaved) { _, isSaved in
            if isSaved {
                viewModelWrapper.consumeSaveSuccess()
                dismiss()
            }
        }
    }
}

struct EditDeviceTypeContentView: View {
    let state: EditDeviceTypeState
    let onUpdateName: (String) -> Void
    let onUpdateBatteryType: (String) -> Void
    let onSelectIcon: (String) -> Void
    let onIncrementQuantity: () -> Void
    let onDecrementQuantity: () -> Void
    let onSave: () -> Void
    let onDelete: () -> Void
    @State private var showDeleteConfirmation = false

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
        Group {
            if state.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if state.isNotFound {
                Text("edit_device_type.not_found")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
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

                        // Delete button
                        Button {
                            showDeleteConfirmation = true
                        } label: {
                            HStack {
                                Spacer()
                                Text("edit_device_type.button.delete")
                                    .fontWeight(.medium)
                                Spacer()
                            }
                            .padding(.vertical, ButlerSpacing.medium)
                        }
                        .foregroundStyle(Color.butlerError)
                    }
                    .padding(.horizontal, ButlerSpacing.standard)
                    .padding(.vertical, ButlerSpacing.standard)
                }
                .background(Color.butlerBackground)
            }
        }
        .navigationTitle("edit_device_type.title")
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("common.save") {
                    onSave()
                }
                .disabled(state.isLoading || state.isNotFound || state.isSaving || state.name.isEmpty)
                .fontWeight(.bold)
            }
        }
        .disabled(state.isSaving)
        .overlay {
            if state.isSaving {
                ProgressView()
            }
        }
        .alert("edit_device_type.alert.delete_title", isPresented: $showDeleteConfirmation) {
            Button("common.delete", role: .destructive) {
                onDelete()
            }
            Button("common.cancel", role: .cancel) {}
        } message: {
            Text("common.action_cannot_be_undone")
        }
    }

    // MARK: - Icon Picker

    private var iconPickerSection: some View {
        VStack(alignment: .leading, spacing: ButlerSpacing.standard) {
            Text("edit_device_type.section.icon")
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
            Text("edit_device_type.section.details")
                .font(.title3)
                .fontWeight(.bold)

            // Name field
            nameField

            // Battery type field
            batteryTypeField

            // Battery quantity controls
            batteryQuantityControl
        }
    }

    private var nameField: some View {
        VStack(alignment: .leading, spacing: ButlerSpacing.small) {
            Text("edit_device_type.field.name_label")
                .font(.subheadline)
                .fontWeight(.medium)

            TextField("edit_device_type.field.name", text: Binding(
                get: { state.name },
                set: { onUpdateName($0) }
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

    private var batteryTypeField: some View {
        VStack(alignment: .leading, spacing: ButlerSpacing.small) {
            Text("edit_device_type.field.battery_type_label")
                .font(.subheadline)
                .fontWeight(.medium)

            TextField("edit_device_type.field.battery_type", text: Binding(
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
            Text("edit_device_type.field.battery_quantity_label")
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

                    Text("edit_device_type.field.batteries_needed")
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
