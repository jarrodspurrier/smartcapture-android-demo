package com.gbg.smartcapture.bigmagic.compositions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gbg.smartcapture.bigmagic.ivs.IvsConfig
import com.gbg.smartcapture.commons.compositions.GBGPreviewView

private val BrandDeep = Color(0xFF0F2D4A)
private val BrandMuted = Color(0xFF5B6B7C)
private val TrackColor = Color(0xFFE6E9EE)

@Composable
fun PollingView(attempt: Int) {
    val total = IvsConfig.POLL_MAX_ATTEMPTS
    val clamped = attempt.coerceIn(1, total)
    val progress = clamped.toFloat() / total.toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            color = BrandDeep,
            strokeWidth = 4.dp,
            modifier = Modifier.size(52.dp),
        )
        Spacer(Modifier.size(20.dp))
        Text(
            text = "Verifying your document",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = BrandDeep,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = "Usually takes 10–20 seconds. Hang tight.",
            fontSize = 14.sp,
            color = BrandMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(28.dp))
        LinearProgressIndicator(
            progress = { progress },
            color = BrandDeep,
            trackColor = TrackColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = "Check $clamped of $total",
            fontSize = 12.sp,
            color = BrandMuted,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewPolling() {
    GBGPreviewView {
        PollingView(attempt = 3)
    }
}
