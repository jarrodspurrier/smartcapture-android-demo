package com.gbg.smartcapture.bigmagic.compositions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gbg.smartcapture.bigmagic.BuildConfig
import com.gbg.smartcapture.bigmagic.R
import com.gbg.smartcapture.bigmagic.compositions.bits.SettingSwitchRow
import com.gbg.smartcapture.bigmagic.compositions.bits.TitledDropdown
import com.gbg.smartcapture.bigmagic.data.DeviceInfo
import com.gbg.smartcapture.bigmagic.data.SettingsManualCaptureToggleDelayType
import com.gbg.smartcapture.bigmagic.data.SettingsSwitch
import com.gbg.smartcapture.bigmagic.viewmodel.IRootViewModel
import com.gbg.smartcapture.bigmagic.viewmodel.MockedRootViewModel
import com.gbg.smartcapture.commons.compositions.GBGPreviewView
import com.gbg.smartcapture.commons.compositions.components.SecondaryButton

private val BrandDeep = Color(0xFF0F2D4A)
private val BrandMuted = Color(0xFF5B6B7C)
private val CardBorder = Color(0xFFE6E9EE)
private val OkGreen = Color(0xFF14532D)
private val OkBg = Color(0xFFDCFCE7)
private val ErrRed = Color(0xFF7F1D1D)
private val ErrBg = Color(0xFFFEE2E2)

@Composable
fun SettingsView(
    viewModel: IRootViewModel,
    onCheckedChange: (settingsSwitch: SettingsSwitch, value: Boolean) -> Unit = { _, _ -> },
    onManualCaptureToggleDelayChange: (value: SettingsManualCaptureToggleDelayType) -> Unit = { },
    onDebugPoll: (String) -> Unit = {},
) {
    val manualCapture = viewModel.settings.manualCaptureToggle.collectAsStateWithLifecycle()
    val showCapturePreview = viewModel.settings.showCapturePreview.collectAsStateWithLifecycle()
    val saveRawImagesToGallery = viewModel.settings.saveRawImagesToGallery.collectAsStateWithLifecycle()
    val delaySelected = viewModel.manualCaptureToggleDelayState.collectAsStateWithLifecycle()
    val hasApiKey = viewModel.hasApiKey.collectAsStateWithLifecycle()
    val lastSessionId = viewModel.lastSessionId.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_screen_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = BrandDeep,
        )
        Spacer(Modifier.size(16.dp))

        ApiKeyStatusCard(hasKey = hasApiKey.value)

        Spacer(Modifier.size(20.dp))

        SectionLabel("Capture")
        Spacer(Modifier.size(8.dp))
        SettingsGroup {
            SettingSwitchRow(
                settingsSwitch = SettingsSwitch.MANUAL_CAPTURE_TOGGLE,
                checked = manualCapture.value,
                onCheckedChange = onCheckedChange,
            )
            TitledDropdown(
                title = stringResource(R.string.settings_manual_capture_toggle_delay),
                items = SettingsManualCaptureToggleDelayType.ALL,
                selectedItem = delaySelected.value,
                onItemSelected = onManualCaptureToggleDelayChange,
                itemLabel = { stringResource(it.titleResource) },
            )
            SettingSwitchRow(
                settingsSwitch = SettingsSwitch.SHOW_CAPTURE_PREVIEW,
                checked = showCapturePreview.value,
                onCheckedChange = onCheckedChange,
            )
        }

        if (BuildConfig.DEBUG) {
            Spacer(Modifier.size(24.dp))
            SectionLabel("Device")
            Spacer(Modifier.size(8.dp))
            val deviceInfo = remember { viewModel.getDeviceInfo() }
            DeviceInfoCard(info = deviceInfo)

            Spacer(Modifier.size(24.dp))
            SectionLabel("Debug")
            Spacer(Modifier.size(8.dp))
            SettingsGroup {
                SettingSwitchRow(
                    settingsSwitch = SettingsSwitch.SAVE_RAW_IMAGES_TO_GALLERY,
                    checked = saveRawImagesToGallery.value,
                    onCheckedChange = onCheckedChange,
                )
            }
            Spacer(Modifier.size(12.dp))
            DebugPollPanel(
                initialSessionId = lastSessionId.value.orEmpty(),
                onDebugPoll = onDebugPoll,
            )
        }

        Spacer(Modifier.size(24.dp))
    }
}

@Composable
private fun DebugPollPanel(
    initialSessionId: String,
    onDebugPoll: (String) -> Unit,
) {
    var sessionId by remember(initialSessionId) { mutableStateOf(initialSessionId) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        Text(
            text = "Poll an existing session",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = BrandDeep,
        )
        OutlinedTextField(
            value = sessionId,
            onValueChange = { sessionId = it },
            label = { Text("Session ID") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
        Spacer(Modifier.size(8.dp))
        SecondaryButton(
            text = "Poll",
            onSubmit = { onDebugPoll(sessionId.trim()) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DeviceInfoCard(info: DeviceInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        DeviceInfoRow(label = "Device ID", value = info.deviceId)
        DeviceInfoRow(label = "Manufacturer", value = info.manufacturer)
        DeviceInfoRow(label = "Model", value = info.model)
        DeviceInfoRow(label = "OS version", value = info.osVersion)
        DeviceInfoRow(label = "Timezone", value = info.timezone)
        DeviceInfoRow(label = "Network", value = info.network)
    }
}

@Composable
private fun DeviceInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = BrandMuted,
            modifier = Modifier.padding(end = 12.dp),
        )
        Text(
            text = value.ifBlank { "—" },
            fontSize = 13.sp,
            color = BrandDeep,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ApiKeyStatusCard(hasKey: Boolean) {
    val bg = if (hasKey) OkBg else ErrBg
    val fg = if (hasKey) OkGreen else ErrRed
    val title = if (hasKey) "IVS API key loaded" else "IVS API key missing"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(bg = bg, fg = fg)
        Spacer(Modifier.size(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandDeep,
            )
            if (!hasKey) {
                Text(
                    text = "Add `ivsApiKey=<your key>` to local.properties and rebuild.",
                    fontSize = 13.sp,
                    color = BrandMuted,
                )
            }
        }
    }
}

@Composable
private fun StatusDot(bg: Color, fg: Color) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(fg),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = BrandMuted,
    )
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSettings() {
    GBGPreviewView {
        SettingsView(viewModel = MockedRootViewModel())
    }
}
