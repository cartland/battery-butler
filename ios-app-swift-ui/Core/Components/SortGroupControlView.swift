import SwiftUI

/// A composite control that displays a sort/group label with a dropdown chevron
/// and an optional direction toggle (ascending/descending arrow).
///
/// This mirrors the Compose `CompositeControl` component used in the Android/Desktop UI.
struct SortGroupControlView: View {
    let label: String
    let isActive: Bool
    let isAscending: Bool
    let onTapped: () -> Void
    let onDirectionToggle: () -> Void

    var body: some View {
        HStack(spacing: 0) {
            if isActive {
                // Direction toggle button (left side)
                Button(action: onDirectionToggle) {
                    Image(systemName: isAscending ? "arrow.down" : "arrow.up")
                        .font(.caption)
                        .foregroundStyle(Color.butlerOnSurface)
                        .frame(width: 32, height: 32)
                }
                .accessibilityLabel(
                    isAscending
                        ? Text("sort.accessibility.ascending")
                        : Text("sort.accessibility.descending")
                )

                // Vertical separator
                Rectangle()
                    .fill(Color.butlerOutline)
                    .frame(width: 1, height: 32)
            }

            // Main label + dropdown chevron (right side)
            Button(action: onTapped) {
                HStack(spacing: 4) {
                    Text(label)
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .foregroundStyle(Color.butlerOnSurface)

                    Image(systemName: "chevron.down")
                        .font(.caption2)
                        .foregroundStyle(Color.butlerOnSurface)
                }
                .padding(.horizontal, ButlerSpacing.medium)
                .frame(height: 32)
            }
        }
        .overlay(
            RoundedRectangle(cornerRadius: ButlerCornerRadius.small)
                .stroke(Color.butlerOutline, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: ButlerCornerRadius.small))
    }
}
