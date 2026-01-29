package com.chriscartland.batterybutler.ai

data object AiToolNames {
    const val ADD_DEVICE = "addDevice"
    const val ADD_DEVICE_TYPE = "addDeviceType"
    const val RECORD_BATTERY_REPLACEMENT = "recordBatteryReplacement"
}

data object AiToolParams {
    const val NAME = "name"
    const val TYPE = "type"
    const val LOCATION = "location"
    const val DEVICE_NAME = "deviceName"
    const val DATE = "date"
    const val DEVICE_TYPE = "deviceType"
    const val BATTERY_TYPE = "batteryType"
    const val BATTERY_QUANTITY = "batteryQuantity"
    const val ICON = "icon"
}
