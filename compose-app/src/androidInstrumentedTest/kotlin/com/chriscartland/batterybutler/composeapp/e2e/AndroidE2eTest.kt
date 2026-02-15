package com.chriscartland.batterybutler.composeapp.e2e

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.chriscartland.batterybutler.composeapp.MainActivity
import com.chriscartland.batterybutler.proto.GrpcBatteryServiceClient
import com.chriscartland.batterybutler.proto.GrpcSyncServiceClient
import com.chriscartland.batterybutler.proto.SyncUpdate
import com.squareup.wire.GrpcClient
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class AndroidE2eTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val testRunId = UUID.randomUUID().toString().take(8)
    private val deviceName = "E2E Device $testRunId"
    
    // Server connection from the test runner (running on device/emulator)
    // to the server running on host machine.
    // Emulator host loopback is 10.0.2.2.
    private val serverUrl = "http://10.0.2.2:50051"
    
    private lateinit var batteryClient: GrpcBatteryServiceClient
    private lateinit var syncClient: GrpcSyncServiceClient

    @Before
    fun setUp() {
        runBlocking {
            // 1. Configure App to use Local Server via Broadcast
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val command = "am broadcast -a com.chriscartland.batterybutler.SET_NETWORK_MODE --es mode GRPC_LOCAL --es url $serverUrl"
            instrumentation.uiAutomation.executeShellCommand(command)
            
            // Give the app a moment to process the broadcast and reconnect
            Thread.sleep(2000)
    
            // 2. Configure Public Client for Auth
            val publicOkHttpClient = OkHttpClient.Builder()
                .protocols(listOf(Protocol.H2_PRIOR_KNOWLEDGE))
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
    
            val publicGrpcClient = GrpcClient.Builder()
                .client(publicOkHttpClient)
                .baseUrl(serverUrl)
                .build()
    
            val authClient = com.chriscartland.batterybutler.proto.GrpcAuthServiceClient(publicGrpcClient)
            
            // 3. Login (Dev Mode)
            val sessionToken = try {
                val response = authClient.VerifyToken().execute(
                    com.chriscartland.batterybutler.proto.VerifyTokenRequest(google_id_token = "dev-token")
                )
                if (!response.valid) {
                    throw RuntimeException("Login failed: ${response.error_message}")
                }
                response.session_token
            } catch (e: Exception) {
                throw RuntimeException("Failed to login: $e", e)
            }
    
            // 4. Configure Authenticated Client
            val authOkHttpClient = publicOkHttpClient.newBuilder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("authorization", "Bearer $sessionToken")
                        .build()
                    chain.proceed(request)
                }
                .build()
                
            val authGrpcClient = GrpcClient.Builder()
                .client(authOkHttpClient)
                .baseUrl(serverUrl)
                .build()
    
            // 5. Inject Token into App (White-Box)
            val app = instrumentation.targetContext.applicationContext as com.chriscartland.batterybutler.BatteryButlerApplication
            app.appComponent.authRepository.setExternalToken(sessionToken)
            Thread.sleep(2000) // Wait for app to process and navigate
    
            batteryClient = GrpcBatteryServiceClient(authGrpcClient)
            syncClient = GrpcSyncServiceClient(authGrpcClient)
            
            // 5. Seed Data
            // Create a Device Type so we can select it
            try {
                val seedUpdate = com.chriscartland.batterybutler.proto.SyncUpdate(
                    is_full_snapshot = false,
                    device_types = listOf(
                        com.chriscartland.batterybutler.proto.ProtoDeviceType(
                            id = testRunId + "-type",
                            name = "E2E Type $testRunId",
                            default_icon = "detector_smoke",
                            battery_type = "9V",
                            battery_quantity = 1
                        )
                    ),
                    devices = emptyList(),
                    events = emptyList(),
                    deleted_device_type_ids = emptyList(),
                    deleted_device_ids = emptyList(),
                    deleted_event_ids = emptyList(),
                )
                syncClient.PushUpdate().execute(seedUpdate)
            } catch (e: Exception) {
                println("Warning: Failed to seed device type. It might already exist or server error: $e")
            }
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            // Best-effort cleanup
        }
    }

    @Test
    fun addDevice_syncsToServer() {
        // 1. Navigate to Add Device
        // MainScreen FAB has content description "Add"
        // Wait for migration from Login Screen
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithContentDescription("Add").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription("Add").performClick()
        
        // 2. Input Device Details
        composeTestRule.onNodeWithText("Device Name").performTextInput(deviceName)
        composeTestRule.onNodeWithText("Location").performTextInput("E2E Lab")
        
        // 3. Select Type
        composeTestRule.onNodeWithText("Device Type").performClick()
        composeTestRule.onNodeWithText("E2E Type $testRunId").performClick()
        
        // 4. Save
        composeTestRule.onNodeWithText("Save").performClick()
        
        // 5. Verify UI (returned to list)
        composeTestRule.waitForIdle()
        // Check if device appears in the list 
        // We might need to scroll or wait for sync
        Thread.sleep(2000) // Wait for sync/UI update
        composeTestRule.onNodeWithText(deviceName).assertExists()
        
        // 6. Verify Server
        // Use Subscribe to get the current state (snapshot)
        val snapshot = runBlocking {
            val (_, responseChannel) = syncClient.Subscribe().execute()
            responseChannel.receive()
        }
        
        val found = snapshot.devices.any { it.name == deviceName }
        assert(found) { "Device '$deviceName' not found on server. Server has: ${snapshot.devices.map { it.name }}" }
    }
}
