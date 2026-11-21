package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.ApiKeyHealth
import com.example.ai.ApiKeyState
import com.example.mining.MiningTelemetry
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun SettingsScreen(
    apiKeys: List<String>,
    apiKeyHealth: Map<String, ApiKeyHealth> = emptyMap(),
    isCheckingApiKeys: Boolean = false,
    telemetry: MiningTelemetry,
    onSaveApiKeys: (List<String>) -> Unit,
    onVerifyApiKeys: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val keyInputs = remember {
        mutableStateListOf<String>().apply {
            val initial = if (apiKeys.size >= 3) apiKeys else apiKeys + List(3 - apiKeys.size) { "" }
            addAll(initial)
        }
    }

    LaunchedEffect(apiKeys) {
        val normalized = if (apiKeys.size >= 3) apiKeys else apiKeys + List(3 - apiKeys.size) { "" }
        if (keyInputs.toList() != normalized) {
            keyInputs.clear()
            keyInputs.addAll(normalized)
        }
    }

    val visibleKeyIndices = remember { mutableStateMapOf<Int, Boolean>() }

    val savedActiveCount = telemetry.activeApiKeysCount
    val draftFilledCount = keyInputs.count { it.isNotBlank() }
    val draftMultiplier = if (draftFilledCount == 0) 1.0 else 1.0 + (draftFilledCount * 0.65)
    val optimizationProgress = (draftFilledCount / 3f).coerceIn(0f, 1f)
    val hasUnsavedChanges = keyInputs.map { it.trim() } != apiKeys.map { it.trim() }

    val unresponsiveCount = keyInputs.count { value ->
        val trimmed = value.trim()
        trimmed.isNotBlank() && apiKeyHealth[trimmed]?.state == ApiKeyState.UNRESPONSIVE
    }

    val statusColor = when {
        unresponsiveCount > 0 -> DangerRed
        draftFilledCount >= 3 -> NeonEmerald
        draftFilledCount > 0 -> WarningAmber
        else -> CyberCyan
    }

    val statusLabel = when {
        unresponsiveCount > 0 -> "Alerte : $unresponsiveCount clé(s) ne répond(ent) pas"
        draftFilledCount >= 4 -> "Ultra Parallèle ($draftFilledCount clés)"
        draftFilledCount == 3 -> "Optimisation Maximale (3 clés)"
        draftFilledCount in 1..2 -> "Partiel ($draftFilledCount/3 clés)"
        else -> "Standard (0 clé API)"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
    ) {
        // En-tête Paramètres & État d'Optimisation des Calculs
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_optimization_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1E32)),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, statusColor.copy(alpha = 0.55f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(statusColor.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, tint = statusColor, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Paramètres de Calcul IA",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = statusLabel,
                                    color = statusColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = statusColor.copy(alpha = 0.16f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "×${String.format("%.1f", draftMultiplier)}",
                                color = statusColor,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Saisissez et sauvegardez manuellement plusieurs clés API (3 minimum recommandées) pour démultiplier la vitesse de calcul et de réflexion des serveurs IA.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { optimizationProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = statusColor,
                        trackColor = Color.Black.copy(alpha = 0.45f)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.35f)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("Clés actives", color = TextMuted, fontSize = 9.sp)
                                    Text("$savedActiveCount sauvegardée(s)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.35f)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("Canaux parallèles", color = TextMuted, fontSize = 9.sp)
                                    Text("${1 + savedActiveCount} nœud(s) • ${telemetry.aiLatencyMs}ms", color = NeonEmerald, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Alerte rouge si au moins une clé API ne répond pas
        if (unresponsiveCount > 0) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_api_key_alert_banner"),
                    colors = CardDefaults.cardColors(containerColor = DangerRed.copy(alpha = 0.16f)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, DangerRed)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alerte clé API",
                            tint = DangerRed,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Alerte : $unresponsiveCount clé(s) API ne répond(ent) pas !",
                                color = DangerRed,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Les clés en rouge ci-dessous sont injoignables ou invalides. Remplacez-les ou relancez un test.",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Titre de section des champs manuels
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Dns, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Configuration Manuelle des Clés API (${keyInputs.size})",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                if (hasUnsavedChanges) {
                    Text(
                        text = "Modifications non sauvées",
                        color = WarningAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Liste des champs de saisie manuelle des clés API (3 slots minimum + slots supplémentaires)
        itemsIndexed(keyInputs) { index, keyValue ->
            val isVisible = visibleKeyIndices[index] == true
            val isFilled = keyValue.isNotBlank()
            val health = apiKeyHealth[keyValue.trim()]
            val isUnresponsive = isFilled && health?.state == ApiKeyState.UNRESPONSIVE
            val isHealthy = isFilled && health?.state == ApiKeyState.HEALTHY
            val isChecking = isFilled && health?.state == ApiKeyState.CHECKING

            val slotTitle = when (index) {
                0 -> "Clé API #1 • Nœud Principal"
                1 -> "Clé API #2 • Réflexion & Relais"
                2 -> "Clé API #3 • Calcul Parallèle"
                else -> "Clé API #${index + 1} • Accélération Turbo"
            }

            val cardBorderColor = when {
                isUnresponsive -> DangerRed
                isHealthy -> NeonEmerald
                isFilled -> NeonEmerald.copy(alpha = 0.45f)
                else -> DarkSurfaceVariant
            }
            val cardBgColor = if (isUnresponsive) DangerRed.copy(alpha = 0.08f) else DarkCard

            val badgeText = when {
                isUnresponsive -> "Ne répond pas"
                isChecking -> "Test..."
                isHealthy -> "Active"
                isFilled -> "Prête"
                else -> "Vide"
            }
            val badgeColor = when {
                isUnresponsive -> DangerRed
                isChecking -> WarningAmber
                isFilled -> NeonEmerald
                else -> TextMuted
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_api_key_card_$index"),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = androidx.compose.foundation.BorderStroke(
                    if (isUnresponsive) 1.5.dp else 1.dp,
                    cardBorderColor
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when {
                                    isUnresponsive -> Icons.Default.Warning
                                    isFilled -> Icons.Default.CheckCircle
                                    else -> Icons.Default.Key
                                },
                                contentDescription = if (isUnresponsive) "Clé API ne répond pas" else null,
                                tint = when {
                                    isUnresponsive -> DangerRed
                                    isFilled -> NeonEmerald
                                    else -> CyberCyan
                                },
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = slotTitle,
                                color = if (isUnresponsive) DangerRed else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = badgeColor.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = badgeText,
                                    color = badgeColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            if (index >= 3) {
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { keyInputs.removeAt(index) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Supprimer cette clé",
                                        tint = DangerRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = keyValue,
                        onValueChange = { newValue -> keyInputs[index] = newValue },
                        placeholder = {
                            Text(
                                "Saisissez ou collez votre clé API #${index + 1}...",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        },
                        singleLine = true,
                        isError = isUnresponsive,
                        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val pasted = clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
                                        if (!pasted.isNullOrBlank()) {
                                            keyInputs[index] = pasted
                                        } else {
                                            Toast.makeText(context, "Presse-papier vide", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ContentPaste,
                                        contentDescription = "Coller",
                                        tint = if (isUnresponsive) DangerRed else CyberCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                if (isFilled) {
                                    IconButton(
                                        onClick = { keyInputs[index] = "" },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Effacer",
                                            tint = TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { visibleKeyIndices[index] = !isVisible },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (isVisible) "Masquer" else "Afficher",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = if (isUnresponsive) DangerRed else TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_api_key_input_$index"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isUnresponsive) DangerRed else CyberCyan,
                            unfocusedBorderColor = if (isUnresponsive) DangerRed else DarkSurfaceVariant,
                            errorBorderColor = DangerRed,
                            errorTextColor = DangerRed
                        )
                    )

                    if (isUnresponsive) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "⚠️ ${health?.message ?: "Cette clé API ne répond pas"}",
                            color = DangerRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("settings_api_key_error_$index")
                        )
                    }
                }
            }
        }

        // Boutons Ajouter une clé, Tester les clés & Sauvegarder manuellement
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { keyInputs.add("") },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("settings_add_key_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ajouter un emplacement de clé API supplémentaire",
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                if (savedActiveCount > 0) {
                    OutlinedButton(
                        onClick = onVerifyApiKeys,
                        enabled = !isCheckingApiKeys,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (unresponsiveCount > 0) DangerRed else NeonEmerald.copy(alpha = 0.65f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("settings_verify_api_keys_button")
                    ) {
                        if (isCheckingApiKeys) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = CyberCyan,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Vérification des clés API...", color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        } else {
                            Icon(
                                imageVector = if (unresponsiveCount > 0) Icons.Default.Warning else Icons.Default.Bolt,
                                contentDescription = null,
                                tint = if (unresponsiveCount > 0) DangerRed else NeonEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tester la réponse des clés API",
                                color = if (unresponsiveCount > 0) DangerRed else NeonEmerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        onSaveApiKeys(keyInputs.toList())
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (draftFilledCount >= 3) NeonEmerald else CyberCyan,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("settings_save_api_keys_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (draftFilledCount > 0) {
                            "SAUVEGARDER $draftFilledCount CLÉ(S) API (×${String.format("%.1f", draftMultiplier)})"
                        } else {
                            "SAUVEGARDER LES PARAMÈTRES"
                        },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
