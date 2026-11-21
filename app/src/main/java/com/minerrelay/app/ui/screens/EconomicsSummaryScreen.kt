package com.minerrelay.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minerrelay.app.mining.MiningEngine
import com.minerrelay.app.mining.MiningTelemetry
import com.minerrelay.app.ui.components.AppIcons
import com.minerrelay.app.ui.components.HonestDisclaimerBanner
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
import java.util.Locale

@Composable
fun EconomicsSummaryScreen(
    telemetry: MiningTelemetry,
    modifier: Modifier = Modifier
) {
    val btcEur = telemetry.btcPriceEur
    val payoutsEur = btcEur?.let { telemetry.totalConfirmedPayoutsBtc * it }
    val totalSubmittedShares = telemetry.acceptedShares + telemetry.rejectedShares + telemetry.staleShares
    val acceptanceRate = if (totalSubmittedShares > 0L) {
        (telemetry.acceptedShares.toDouble() / totalSubmittedShares.toDouble()) * 100.0
    } else {
        0.0
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 14.dp)
            .testTag("economics_summary_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
    ) {
        item { HonestDisclaimerBanner() }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.TrendingUp, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Coût du service vs Paiements reçus",
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Black.copy(alpha = 0.32f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Paiements confirmés reçus (mempool.space)", color = TextSecondary, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${String.format(Locale.US, "%.8f", telemetry.totalConfirmedPayoutsBtc)} BTC",
                                color = NeonEmerald,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (payoutsEur != null) {
                                    "≈ ${String.format(Locale.US, "%.2f", payoutsEur)} € (cours mempool.space : ${String.format(Locale.US, "%.0f", btcEur)} €/BTC)"
                                } else {
                                    "Contre-valeur € indisponible (hors-ligne)"
                                },
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Black.copy(alpha = 0.32f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Coût cumulé du service de calcul distant", color = TextSecondary, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = telemetry.accumulatedServiceCostUsd?.let {
                                    "$${String.format(Locale.US, "%.4f", it)} USD"
                                } ?: "Non fourni par l'API (N/A)",
                                color = BitcoinOrange,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Lots envoyés : ${telemetry.batchesDispatched} • Latence : ${telemetry.serviceLatencyMs?.let { "$it ms" } ?: "N/A"}",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Qualité réelle des parts soumises", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    SummaryRow("Parts acceptées", "${telemetry.acceptedShares}", NeonEmerald)
                    SummaryRow("Parts rejetées", "${telemetry.rejectedShares}", DangerRed)
                    SummaryRow("Parts périmées (stale)", "${telemetry.staleShares}", WarningAmber)
                    SummaryRow("Candidats rejetés localement (SHA-256)", "${telemetry.locallyRejectedCandidates}", TextSecondary)
                    SummaryRow("Taux d'acceptation pool", "${String.format(Locale.US, "%.1f", acceptanceRate)} %", CyberCyan)
                    SummaryRow("Hashrate mesuré (15 min)", MiningEngine.formatHashrate(telemetry.hashrate15mHps), TextPrimary)
                    SummaryRow("Hashrate mesuré (24 h)", MiningEngine.formatHashrate(telemetry.hashrate24hHps), TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, fontSize = 12.sp)
        Text(text = value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
