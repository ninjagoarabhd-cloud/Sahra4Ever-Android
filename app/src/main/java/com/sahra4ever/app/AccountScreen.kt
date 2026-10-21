package com.sahra4ever.app

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.imageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Pink = Color(0xFFFF2FA6)
private val Ink = Color(0xFF0F0E14)
private val GlassFill = Color.White.copy(alpha = 0.10f)
private val GlassStroke = Color.White.copy(alpha = 0.20f)
private val Muted = Color(0xFFD0CFDA)

@Suppress("DEPRECATION")
private fun appVersion(context: Context): String = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
} catch (e: Exception) {
    "1.0.0"
}

@Composable
fun AccountScreen(
    onBack: () -> Unit,
    onFavoritesClick: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val user by AuthManager.user.collectAsState()
        var showAuth by remember { mutableStateOf(false) }
        val version = remember { appVersion(context) }

        Column(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF2A1B45), Ink)))
        ) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SimpleBackButton(onBack)
                Spacer(Modifier.width(12.dp))
                Text("حسابي", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val profileShape = RoundedCornerShape(16.dp)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(profileShape)
                        .background(GlassFill)
                        .border(1.dp, GlassStroke, profileShape)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val u = user
                    UserAvatar(
                        name = u?.name ?: GUEST_NAME,
                        photoUrl = u?.photoUrl,
                        size = 88.dp,
                        showInitial = u != null
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        u?.name ?: GUEST_NAME,
                        color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        if (u == null) "زائر" else "عضو مسجّل",
                        color = Muted, fontSize = 13.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    if (u == null) {
                        Button(
                            onClick = { showAuth = true },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Pink, contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) { Text("تسجيل الدخول", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                    } else {
                        Button(
                            onClick = { scope.launch { AuthManager.signOut() } }, // ✅ تم إصلاح هذا السطر
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f), contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.ExitToApp, null)
                            Spacer(Modifier.width(8.dp))
                            Text("تسجيل الخروج", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }

                SettingsItem(Icons.Outlined.FavoriteBorder, "قائمتي / المفضلة", onClick = onFavoritesClick)

                SettingsItem(Icons.Outlined.DeleteSweep, "مسح الذاكرة المؤقتة (Clear Cache)") {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            try {
                                context.imageLoader.memoryCache?.clear()
                                context.imageLoader.diskCache?.clear()
                            } catch (e: Exception) {}
                        }
                        Toast.makeText(context, "تم مسح الذاكرة المؤقتة", Toast.LENGTH_SHORT).show()
                    }
                }

                SettingsItem(Icons.Outlined.Info, "رقم الإصدار", value = "Version $version")

                Spacer(Modifier.height(8.dp))
            }

            Text(
                "جميع الحقوق محفوظة لدى Sahra4Ever - سهرة فور إيفر © 2026",
                color = Muted.copy(alpha = 0.8f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            )
        }

        if (showAuth) AuthSheet(onDismiss = { showAuth = false })
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    value: String? = null,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(GlassFill)
            .border(1.dp, GlassStroke, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White)
        Spacer(Modifier.width(12.dp))
        Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (value != null) Text(value, color = Muted, fontSize = 14.sp)
    }
}