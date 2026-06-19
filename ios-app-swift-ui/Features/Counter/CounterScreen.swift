import SwiftUI
import shared
import KMPObservableViewModelSwiftUI

struct CounterScreen: View {
    // bb-ovm1: @StateViewModel replaces CounterViewModelWrapper.
    @StateViewModel private var viewModel: CounterViewModel

    init(viewModel: CounterViewModel) {
        _viewModel = StateViewModel(wrappedValue: viewModel)
    }

    var body: some View {
        CounterContentView(
            counterRunning: viewModel.counterRunningValue,
            appCounterRunning: viewModel.appCounterRunningValue,
            observeState: viewModel.observeStateValue,
            getState: viewModel.getStateValue,
            onStartCounter: { viewModel.startCounter() },
            onStopCounter: { viewModel.stopCounter() },
            onStartAppCounter: { viewModel.startAppCounter() },
            onStopAppCounter: { viewModel.stopAppCounter() },
            onStartObserving: { viewModel.startObserving() },
            onStopObserving: { viewModel.stopObserving() },
            onGetOnce: { viewModel.getOnce() }
        )
    }
}

// bb-ovm1: Option A manual state accessors (no NativeCoroutines).
extension CounterViewModel {
    var counterRunningValue: Bool { (counterRunning.value as? Bool) ?? false }
    var appCounterRunningValue: Bool { (appCounterRunning.value as? Bool) ?? false }
    var observeStateValue: CounterState { observeState.value }
    var getStateValue: CounterState { getState.value }
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

#Preview("Idle") {
    CounterContentView(
        counterRunning: false,
        appCounterRunning: false,
        observeState: CounterState.Idle.shared,
        getState: CounterState.Idle.shared,
        onStartCounter: {},
        onStopCounter: {},
        onStartAppCounter: {},
        onStopAppCounter: {},
        onStartObserving: {},
        onStopObserving: {},
        onGetOnce: {}
    )
}

#Preview("Active") {
    CounterContentView(
        counterRunning: true,
        appCounterRunning: true,
        observeState: CounterState.Active(value: 42),
        getState: CounterState.Active(value: 7),
        onStartCounter: {},
        onStopCounter: {},
        onStartAppCounter: {},
        onStopAppCounter: {},
        onStartObserving: {},
        onStopObserving: {},
        onGetOnce: {}
    )
}

#Preview("Loading") {
    CounterContentView(
        counterRunning: true,
        appCounterRunning: true,
        observeState: CounterState.Loading.shared,
        getState: CounterState.Loading.shared,
        onStartCounter: {},
        onStopCounter: {},
        onStartAppCounter: {},
        onStopAppCounter: {},
        onStartObserving: {},
        onStopObserving: {},
        onGetOnce: {}
    )
}

#Preview("Error") {
    CounterContentView(
        counterRunning: false,
        appCounterRunning: false,
        observeState: CounterState.Error(message: "Failed to observe counter"),
        getState: CounterState.Error(message: "Failed to read counter"),
        onStartCounter: {},
        onStopCounter: {},
        onStartAppCounter: {},
        onStopAppCounter: {},
        onStartObserving: {},
        onStopObserving: {},
        onGetOnce: {}
    )
}
