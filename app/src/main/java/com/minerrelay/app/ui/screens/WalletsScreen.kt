package com.minerrelay.app.ui.screens

import android.content.ClipData
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minerrelay.app.data.local.WalletEntity
import com.minerrelay.app.mining.AddressValidationResult
import com.minerrelay.app.security.EncryptedKeyStore
import com.minerrelay.app.ui.components.AddWalletDialog
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

@Composable
fun WalletsScreen(
    wallets: List<WalletEntity>,
    onAddWallet: (name: String, address: String) -> AddressValidationResult,
    onDeleteWallet: (WalletEntity) -> Unit,
    onSelectWalletForRelay: (WalletEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(DarkBg)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0C243B)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CyberCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(AppIcons.Security, contentDescription = null, tint = CyberCyan)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Adresses Publiques Bitcoin Uniquement",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Seules les adresses publiques validées (Base58Check 1.../3... et Bech32/Bech32m bc1...) sont acceptées. Ne saisissez jamais de clé privée ni de phrase secrète.",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            if (wallets.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkCard)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = TextMuted, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Aucune adresse enregistrée", color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text("Ajoutez une adresse publique Bitcoin (1..., 3... ou bc1...).", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(wallets, key = { it.id }) { wallet ->
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("wallet_card_${wallet.id}"),
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
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(BitcoinOrange.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("BTC", color = BitcoinOrange, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(wallet.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Bitcoin Mainnet", color = TextMuted, fontSize = 11.sp)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(
                                        onClick = { onSelectWalletForRelay(wallet) },
                                        colors = ButtonDefaults.textButtonColors(contentColor = NeonEmerald)
                                    ) {
                                        Text("Utiliser", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                    IconButton(onClick = { onDeleteWallet(wallet) }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = DangerRed, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.35f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = EncryptedKeyStore.maskAddress(wallet.publicAddress),
                                        color = TextSecondary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Bitcoin Address", wallet.publicAddress))
                                            Toast.makeText(context, "Adresse copiée", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(AppIcons.ContentCopy, contentDescription = "Copier", tint = CyberCyan, modifier = Modifier.size(15.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp).testTag("add_wallet_fab"),
            containerColor = CyberCyan,
            contentColor = Color.Black
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Ajouter une adresse Bitcoin")
        }
    }

    if (showAddDialog) {
        AddWalletDialog(
            onDismiss = { showAddDialog = false },
            onSave = onAddWallet
        )
    }
}
