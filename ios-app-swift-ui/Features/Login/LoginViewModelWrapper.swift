import SwiftUI
import shared
import Combine

class LoginViewModelWrapper: ObservableObject {
    @Published var authState: AuthState? = nil
    
    let isSignInAvailable: Bool
    
    private let viewModel: LoginViewModel
    private let viewModelStore = KmpViewModelStore()
    private var authTask: Task<Void, Never>?
    
    init(_ viewModel: LoginViewModel) {
        self.viewModel = viewModel
        viewModelStore.put(key: "vm", viewModel: viewModel)
        
        self.isSignInAvailable = viewModel.isSignInAvailable
        self.authState = viewModel.authState.value as? AuthState
        
        self.authTask = Task { @MainActor [weak self] in
            for await state in viewModel.authState {
                self?.authState = state as? AuthState
            }
        }
    }
    
    deinit {
        authTask?.cancel()
        viewModelStore.clear()
    }
    
    func signInWithGoogle() {
        viewModel.signInWithGoogle()
    }
    
    func dismissError() {
        viewModel.dismissError()
    }
}
