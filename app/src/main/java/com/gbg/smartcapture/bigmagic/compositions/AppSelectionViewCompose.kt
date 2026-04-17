package com.gbg.smartcapture.bigmagic.compositions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gbg.smartcapture.commons.compositions.GBGPreviewView
import com.gbg.smartcapture.commons.compositions.components.PrimaryButton

private val BrandDeep = Color(0xFF0F2D4A)
private val BrandMuted = Color(0xFF5B6B7C)
private val StepCardBg = Color(0xFFF4F6F9)
private val StepBadgeBg = Color(0xFF0F2D4A)
private val WarnBg = Color(0xFFFFF4E5)
private val WarnOn = Color(0xFF7A4E00)

@Composable
fun LandingView(
    hasApiKey: Boolean,
    onStart: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        Text(
            text = "Verify your ID",
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = BrandDeep,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = "Scan the front and back of your ID. It takes about 30 seconds.",
            fontSize = 15.sp,
            color = BrandMuted,
        )

        Spacer(Modifier.size(28.dp))

        StepsCard()

        Spacer(Modifier.size(20.dp))

        if (!hasApiKey) {
            ApiKeyMissingBanner()
            Spacer(Modifier.size(16.dp))
        }

        PrimaryButton(
            text = "Start verification",
            onSubmit = onStart,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.size(16.dp))
    }
}

@Composable
private fun StepsCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(StepCardBg)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        StepRow(number = 1, title = "Scan the front of your ID", hint = "Lay it flat, fill the frame.")
        StepRow(number = 2, title = "Flip it and scan the back", hint = "We'll prompt you when it's time.")
        StepRow(number = 3, title = "Get your result", hint = "Verification completes on-device and in the cloud.")
    }
}

@Composable
private fun StepRow(number: Int, title: String, hint: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(StepBadgeBg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
        Spacer(Modifier.size(14.dp))
        Column {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandDeep,
            )
            Text(
                text = hint,
                fontSize = 13.sp,
                color = BrandMuted,
            )
        }
    }
}

@Composable
private fun ApiKeyMissingBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(WarnBg)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = WarnOn,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.size(10.dp))
        Text(
            text = "IVS API key missing. Add `ivsApiKey=<key>` to local.properties and rebuild.",
            fontSize = 13.sp,
            color = WarnOn,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewHasKey() {
    GBGPreviewView {
        LandingView(hasApiKey = true, onStart = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewNoKey() {
    GBGPreviewView {
        LandingView(hasApiKey = false, onStart = {})
    }
}
