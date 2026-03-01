package com.chriscartland.batterybutler.domain.model.ai

data object AiToolNames {
    const val ADD_DEVICE = "addDevice"
    const val ADD_DEVICE_TYPE = "addDeviceType"
    const val RECORD_BATTERY_REPLACEMENT = "recordBatteryReplacement"
    const val UPDATE_DEVICE = "updateDevice"
    const val DELETE_DEVICE = "deleteDevice"
    const val UPDATE_DEVICE_TYPE = "updateDeviceType"
    const val DELETE_DEVICE_TYPE = "deleteDeviceType"
    const val UPDATE_BATTERY_EVENT = "updateBatteryEvent"
    const val DELETE_BATTERY_EVENT = "deleteBatteryEvent"
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
    const val ID = "id"
    const val NEW_NAME = "newName"
    const val NEW_LOCATION = "newLocation"
    const val NEW_TYPE = "newType"
    const val NEW_DATE = "newDate"
    const val NOTES = "notes"
    const val NEW_BATTERY_TYPE = "newBatteryType"
    const val NEW_BATTERY_QUANTITY = "newBatteryQuantity"
    const val NEW_ICON = "newIcon"
}
