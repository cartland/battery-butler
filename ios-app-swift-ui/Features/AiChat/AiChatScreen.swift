import SwiftUI
import shared

struct AiChatScreen: View {
    @StateObject private var wrapper: AiChatViewModelWrapper
    @State private var inputText: String = ""
    
    init(viewModel: AiChatViewModel) {
        _wrapper = StateObject(wrappedValue: AiChatViewModelWrapper(viewModel))
    }
    
    var body: some View {
        AiChatContentView(
            messages: wrapper.messages,
            isProcessing: wrapper.isProcessing,
            inputText: $inputText,
            onSend: {
                let text = inputText.trimmingCharacters(in: .whitespacesAndNewlines)
                guard !text.isEmpty else { return }
                wrapper.sendMessage(text: text)
                inputText = ""
            },
            onClear: { wrapper.clearChat() }
        )
    }
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
                LazyVStack(spacing: 12) {
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
                TextField("Message", text: $inputText)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .disabled(isProcessing)

                Button(action: onSend) {
                    Image(systemName: "paperplane.fill")
                        .foregroundColor(inputText.isEmpty || isProcessing ? .gray : .blue)
                }
                .accessibilityLabel("Send message")
                .disabled(inputText.isEmpty || isProcessing)
            }
            .padding()
        }
        .navigationTitle("AI Butler")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: onClear) {
                    Image(systemName: "trash")
                }
                .accessibilityLabel("Clear chat")
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
                .padding(12)
                .background(isUser ? Color.blue : Color(UIColor.systemGray5))
                .foregroundColor(isUser ? .white : .primary)
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
                isUser ? .bottomLeft : .bottomRight
            ],
            cornerRadii: CGSize(width: 16, height: 16)
        )
        return Path(path.cgPath)
    }
}
