package com.minerrelay.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minerrelay.app.ai.ApiKeyHealth
import com.minerrelay.app.ai.ApiKeyState
import com.minerrelay.app.compute.ComputeHealthResult
import com.minerrelay.app.data.model.RelayConfig
import com.minerrelay.app.mining.AddressValidationResult
import com.minerrelay.app.mining.SecurityValidator
import com.minerrelay.app.ui.components.AppIcons
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

@Composable
fun SettingsScreen(
    relayConfig: RelayConfig,
    poolPassword: String,
    computeApiKey: String,
    geminiApiKey: String,
    computeKeyHealth: ComputeHealthResult?,
    geminiKeyHealth: ApiKeyHealth,
    isCheckingComputeKey: Boolean,
    onSaveSettings: (
        poolHost: String,
        poolPortText: String,
        poolLogin: String,
        poolPassword: String,
        backupPoolEnabled: Boolean,
        backupPoolPortText: String,
        backupPoolUseTls: Boolean,
        btcAddress: String,
        computeApiUrl: String,
        computeApiKey: String,
        geminiApiKey: String,
        wifiOnly: Boolean,
        chargingOnly: Boolean,
        testMode: Boolean
    ) -> Boolean,
    onTestComputeKey: (key: String, endpointUrl: String) -> Unit,
    onTestGeminiKey: (key: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var hostInput by remember { mutableStateOf(relayConfig.poolHost) }
    var portInput by remember { mutableStateOf(relayConfig.poolPort?.toString() ?: "") }
    var poolLoginInput by remember { mutableStateOf(relayConfig.poolLogin) }
    var poolPasswordInput by remember { mutableStateOf(poolPassword) }
    var backupPoolEnabled by remember { mutableStateOf(relayConfig.backupPoolEnabled) }
    var backupPoolPortInput by remember { mutableStateOf((relayConfig.backupPoolPort ?: 443).toString()) }
    var backupPoolUseTls by remember { mutableStateOf(relayConfig.backupPoolUseTls) }
    var addressInput by remember { mutableStateOf(relayConfig.btcAddress) }
    var computeUrlInput by remember { mutableStateOf(relayConfig.computeApiUrl) }
    var computeKeyInput by remember { mutableStateOf(computeApiKey) }
    var geminiKeyInput by remember { mutableStateOf(geminiApiKey) }
    var wifiOnly by remember { mutableStateOf(relayConfig.wifiOnly) }
    var chargingOnly by remember { mutableStateOf(relayConfig.chargingOnly) }
    var testMode by remember { mutableStateOf(relayConfig.testMode) }

    var showPoolPassword by remember { mutableStateOf(false) }
    var showComputeKey by remember { mutableStateOf(false) }
    var showGeminiKey by remember { mutableStateOf(false) }

    LaunchedEffect(relayConfig, poolPassword, computeApiKey, geminiApiKey) {
        hostInput = relayConfig.poolHost
        portInput = relayConfig.poolPort?.toString() ?: ""
        poolLoginInput = relayConfig.poolLogin
        poolPasswordInput = poolPassword
        backupPoolEnabled = relayConfig.backupPoolEnabled
        backupPoolPortInput = (relayConfig.backupPoolPort ?: 443).toString()
        backupPoolUseTls = relayConfig.backupPoolUseTls
        addressInput = relayConfig.btcAddress
        computeUrlInput = relayConfig.computeApiUrl
        computeKeyInput = computeApiKey
        geminiKeyInput = geminiApiKey
        wifiOnly = relayConfig.wifiOnly
        chargingOnly = relayConfig.chargingOnly
        testMode = relayConfig.testMode
    }

    val addressValidation = remember(addressInput) {
        if (addressInput.isBlank()) null else SecurityValidator.validateBitcoinMainnetAddress(addressInput)
    }
    val addressErrorText = when (addressValidation) {
        is AddressValidationResult.DangerousSeedPhraseDetected -> addressValidation.message
        is AddressValidationResult.DangerousPrivateKeyDetected -> addressValidation.message
        is AddressValidationResult.InvalidFormat -> addressValidation.reason
        else -> null
    }

    val computeIsError = computeKeyHealth is ComputeHealthResult.Unhealthy

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 14.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
    ) {
        // 1. Pool Stratum v1 + Identifiant du pool + Pool de secours (même hôte, port 443)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Settings, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pool Stratum v1 & Identifiant", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Text(
                        text = "Saisissez votre propre pool Stratum v1 (aucune adresse pré-remplie par défaut).",
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = hostInput,
                            onValueChange = { hostInput = it },
                            label = { Text("Hôte du pool") },
                            placeholder = { Text("ex: stratum+tcp://...", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(2f).testTag("settings_pool_host_input"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = DarkSurfaceVariant)
                        )
                        OutlinedTextField(
                            value = portInput,
                            onValueChange = { if (it.all(Char::isDigit) && it.length <= 5) portInput = it },
                            label = { Text("Port") },
                            placeholder = { Text("Port", fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("settings_pool_port_input"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = DarkSurfaceVariant)
                        )
                    }

                    OutlinedTextField(
                        value = poolLoginInput,
                        onValueChange = { poolLoginInput = it },
                        label = { Text("Identifiant du pool") },
                        placeholder = { Text("ex : Hfurtif.worker1", fontSize = 11.sp) },
                        supportingText = {
                            Text(
                                text = "Utilisé TEL QUEL dans mining.authorize et mining.submit (sans adresse BTC).",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_pool_login_input"),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = DarkSurfaceVariant)
                    )

                    OutlinedTextField(
                        value = poolPasswordInput,
                        onValueChange = { poolPasswordInput = it },
                        label = { Text("Mot de passe du pool (facultatif)") },
                        placeholder = { Text("Vide = « x » envoyé par défaut", fontSize = 11.sp) },
                        singleLine = true,
                        visualTransformation = if (showPoolPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPoolPassword = !showPoolPassword }) {
                                Icon(
                                    imageVector = if (showPoolPassword) AppIcons.VisibilityOff else AppIcons.Visibility,
                                    contentDescription = if (showPoolPassword) "Masquer" else "Afficher",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("settings_pool_password_input"),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = DarkSurfaceVariant)
                    )

                    HorizontalDivider(color = DarkSurfaceVariant, thickness = 1.dp)

                    SettingToggleRow(
                        icon = AppIcons.Dns,
                        title = "Pool de secours (même hôte)",
                        subtitle = "Bascule automatiquement vers le port 443 si le port principal échoue",
                        checked = backupPoolEnabled,
                        onCheckedChange = { enabled ->
                            backupPoolEnabled = enabled
                            if (enabled && backupPoolPortInput.isBlank()) {
                                backupPoolPortInput = "443"
                            }
                        },
                        tag = "settings_backup_pool_switch"
                    )

                    AnimatedVisibility(visible = backupPoolEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.Black.copy(alpha = 0.28f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(2f)) {
                                            Text("Hôte de secours (identique)", color = TextMuted, fontSize = 10.sp)
                                            Text(
                                                text = hostInput.trim().ifBlank { "Saisissez l'hôte principal" },
                                                color = TextPrimary,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        OutlinedTextField(
                                            value = backupPoolPortInput,
                                            onValueChange = { if (it.all(Char::isDigit) && it.length <= 5) backupPoolPortInput = it },
                                            label = { Text("Port secours") },
                                            placeholder = { Text("443", fontSize = 11.sp) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1.2f).testTag("settings_backup_pool_port_input"),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = CyberCyan,
                                                unfocusedBorderColor = DarkSurfaceVariant
                                            )
                                        )
                                    }

                                    SettingToggleRow(
                                        icon = AppIcons.Security,
                                        title = "SSL/TLS sur le pool de secours",
                                        subtitle = "Activez si le port 443 du pool attend du Stratum+SSL",
                                        checked = backupPoolUseTls,
                                        onCheckedChange = { backupPoolUseTls = it },
                                        tag = "settings_backup_tls_switch"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Adresse Bitcoin uniquement pour lire les paiements sur mempool.space
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (addressErrorText != null) DangerRed else DarkSurfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.Wallet, contentDescription = null, tint = BitcoinOrange, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Suivi Paiements BTC (mempool.space)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Text(
                        text = "Optionnelle. Sert uniquement à lire vos paiements reçus sur mempool.space et n'est jamais envoyée dans l'identifiant Stratum.",
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        label = { Text("Adresse Bitcoin de réception (1..., 3..., bc1...)") },
                        isError = addressErrorText != null,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("settings_btc_address_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (addressErrorText != null) DangerRed else CyberCyan,
                            unfocusedBorderColor = if (addressErrorText != null) DangerRed else DarkSurfaceVariant
                        )
                    )

                    if (addressErrorText != null) {
                        Text(text = addressErrorText, color = DangerRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. Service de calcul distant (clé chiffrée AndroidKeyStore AES/GCM)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (computeIsError) DangerRed.copy(alpha = 0.08f) else DarkCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (computeIsError) DangerRed else DarkSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.Key, contentDescription = null, tint = if (computeIsError) DangerRed else CyberCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Service de Calcul Distant (AES/GCM)", color = if (computeIsError) DangerRed else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    OutlinedTextField(
                        value = computeUrlInput,
                        onValueChange = { computeUrlInput = it },
                        label = { Text("URL Endpoint API Calcul (HTTPS)") },
                        placeholder = { Text("https://...", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_compute_url_input"),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = DarkSurfaceVariant)
                    )

                    OutlinedTextField(
                        value = computeKeyInput,
                        onValueChange = { computeKeyInput = it },
                        label = { Text("Clé API Service de Calcul (chiffrée)") },
                        singleLine = true,
                        isError = computeIsError,
                        visualTransformation = if (showComputeKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showComputeKey = !showComputeKey }) {
                                Icon(
                                    imageVector = if (showComputeKey) AppIcons.VisibilityOff else AppIcons.Visibility,
                                    contentDescription = if (showComputeKey) "Masquer" else "Afficher",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                        modifier = Modifier.fillMaxWidth().testTag("settings_compute_key_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (computeIsError) DangerRed else CyberCyan,
                            unfocusedBorderColor = if (computeIsError) DangerRed else DarkSurfaceVariant
                        )
                    )

                    when (computeKeyHealth) {
                        is ComputeHealthResult.Healthy ->
                            Text("OK : ${computeKeyHealth.detail}", color = NeonEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        is ComputeHealthResult.IncompleteConfiguration ->
                            Text(computeKeyHealth.reason, color = WarningAmber, fontSize = 11.sp)
                        is ComputeHealthResult.Unhealthy ->
                            Text("Alerte : ${computeKeyHealth.reason}", color = DangerRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        null -> Unit
                    }

                    OutlinedButton(
                        onClick = { onTestComputeKey(computeKeyInput, computeUrlInput) },
                        enabled = !isCheckingComputeKey,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("settings_test_compute_button")
                    ) {
                        if (isCheckingComputeKey) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CyberCyan, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test en cours...", color = CyberCyan)
                        } else {
                            Text("Tester le service de calcul", color = CyberCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 4. Clé API Gemini optionnelle (séparée du service de calcul)
        item {
            val geminiUnresponsive = geminiKeyHealth.state == ApiKeyState.UNRESPONSIVE
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (geminiUnresponsive) DangerRed.copy(alpha = 0.08f) else DarkCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (geminiUnresponsive) DangerRed else DarkSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.AutoAwesome, contentDescription = null, tint = if (geminiUnresponsive) DangerRed else NeonEmerald, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Assistant Gemini (Clé distincte, optionnelle)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Text(
                        text = "Sert uniquement à expliquer les métriques réelles. Distincte de la clé du service de calcul.",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                    OutlinedTextField(
                        value = geminiKeyInput,
                        onValueChange = { geminiKeyInput = it },
                        label = { Text("Clé API Gemini (optionnelle, chiffrée)") },
                        singleLine = true,
                        isError = geminiUnresponsive,
                        visualTransformation = if (showGeminiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showGeminiKey = !showGeminiKey }) {
                                Icon(
                                    imageVector = if (showGeminiKey) AppIcons.VisibilityOff else AppIcons.Visibility,
                                    contentDescription = if (showGeminiKey) "Masquer" else "Afficher",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                        modifier = Modifier.fillMaxWidth().testTag("settings_gemini_key_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (geminiUnresponsive) DangerRed else CyberCyan,
                            unfocusedBorderColor = if (geminiUnresponsive) DangerRed else DarkSurfaceVariant
                        )
                    )
                    if (!geminiKeyHealth.message.isNullOrBlank() && geminiKeyHealth.state != ApiKeyState.UNCHECKED) {
                        Text(
                            text = geminiKeyHealth.message,
                            color = if (geminiUnresponsive) DangerRed else NeonEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(
                        onClick = { onTestGeminiKey(geminiKeyInput) },
                        enabled = geminiKeyInput.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Vérifier la clé Gemini", color = CyberCyan, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 5. Énergie, Réseau & Mode Test
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Contraintes Réseau, Énergie & Test", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    SettingToggleRow(
                        icon = AppIcons.Wifi,
                        title = "Miner seulement en Wi-Fi",
                        subtitle = "Suspend le relais sur données cellulaires",
                        checked = wifiOnly,
                        onCheckedChange = { wifiOnly = it },
                        tag = "settings_wifi_only_switch"
                    )
                    SettingToggleRow(
                        icon = AppIcons.BatteryCharging,
                        title = "Miner seulement en charge",
                        subtitle = "Suspend le relais sur batterie",
                        checked = chargingOnly,
                        onCheckedChange = { chargingOnly = it },
                        tag = "settings_charging_only_switch"
                    )
                    SettingToggleRow(
                        icon = Icons.Filled.Warning,
                        title = "Mode Test (faible difficulté)",
                        subtitle = "Envoie mining.suggest_difficulty(0.01) et capture les réponses brutes",
                        checked = testMode,
                        onCheckedChange = { testMode = it },
                        tag = "settings_test_mode_switch"
                    )
                }
            }
        }

        // 6. Bouton Sauvegarder
        item {
            Button(
                onClick = {
                    onSaveSettings(
                        hostInput,
                        portInput,
                        poolLoginInput,
                        poolPasswordInput,
                        backupPoolEnabled,
                        backupPoolPortInput,
                        backupPoolUseTls,
                        addressInput,
                        computeUrlInput,
                        computeKeyInput,
                        geminiKeyInput,
                        wifiOnly,
                        chargingOnly,
                        testMode
                    )
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth().height(54.dp).testTag("settings_save_button")
            ) {
                Icon(AppIcons.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("SAUVEGARDER & CHIFFRER", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = TextMuted, fontSize = 10.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag),
            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = NeonEmerald)
        )
    }
}
