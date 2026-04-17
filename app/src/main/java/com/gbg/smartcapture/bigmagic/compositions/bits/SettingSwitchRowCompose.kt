package com.gbg.smartcapture.bigmagic.compositions.bits

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gbg.smartcapture.bigmagic.data.SettingsSwitch
import com.gbg.smartcapture.commons.compositions.GBGPreviewView


@Composable
fun SettingSwitchRow(
    settingsSwitch: SettingsSwitch,
    checked: Boolean,
    onCheckedChange: (SettingsSwitch, Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable {
                onCheckedChange(settingsSwitch, !checked)
            }
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
        ) {
            Text(
                text = stringResource(settingsSwitch.title),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = {
                onCheckedChange(settingsSwitch, it)
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    GBGPreviewView {
        SettingSwitchRow(
            SettingsSwitch.MANUAL_CAPTURE_TOGGLE,
            true,
            onCheckedChange = { _, _ ->

            }
        )
        SettingSwitchRow(
            SettingsSwitch.MANUAL_CAPTURE_TOGGLE,
            false,
            onCheckedChange = { _, _ ->

            }
        )
    }
}
