package com.minerrelay.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minerrelay.app.data.model.MiningStatus
import com.minerrelay.app.mining.AddressValidationResult
import com.minerrelay.app.mining.MiningEngine
import com.minerrelay.app.mining.SecurityValidator
import com.minerrelay.app.ui.theme.BitcoinOrange
import com.minerrelay.app.ui.theme.CyberCyan
import com.minerrelay.app.ui.theme.DangerRed
import com.minerrelay.app.ui.theme.DarkCard
import com.minerrelay.app.ui.theme.DarkSurface
import com.minerrelay.app.ui.theme.DarkSurfaceVariant
import com.minerrelay.app.ui.theme.NeonEmerald
import com.minerrelay.app.ui.theme.TextMuted
import com.minerrelay.app.ui.theme.TextPrimary
import com.minerrelay.app.ui.theme.TextSecondary
import com.minerrelay.app.ui.theme.WarningAmber
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.shader.ShaderProvider

@Composable
fun HonestDisclaimerBanner(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().testTag("honest_disclaimer_banner"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF102038)),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = "Information de fonctionnement",
                tint = CyberCyan,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "L'application doit rester active (service en avant-plan) pour relayer les calculs. Les gains dépendent du pool et du coût du service de calcul ; aucun rendement n'est garanti.",
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun StatusBadge(status: MiningStatus, modifier: Modifier = Modifier) {
    val color = when (status) {
        MiningStatus.IDLE -> TextMuted
        MiningStatus.CONNECTING -> WarningAmber
        MiningStatus.ACTIVE -> NeonEmerald
        MiningStatus.WAITING_WIFI, MiningStatus.WAITING_CHARGING, MiningStatus.PAUSED -> WarningAmber
        MiningStatus.ERROR -> DangerRed
    }
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
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = status.labelFr,
                color = color,
                fontSize = 11.sp,
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
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun RealMeasuredHashrateChartCard(
    historyHps: List<Float>,
    hashrate15mHps: Double,
    hashrate24hHps: Double,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val chartData = remember(historyHps) {
        when {
            historyHps.size >= 2 -> historyHps
            historyHps.size == 1 -> listOf(historyHps.first(), historyHps.first())
            else -> listOf(0f, 0f)
        }
    }

    LaunchedEffect(chartData) {
        modelProducer.runTransaction {
            lineSeries {
                series(chartData.map { it.toDouble() })
            }
        }
    }

    val axisLabel = rememberTextComponent(color = TextSecondary, textSize = 10.sp)
    val axisLine = rememberLineComponent(fill = fill(DarkSurfaceVariant), thickness = 1.dp)
    val guideline = rememberLineComponent(fill = fill(DarkSurfaceVariant.copy(alpha = 0.45f)), thickness = 1.dp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("realtime_hashrate_chart_card"),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = AppIcons.ShowChart,
                        contentDescription = "Graphique du hashrate mesuré",
                        tint = BitcoinOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Hashrate Mesuré (Parts Acceptées)",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Calculé uniquement depuis les parts validées par le pool",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = (if (isActive) NeonEmerald else TextMuted).copy(alpha = 0.14f)
                ) {
                    Text(
                        text = if (isActive) "Mesure live" else "Inactif",
                        color = if (isActive) NeonEmerald else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.3f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Fenêtre 15 min", color = TextMuted, fontSize = 9.sp)
                        Text(
                            text = MiningEngine.formatHashrate(hashrate15mHps),
                            color = CyberCyan,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.3f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Fenêtre 24 h", color = TextMuted, fontSize = 9.sp)
                        Text(
                            text = MiningEngine.formatHashrate(hashrate24hHps),
                            color = BitcoinOrange,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.28f))
                    .padding(horizontal = 6.dp, vertical = 8.dp)
            ) {
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberLineCartesianLayer(
                            lineProvider = LineCartesianLayer.LineProvider.series(
                                LineCartesianLayer.rememberLine(
                                    fill = LineCartesianLayer.LineFill.single(fill(BitcoinOrange)),
                                    areaFill = LineCartesianLayer.AreaFill.single(
                                        fill(
                                            ShaderProvider.verticalGradient(
                                                BitcoinOrange.copy(alpha = 0.35f).toArgb(),
                                                Color.Transparent.toArgb()
                                            )
                                        )
                                    )
                                )
                            )
                        ),
                        startAxis = VerticalAxis.rememberStart(
                            label = axisLabel,
                            line = axisLine,
                            guideline = guideline
                        ),
                        bottomAxis = HorizontalAxis.rememberBottom(
                            label = axisLabel,
                            line = axisLine,
                            guideline = null
                        )
                    ),
                    modelProducer = modelProducer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(154.dp)
                        .testTag("vico_hashrate_line_chart")
                )
            }
        }
    }
}

