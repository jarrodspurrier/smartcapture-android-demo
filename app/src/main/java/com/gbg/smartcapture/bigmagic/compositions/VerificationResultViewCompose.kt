package com.gbg.smartcapture.bigmagic.compositions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gbg.smartcapture.bigmagic.ivs.SessionStatus
import com.gbg.smartcapture.bigmagic.ivs.SessionStatusResponse
import com.gbg.smartcapture.bigmagic.ivs.VerificationResult
import com.gbg.smartcapture.commons.compositions.GBGPreviewView
import com.gbg.smartcapture.commons.compositions.components.PrimaryButton
import com.gbg.smartcapture.commons.compositions.components.SecondaryButton

private val BrandDeep = Color(0xFF0F2D4A)
private val BrandMuted = Color(0xFF5B6B7C)
private val CardBorder = Color(0xFFE6E9EE)
private val SubtleBg = Color(0xFFF4F6F9)

@Composable
fun VerificationResultView(
    response: SessionStatusResponse,
    onDone: () -> Unit,
    onRefresh: () -> Unit = {},
) {
    val status = response.statusEnum()
    val tone = toneFor(status, response.result?.decision)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OutcomeHero(tone = tone, decision = response.result?.decision)
            Spacer(Modifier.size(20.dp))
            SectionCard(title = "Session") {
                DetailRow("Status", response.status)
                DetailRow("Session ID", response.sessionId, monospace = true)
                response.completedAt?.let { DetailRow("Completed at", it) }
            }

            val result = response.result
            if (result != null && result.hasDetails()) {
                Spacer(Modifier.size(12.dp))
                SectionCard(title = "Document") {
                    result.ivsOverallResult?.let { DetailRow("Overall", it) }
                    result.documentVerified?.let { DetailRow("Verified", boolLabel(it)) }
                    result.documentExpired?.let { DetailRow("Expired", boolLabel(it)) }
                    result.failureReason?.takeIf { it.isNotBlank() }?.let {
                        DetailRow("Failure reason", it)
                    }
                }
            }
            if (result != null && !result.attentionNotices.isNullOrEmpty()) {
                Spacer(Modifier.size(12.dp))
                AttentionNotices(result.attentionNotices)
            }
            Spacer(Modifier.size(16.dp))
        }
        SecondaryButton(
            text = "Refresh",
            onSubmit = onRefresh,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.size(8.dp))
        PrimaryButton(
            text = "Done",
            onSubmit = onDone,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun OutcomeHero(tone: Tone, decision: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(tone.background),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = tone.icon,
                contentDescription = null,
                tint = tone.onBackground,
                modifier = Modifier.size(44.dp),
            )
        }
        Spacer(Modifier.size(14.dp))
        Text(
            text = tone.title,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            color = BrandDeep,
            textAlign = TextAlign.Center,
        )
        decision?.let {
            Spacer(Modifier.size(4.dp))
            Text(
                text = "Decision: $it",
                fontSize = 14.sp,
                color = BrandMuted,
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = BrandMuted,
        )
        Spacer(Modifier.size(8.dp))
        content()
    }
}

@Composable
private fun DetailRow(label: String, value: String, monospace: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = BrandMuted,
            modifier = Modifier.padding(end = 12.dp),
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = BrandDeep,
            textAlign = TextAlign.End,
            fontFamily = if (monospace) androidx.compose.ui.text.font.FontFamily.Monospace else null,
        )
    }
}

@Composable
private fun AttentionNotices(notices: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SubtleBg)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = "ATTENTION NOTICES",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF7A4E00),
        )
        Spacer(Modifier.size(6.dp))
        notices.forEach { notice ->
            Row(
                modifier = Modifier.padding(vertical = 3.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(text = "•  ", fontSize = 14.sp, color = BrandDeep)
                Text(text = notice, fontSize = 14.sp, color = BrandDeep)
            }
        }
    }
}

private fun VerificationResult.hasDetails(): Boolean =
    ivsOverallResult != null ||
        documentVerified != null ||
        documentExpired != null ||
        !failureReason.isNullOrBlank()

private fun boolLabel(value: Boolean): String = if (value) "Yes" else "No"

private data class Tone(
    val title: String,
    val background: Color,
    val onBackground: Color,
    val icon: ImageVector,
)

private fun toneFor(status: SessionStatus, decision: String?): Tone {
    val lower = decision?.lowercase()
    return when {
        status == SessionStatus.Completed && lower == "pass" ->
            Tone("Verified", Color(0xFFDCFCE7), Color(0xFF14532D), Icons.Default.Check)
        status == SessionStatus.Completed && lower == "attention" ->
            Tone("Needs attention", Color(0xFFFEF3C7), Color(0xFF78350F), Icons.Default.Warning)
        status == SessionStatus.Completed && lower == "fail" ->
            Tone("Failed", Color(0xFFFEE2E2), Color(0xFF7F1D1D), Icons.Default.Close)
        status == SessionStatus.Failed ->
            Tone("Failed", Color(0xFFFEE2E2), Color(0xFF7F1D1D), Icons.Default.Close)
        status == SessionStatus.Error ->
            Tone("Error", Color(0xFFFEE2E2), Color(0xFF7F1D1D), Icons.Default.Close)
        status == SessionStatus.TimedOut ->
            Tone("Timed out", Color(0xFFFEE2E2), Color(0xFF7F1D1D), Icons.Default.Close)
        else ->
            Tone(
                status.wire.ifBlank { "Unknown" }.replaceFirstChar(Char::uppercase),
                Color(0xFFE5E7EB), Color(0xFF1F2937), Icons.Default.Warning,
            )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewPass() {
    GBGPreviewView {
        VerificationResultView(
            response = SessionStatusResponse(
                sessionId = "vs_demo_pass",
                status = "completed",
                result = VerificationResult(
                    ivsOverallResult = "pass",
                    decision = "Pass",
                    documentVerified = true,
                    documentExpired = false,
                ),
                completedAt = "2026-04-16T14:01:18Z",
            ),
            onDone = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewAttention() {
    GBGPreviewView {
        VerificationResultView(
            response = SessionStatusResponse(
                sessionId = "vs_demo_attn",
                status = "completed",
                result = VerificationResult(
                    ivsOverallResult = "warn",
                    decision = "Attention",
                    documentVerified = true,
                    documentExpired = false,
                    attentionNotices = listOf(
                        "Portrait quality is low",
                        "Hologram could not be verified",
                    ),
                ),
            ),
            onDone = {},
        )
    }
}
