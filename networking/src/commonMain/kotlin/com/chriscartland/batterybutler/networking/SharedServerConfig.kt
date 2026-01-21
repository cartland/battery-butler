package com.chriscartland.batterybutler.networking

object SharedServerConfig {
    const val LOCAL_GRPC_ADDRESS_ANDROID = "http://10.0.2.2:50051"
    const val LOCAL_GRPC_ADDRESS_DESKTOP = "http://0.0.0.0:50051"
    const val LOCAL_GRPC_ADDRESS_IOS = "http://localhost:50051"

    // Placeholder until deployment completes.
    // Once we have the ALB DNS name, we will update this value.
    const val PRODUCTION_SERVER_URL = "http://battery-butler-nlb-847feaa773351518.elb.us-west-1.amazonaws.com:80"
}
