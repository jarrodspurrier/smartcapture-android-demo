package com.gbg.smartcapture.bigmagic.compositions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
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
import com.gbg.smartcapture.commons.compositions.GBGPreviewView

private val BrandDeep = Color(0xFF0F2D4A)
private val BrandMuted = Color(0xFF5B6B7C)

@Composable
fun SubmittingView(message: String = "Submitting images…") {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            color = BrandDeep,
            strokeWidth = 4.dp,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.size(20.dp))
        Text(
            text = "PLEASE WAIT",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = BrandMuted,
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = message,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = BrandDeep,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSubmitting() {
    GBGPreviewView {
        SubmittingView()
    }
}
