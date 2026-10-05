package com.fearmikey.garage.ui.obd

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fearmikey.garage.R
import com.fearmikey.garage.obd.FreezeFrame
import com.fearmikey.garage.obd.MonitorStatus
import com.fearmikey.garage.obd.ObdAdapterConfig
import com.fearmikey.garage.obd.ObdAdapterType
import com.fearmikey.garage.obd.ObdPid
import com.fearmikey.garage.obd.ObdReading
import com.fearmikey.garage.obd.ObdScanResult
import com.fearmikey.garage.obd.ReadinessMonitor
import com.fearmikey.garage.ui.theme.GarageTheme
import com.fearmikey.garage.ui.util.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** UI tests for the stateless OBD2 scanner content and the adapter picker dialog. */
@RunWith(AndroidJUnit4::class)
class ObdScannerContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun str(@StringRes id: Int, vararg args: Any) = context.getString(id, *args)

    private class Callbacks {
        var requestPermissions = 0
        var openPicker = 0
        var rescan = 0
        var clearCodes = 0
        var toggleLive = 0
        var saveVin = 0
        var logAnyway = 0
    }

    private val adapter = ObdAdapterConfig(ObdAdapterType.CLASSIC, "AA:BB:CC:DD:EE:FF", "Veepeak VP11")

    private val scanWithCodes = ObdScanResult(
        storedDtcs = listOf("P0420"),
        pendingDtcs = listOf("P0171"),
        dtcSources = mapOf("P0420" to listOf("Engine (ECM)")),
        monitorStatus = MonitorStatus(
            milOn = true,
            storedDtcCount = 1,
            isDiesel = false,
            monitors = listOf(ReadinessMonitor("Catalyst", true), ReadinessMonitor("Evaporative System", false)),
        ),
        freezeFrame = FreezeFrame("P0420", listOf(ObdReading(ObdPid.RPM, 2450.0))),
        vin = "1HGCM82633A004352",
    )

    private fun setContent(
        state: ObdViewModel.UiState,
        needsPermission: Boolean = false,
        callbacks: Callbacks = Callbacks(),
    ): Callbacks {
        composeRule.setContent {
            GarageTheme {
                ObdScannerContent(
                    uiState = state,
                    unitSystem = UnitSystem.IMPERIAL,
                    needsPermission = needsPermission,
                    onNavigateBack = {},
                    onRequestPermissions = { callbacks.requestPermissions++ },
                    onOpenDevicePicker = { callbacks.openPicker++ },
                    onRescan = { callbacks.rescan++ },
                    onClearCodes = { callbacks.clearCodes++ },
                    onToggleLiveData = { callbacks.toggleLive++ },
                    onSaveVin = { callbacks.saveVin++ },
                    onLogAnyway = { callbacks.logAnyway++ },
                )
            }
        }
        return callbacks
    }

    private fun connectedState(result: ObdScanResult, vinCheck: ObdViewModel.VinCheck? = ObdViewModel.VinCheck.MATCH) =
        ObdViewModel.UiState(
            connectionStatus = ObdViewModel.ConnectionStatus.CONNECTED,
            isPreferenceLoaded = true,
            savedAdapter = adapter,
            scanResult = result,
            vinCheck = vinCheck,
            vehicleVin = if (vinCheck == ObdViewModel.VinCheck.MISMATCH) "JH4KA8260MC000000" else null,
            timelineLogResult = if (vinCheck == ObdViewModel.VinCheck.MISMATCH) {
                ObdViewModel.TimelineLogResult.SKIPPED_VIN_MISMATCH
            } else {
                ObdViewModel.TimelineLogResult.SAVED
            },
        )

    private fun scrollTo(text: String) {
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(text, substring = true))
    }

    @Test
    fun noAdapter_showsOnboarding_andOpensPicker() {
        val callbacks = setContent(ObdViewModel.UiState(isPreferenceLoaded = true))

        composeRule.onNodeWithText(str(R.string.obd_no_adapter_title)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.obd_no_adapter_action)).performClick()
        assertEquals(1, callbacks.openPicker)
    }

    @Test
    fun missingPermission_showsGrantButton() {
        val callbacks = setContent(
            ObdViewModel.UiState(isPreferenceLoaded = true, savedAdapter = adapter),
            needsPermission = true,
        )

        composeRule.onNodeWithText(str(R.string.obd_permission_action)).performClick()
        assertEquals(1, callbacks.requestPermissions)
    }

    @Test
    fun connected_showsAdapterHealthAndCodes() {
        setContent(connectedState(scanWithCodes))

        composeRule.onNodeWithText("Veepeak VP11").assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.obd_status_connected)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.obd_mil_on)).assertIsDisplayed()

        scrollTo("P0420")
        composeRule.onNodeWithText("P0420").assertIsDisplayed()
        composeRule.onNodeWithText("Catalyst system efficiency below threshold (Bank 1)").assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.obd_dtc_reported_by, "Engine (ECM)")).assertIsDisplayed()
        scrollTo("P0171")
        composeRule.onNodeWithText("P0171").assertIsDisplayed()
    }

    @Test
    fun readinessMonitors_expandOnTap() {
        setContent(connectedState(scanWithCodes))

        composeRule.onNodeWithContentDescription(str(R.string.obd_cd_show_monitors)).performClick()
        composeRule.onNodeWithText("Evaporative System").assertIsDisplayed()
    }

    @Test
    fun clearCodes_requiresConfirmation() {
        val callbacks = setContent(connectedState(scanWithCodes))
        val clear = str(R.string.obd_clear_codes)

        scrollTo(clear)
        composeRule.onNodeWithText(clear).performClick()
        assertEquals(0, callbacks.clearCodes)
        composeRule.onNodeWithText(str(R.string.obd_clear_codes_confirm_title)).assertIsDisplayed()

        // The dialog's confirm button is the second "Clear Codes" node.
        composeRule.onAllNodesWithText(clear).onLast().performClick()
        assertEquals(1, callbacks.clearCodes)
    }

    @Test
    fun noCodes_showsCleanBanner_andHidesClearButton() {
        setContent(connectedState(ObdScanResult()))

        composeRule.onNodeWithText(str(R.string.obd_no_codes_title)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.obd_clear_codes)).assertDoesNotExist()
    }

    @Test
    fun freezeFrame_isShown() {
        setContent(connectedState(scanWithCodes))

        val title = str(R.string.obd_freeze_frame_title)
        scrollTo(title)
        composeRule.onNodeWithText(title).assertIsDisplayed()
        composeRule.onNodeWithText("2450 rpm").assertIsDisplayed()
    }

    @Test
    fun vinMismatch_offersSaveAnyway() {
        val callbacks = setContent(connectedState(scanWithCodes, ObdViewModel.VinCheck.MISMATCH))

        composeRule.onNodeWithText(str(R.string.obd_vin_mismatch_title)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.obd_save_scan_anyway)).performClick()
        assertEquals(1, callbacks.logAnyway)
    }

    @Test
    fun missingVehicleVin_offersSaveVin() {
        val callbacks = setContent(connectedState(scanWithCodes, ObdViewModel.VinCheck.VEHICLE_HAS_NO_VIN))

        composeRule.onNodeWithText(str(R.string.obd_save_vin)).performClick()
        assertEquals(1, callbacks.saveVin)
    }

    @Test
    fun liveData_toggle() {
        val callbacks = setContent(connectedState(scanWithCodes))
        val start = str(R.string.obd_start_live_data)

        scrollTo(start)
        composeRule.onNodeWithText(start).performClick()
        assertEquals(1, callbacks.toggleLive)
    }

    @Test
    fun picker_wifiTab_savesHostAndPort() {
        var selected: ObdAdapterConfig? = null
        composeRule.setContent {
            GarageTheme {
                ObdDevicePickerDialog(
                    pairedDevices = emptyList(),
                    bleDevices = emptyList(),
                    isBleScanning = false,
                    selected = null,
                    onAdapterSelected = { selected = it },
                    onDismissRequest = {},
                    onRefreshPaired = {},
                    onStartBleScan = {},
                )
            }
        }

        composeRule.onNodeWithTag(ObdPickerTestTags.TAB_WIFI).performClick()
        composeRule.onNodeWithTag(ObdPickerTestTags.WIFI_HOST).performTextReplacement("192.168.4.1")
        composeRule.onNodeWithTag(ObdPickerTestTags.WIFI_PORT).performTextReplacement("23")
        composeRule.onNodeWithTag(ObdPickerTestTags.WIFI_SAVE).performClick()

        val config = selected
        assertTrue(config != null)
        assertEquals(ObdAdapterType.WIFI, config!!.type)
        assertEquals("192.168.4.1" to 23, config.wifiHostPort)
    }

    @Test
    fun picker_bleTab_showsScanButton() {
        var scans = 0
        composeRule.setContent {
            GarageTheme {
                ObdDevicePickerDialog(
                    pairedDevices = emptyList(),
                    bleDevices = emptyList(),
                    isBleScanning = true,
                    selected = null,
                    onAdapterSelected = {},
                    onDismissRequest = {},
                    onRefreshPaired = {},
                    onStartBleScan = { scans++ },
                )
            }
        }

        composeRule.onNodeWithTag(ObdPickerTestTags.TAB_BLE).performClick()
        composeRule.onNodeWithText(str(R.string.obd_picker_scanning)).assertIsDisplayed()
    }
}
