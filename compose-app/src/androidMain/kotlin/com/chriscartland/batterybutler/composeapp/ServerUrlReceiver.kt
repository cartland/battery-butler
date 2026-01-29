package com.chriscartland.batterybutler.composeapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import co.touchlab.kermit.Logger
import com.chriscartland.batterybutler.BatteryButlerApplication
import com.chriscartland.batterybutler.datanetwork.BuildConfig
import kotlinx.coroutines.launch

class ServerUrlReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val application = context.applicationContext as? BatteryButlerApplication
        if (application == null) {
            Logger.e("ServerUrlReceiver") { "Application context is not BatteryButlerApplication" }
            return
        }
        // Access repository through AppComponent
        // Note: AppComponent interface needs to expose ServerUrlRepository if it's not already accessible?
        // Actually we can just inject it if we could, but BroadcastReceiver is not easily sizable.
        // We assume AppComponent has a getter or we can resolve it.
        // Since we defined `provideServerUrlRepository`, we can add an abstract property to AppComponent if needed,
        // OR we can rely on `appComponent.serverUrlRepository` if we add it to the interface.
        // Let's check AppComponent. It's an abstract class. We added the provider.
        // Me.tatarka.inject doesn't automatically generate getters for provided types unless requested?
        // We should add `abstract val serverUrlRepository: ServerUrlRepository` to AppComponent.

        // Wait, I haven't added that abstract val yet. I added the @Provides method.
        // I need to update AppComponent to expose it.

        /*
        For now, I'll update this code assuming the property exists,
        and then I'll go update AppComponent to ensure it exists.
         */

        // Access use case through AppComponent
        val setNetworkModeUseCase = application.appComponent.setNetworkModeUseCase
        // We also need the repository to get the current mode?
        // Or we just force a specific mode. If we set URL, we probably imply "AWS" logic (custom URL) or "Local"?
        // Typically explicit URL means we want to point to a specific GRPC server.
        // Let's assume we want to set GrpcAws(url) if url is external, or GrpcLocal(url) if internal?
        // Simpler: Just map to GrpcAws(url) as a generic "Remote" manual override, OR
        // if the intent comes from `adb shell`, maybe we want to force Local?
        // The previous logic was just `setServerUrl`.
        // Let's assume we map to GrpcLocal if it looks local, or allow caller to specify?
        // For simplicity, if we receive a URL, we treat it as a Manual Override.
        // Let's use GrpcAws(url) for now as "Custom/Production-like",
        // UNLESS the previous code implied `GRPC_LOCAL`.
        // Previous `DelegatingGrpcClient` used `localUrl` when in `GRPC_LOCAL`.
        // `ServerUrlReceiver` was used to override `SharedServerConfig` which was read by both?
        // Actually, `ServerUrlReceiver` used to set `SharedServerConfig.setServerUrl`.
        // `NetworkMode.GRPC_LOCAL` forced `LOCAL_GRPC_ADDRESS_ANDROID`.
        // So `ServerUrlReceiver` was likely used for *testing* on AWS mode or overriding.

        // Let's launch a coroutine scope to call suspend function? BroadcastReceiver goAsync?
        // `setNetworkMode` is suspend.
        // We can use `goAsync()` and a scope.

        val pendingResult = goAsync()
        val scope = application.appComponent.provideAppScope() // We need a scope. AppComponent provides one?
        // We added provideAppScope() in AppComponent, but it returns CoroutineScope.
        // We can't access it easily if it's not a property.
        // Let's use GlobalScope or construct one, or access the one from Application if available.
        // Ideally, `setNetworkModeUseCase` shouldn't be suspend if it just updates a stateflow?
        // `InMemoryNetworkModeRepository` sets a value on MutableStateFlow. It doesn't strictly need to be suspend,
        // but the interface says so.

        // Let's use kotlinx.coroutines.runBlocking for simplicity in Receiver if we expect it to be fast (InMemory)?
        // Or standard goAsync pattern.

        if (intent.action == "com.chriscartland.batterybutler.SET_SERVER_URL") {
            val url = intent.getStringExtra("url")
            val mode = if (url != null) {
                Logger.d("ServerUrlReceiver") { "Updating Server URL" }
                // If 10.0.2.2, maybe Local? But let's just use GrpcAws(url) as "Custom" for now.
                // Or wait, if we are in Local mode, we might want to stay in Local mode but change URL?
                // But simplified NetworkMode means the Mode CARRIES the URL.
                // So we switch to a Mode that has this URL.
                com.chriscartland.batterybutler.domain.model.NetworkMode
                    .GrpcAws(url)
            } else {
                Logger.d("ServerUrlReceiver") { "Resetting Server URL to default" }
                // Resetting usually meant "Use default".
                // We can reset to GrpcAws(DefaultUrl) or just leave it?
                // Previous code `resetServerUrl()` cleared the override.
                // Here, let's switch to standard AWS.
                com.chriscartland.batterybutler.domain.model.NetworkMode.GrpcAws(
                    BuildConfig.PRODUCTION_SERVER_URL,
                )
            }

            application.appComponent.appScope.launch {
                try {
                    setNetworkModeUseCase(mode)
                } finally {
                    pendingResult.finish()
                }
            }
        } else {
            pendingResult.finish()
        }
    }
}
