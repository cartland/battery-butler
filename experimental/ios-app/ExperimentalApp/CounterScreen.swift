import SwiftUI
import ExperimentalShared

struct CounterScreen: View {
    @StateObject private var wrapper: CounterViewModelWrapper

    init(viewModel: CounterViewModel) {
        _wrapper = StateObject(wrappedValue: CounterViewModelWrapper(viewModel))
    }

    var body: some View {
        CounterContentView(
            counterRunning: wrapper.counterRunning,
            appCounterRunning: wrapper.appCounterRunning,
            observeState: wrapper.observeState,
            getState: wrapper.getState,
            onStartCounter: { wrapper.startCounter() },
            onStopCounter: { wrapper.stopCounter() },
            onStartAppCounter: { wrapper.startAppCounter() },
            onStopAppCounter: { wrapper.stopAppCounter() },
            onStartObserving: { wrapper.startObserving() },
            onStopObserving: { wrapper.stopObserving() },
            onGetOnce: { wrapper.getOnce() }
        )
        .navigationTitle("Counter")
    }
}

struct CounterContentView: View {
    let counterRunning: Bool
    let appCounterRunning: Bool
    let observeState: CounterState
    let getState: CounterState
    let onStartCounter: () -> Void
    let onStopCounter: () -> Void
    let onStartAppCounter: () -> Void
    let onStopAppCounter: () -> Void
    let onStartObserving: () -> Void
    let onStopObserving: () -> Void
    let onGetOnce: () -> Void

    var body: some View {
        VStack(spacing: 24) {
            Text("Experimental Counter")
                .font(.title)

            Spacer().frame(height: 8)

            Text("VM Counter")
                .font(.title3)

            Text(counterRunning ? "Running" : "Stopped")

            HStack(spacing: 16) {
                Button("Start", action: onStartCounter)
                    .buttonStyle(.borderedProminent)
                Button("Stop", action: onStopCounter)
                    .buttonStyle(.bordered)
            }

            Divider()

            Text("App Counter")
                .font(.title3)

            Text(appCounterRunning ? "Running" : "Stopped")

            HStack(spacing: 16) {
                Button("Start", action: onStartAppCounter)
                    .buttonStyle(.borderedProminent)
                Button("Stop", action: onStopAppCounter)
                    .buttonStyle(.bordered)
            }

            Divider()

            Text("Observe")
                .font(.title3)

            CounterValueDisplay(state: observeState)

            HStack(spacing: 16) {
                Button("Start", action: onStartObserving)
                    .buttonStyle(.borderedProminent)
                Button("Stop", action: onStopObserving)
                    .buttonStyle(.bordered)
            }

            Divider()

            Text("Get")
                .font(.title3)

            CounterValueDisplay(state: getState)

            Button("Get Once", action: onGetOnce)
                .buttonStyle(.bordered)
        }
        .padding()
    }
}

private struct CounterValueDisplay: View {
    let state: CounterState

    var body: some View {
        if state is CounterState.Idle {
            Text("—")
                .font(.system(size: 64, weight: .bold, design: .rounded))
        } else if state is CounterState.Loading {
            ProgressView()
        } else if let active = state as? CounterState.Active {
            Text("\(active.value)")
                .font(.system(size: 64, weight: .bold, design: .rounded))
        } else if let error = state as? CounterState.Error {
            Text(error.message)
                .foregroundStyle(.red)
        }
    }
}
