import KMPObservableViewModelCore
import shared

// bb-ovm1 spike: bridge KMP-ObservableViewModel into the SwiftUI app (Option A — SKIE
// kept, no KMP-NativeCoroutines).
//
// SKIE exports the rickclephas ViewModel base class as `shared.ViewModel`. This one-time
// retroactive conformance makes it adopt KMP-ObservableViewModel's Swift `ViewModel`
// protocol so `@StateViewModel` / `@ObservedViewModel` can own and observe it. The
// protocol requirements (`viewModelScope`, `viewModelWillChange`, `clear()`) are already
// satisfied by the exported Kotlin base class + the vendored KMPObservableViewModelCoreObjC.
extension shared.ViewModel: @retroactive KMPObservableViewModelCore.ViewModel { }

// Option A state accessor: without `@NativeCoroutinesState` (which would require
// KMP-NativeCoroutines) we expose the StateFlow's current value by hand. The view re-reads
// this whenever `@ObservedViewModel` republishes — which happens because `uiState` is built
// with KMP-ObservableViewModel's observable `stateIn(viewModelScope, …)` (see
// DeviceDetailViewModel / safeStateIn).
extension DeviceDetailViewModel {
    var uiStateValue: DeviceDetailScreenState { uiState.value }
}
