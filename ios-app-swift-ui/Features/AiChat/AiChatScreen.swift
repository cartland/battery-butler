import SwiftUI
import shared

struct AiChatScreen: View {
    @StateObject private var wrapper: AiChatViewModelWrapper
    @State private var inputText: String = ""
    
    init(viewModel: AiChatViewModel) {
        _wrapper = StateObject(wrappedValue: AiChatViewModelWrapper(viewModel))
    }
    
    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                LazyVStack(spacing: 12) {
                    ForEach(wrapper.messages, id: \.id) { message in
                        MessageRow(message: message)
                    }
                    if wrapper.isProcessing {
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
                    .disabled(wrapper.isProcessing)
                
                Button(action: {
                    let text = inputText.trimmingCharacters(in: .whitespacesAndNewlines)
                    guard !text.isEmpty else { return }
                    wrapper.sendMessage(text: text)
                    inputText = ""
                }) {
                    Image(systemName: "paperplane.fill")
                        .foregroundColor(inputText.isEmpty || wrapper.isProcessing ? .gray : .blue)
                }
                .accessibilityLabel("Send message")
                .disabled(inputText.isEmpty || wrapper.isProcessing)
            }
            .padding()
        }
        .navigationTitle("AI Butler")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: {
                    wrapper.clearChat()
                }) {
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
