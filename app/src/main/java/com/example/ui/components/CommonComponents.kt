package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CryptoCurrency
import com.example.data.model.MiningStatus
import com.example.data.model.SupportedCryptos
import com.example.mining.AddressValidationResult
import com.example.mining.SecurityValidator
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.HotOrange
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun StatusBadge(status: MiningStatus, modifier: Modifier = Modifier) {
    val (color, text) = when (status) {
        MiningStatus.IDLE -> TextMuted to "Prêt"
        MiningStatus.CONFIGURING -> CyberCyan to "Configuration"
        MiningStatus.DOWNLOADING -> CyberCyan to "Téléchargement"
        MiningStatus.VERIFYING -> CyberCyan to "Vérification SHA256"
        MiningStatus.CONNECTING -> WarningAmber to "Connexion Pool"
        MiningStatus.MINING -> NeonEmerald to "Minage Actif"
        MiningStatus.PAUSED -> WarningAmber to "En Pause"
        MiningStatus.OVERHEAT_PROTECTION -> DangerRed to "Arrêt Température !"
        MiningStatus.ERROR -> DangerRed to "Erreur"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (status == MiningStatus.MINING) color.copy(alpha = alpha) else color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun MetricStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun HashrateSparkline(
    history: List<Float>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (history.isEmpty()) return@Canvas

        val maxVal = (history.maxOrNull() ?: 100f).coerceAtLeast(10f)
        val minVal = (history.minOrNull() ?: 0f).coerceAtLeast(0f)
        val range = (maxVal - minVal).coerceAtLeast(1f)

        val width = size.width
        val height = size.height

        val stepX = width / (history.size - 1).coerceAtLeast(1)

        val path = Path()
        val fillPath = Path()

        fillPath.moveTo(0f, height)

        history.forEachIndexed { index, value ->
            val x = index * stepX
            val normalizedY = 1f - ((value - minVal) / range)
            val y = (normalizedY * (height * 0.75f)) + (height * 0.12f)

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        fillPath.lineTo(width, height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.28f), Color.Transparent)
            )
        )

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun TemperatureGauge(
    currentTemp: Float,
    maxSafeTemp: Float,
    modifier: Modifier = Modifier
) {
    val tempColor = when {
        currentTemp >= maxSafeTemp -> DangerRed
        currentTemp >= 68.0f -> HotOrange
        currentTemp >= 58.0f -> WarningAmber
        else -> NeonEmerald
    }

    val progress = (currentTemp / 100.0f).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCard)
            .border(1.dp, DarkSurfaceVariant, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Température Matérielle",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${String.format("%.1f", currentTemp)}°C",
                color = tempColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(DarkSurfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(NeonEmerald, WarningAmber, HotOrange, DangerRed)
                        )
                    )
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("25°C", color = TextMuted, fontSize = 10.sp)
            Text("Seuil sécurité : ${maxSafeTemp.toInt()}°C", color = TextSecondary, fontSize = 10.sp)
            Text("100°C", color = TextMuted, fontSize = 10.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWalletDialog(
    initialCrypto: String = "XMR",
    onDismiss: () -> Unit,
    onSave: (name: String, cryptoSymbol: String, address: String, isDefault: Boolean) -> AddressValidationResult
) {
    var name by remember { mutableStateOf("") }
    var selectedCrypto by remember { mutableStateOf(initialCrypto) }
    var address by remember { mutableStateOf("") }
    var isDefault by remember { mutableStateOf(true) }
    var expandedDropdown by remember { mutableStateOf(false) }

    // Instant security live check
    var securityWarning by remember { mutableStateOf<String?>(null) }
    var hasSeedPhrase by remember { mutableStateOf(false) }

    fun checkSecurity(input: String) {
        address = input
        val res = SecurityValidator.validatePublicAddress(selectedCrypto, input)
        when (res) {
            is AddressValidationResult.DangerousSeedPhraseDetected -> {
                securityWarning = res.message
                hasSeedPhrase = true
            }
            is AddressValidationResult.DangerousPrivateKeyDetected -> {
                securityWarning = res.message
                hasSeedPhrase = true
            }
            else -> {
                securityWarning = null
                hasSeedPhrase = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = CyberCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ajouter un Portefeuille", color = TextPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mandatory Security Notice
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2537)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Règle de sécurité : Seule votre adresse PUBLIQUE de réception est nécessaire. Ne collez JAMAIS votre seed phrase (12-24 mots) ni clé privée.",
                            color = TextPrimary,
                            fontSize = 11.sp
                        )
                    }
                }

                // DANGER ALERT BANNER IF SEED PHRASE DETECTED
                AnimatedVisibility(visible = securityWarning != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DangerRed.copy(alpha = 0.18f)),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, DangerRed)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = DangerRed,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = securityWarning ?: "",
                                color = DangerRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Crypto Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown }
                ) {
                    OutlinedTextField(
                        value = SupportedCryptos.ALL.find { it.symbol == selectedCrypto }?.name ?: selectedCrypto,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cryptomonnaie") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkSurfaceVariant
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        SupportedCryptos.ALL.forEach { crypto ->
                            DropdownMenuItem(
                                text = { Text("${crypto.name} (${crypto.symbol})") },
                                onClick = {
                                    selectedCrypto = crypto.symbol
                                    expandedDropdown = false
                                    checkSecurity(address)
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du portefeuille (ex: Monero Ledger)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wallet_name_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkSurfaceVariant
                    )
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { checkSecurity(it) },
                    label = { Text("Adresse publique de réception") },
                    placeholder = {
                        Text(
                            SupportedCryptos.ALL.find { it.symbol == selectedCrypto }?.addressPrefixHint ?: "Adresse publique",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wallet_address_input"),
                    isError = hasSeedPhrase,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (hasSeedPhrase) DangerRed else CyberCyan,
                        unfocusedBorderColor = if (hasSeedPhrase) DangerRed else DarkSurfaceVariant
                    )
                )

            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val result = onSave(name, selectedCrypto, address, isDefault)
                    if (result is AddressValidationResult.Valid) {
                        onDismiss()
                    }
                },
                enabled = !hasSeedPhrase && address.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                modifier = Modifier.testTag("save_wallet_button")
            ) {
                Text("Enregistrer", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = TextSecondary)
            }
        },
        containerColor = DarkSurface
    )
}

