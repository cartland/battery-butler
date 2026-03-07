import SwiftUI

struct ButlerIconBox: View {
    let systemName: String
    var containerColor: Color = .butlerPrimaryContainer
    var contentColor: Color = .butlerOnPrimaryContainer

    var body: some View {
        Image(systemName: systemName)
            .font(.system(size: ButlerIconSize.medium))
            .foregroundStyle(contentColor)
            .frame(width: 44, height: 44)
            .background(containerColor, in: RoundedRectangle(cornerRadius: ButlerCornerRadius.small))
    }
}
