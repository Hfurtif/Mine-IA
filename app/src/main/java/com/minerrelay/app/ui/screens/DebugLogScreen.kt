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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minerrelay.app.data.model.LogLevel
import com.minerrelay.app.mining.MiningTelemetry
import com.minerrelay.app.ui.components.AppIcons
import com.minerrelay.app.ui.theme.CyberCyan
import com.minerrelay.app.ui.theme.DangerRed
import com.minerrelay.app.ui.theme.DarkBg
import com.minerrelay.app.ui.theme.DarkCard
import com.minerrelay.app.ui.theme.DarkSurfaceVariant
import com.minerrelay.app.ui.theme.NeonEmerald
import com.minerrelay.app.ui.theme.TerminalBg
import com.minerrelay.app.ui.theme.TerminalGreen
import com.minerrelay.app.ui.theme.TextMuted
import com.minerrelay.app.ui.theme.TextPrimary
import com.minerrelay.app.ui.theme.TextSecondary
import com.minerrelay.app.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DebugLogScreen(
    telemetry: MiningTelemetry,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.FRANCE) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("debug_log_screen")
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(AppIcons.BugReport, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Journal de Débogage Stratum v1",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Clés API et adresses Bitcoin automatiquement masquées",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
                IconButton(onClick = onClearLogs, modifier = Modifier.size(40.dp).testTag("clear_logs_button")) {
                    Icon(AppIcons.DeleteSweep, contentDescription = "Effacer le journal", tint = WarningAmber, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(TerminalBg, RoundedCornerShape(12.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            if (telemetry.activityLogs.isEmpty() && telemetry.debugLogs.isEmpty()) {
                item {
                    Text(
                        text = "Aucun événement Stratum ou relais enregistré pour le moment.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (telemetry.activityLogs.isNotEmpty()) {
                item {
                    Text(
                        text = "--- Événements du relais ---",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                items(telemetry.activityLogs.reversed(), key = { "act_${it.id}" }) { log ->
                    val color = when (log.level) {
                        LogLevel.INFO -> TextSecondary
                        LogLevel.SUCCESS -> NeonEmerald
                        LogLevel.WARN -> WarningAmber
                        LogLevel.ERROR -> DangerRed
                    }
                    Text(
                        text = "[${timeFormatter.format(Date(log.timestamp))}] ${log.message}",
                        color = color,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (telemetry.debugLogs.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "--- Messages Stratum v1 (masqués) ---",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                items(telemetry.debugLogs.reversed(), key = { "dbg_${it.id}" }) { entry ->
                    val dirColor = when (entry.direction) {
                        "TX" -> CyberCyan
                        "RX" -> TerminalGreen
                        "VERIFY" -> WarningAmber
                        else -> DangerRed
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.04f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "[${timeFormatter.format(Date(entry.timestampMs))}] ${entry.direction}: ${entry.message}",
                            color = dirColor,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
            }
        }
    }
}