@Composable
fun AddWalletDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, address: String) -> AddressValidationResult
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun validateInput(input: String) {
        address = input
        if (input.isBlank()) {
            errorMessage = null
            return
        }
        errorMessage = when (val res = SecurityValidator.validateBitcoinMainnetAddress(input)) {
            is AddressValidationResult.DangerousSeedPhraseDetected -> res.message
            is AddressValidationResult.DangerousPrivateKeyDetected -> res.message
            is AddressValidationResult.InvalidFormat -> res.reason
            AddressValidationResult.Valid -> null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.Security, contentDescription = null, tint = CyberCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ajouter une adresse Bitcoin", color = TextPrimary, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Saisissez uniquement une adresse publique Bitcoin valide (1..., 3... ou bc1...). Ne collez jamais de phrase secrète ni de clé privée.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                AnimatedVisibility(visible = errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DangerRed.copy(alpha = 0.16f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed)
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = errorMessage.orEmpty(), color = DangerRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du portefeuille") },
                    modifier = Modifier.fillMaxWidth().testTag("wallet_name_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = DarkSurfaceVariant)
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { validateInput(it) },
                    label = { Text("Adresse publique Bitcoin (1..., 3..., bc1...)") },
                    modifier = Modifier.fillMaxWidth().testTag("wallet_address_input"),
                    isError = errorMessage != null,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (errorMessage != null) DangerRed else CyberCyan,
                        unfocusedBorderColor = if (errorMessage != null) DangerRed else DarkSurfaceVariant
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val result = onSave(name, address)
                    if (result is AddressValidationResult.Valid) {
                        onDismiss()
                    } else if (result is AddressValidationResult.InvalidFormat) {
                        errorMessage = result.reason
                    }
                },
                enabled = address.isNotBlank() && errorMessage == null,
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
fun ArchitectureGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.HelpOutline, contentDescription = null, tint = CyberCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Architecture Relais Stratum v1", color = TextPrimary, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "1. Pool (Stratum v1 TCP) ⇄ Application ⇄ Service de calcul distant\n" +
                        "L'application ouvre une socket TCP vers le pool (mining.subscribe, mining.authorize, mining.notify).",
                    color = TextPrimary,
                    fontSize = 12.sp
                )
                Text(
                    text = "2. En-tête 80 octets & lots de nonces\n" +
                        "À chaque job, l'app assemble le coinbase + arbre de Merkle + en-tête de 80 octets et envoie des intervalles de nonces au service distant.",
                    color = TextPrimary,
                    fontSize = 12.sp
                )
                Text(
                    text = "3. Re-vérification locale double SHA-256\n" +
                        "Chaque nonce renvoyé par le service est re-hashé localement par double SHA-256 et comparé à la cible avant d'être soumis via mining.submit.",
                    color = NeonEmerald,
                    fontSize = 12.sp
                )
                Text(
                    text = "4. Service en avant-plan & transparence\n" +
                        "L'application doit rester active via sa notification permanente pour relayer les calculs. Aucun rendement n'est garanti.",
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
                Text("Fermer", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = DarkSurface
    )
}
