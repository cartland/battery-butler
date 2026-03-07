import SwiftUI
import shared

struct LoginScreen: View {
    @StateObject private var wrapper: LoginViewModelWrapper

    let onLoginSuccess: () -> Void
    let onSkipLogin: () -> Void

    init(viewModel: LoginViewModel, onLoginSuccess: @escaping () -> Void, onSkipLogin: @escaping () -> Void) {
        _wrapper = StateObject(wrappedValue: LoginViewModelWrapper(viewModel))
        self.onLoginSuccess = onLoginSuccess
        self.onSkipLogin = onSkipLogin
    }

    var body: some View {
        LoginContentView(
            authState: wrapper.authState,
            isSignInAvailable: wrapper.isSignInAvailable,
            errorTitle: wrapper.errorTitle,
            errorMessage: wrapper.errorMessage,
            showRetryButton: wrapper.showRetryButton,
            showError: Binding(
                get: { wrapper.authState is AuthStateFailed },
                set: { _ in wrapper.dismissError() }
            ),
            onSignIn: { wrapper.signInWithGoogle() },
            onSkipLogin: onSkipLogin,
            onRetry: {
                wrapper.dismissError()
                wrapper.signInWithGoogle()
            },
            onDismissError: { wrapper.dismissError() }
        )
        .onAppear {
            if wrapper.authState is AuthStateAuthenticated {
                onLoginSuccess()
            }
        }
        .onReceive(wrapper.$authState) { newState in
            if newState is AuthStateAuthenticated {
                onLoginSuccess()
            }
        }
    }
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

            Text("Battery Butler")
                .font(.largeTitle)
                .fontWeight(.bold)

            Text("Manage your devices and battery replacements securely across all platforms.")
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
                .padding(.horizontal)

            Spacer()

            if authState is AuthStateAuthenticating {
                ProgressView("Signing in...")
            } else {
                if isSignInAvailable {
                    Button(action: {
                        onSignIn()
                    }) {
                        HStack {
                            Image(systemName: "person.crop.circle.badge.plus")
                                .accessibilityHidden(true)
                            Text("Sign in with Google")
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
                    Text("Skip for now")
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
                Button("Try Again") {
                    onRetry()
                }
            }
            Button("OK", role: .cancel) {
                onDismissError()
            }
        } message: {
            Text(errorMessage)
        }
    }
}
