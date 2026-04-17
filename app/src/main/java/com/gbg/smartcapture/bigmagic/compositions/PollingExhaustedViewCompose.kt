package com.gbg.smartcapture.bigmagic.compositions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gbg.smartcapture.commons.compositions.GBGPreviewView
import com.gbg.smartcapture.commons.compositions.components.PrimaryButton
import com.gbg.smartcapture.commons.compositions.components.SecondaryButton

private val BrandDeep = Color(0xFF0F2D4A)
private val BrandMuted = Color(0xFF5B6B7C)
private val HintBg = Color(0xFFFFF4E5)
private val HintFg = Color(0xFF7A4E00)

@Composable
fun PollingExhaustedView(
    onTryAgain: () -> Unit,
    onReset: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(HintBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = HintFg,
                modifier = Modifier.size(38.dp),
            )
        }
        Spacer(Modifier.size(20.dp))
        Text(
            text = "Still working on it",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = BrandDeep,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(10.dp))
        Text(
            text = "Verification is taking longer than usual. Keep checking, or start over.",
            fontSize = 15.sp,
            color = BrandMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        Spacer(Modifier.weight(1f))

        PrimaryButton(
            text = "Check again",
            onSubmit = onTryAgain,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.size(8.dp))
        SecondaryButton(
            text = "Start over",
            onSubmit = onReset,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewExhausted() {
    GBGPreviewView {
        PollingExhaustedView(onTryAgain = {}, onReset = {})
    }
}
