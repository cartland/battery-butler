import SwiftUI
import shared
import Combine

class AiChatViewModelWrapper: ObservableObject {
    @Published var messages: [AiMessage] = []
    @Published var isProcessing: Bool = false
    
    private let viewModel: AiChatViewModel
    private let viewModelStore = KmpViewModelStore()
    
    private var messagesTask: Task<Void, Never>?
    private var processingTask: Task<Void, Never>?
    
    init(_ viewModel: AiChatViewModel) {
        self.viewModel = viewModel
        viewModelStore.put(key: "vm", viewModel: viewModel)
        
        self.messages = viewModel.messages.value as? [AiMessage] ?? []
        self.isProcessing = (viewModel.isProcessing.value as? Bool) ?? false
        
        self.messagesTask = Task { @MainActor [weak self] in
            for await msgs in viewModel.messages {
                self?.messages = msgs as? [AiMessage] ?? []
            }
        }
        
        self.processingTask = Task { @MainActor [weak self] in
            for await processing in viewModel.isProcessing {
                self?.isProcessing = processing.boolValue
            }
        }
    }
    
    deinit {
        messagesTask?.cancel()
        processingTask?.cancel()
        viewModelStore.clear()
    }
    
    func sendMessage(text: String) {
        viewModel.sendMessage(text: text, hints: [:])
    }
    
    func clearChat() {
        viewModel.clearChat()
    }
}
