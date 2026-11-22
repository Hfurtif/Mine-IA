package com.minerrelay.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minerrelay.app.data.model.MiningStatus
import com.minerrelay.app.data.model.RelayConfig
import com.minerrelay.app.mining.MiningEngine
import com.minerrelay.app.mining.MiningTelemetry
import com.minerrelay.app.network.PoolConnectionState
import com.minerrelay.app.security.EncryptedKeyStore
import com.minerrelay.app.ui.components.AppIcons
import com.minerrelay.app.ui.components.HonestDisclaimerBanner
import com.minerrelay.app.ui.components.MetricStatCard
import com.minerrelay.app.ui.components.RealMeasuredHashrateChartCard
import com.minerrelay.app.ui.components.StatusBadge
import com.minerrelay.app.ui.theme.BitcoinOrange
import com.minerrelay.app.ui.theme.CyberCyan
import com.minerrelay.app.ui.theme.DangerRed
import com.minerrelay.app.ui.theme.DarkBg
import com.minerrelay.app.ui.theme.DarkCard
import com.minerrelay.app.ui.theme.DarkSurfaceVariant
import com.minerrelay.app.ui.theme.NeonEmerald
import com.minerrelay.app.ui.theme.TextMuted
import com.minerrelay.app.ui.theme.TextPrimary
import com.minerrelay.app.ui.theme.TextSecondary
import com.minerrelay.app.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    telemetry: MiningTelemetry,
    relayConfig: RelayConfig,
    hasComputeKey: Boolean,
    onStartRelay: (testMode: Boolean?) -> Unit,
    onStopRelay: () -> Unit,
    onOpenSettings: () -> Unit,
    onRefreshBlockchain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRunning = telemetry.status == MiningStatus.ACTIVE ||
        telemetry.status == MiningStatus.CONNECTING ||
        telemetry.status == MiningStatus.WAITING_WIFI ||
        telemetry.status == MiningStatus.WAITING_CHARGING

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.FRANCE) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp)
    ) {
        // 1. Mention transparente obligatoire
        item {
            HonestDisclaimerBanner()
        }

        // 2. Carte État Pool Stratum v1 & Contrôles ForegroundService
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36)),
                border = androidx.compose.foundation.BorderStroke(
                    1.2.dp,
                    when {
                        telemetry.isServiceKeyUnresponsive || telemetry.computeErrorMessage != null -> DangerRed
                        telemetry.poolConnectionState == PoolConnectionState.CONNECTED -> NeonEmerald
                        else -> DarkSurfaceVariant
                    }
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Relais Stratum v1 Bitcoin",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Pool : ${telemetry.poolConnectionState.labelFr}",
                                color = if (telemetry.poolConnectionState == PoolConnectionState.CONNECTED) NeonEmerald else TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        StatusBadge(status = telemetry.status)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            val backupSuffix = if (relayConfig.isBackupPoolConfigured) {
                                " • Secours :${relayConfig.backupPoolPort}"
                            } else {
                                ""
                            }
                            Text(
                                text = if (relayConfig.isPoolConfigured) {
                                    val endpointDesc = if (telemetry.activePoolEndpoint.isNotBlank()) {
                                        telemetry.activePoolEndpoint
                                    } else {
                                        "${relayConfig.poolHost}:${relayConfig.poolPort}$backupSuffix"
                                    }
                                    "Serveur : $endpointDesc • Identifiant : ${relayConfig.poolLogin}"
                                } else {
                                    "Pool non configuré — renseignez hôte, port et identifiant dans Réglages."
                                },
                                color = if (telemetry.isUsingBackupPool) WarningAmber else TextPrimary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "BTC (mempool) : ${if (relayConfig.btcAddress.isBlank()) "Aucune" else EncryptedKeyStore.maskAddress(relayConfig.btcAddress)} • Clé calcul : ${if (hasComputeKey) "Chiffrée (Keystore)" else "Manquante"}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Détail : ${telemetry.poolDetail} • Job : ${telemetry.currentJobId ?: "Aucun"} • Diff : ${if (telemetry.currentDifficulty > 0.0) telemetry.currentDifficulty.toString() else "N/A"}",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    AnimatedVisibility(visible = telemetry.computeErrorMessage != null) {
                        Card(
                            modifier = Modifier.padding(top = 10.dp).fillMaxWidth().testTag("compute_error_banner"),
                            colors = CardDefaults.cardColors(containerColor = DangerRed.copy(alpha = 0.16f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed)
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Warning, contentDescription = "Erreur service", tint = DangerRed, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = telemetry.computeErrorMessage.orEmpty(),
                                    color = DangerRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isRunning) {
                            Button(
                                onClick = {
                                    if (!relayConfig.isPoolConfigured) {
                                        onOpenSettings()
                                    } else {
                                        onStartRelay(false)
                                    }
                                },
                                modifier = Modifier.weight(1f).height(48.dp).testTag("start_relay_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = Color.Black)
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (relayConfig.isPoolConfigured) "Démarrer Relais" else "Configurer",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            Button(
                                onClick = onStopRelay,
                                modifier = Modifier.weight(1f).height(48.dp).testTag("stop_relay_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White)
                            ) {
                                Icon(AppIcons.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Arrêter Relais", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = { onStartRelay(true) },
                            enabled = relayConfig.isPoolConfigured,
                            modifier = Modifier.height(48.dp).testTag("test_mode_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.7f))
                        ) {
                            Text("Mode Test", color = WarningAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 3. Métriques 100 % réelles (Parts & Latence / Coût)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricStatCard(
                    title = "Parts Acceptées",
                    value = "${telemetry.acceptedShares}",
                    subtitle = "Vérifiées localement + pool",
                    icon = Icons.Filled.CheckCircle,
                    accentColor = NeonEmerald,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "Rejetées / Périmées",
                    value = "${telemetry.rejectedShares} / ${telemetry.staleShares}",
                    subtitle = "Bloquées localement : ${telemetry.locallyRejectedCandidates}",
                    icon = Icons.Filled.Warning,
                    accentColor = WarningAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricStatCard(
                    title = "Latence Service",
                    value = telemetry.serviceLatencyMs?.let { "$it ms" } ?: "N/A",
                    subtitle = "Lots envoyés : ${telemetry.batchesDispatched}",
                    icon = AppIcons.Dns,
                    accentColor = CyberCyan,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "Coût Cumulé Service",
                    value = telemetry.accumulatedServiceCostUsd?.let { "$${String.format(Locale.US, "%.4f", it)}" } ?: "N/A",
                    subtitle = "Si fourni par l'API",
                    icon = AppIcons.TrendingUp,
                    accentColor = BitcoinOrange,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4. Graphique Hashrate Réel (0 H/s si aucune part acceptée)
        item {
            RealMeasuredHashrateChartCard(
                historyHps = telemetry.hashrateHistoryHps,
                hashrate15mHps = telemetry.hashrate15mHps,
                hashrate24hHps = telemetry.hashrate24hHps,
                isActive = telemetry.status == MiningStatus.ACTIVE
            )
        }

        // 5. Réponses brutes en Mode Test (si activé)
        if (telemetry.isTestMode || telemetry.rawTestResponses.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("test_mode_raw_responses_card"),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.55f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Mode Test (faible difficulté) • Réponses brutes du pool",
                            color = WarningAmber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (telemetry.rawTestResponses.isEmpty()) {
                            Text("En attente des réponses Stratum du pool...", color = TextMuted, fontSize = 11.sp)
                        } else {
                            telemetry.rawTestResponses.takeLast(5).forEach { line ->
                                Text(
                                    text = line,
                                    color = TextPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Historique réel mempool.space (aucune transaction factice)
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("recent_btc_transactions_card"),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(AppIcons.History, contentDescription = null, tint = BitcoinOrange, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Transactions Reçues (mempool.space)",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (relayConfig.btcAddress.isBlank()) "Aucune adresse BTC saisie" else EncryptedKeyStore.maskAddress(relayConfig.btcAddress),
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        IconButton(onClick = onRefreshBlockchain, modifier = Modifier.size(40.dp)) {
                            if (telemetry.isFetchingBlockchain) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CyberCyan, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.Refresh, contentDescription = "Actualiser mempool.space", tint = CyberCyan, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    telemetry.blockchainError?.let { err ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = err, color = DangerRed, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (telemetry.recentTransactions.isEmpty()) {
                        Text(
                            text = "Aucune transaction entrante détectée sur mempool.space pour cette adresse.",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            telemetry.recentTransactions.take(8).forEach { tx ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.Black.copy(alpha = 0.3f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(if (tx.confirmed) NeonEmerald else WarningAmber)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = if (tx.confirmed) "Bloc #${tx.blockHeight ?: "?"} • Confirmée" else "En attente (mempool)",
                                                    color = TextPrimary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "${dateFormatter.format(Date(tx.timestampMs))} • ${tx.txId.take(10)}…",
                                                    color = TextMuted,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                        Text(
                                            text = "+${String.format(Locale.US, "%.8f", tx.amountBtc)} BTC",
                                            color = if (tx.confirmed) NeonEmerald else WarningAmber,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
