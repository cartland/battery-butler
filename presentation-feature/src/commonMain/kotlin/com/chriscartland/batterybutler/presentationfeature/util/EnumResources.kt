package com.chriscartland.batterybutler.presentationfeature.util

import com.chriscartland.batterybutler.composeresources.generated.resources.Res
import com.chriscartland.batterybutler.composeresources.generated.resources.group_battery_type
import com.chriscartland.batterybutler.composeresources.generated.resources.group_location
import com.chriscartland.batterybutler.composeresources.generated.resources.group_none
import com.chriscartland.batterybutler.composeresources.generated.resources.group_type
import com.chriscartland.batterybutler.composeresources.generated.resources.sort_battery_age
import com.chriscartland.batterybutler.composeresources.generated.resources.sort_battery_type
import com.chriscartland.batterybutler.composeresources.generated.resources.sort_location
import com.chriscartland.batterybutler.composeresources.generated.resources.sort_name
import com.chriscartland.batterybutler.composeresources.generated.resources.sort_type
import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeGroupOption
import com.chriscartland.batterybutler.presentationmodel.devicetypes.DeviceTypeSortOption
import com.chriscartland.batterybutler.presentationmodel.home.GroupOption
import com.chriscartland.batterybutler.presentationmodel.home.SortOption
import org.jetbrains.compose.resources.StringResource

fun SortOption.labelRes(): StringResource =
    when (this) {
        SortOption.NAME -> Res.string.sort_name
        SortOption.LOCATION -> Res.string.sort_location
        SortOption.BATTERY_AGE -> Res.string.sort_battery_age
        SortOption.TYPE -> Res.string.sort_type
    }

fun GroupOption.labelRes(): StringResource =
    when (this) {
        GroupOption.NONE -> Res.string.group_none
        GroupOption.TYPE -> Res.string.group_type
        GroupOption.LOCATION -> Res.string.group_location
    }

fun DeviceTypeSortOption.labelRes(): StringResource =
    when (this) {
        DeviceTypeSortOption.NAME -> Res.string.sort_name
        DeviceTypeSortOption.BATTERY_TYPE -> Res.string.sort_battery_type
    }

fun DeviceTypeGroupOption.labelRes(): StringResource =
    when (this) {
        DeviceTypeGroupOption.NONE -> Res.string.group_none
        DeviceTypeGroupOption.BATTERY_TYPE -> Res.string.group_battery_type
    }
