import SwiftUI
import shared
import KMPObservableViewModelSwiftUI

struct LoginScreen: View {
    // bb-ovm1: @StateViewModel replaces LoginViewModelWrapper. Error fields are derived
    // from authState via the relocated LoginErrorInfo helper.
    @StateViewModel private var viewModel: LoginViewModel

    let onLoginSuccess: () -> Void
    let onSkipLogin: () -> Void

    init(viewModel: LoginViewModel, onLoginSuccess: @escaping () -> Void, onSkipLogin: @escaping () -> Void) {
        _viewModel = StateViewModel(wrappedValue: viewModel)
        self.onLoginSuccess = onLoginSuccess
        self.onSkipLogin = onSkipLogin
    }

    var body: some View {
        let authState = viewModel.authStateValue
        let errorInfo = (authState as? AuthStateFailed).map { LoginErrorInfo.errorInfo(for: $0.error) }

        return LoginContentView(
            authState: authState,
            isSignInAvailable: viewModel.isSignInAvailable,
            errorTitle: errorInfo?.0 ?? "",
            errorMessage: errorInfo?.1 ?? "",
            showRetryButton: errorInfo?.2 ?? true,
            showError: Binding(
                get: { authState is AuthStateFailed },
                set: { _ in viewModel.dismissError() }
            ),
            onSignIn: { viewModel.signInWithGoogle() },
            onSkipLogin: onSkipLogin,
            onRetry: {
                viewModel.dismissError()
                viewModel.signInWithGoogle()
            },
            onDismissError: { viewModel.dismissError() }
        )
        .onAppear {
            if authState is AuthStateAuthenticated {
                onLoginSuccess()
            }
        }
        .onChange(of: authState is AuthStateAuthenticated) { _, isAuthenticated in
            if isAuthenticated {
                onLoginSuccess()
            }
        }
    }
}

// bb-ovm1: Option A manual state accessor (no NativeCoroutines).
extension LoginViewModel {
    var authStateValue: AuthState { authState.value }
}

struct LoginContentView: View {
    let authState: AuthState?
    let isSignInAvailable: Bool
    let errorTitle: String
    let errorMessage: String
    let showRetryButton: Bool
    @Binding var showError: Bool
    let onSignIn: () -> Void
    let onSkipLogin: () -> Void
    let onRetry: () -> Void
    let onDismissError: () -> Void

    var body: some View {
        VStack(spacing: ButlerSpacing.large) {
            Image(systemName: "bolt.batteryblock.fill")
                .resizable()
                .scaledToFit()
                .accessibilityHidden(true)
                .frame(width: 100, height: 100)
                .foregroundStyle(Color.butlerPrimary)
                .padding(.top, 40)

            Text("login.app_name")
                .font(.largeTitle)
                .fontWeight(.bold)

            Text("login.subtitle")
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
                .padding(.horizontal)

            Spacer()

            if authState is AuthStateAuthenticating {
                ProgressView("login.signing_in")
            } else {
                if isSignInAvailable {
                    Button(action: {
                        onSignIn()
                    }) {
                        HStack {
                            Image(systemName: "person.crop.circle.badge.plus")
                                .accessibilityHidden(true)
                            Text("login.button.sign_in")
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.butlerPrimary)
                        .foregroundStyle(.white)
                        .clipShape(RoundedRectangle(cornerRadius: ButlerCornerRadius.medium))
                    }
                }

                Button(action: {
                    onSkipLogin()
                }) {
                    Text("login.button.skip")
                        .frame(maxWidth: .infinity)
                        .padding()
                        .foregroundStyle(Color.butlerPrimary)
                }
            }

            Spacer()
        }
        .padding()
        .alert(errorTitle, isPresented: $showError) {
            if showRetryButton {
                Button("common.try_again") {
                    onRetry()
                }
            }
            Button("common.ok", role: .cancel) {
                onDismissError()
            }
        } message: {
            Text(errorMessage)
        }
    }
}
