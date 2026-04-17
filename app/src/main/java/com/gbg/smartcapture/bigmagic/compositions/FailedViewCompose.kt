package com.gbg.smartcapture.bigmagic.compositions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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

@Composable
fun FailedView(reason: String, onReset: () -> Unit) {
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
                .background(Color(0xFFFEE2E2)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = Color(0xFF7F1D1D),
                modifier = Modifier.size(40.dp),
            )
        }
        Spacer(Modifier.size(20.dp))
        Text(
            text = "Verification could not complete",
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            textAlign = TextAlign.Center,
            color = Color(0xFF0F2D4A),
        )
        Spacer(Modifier.size(12.dp))
        Text(
            text = reason,
            fontSize = 15.sp,
            color = Color(0xFF5B6B7C),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Spacer(Modifier.weight(1f))
        PrimaryButton(
            text = "Start Over",
            onSubmit = onReset,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewFailed() {
    GBGPreviewView {
        FailedView(
            reason = "We couldn't reach the verification service. Check your connection and try again.",
            onReset = {},
        )
    }
}
