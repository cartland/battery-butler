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
        VStack(spacing: 24) {
            Image(systemName: "bolt.batteryblock.fill")
                .resizable()
                .scaledToFit()
                .accessibilityHidden(true)
                .frame(width: 100, height: 100)
                .foregroundStyle(.blue)
                .padding(.top, 40)

            Text("Battery Butler")
                .font(.largeTitle)
                .fontWeight(.bold)

            Text("Manage your devices and battery replacements securely across all platforms.")
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
                .padding(.horizontal)

            Spacer()

            if wrapper.authState is AuthStateAuthenticating {
                ProgressView("Signing in...")
            } else {
                if wrapper.isSignInAvailable {
                    Button(action: {
                        wrapper.signInWithGoogle()
                    }) {
                        HStack {
                            Image(systemName: "person.crop.circle.badge.plus")
                                .accessibilityHidden(true)
                            Text("Sign in with Google")
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.blue)
                        .foregroundStyle(.white)
                        .cornerRadius(12)
                    }
                }

                Button(action: {
                    onSkipLogin()
                }) {
                    Text("Skip for now")
                        .frame(maxWidth: .infinity)
                        .padding()
                        .foregroundStyle(.blue)
                }
            }

            Spacer()
        }
        .padding()
        // If the state initializes to Authenticated from cache/storage
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
        .alert(wrapper.errorTitle, isPresented: Binding(
            get: { wrapper.authState is AuthStateFailed },
            set: { _ in wrapper.dismissError() }
        )) {
            if wrapper.showRetryButton {
                Button("Try Again") {
                    wrapper.dismissError()
                    wrapper.signInWithGoogle()
                }
            }
            Button("OK", role: .cancel) {
                wrapper.dismissError()
            }
        } message: {
            Text(wrapper.errorMessage)
        }
    }
}
