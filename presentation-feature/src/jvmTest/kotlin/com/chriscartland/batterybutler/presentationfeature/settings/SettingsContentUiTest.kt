package com.chriscartland.batterybutler.presentationfeature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import com.chriscartland.batterybutler.domain.model.AppVersion
import com.chriscartland.batterybutler.domain.model.AuthState
import com.chriscartland.batterybutler.domain.model.DataMode
import com.chriscartland.batterybutler.domain.model.User
import com.chriscartland.batterybutler.domain.model.ai.AiEngineType
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Headless pins for the Settings tab's section layout: user-relevant sections
 * (Account, Data, About) before the developer-facing Advanced section, and the
 * developer controls (Data Mode, AI engine, Labs token) inside Advanced.
 */
@OptIn(ExperimentalTestApi::class)
class SettingsContentUiTest {
    private val labsUser = User(
        id = "labs-user",
        email = "labs@example.com",
        displayName = "Labs User",
        photoUrl = null,
    )

    @Composable
    private fun TestSettingsContent(
        dataMode: DataMode,
        currentUser: User?,
        isLabsMode: Boolean,
        labsAuthState: AuthState,
    ) {
        BatteryButlerTheme {
            SettingsContent(
                dataMode = dataMode,
                availableDataModes = listOf(dataMode),
                onDataModeSelected = {},
                aiEngineType = AiEngineType.Cloud,
                availableAiEngines = AiEngineType.entries,
                onAiEngineSelected = {},
                onExportData = {},
                onImportData = {},
                importResult = null,
                importError = null,
                importInProgress = false,
                onImportResultConsumed = {},
                onBack = {},
                appVersion = AppVersion.Android("1.0.0", 123),
                currentUser = currentUser,
                onSignOut = {},
                currentDatabaseFileName = "battery_butler.db",
                isLabsMode = isLabsMode,
                labsAuthState = labsAuthState,
            )
        }
    }

    private fun SemanticsNodeInteraction.topY(): Float = fetchSemanticsNode().positionInRoot.y

    @Test
    fun `sections run Account, Data, About, Advanced when signed in to Labs`() =
        runComposeUiTest {
            setContent {
                TestSettingsContent(
                    dataMode = DataMode.LabsStaging(null),
                    currentUser = null,
                    isLabsMode = true,
                    labsAuthState = AuthState.Authenticated(labsUser),
                )
            }

            val account = onNodeWithTag(SettingsTestTags.SECTION_ACCOUNT).topY()
            val data = onNodeWithTag(SettingsTestTags.SECTION_DATA).topY()
            val about = onNodeWithTag(SettingsTestTags.SECTION_ABOUT).topY()
            val advanced = onNodeWithTag(SettingsTestTags.SECTION_ADVANCED).topY()
            assertTrue(account < data, "Account ($account) should be above Data ($data)")
            assertTrue(data < about, "Data ($data) should be above About ($about)")
            assertTrue(about < advanced, "About ($about) should be above Advanced ($advanced)")

            // The developer controls all live below the Advanced header.
            val dataModeRow = onNodeWithTag(SettingsTestTags.DATA_MODE).topY()
            val copyToken = onNodeWithTag(SettingsTestTags.COPY_LABS_TOKEN).topY()
            val aiEngine = onNodeWithTag(SettingsTestTags.AI_ENGINE).topY()
            assertTrue(advanced < dataModeRow, "Data Mode should be inside Advanced")
            assertTrue(advanced < copyToken, "Copy Labs token should be inside Advanced")
            assertTrue(advanced < aiEngine, "AI engine should be inside Advanced")
        }

    @Test
    fun `account section is hidden when there is no account to show`() =
        runComposeUiTest {
            setContent {
                TestSettingsContent(
                    dataMode = DataMode.Mock,
                    currentUser = null,
                    isLabsMode = false,
                    labsAuthState = AuthState.Unauthenticated(),
                )
            }

            onNodeWithTag(SettingsTestTags.SECTION_ACCOUNT).assertDoesNotExist()
            onNodeWithTag(SettingsTestTags.COPY_LABS_TOKEN).assertDoesNotExist()
            onNodeWithTag(SettingsTestTags.SECTION_DATA).assertExists()
            onNodeWithTag(SettingsTestTags.SECTION_ABOUT).assertExists()
            onNodeWithTag(SettingsTestTags.SECTION_ADVANCED).assertExists()
        }

    @Test
    fun `signed-out Labs mode shows the Account section without the token card`() =
        runComposeUiTest {
            setContent {
                TestSettingsContent(
                    dataMode = DataMode.LabsStaging(null),
                    currentUser = null,
                    isLabsMode = true,
                    labsAuthState = AuthState.Unauthenticated(),
                )
            }

            onNodeWithTag(SettingsTestTags.SECTION_ACCOUNT).assertExists()
            onNodeWithTag(SettingsTestTags.COPY_LABS_TOKEN).assertDoesNotExist()
        }
}
