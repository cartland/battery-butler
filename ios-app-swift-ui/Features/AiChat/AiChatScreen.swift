import SwiftUI
import shared
import KMPObservableViewModelSwiftUI

struct AiChatScreen: View {
    // bb-ovm1: @StateViewModel replaces AiChatViewModelWrapper.
    @StateViewModel private var viewModel: AiChatViewModel
    @State private var inputText: String = ""

    init(viewModel: AiChatViewModel) {
        _viewModel = StateViewModel(wrappedValue: viewModel)
    }

    var body: some View {
        AiChatContentView(
            messages: viewModel.messagesValue,
            isProcessing: viewModel.isProcessingValue,
            inputText: $inputText,
            onSend: {
                let text = inputText.trimmingCharacters(in: .whitespacesAndNewlines)
                guard !text.isEmpty else { return }
                viewModel.sendMessage(text: text, hints: [:])
                inputText = ""
            },
            onClear: { viewModel.clearChat() }
        )
    }
}

// bb-ovm1: Option A manual state accessors (no NativeCoroutines).
extension AiChatViewModel {
    var messagesValue: [AiMessage] { messages.value }
    var isProcessingValue: Bool { (isProcessing.value as? Bool) ?? false }
}

struct AiChatContentView: View {
    let messages: [AiMessage]
    let isProcessing: Bool
    @Binding var inputText: String
    let onSend: () -> Void
    let onClear: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                LazyVStack(spacing: ButlerSpacing.medium) {
                    ForEach(messages, id: \.id) { message in
                        MessageRow(message: message)
                    }
                    if isProcessing {
                        HStack {
                            ProgressView()
                                .padding()
                            Spacer()
                        }
                    }
                }
                .padding()
            }

            Divider()

            HStack {
                TextField("ai_chat.field.message", text: $inputText)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .disabled(isProcessing)

                Button(action: onSend) {
                    Image(systemName: "paperplane.fill")
                        .foregroundStyle(inputText.isEmpty || isProcessing ? Color.gray : Color.butlerPrimary)
                }
                .accessibilityLabel("ai_chat.accessibility.send")
                .disabled(inputText.isEmpty || isProcessing)
            }
            .padding()
        }
        .navigationTitle("ai_chat.title")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: onClear) {
                    Image(systemName: "trash")
                }
                .accessibilityLabel("ai_chat.accessibility.clear")
            }
        }
    }
}

struct MessageRow: View {
    let message: AiMessage

    private var isUser: Bool {
        message.role == .user
    }

    var body: some View {
        HStack {
            if isUser { Spacer() }

            Text(message.text)
                .padding(ButlerSpacing.medium)
                .background(isUser ? Color.butlerPrimaryContainer : Color.butlerTertiaryContainer)
                .foregroundStyle(isUser ? Color.butlerOnPrimaryContainer : Color.butlerOnTertiaryContainer)
                .clipShape(ChatBubbleShape(isUser: isUser))

            if !isUser { Spacer() }
        }
    }
}

struct ChatBubbleShape: Shape {
    let isUser: Bool

    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: [
                .topLeft,
                .topRight,
                isUser ? .bottomLeft : .bottomRight,
            ],
            cornerRadii: CGSize(width: ButlerCornerRadius.large, height: ButlerCornerRadius.large)
        )
        return Path(path.cgPath)
    }
}
