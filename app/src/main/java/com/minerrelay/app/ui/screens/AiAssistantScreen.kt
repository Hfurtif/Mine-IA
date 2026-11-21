package com.minerrelay.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minerrelay.app.ai.AiMessage
import com.minerrelay.app.ui.components.AppIcons
import com.minerrelay.app.ui.theme.CyberCyan
import com.minerrelay.app.ui.theme.DarkBg
import com.minerrelay.app.ui.theme.DarkCard
import com.minerrelay.app.ui.theme.DarkSurfaceVariant
import com.minerrelay.app.ui.theme.NeonEmerald
import com.minerrelay.app.ui.theme.TextMuted
import com.minerrelay.app.ui.theme.TextPrimary
import com.minerrelay.app.ui.theme.TextSecondary

@Composable
fun AiAssistantScreen(
    messages: List<AiMessage>,
    isLoading: Boolean,
    hasGeminiKey: Boolean,
    onSendPrompt: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var promptInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val quickPrompts = listOf(
        "Explique mon hashrate mesuré actuel (15 min et 24 h)",
        "Pourquoi ai-je des parts rejetées ou périmées ?",
        "Compare le coût cumulé du service et mes paiements BTC"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = CyberCyan)
                }
                Icon(AppIcons.AutoAwesome, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Assistant Gemini (Analyse des chiffres réels)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        text = if (hasGeminiKey) "Clé Gemini configurée (distincte du calcul)" else "Configurez une clé Gemini dans Réglages",
                        color = if (hasGeminiKey) NeonEmerald else TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickPrompts) { prompt ->
                Surface(
                    onClick = { onSendPrompt(prompt) },
                    shape = RoundedCornerShape(16.dp),
                    color = DarkSurfaceVariant
                ) {
                    Text(
                        text = prompt,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (msg.isUser) Color(0xFF004D61) else DarkCard
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant),
                        modifier = Modifier.fillMaxWidth(fraction = 0.88f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (msg.isUser) "Vous" else "Assistant",
                                color = if (msg.isUser) CyberCyan else NeonEmerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = msg.text, color = TextPrimary, fontSize = 12.sp, lineHeight = 17.sp)
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CyberCyan, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyse des métriques réelles...", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    placeholder = { Text("Question sur les chiffres réels du relais...", color = TextMuted, fontSize = 12.sp) },
                    modifier = Modifier.weight(1f).testTag("ai_prompt_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    maxLines = 3
                )
                IconButton(
                    onClick = {
                        val text = promptInput
                        promptInput = ""
                        onSendPrompt(text)
                    },
                    enabled = promptInput.isNotBlank() && !isLoading,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (promptInput.isNotBlank()) CyberCyan else DarkSurfaceVariant)
                        .testTag("send_prompt_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Envoyer",
                        tint = if (promptInput.isNotBlank()) Color.Black else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
