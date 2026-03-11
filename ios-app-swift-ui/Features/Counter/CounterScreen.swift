import SwiftUI
import shared

struct CounterScreen: View {
    @StateObject private var wrapper: CounterViewModelWrapper

    init(viewModel: CounterViewModel) {
        _wrapper = StateObject(wrappedValue: CounterViewModelWrapper(viewModel))
    }

    var body: some View {
        CounterContentView(
            state: wrapper.state,
            onStart: { wrapper.start() },
            onGet: { wrapper.get() }
        )
    }
}

struct CounterContentView: View {
    let state: CounterState
    let onStart: () -> Void
    let onGet: () -> Void

    var body: some View {
        VStack(spacing: 24) {
            Text("Experimental Counter")
                .font(.title)

            Spacer().frame(height: 16)

            if state is CounterStateIdle {
                Text("Press Start to begin counting")
                    .foregroundStyle(.secondary)
            } else if state is CounterStateLoading {
                ProgressView()
            } else if let active = state as? CounterStateActive {
                Text("\(active.value)")
                    .font(.system(size: 64, weight: .bold, design: .rounded))
            } else if let error = state as? CounterStateError {
                Text(error.message)
                    .foregroundStyle(.red)
            }

            Spacer().frame(height: 16)

            HStack(spacing: 16) {
                Button("Start", action: onStart)
                    .buttonStyle(.borderedProminent)
                Button("Get", action: onGet)
                    .buttonStyle(.bordered)
            }
        }
        .padding()
    }
}

#Preview("Idle") {
    CounterContentView(
        state: CounterStateIdle(),
        onStart: {},
        onGet: {}
    )
}

#Preview("Active") {
    CounterContentView(
        state: CounterStateActive(value: 42),
        onStart: {},
        onGet: {}
    )
}

#Preview("Loading") {
    CounterContentView(
        state: CounterStateLoading(),
        onStart: {},
        onGet: {}
    )
}

#Preview("Error") {
    CounterContentView(
        state: CounterStateError(message: "Failed to read counter"),
        onStart: {},
        onGet: {}
    )
}
