package com.example.ui.screens

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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CryptoCurrency
import com.example.data.model.LogLevel
import com.example.data.model.MiningStatus
import com.example.data.model.SupportedCryptos
import com.example.mining.AddressValidationResult
import com.example.mining.MiningTelemetry
import com.example.mining.SecurityValidator
import com.example.ui.components.HashrateSparkline
import com.example.ui.components.MetricStatCard
import com.example.ui.components.RealtimeHashrateChartCard
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun DashboardScreen(
    telemetry: MiningTelemetry,
    selectedCrypto: CryptoCurrency,
    walletAddress: String,
    aiServerIp: String,
    onSelectCrypto: (CryptoCurrency) -> Unit,
    onUpdateWalletAddress: (String) -> Unit,
    onUpdateAiServerIp: (String) -> Unit,
    onStartMining: (address: String, serverIp: String) -> Unit,
    onPauseMining: () -> Unit,
    onResumeMining: () -> Unit,
    onStopMining: () -> Unit,
    onClearLogs: () -> Unit,
    onOpenApiKeys: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var addressInput by remember { mutableStateOf(walletAddress) }
    var serverIpInput by remember { mutableStateOf(aiServerIp) }
    val consoleListState = rememberLazyListState()

    LaunchedEffect(walletAddress) {
        if (addressInput != walletAddress) addressInput = walletAddress
    }
    LaunchedEffect(aiServerIp) {
        if (serverIpInput != aiServerIp) serverIpInput = aiServerIp
    }

    LaunchedEffect(telemetry.logs.size) {
        if (telemetry.logs.isNotEmpty()) {
            consoleListState.animateScrollToItem(telemetry.logs.size - 1)
        }
    }

    var securityWarning by remember { mutableStateOf<String?>(null) }
    var isSeedPhraseDetected by remember { mutableStateOf(false) }

    fun checkAddressSecurity(input: String) {
        addressInput = input
        onUpdateWalletAddress(input)
        if (input.isBlank()) {
            securityWarning = null
            isSeedPhraseDetected = false
            return
        }
        val res = SecurityValidator.validatePublicAddress(selectedCrypto.symbol, input)
        when (res) {
            is AddressValidationResult.DangerousSeedPhraseDetected -> {
                securityWarning = res.message
                isSeedPhraseDetected = true
            }
            is AddressValidationResult.DangerousPrivateKeyDetected -> {
                securityWarning = res.message
                isSeedPhraseDetected = true
            }
            else -> {
                securityWarning = null
                isSeedPhraseDetected = false
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(750),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 30.dp)
    ) {
        // COMPOSANT TABLEAU DE BORD : ÉTAT 'MINAGE ACTIF' AVEC DONNÉES DE CALCUL EN DIRECT
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonEmerald.copy(alpha = 0.85f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(NeonEmerald.copy(alpha = pulseGlow))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Minage Actif",
                                color = NeonEmerald,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = WarningAmber.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = if (telemetry.isMockSimulation) "Données factices de calcul" else "Connecté au Serveur IA",
                                color = WarningAmber,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF7931A).copy(alpha = 0.16f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF7931A).copy(alpha = 0.55f)),
                            modifier = Modifier.testTag("bitcoin_only_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF7931A))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Bitcoin (BTC) • SHA-256",
                                    color = Color(0xFFF7931A),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Minage dédié Bitcoin : les calculs SHA-256 sont envoyés directement au serveur IA qui renvoie les résultats validés.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Gains en direct
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Gains Virés sur Portefeuille :", color = TextSecondary, fontSize = 11.sp)
                            Text(
                                text = "${String.format("%.6f", telemetry.totalAccumulatedCrypto)} ${selectedCrypto.symbol}",
                                color = NeonEmerald,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "≈ ${String.format("%.2f", telemetry.totalAccumulatedEur)} € (calculé en direct)",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Vitesse Serveur IA :", color = TextSecondary, fontSize = 11.sp)
                            Text(
                                text = "${String.format("%.1f", telemetry.hashrate)} ${telemetry.hashrateUnit}",
                                color = CyberCyan,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HashrateSparkline(
                        history = telemetry.hashrateHistory,
                        lineColor = CyberCyan,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricStatCard(
                            title = "Calculs Envoyés à l'IA",
                            value = "${telemetry.aiCalculationsSent}",
                            subtitle = "Transmis au serveur",
                            icon = Icons.Default.Dns,
                            accentColor = CyberCyan,
                            modifier = Modifier.weight(1f)
                        )
                        MetricStatCard(
                            title = "Résultats Renvoyés",
                            value = "${telemetry.aiCalculationsReceived}",
                            subtitle = "Résolus et validés",
                            icon = Icons.Default.CheckCircle,
                            accentColor = NeonEmerald,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = telemetry.activeAiPhase,
                                color = CyberCyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    val hasUnresponsiveKey = telemetry.unresponsiveApiKeysCount > 0
                    val boostReady = telemetry.activeApiKeysCount >= 3 && !hasUnresponsiveKey
                    val boostBadgeColor = when {
                        hasUnresponsiveKey -> DangerRed
                        boostReady -> NeonEmerald
                        else -> WarningAmber
                    }
                    Surface(
                        onClick = onOpenApiKeys,
                        shape = RoundedCornerShape(8.dp),
                        color = boostBadgeColor.copy(alpha = if (hasUnresponsiveKey) 0.16f else 0.12f),
                        border = androidx.compose.foundation.BorderStroke(
                            if (hasUnresponsiveKey) 1.5.dp else 1.dp,
                            boostBadgeColor.copy(alpha = if (hasUnresponsiveKey) 0.9f else 0.45f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("dashboard_api_keys_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (hasUnresponsiveKey) Icons.Default.Warning else Icons.Default.Key,
                                    contentDescription = if (hasUnresponsiveKey) "Alerte clé API" else null,
                                    tint = boostBadgeColor,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when {
                                        hasUnresponsiveKey -> "Alerte : ${telemetry.unresponsiveApiKeysCount} clé(s) API ne répond(ent) pas !"
                                        boostReady -> "Multi-Clés API (${telemetry.activeApiKeysCount}) • Calcul & Réflexion ×${String.format("%.1f", telemetry.speedMultiplier)}"
                                        else -> "Clés API : ${telemetry.activeApiKeysCount}/3 • Ajoutez 3+ clés pour accélérer"
                                    },
                                    color = boostBadgeColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (hasUnresponsiveKey) "Vérifier →" else "Gérer →",
                                color = if (hasUnresponsiveKey) DangerRed else CyberCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // CARTE MINAGE CLOUD AUTONOME (0% CPU/GPU LOCAL - DÉCONNECTABLE)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("autonomous_cloud_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF092136)),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Minage Cloud Autonome 24h/24",
                                color = NeonEmerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NeonEmerald.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "0% CPU/GPU Téléphone",
                                color = NeonEmerald,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Les serveurs de l'IA prennent 100% des calculs en charge. Vous pouvez fermer l'application ou vous déconnecter : l'IA continue à miner en ligne 24h/24 et à verser vos gains directement sur votre portefeuille.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
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
                                Icon(Icons.Default.Smartphone, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("Appareil local", color = TextMuted, fontSize = 9.sp)
                                    Text("Déconnectable (0% batterie)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
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
                                Icon(Icons.Default.Dns, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("Serveur IA en ligne", color = TextMuted, fontSize = 9.sp)
                                    Text("Actif 24/7 en continu", color = NeonEmerald, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // GRAPHIQUE LINÉAIRE EN TEMPS RÉEL DU TAUX DE HASHAGE BITCOIN (VICO)
        item {
            RealtimeHashrateChartCard(
                history = telemetry.hashrateHistory,
                currentHashrate = telemetry.hashrate,
                peakHashrate = telemetry.peakHashrate,
                hashrateUnit = telemetry.hashrateUnit,
                isActive = telemetry.status == MiningStatus.MINING
            )
        }

        // ÉTAPE 1 : ADRESSE WALLET BITCOIN (VIDE, C'EST À L'UTILISATEUR D'AJOUTER SON ADRESSE)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSeedPhraseDetected) DangerRed else DarkSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1. Adresse Wallet Bitcoin (BTC) :",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        TextButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                if (!clip.isNullOrBlank()) {
                                    checkAddressSecurity(clip.trim())
                                } else {
                                    Toast.makeText(context, "Presse-papier vide", Toast.LENGTH_SHORT).show()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Coller mon adresse", color = CyberCyan, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { checkAddressSecurity(it) },
                        placeholder = { Text("Entrez ou collez votre adresse Bitcoin (1..., 3..., bc1...)...", color = TextMuted, fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wallet_address_main_input"),
                        maxLines = 2,
                        isError = isSeedPhraseDetected,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isSeedPhraseDetected) DangerRed else CyberCyan,
                            unfocusedBorderColor = if (isSeedPhraseDetected) DangerRed else DarkSurfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Les gains Bitcoin (BTC) des calculs résolus par l'IA seront directement virés sur cette adresse.",
                        color = TextMuted,
                        fontSize = 10.sp
                    )

                    // Alerte de sécurité stricte anti-seed phrase
                    AnimatedVisibility(visible = securityWarning != null) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DangerRed.copy(alpha = 0.18f)),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, DangerRed)
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = securityWarning ?: "",
                                        color = DangerRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ÉTAPE 2 : ADRESSE IP DU SERVEUR IA (L'IA FAIT TOUS LES CALCULS BITCOIN)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Dns, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "2. Adresse IP du Serveur IA :",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        TextButton(
                            onClick = {
                                serverIpInput = "192.168.1.50:8080"
                                onUpdateAiServerIp(serverIpInput)
                                Toast.makeText(context, "Adresse IP IA configurée !", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Exemple IP", color = NeonEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = serverIpInput,
                        onValueChange = {
                            serverIpInput = it
                            onUpdateAiServerIp(it)
                        },
                        placeholder = { Text("Ex: 192.168.1.50 ou ai-node.network:8080", color = TextMuted, fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_server_ip_main_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkSurfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Dès que l'adresse IP est entrée, tous les calculs Bitcoin sont directement envoyés au serveur IA.",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // BOUTONS D'ACTION
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (addressInput.isNotBlank()) {
                    Button(
                        onClick = {
                            onStartMining(addressInput, serverIpInput)
                        },
                        enabled = !isSeedPhraseDetected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("start_ai_mining_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonEmerald,
                            contentColor = Color(0xFF002911)
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DÉMARRER LE MINAGE BITCOIN SUR LE SERVEUR IA",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Boutons Pause & Arrêt
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (telemetry.status == MiningStatus.MINING) {
                        Button(
                            onClick = onPauseMining,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("pause_mining_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WarningAmber, contentColor = Color.Black)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mettre en Pause", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else if (telemetry.status == MiningStatus.PAUSED) {
                        Button(
                            onClick = onResumeMining,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("resume_mining_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = Color.Black)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reprendre", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = onStopMining,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("stop_mining_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Arrêter", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // DERNIÈRES TRANSACTIONS BITCOIN VALIDÉES
        item {
            val dateFormatter = remember {
                java.text.SimpleDateFormat("dd/MM/yyyy • HH:mm:ss", java.util.Locale.FRANCE)
            }
            val displayedTransactions = telemetry.recentTransactions.take(6)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recent_btc_transactions_card"),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Historique des transactions Bitcoin",
                                tint = Color(0xFFF7931A),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Dernières Transactions Bitcoin",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NeonEmerald.copy(alpha = 0.14f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "${telemetry.recentTransactions.size} validée(s)",
                                color = NeonEmerald,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (displayedTransactions.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CurrencyBitcoin,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Aucune transaction Bitcoin validée pour le moment.",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            displayedTransactions.forEach { tx ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.Black.copy(alpha = 0.32f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("btc_transaction_item_${tx.blockNumber}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(NeonEmerald.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Transaction validée",
                                                    tint = NeonEmerald,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "Bloc #${tx.blockNumber} • Validée",
                                                    color = TextPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = dateFormatter.format(java.util.Date(tx.timestamp)),
                                                    color = TextMuted,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }

                                        Text(
                                            text = "+${String.format(java.util.Locale.US, "%.6f", tx.amountBtc)} BTC",
                                            color = NeonEmerald,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
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

        // CONSOLE TERMINAL EN DIRECT DES CALCULS AVEC LE SERVEUR IA
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TerminalBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NeonEmerald))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Console des Calculs Serveur IA", color = TerminalGreen, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = onClearLogs, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Effacer les logs", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                            .padding(8.dp)
                    ) {
                        LazyColumn(state = consoleListState) {
                            items(telemetry.logs) { log ->
                                val logColor = when (log.level) {
                                    LogLevel.INFO -> TextSecondary
                                    LogLevel.SUCCESS -> TerminalGreen
                                    LogLevel.WARN -> WarningAmber
                                    LogLevel.ERROR -> DangerRed
                                }
                                Text(
                                    text = "> ${log.message}",
                                    color = logColor,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
