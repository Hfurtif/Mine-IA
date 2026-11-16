package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.MiningStatus
import com.example.data.model.MiningTarget
import com.example.data.model.SupportedCryptos
import com.example.ui.components.BeginnerGuideDialog
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.CryptosScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WalletsScreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.HotOrange
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

enum class NavTab(val title: String, val icon: ImageVector, val tag: String) {
    DASHBOARD("Minage", Icons.Default.Speed, "tab_dashboard"),
    WALLETS("Wallets", Icons.Default.AccountBalanceWallet, "tab_wallets"),
    CRYPTOS("Cryptos", Icons.Default.MonetizationOn, "tab_cryptos"),
    AI_ASSISTANT("Assistant", Icons.Default.AutoAwesome, "tab_ai"),
    SETTINGS("Paramètres", Icons.Default.Settings, "tab_settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MiningViewModel,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(NavTab.DASHBOARD) }
    var showGuide by remember { mutableStateOf(false) }

    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val wallets by viewModel.wallets.collectAsStateWithLifecycle()
    val selectedCrypto by viewModel.selectedCrypto.collectAsStateWithLifecycle()
    val notification by viewModel.notification.collectAsStateWithLifecycle()
    val aiMessages by viewModel.aiMessages.collectAsStateWithLifecycle()
    val apiKeySlots by viewModel.apiKeySlots.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()

    val walletAddressInput by viewModel.walletAddressInput.collectAsStateWithLifecycle()
    val aiServerIpInput by viewModel.aiServerIpInput.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Android back button: return to Dashboard tab before exiting
    BackHandler(enabled = currentTab != NavTab.DASHBOARD) {
        currentTab = NavTab.DASHBOARD
    }

    LaunchedEffect(notification) {
        notification?.let {
            snackbarHostState.showSnackbar(it.message)
            viewModel.dismissNotification()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Speed,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Miner IA",
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = CyberCyan.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "V1.0",
                                        color = CyberCyan,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Minage Automatique Déporté par IA",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                actions = {
                    // Settings shortcut button
                    IconButton(
                        onClick = { currentTab = NavTab.SETTINGS },
                        modifier = Modifier.testTag("topbar_settings_button")
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Paramètres Clés API",
                            tint = if (currentTab == NavTab.SETTINGS) NeonEmerald else CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    // Guide button
                    IconButton(onClick = { showGuide = true }) {
                        Icon(
                            Icons.Default.HelpOutline,
                            contentDescription = "Guide débutant",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp
            ) {
                NavTab.values().forEach { tab ->
                    val selected = currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 10.sp,
                                maxLines = 1,
                                softWrap = false,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = CyberCyan,
                            indicatorColor = CyberCyan,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentTab) {
                NavTab.DASHBOARD -> DashboardScreen(
                    telemetry = telemetry,
                    selectedCrypto = selectedCrypto,
                    walletAddress = walletAddressInput,
                    aiServerIp = aiServerIpInput,
                    onSelectCrypto = viewModel::selectCrypto,
                    onUpdateWalletAddress = { viewModel.walletAddressInput.value = it },
                    onUpdateAiServerIp = viewModel::updateAiServerIp,
                    onStartMining = { addr, ip -> viewModel.startDirectAiMining(addr, ip) },
                    onPauseMining = viewModel::pauseMining,
                    onResumeMining = viewModel::resumeMining,
                    onStopMining = viewModel::stopMining,
                    onClearLogs = viewModel::clearLogs,
                    onOpenApiKeys = { currentTab = NavTab.SETTINGS }
                )

                NavTab.WALLETS -> WalletsScreen(
                    wallets = wallets,
                    onAddWallet = viewModel::addWallet,
                    onDeleteWallet = viewModel::deleteWallet,
                    onSelectWalletForMining = { wallet ->
                        viewModel.selectCrypto(SupportedCryptos.ALL.find { it.symbol == wallet.cryptoSymbol } ?: viewModel.selectedCrypto.value)
                        viewModel.walletAddressInput.value = wallet.publicAddress
                        currentTab = NavTab.DASHBOARD
                    }
                )

                NavTab.CRYPTOS -> CryptosScreen(
                    currentSelectedId = selectedCrypto.id,
                    onSelectAndMine = { crypto ->
                        viewModel.selectCrypto(crypto)
                        currentTab = NavTab.DASHBOARD
                    }
                )

                NavTab.AI_ASSISTANT -> AiAssistantScreen(
                    messages = aiMessages,
                    isLoading = isAiLoading,
                    apiKeys = apiKeySlots,
                    onSaveApiKeys = viewModel::saveApiKeys,
                    onSendPrompt = viewModel::sendAiPrompt,
                    onOpenSettings = { currentTab = NavTab.SETTINGS }
                )

                NavTab.SETTINGS -> SettingsScreen(
                    apiKeys = apiKeySlots,
                    telemetry = telemetry,
                    onSaveApiKeys = viewModel::saveApiKeys
                )
            }
        }
    }

    if (showGuide) {
        BeginnerGuideDialog(onDismiss = { showGuide = false })
    }
}