@Composable
fun BeginnerGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HelpOutline, contentDescription = null, tint = CyberCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Fonctionnement de Miner IA", color = TextPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF092136)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Minage 100% Déporté sur Serveur IA",
                            color = NeonEmerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Text(
                    text = "1. 0% de charge sur votre téléphone :\n" +
                            "Les calculs de hachage sont 100% exécutés par les serveurs de l'IA. Votre appareil ne chauffe pas, n'use pas sa batterie et ne fait aucun calcul intensif local.",
                    color = TextPrimary,
                    fontSize = 12.sp
                )

                Text(
                    text = "2. Minage Autonome en Ligne (24h/24) :\n" +
                            "Une fois l'adresse IP du serveur IA et votre adresse wallet configurées, vous pouvez fermer l'application ou vous déconnecter d'Internet. L'IA continue ses calculs dans le cloud en toute autonomie.",
                    color = TextPrimary,
                    fontSize = 12.sp
                )

                Text(
                    text = "3. Versements directs sur votre portefeuille :\n" +
                            "Les récompenses des calculs résolus sont automatiquement créditées sur votre adresse publique de réception.",
                    color = TextPrimary,
                    fontSize = 12.sp
                )

                Text(
                    text = "4. Sécurité Non-Custodial :\n" +
                            "Ne partagez JAMAIS votre seed phrase (12-24 mots) ou clé privée. Seule votre adresse publique de réception est nécessaire.",
                    color = WarningAmber,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black)
            ) {
                Text("J'ai compris", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = DarkSurface
    )
}
