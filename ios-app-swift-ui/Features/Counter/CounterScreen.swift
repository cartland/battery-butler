import SwiftUI
import shared

struct CounterScreen: View {
    @StateObject private var wrapper: CounterViewModelWrapper

    init(viewModel: CounterViewModel) {
        _wrapper = StateObject(wrappedValue: CounterViewModelWrapper(viewModel))
    }

    var body: some View {
        CounterContentView(
            observeState: wrapper.observeState,
            getState: wrapper.getState,
            onStart: { wrapper.start() },
            onStop: { wrapper.stop() },
            onGet: { wrapper.get() }
        )
    }
}

struct CounterContentView: View {
    let observeState: CounterState
    let getState: CounterState
    let onStart: () -> Void
    let onStop: () -> Void
    let onGet: () -> Void

    var body: some View {
        VStack(spacing: 24) {
            Text("Experimental Counter")
                .font(.title)

            Spacer().frame(height: 8)

            Text("Observe")
                .font(.title3)

            CounterValueDisplay(state: observeState)

            HStack(spacing: 16) {
                Button("Start", action: onStart)
                    .buttonStyle(.borderedProminent)
                Button("Stop", action: onStop)
                    .buttonStyle(.bordered)
            }

            Divider()

            Text("Get")
                .font(.title3)

            CounterValueDisplay(state: getState)

            Button("Get", action: onGet)
                .buttonStyle(.bordered)
        }
        .padding()
    }
}

private struct CounterValueDisplay: View {
    let state: CounterState

    var body: some View {
        if state is CounterStateIdle {
            Text("—")
                .font(.system(size: 64, weight: .bold, design: .rounded))
        } else if state is CounterStateLoading {
            ProgressView()
        } else if let active = state as? CounterStateActive {
            Text("\(active.value)")
                .font(.system(size: 64, weight: .bold, design: .rounded))
        } else if let error = state as? CounterStateError {
            Text(error.message)
                .foregroundStyle(.red)
        }
    }
}

#Preview("Idle") {
    CounterContentView(
        observeState: CounterStateIdle(),
        getState: CounterStateIdle(),
        onStart: {},
        onStop: {},
        onGet: {}
    )
}

#Preview("Active") {
    CounterContentView(
        observeState: CounterStateActive(value: 42),
        getState: CounterStateActive(value: 7),
        onStart: {},
        onStop: {},
        onGet: {}
    )
}

#Preview("Loading") {
    CounterContentView(
        observeState: CounterStateLoading(),
        getState: CounterStateLoading(),
        onStart: {},
        onStop: {},
        onGet: {}
    )
}

#Preview("Error") {
    CounterContentView(
        observeState: CounterStateError(message: "Failed to observe counter"),
        getState: CounterStateError(message: "Failed to read counter"),
        onStart: {},
        onStop: {},
        onGet: {}
    )
}
