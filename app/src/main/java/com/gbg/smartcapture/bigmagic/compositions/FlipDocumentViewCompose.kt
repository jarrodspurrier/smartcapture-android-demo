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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
private val SuccessBg = Color(0xFFDCFCE7)
private val SuccessFg = Color(0xFF14532D)
private val CardBg = Color(0xFFF4F6F9)

@Composable
fun FlipDocumentView(
    onContinue: () -> Unit,
    onCancel: () -> Unit,
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
                .background(SuccessBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = SuccessFg,
                modifier = Modifier.size(40.dp),
            )
        }
        Spacer(Modifier.size(20.dp))
        Text(
            text = "Front captured",
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = BrandDeep,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(10.dp))
        Text(
            text = "Now flip your ID over and capture the back.",
            fontSize = 15.sp,
            color = BrandMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        )

        Spacer(Modifier.size(28.dp))
        TipCard()

        Spacer(Modifier.weight(1f))

        PrimaryButton(
            text = "Capture back",
            onSubmit = onContinue,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.size(8.dp))
        SecondaryButton(
            text = "Cancel",
            onSubmit = onCancel,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TipCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = "TIP",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = BrandMuted,
        )
        Spacer(Modifier.size(4.dp))
        Text(
            text = "Keep the ID on a flat, well-lit surface. Avoid glare and shadows.",
            fontSize = 14.sp,
            color = BrandDeep,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewFlip() {
    GBGPreviewView {
        FlipDocumentView(onContinue = {}, onCancel = {})
    }
}
