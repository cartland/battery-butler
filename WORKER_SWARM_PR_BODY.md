# CoroutineConductor Audit - Standardizing Structured Concurrency

## WHAT
Standardized coroutine execution and scope creation across the project by favoring `DispatcherProvider` injection over hardcoded `Dispatchers`.

## WHY
Direct usage of `Dispatchers.IO` or `Dispatchers.Default` makes unit testing difficult, as tests cannot easily override these dispatchers with `TestDispatcher`. By injecting a `DispatcherProvider`, we ensure that all layers of the application are testable and follow structured concurrency principles.

## HOW
1. **DI Layer Standardization**:
    - Updated `AppComponent` and `NativeComponent` to inject `DispatcherProvider` when creating `appScope`.
    - Updated `DelegatingGrpcClient` to take `DispatcherProvider` and propagate it to platform-specific client factories.
2. **Platform Bridge Refactoring**:
    - Refactored `GoogleSignInBridge` to accept `DispatcherProvider` in its `initialize` method across Android, iOS, and Desktop.
    - Updated `GoogleSignInBridge.desktop.kt` to use the injected `io` dispatcher for network authentication flows.
3. **Network Component Refactoring**:
    - Updated `NetworkComponent` and its platform-specific implementations to use injected dispatchers.
    - Specifically refactored `IosGrpcCall` and `IosGrpcStreamingCall` in the `iosMain` source set to use `dispatcherProvider.default` instead of hardcoded `Dispatchers.Default`.
4. **Call Site Updates**:
    - Updated all entry points (`Main.kt`, `BatteryButlerApplication.kt`, `IosComponentHelper.kt`) to provide the appropriate `DispatcherProvider`.
