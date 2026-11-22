package com.minerrelay.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minerrelay.app.ui.components.AppIcons
import com.minerrelay.app.ui.components.ArchitectureGuideDialog
import com.minerrelay.app.ui.screens.AiAssistantScreen
import com.minerrelay.app.ui.screens.DashboardScreen
import com.minerrelay.app.ui.screens.DebugLogScreen
import com.minerrelay.app.ui.screens.EconomicsSummaryScreen
import com.minerrelay.app.ui.screens.SettingsScreen
import com.minerrelay.app.ui.screens.WalletsScreen
import com.minerrelay.app.ui.theme.CyberCyan
import com.minerrelay.app.ui.theme.DangerRed
import com.minerrelay.app.ui.theme.DarkBg
import com.minerrelay.app.ui.theme.DarkSurface
import com.minerrelay.app.ui.theme.DarkSurfaceVariant
import com.minerrelay.app.ui.theme.NeonEmerald
import com.minerrelay.app.ui.theme.TextMuted
import com.minerrelay.app.ui.theme.TextPrimary
import com.minerrelay.app.ui.theme.TextSecondary

enum class NavTab(val title: String, val icon: ImageVector, val tag: String) {
    DASHBOARD("Relais", AppIcons.Speed, "tab_dashboard"),
    SUMMARY("Résumé", AppIcons.TrendingUp, "tab_summary"),
    WALLETS("Wallets", AppIcons.Wallet, "tab_wallets"),
    LOGS("Journal", AppIcons.BugReport, "tab_logs"),
    SETTINGS("Réglages", Icons.Filled.Settings, "tab_settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MiningViewModel,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(NavTab.DASHBOARD) }
    var showAiAssistant by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }

    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val wallets by viewModel.wallets.collectAsStateWithLifecycle()
    val relayConfig by viewModel.relayConfig.collectAsStateWithLifecycle()
    val poolPassword by viewModel.poolPasswordInput.collectAsStateWithLifecycle()
    val computeApiKey by viewModel.computeApiKeyInput.collectAsStateWithLifecycle()
    val geminiApiKey by viewModel.geminiApiKeyInput.collectAsStateWithLifecycle()
    val computeKeyHealth by viewModel.computeKeyHealth.collectAsStateWithLifecycle()
    val geminiKeyHealth by viewModel.geminiKeyHealth.collectAsStateWithLifecycle()
    val isCheckingComputeKey by viewModel.isCheckingComputeKey.collectAsStateWithLifecycle()
    val notification by viewModel.notification.collectAsStateWithLifecycle()
    val aiMessages by viewModel.aiMessages.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var lastSnackbarIsError by remember { mutableStateOf(false) }

    BackHandler(enabled = !showAiAssistant && currentTab != NavTab.DASHBOARD) {
        currentTab = NavTab.DASHBOARD
    }

    LaunchedEffect(notification) {
        notification?.let {
            lastSnackbarIsError = it.isError
            snackbarHostState.showSnackbar(it.message)
            viewModel.dismissNotification()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBg,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = if (lastSnackbarIsError) DangerRed else DarkSurface,
                    contentColor = if (lastSnackbarIsError) Color.White else TextPrimary,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
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
                            Icon(AppIcons.Speed, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Miner IA", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Relais Bitcoin Stratum v1", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAiAssistant = !showAiAssistant },
                        modifier = Modifier.testTag("topbar_ai_button")
                    ) {
                        Icon(
                            AppIcons.AutoAwesome,
                            contentDescription = "Assistant Gemini (analyse réelle)",
                            tint = if (showAiAssistant) NeonEmerald else CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = { showGuide = true }) {
                        Icon(AppIcons.HelpOutline, contentDescription = "Architecture", tint = CyberCyan, modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = DarkSurface, tonalElevation = 8.dp) {
                NavTab.entries.forEach { tab ->
                    val selected = !showAiAssistant && currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            showAiAssistant = false
                            currentTab = tab
                        },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.title, modifier = Modifier.size(20.dp)) },
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
            if (showAiAssistant) {
                AiAssistantScreen(
                    messages = aiMessages,
                    isLoading = isAiLoading,
                    hasGeminiKey = geminiApiKey.isNotBlank(),
                    onSendPrompt = viewModel::sendAiPrompt,
                    onBack = { showAiAssistant = false }
                )
            } else {
                when (currentTab) {
                    NavTab.DASHBOARD -> DashboardScreen(
                        telemetry = telemetry,
                        relayConfig = relayConfig,
                        hasComputeKey = computeApiKey.isNotBlank(),
                        onStartRelay = viewModel::startForegroundRelay,
                        onStopRelay = viewModel::stopForegroundRelay,
                        onOpenSettings = { currentTab = NavTab.SETTINGS },
                        onRefreshBlockchain = viewModel::refreshMempoolTransactions
                    )

                    NavTab.SUMMARY -> EconomicsSummaryScreen(telemetry = telemetry)

                    NavTab.WALLETS -> WalletsScreen(
                        wallets = wallets,
                        onAddWallet = viewModel::addWallet,
                        onDeleteWallet = viewModel::deleteWallet,
                        onSelectWalletForRelay = { wallet ->
                            viewModel.useWalletForRelay(wallet)
                            currentTab = NavTab.DASHBOARD
                        }
                    )

                    NavTab.LOGS -> DebugLogScreen(
                        telemetry = telemetry,
                        onClearLogs = viewModel::clearLogs
                    )

                    NavTab.SETTINGS -> SettingsScreen(
                        relayConfig = relayConfig,
                        poolPassword = poolPassword,
                        computeApiKey = computeApiKey,
                        geminiApiKey = geminiApiKey,
                        computeKeyHealth = computeKeyHealth,
                        geminiKeyHealth = geminiKeyHealth,
                        isCheckingComputeKey = isCheckingComputeKey,
                        onSaveSettings = viewModel::saveSettings,
                        onTestComputeKey = viewModel::testComputeApiHealth,
                        onTestGeminiKey = viewModel::testGeminiKey
                    )
                }
            }
        }
    }

    if (showGuide) {
        ArchitectureGuideDialog(onDismiss = { showGuide = false })
    }
}
