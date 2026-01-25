package com.chriscartland.batterybutler.datanetwork.grpc

object SharedServerConfig {
    const val LOCAL_GRPC_ADDRESS_ANDROID = "http://10.0.2.2:50051"
    const val LOCAL_GRPC_ADDRESS_DESKTOP = "http://0.0.0.0:50051"
    const val LOCAL_GRPC_ADDRESS_IOS = "http://localhost:50051"

    private const val DEFAULT_PRODUCTION_URL = "http://battery-butler-nlb-847feaa773351518.elb.us-west-1.amazonaws.com:80"

    // Runtime configuration
    private var configuredServerUrl: String? = null

    val PRODUCTION_SERVER_URL: String
        get() = configuredServerUrl ?: DEFAULT_PRODUCTION_URL

    /**
     * Updates the server URL at runtime.
     * Useful for end-to-end testing with adb broadcast commands.
     */
    fun setServerUrl(url: String) {
        configuredServerUrl = url
    }

    fun resetServerUrl() {
        configuredServerUrl = null
    }
}
