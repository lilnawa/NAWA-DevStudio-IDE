package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AIProviderId
import com.example.ui.theme.*

fun getProviderColor(id: AIProviderId): Color {
    return when (id) {
        AIProviderId.OPENAI -> Color(0xFF10A37F)
        AIProviderId.GEMINI -> Color(0xFF38BDF8)
        AIProviderId.CLAUDE -> Color(0xFFD97706)
        AIProviderId.DEEPSEEK -> Color(0xFF4F46E5)
        AIProviderId.MISTRAL -> Color(0xFFEA580C)
        AIProviderId.GROQ -> Color(0xFFF43F5E)
        AIProviderId.OPENROUTER -> Color(0xFF9333EA)
        AIProviderId.OLLAMA -> Color(0xFFFACC15)
    }
}

@Composable
fun ProviderBadge(
    providerId: AIProviderId,
    modifier: Modifier = Modifier,
    modelName: String? = null
) {
    val brandColor = getProviderColor(providerId)

    Row(
        modifier = modifier
            .background(brandColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .border(1.dp, brandColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(brandColor, CircleShape)
        )
        Text(
            text = providerId.shortName,
            color = brandColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        if (!modelName.isNullOrBlank()) {
            Text(
                text = "• $modelName",
                color = TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun ConnectionStatusDot(
    testedSuccess: Boolean?,
    isEnabled: Boolean,
    hasApiKey: Boolean,
    requiresKey: Boolean,
    modifier: Modifier = Modifier
) {
    val (color, text) = when {
        !isEnabled -> Pair(TextMuted, "Disabled")
        testedSuccess == true -> Pair(NeonGreen, "Connected")
        testedSuccess == false -> Pair(NeonRed, "Error")
        requiresKey && !hasApiKey -> Pair(NeonAmber, "Key Needed")
        else -> Pair(CyanPrimary, "Ready to Test")
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
