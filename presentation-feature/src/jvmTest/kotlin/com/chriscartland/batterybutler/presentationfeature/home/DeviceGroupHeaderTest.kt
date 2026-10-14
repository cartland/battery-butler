package com.chriscartland.batterybutler.presentationfeature.home

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.runComposeUiTest
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationmodel.home.GroupOption
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class DeviceGroupHeaderTest {
    private val now = Instant.parse("2026-01-18T17:00:00Z")
    private val motion = DeviceType("motion", "Smart Motion Sensor", "sensors", batteryType = "CR2032", batteryQuantity = 2)
    private val motionDuplicateName = DeviceType("motion2", "Smart Motion Sensor", "sensors", batteryType = "AAA")
    private val types = mapOf(motion.id to motion, motionDuplicateName.id to motionDuplicateName)

    private fun device(
        id: String,
        typeId: String,
    ) = Device(id, "Device $id", typeId, now, now, "Hallway")

    @Test
    fun `type grouping resolves the group's device type`() {
        val devices = listOf(device("a", "motion"), device("b", "motion"))
        assertEquals(motion, groupHeaderDeviceType(GroupOption.TYPE, devices, types))
    }

    @Test
    fun `location grouping has no device type`() {
        val devices = listOf(device("a", "motion"))
        assertNull(groupHeaderDeviceType(GroupOption.LOCATION, devices, types))
        assertNull(groupHeaderDeviceType(GroupOption.NONE, devices, types))
    }

    @Test
    fun `two types sharing a name do not claim one battery for the merged group`() {
        val devices = listOf(device("a", "motion"), device("b", "motion2"))
        assertNull(groupHeaderDeviceType(GroupOption.TYPE, devices, types))
    }

    @Test
    fun `unknown type id has no device type`() {
        assertNull(groupHeaderDeviceType(GroupOption.TYPE, listOf(device("a", "missing")), types))
    }

    @Test
    fun `type header reads as one heading - name then battery, without the bullet`() =
        runComposeUiTest {
            setContent {
                BatteryButlerTheme { DeviceGroupHeader(title = motion.name, deviceType = motion) }
            }
            val heading = onNode(isHeading()).fetchSemanticsNode()
            assertEquals(
                listOf("Smart Motion Sensor", "2 x CR2032"),
                heading.config[SemanticsProperties.Text].map { it.text },
            )
        }

    @Test
    fun `header without a device type is the plain title`() =
        runComposeUiTest {
            setContent {
                BatteryButlerTheme { DeviceGroupHeader(title = "Kitchen", deviceType = null) }
            }
            val heading = onNode(isHeading()).fetchSemanticsNode()
            assertEquals(listOf("Kitchen"), heading.config[SemanticsProperties.Text].map { it.text })
        }
}
