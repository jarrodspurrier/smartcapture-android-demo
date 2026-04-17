package com.gbg.smartcapture.bigmagic.compositions.bits

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gbg.smartcapture.bigmagic.R
import com.gbg.smartcapture.commons.compositions.GBGPreviewView

private val BannerAccent = Color(0xFF0F2D4A)

enum class BannerAction { NONE, SETTINGS, BACK }

@Composable
fun EnterpriseBanner(
    action: BannerAction = BannerAction.NONE,
    onAction: () -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = R.drawable.enterprise_banner),
                contentDescription = "Enterprise",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxHeight(),
            )
            Spacer(Modifier.weight(1f))
            when (action) {
                BannerAction.SETTINGS -> IconButton(onClick = onAction) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = BannerAccent,
                    )
                }
                BannerAction.BACK -> IconButton(onClick = onAction) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = BannerAccent,
                    )
                }
                BannerAction.NONE -> Unit
            }
        }
        HorizontalDivider(color = Color(0xFFE6E9EE))
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewMain() {
    GBGPreviewView {
        EnterpriseBanner(action = BannerAction.SETTINGS)
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewBack() {
    GBGPreviewView {
        EnterpriseBanner(action = BannerAction.BACK)
    }
}
