package com.sahra4ever.app

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Edit
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
import java.util.Locale

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

private fun computeCacheSizeBytes(context: Context): Long {
    return try {
        var total = 0L
        context.cacheDir?.let { dir ->
            total += dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        }
        context.externalCacheDir?.let { dir ->
            total += dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        }
        total
    } catch (e: Exception) {
        0L
    }
}

private fun formatCacheSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.0f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024.0) return String.format(Locale.US, "%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format(Locale.US, "%.2f GB", gb)
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
        var showClearCacheDialog by remember { mutableStateOf(false) }
        var showEditNameDialog by remember { mutableStateOf(false) }
        val version = remember { appVersion(context) }

        var cacheSizeBytes by remember { mutableStateOf(0L) }
        LaunchedEffect(Unit) {
            cacheSizeBytes = withContext(Dispatchers.IO) { computeCacheSizeBytes(context) }
        }

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

                    Box {
                        UserAvatar(
                            name = u?.name ?: GUEST_NAME,
                            photoUrl = u?.photoUrl,
                            size = 88.dp,
                            showInitial = u != null
                        )
                        if (u != null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Pink)
                                    .border(2.dp, Ink, CircleShape)
                                    .clickable { showEditNameDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Edit,
                                    "تعديل الاسم",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

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
                            onClick = { scope.launch { AuthManager.signOut() } },
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

                SettingsItem(
                    Icons.Outlined.DeleteSweep,
                    "مسح الذاكرة المؤقتة (Clear Cache)",
                    value = formatCacheSize(cacheSizeBytes)
                ) {
                    showClearCacheDialog = true
                }

                SettingsItem(Icons.Outlined.Info, "رقم الإصدار", value = "Version $version")

                Spacer(modifier = Modifier.height(8.dp))
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

        // ✅ نافذة تعديل الاسم
        if (showEditNameDialog && user != null) {
            var nameInput by remember { mutableStateOf(user!!.name) }
            var isSaving by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            AlertDialog(
                onDismissRequest = { if (!isSaving) showEditNameDialog = false },
                title = { Text("تعديل الاسم", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("الاسم الجديد") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Pink,
                            unfocusedBorderColor = GlassStroke,
                            cursorColor = Pink
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = !isSaving && nameInput.isNotBlank(),
                        onClick = {
                            isSaving = true
                            scope.launch {
                                try {
                                    AuthManager.updateProfile(nameInput.trim())
                                    Toast.makeText(context, "تم تحديث الاسم بنجاح", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "فشل التحديث", Toast.LENGTH_SHORT).show()
                                }
                                isSaving = false
                                showEditNameDialog = false
                            }
                        }
                    ) { Text("حفظ", color = Pink, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { if (!isSaving) showEditNameDialog = false }) { Text("إلغاء") }
                },
                containerColor = Color(0xFF1E1E2E),
                titleContentColor = Color.White,
                textContentColor = Muted
            )
        }

        // ✅ نافذة تأكيد المسح
        if (showClearCacheDialog) {
            AlertDialog(
                onDismissRequest = { showClearCacheDialog = false },
                title = { Text("تأكيد المسح", fontWeight = FontWeight.Bold) },
                text = { Text("هل أنت متأكد من مسح ذاكرة التطبيق المؤقتة؟") },
                confirmButton = {
                    TextButton(onClick = {
                        showClearCacheDialog = false
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                try {
                                    context.imageLoader.memoryCache?.clear()
                                    context.imageLoader.diskCache?.clear()
                                    context.cacheDir?.deleteRecursively()
                                    context.cacheDir?.mkdirs()
                                    context.externalCacheDir?.deleteRecursively()
                                    context.externalCacheDir?.mkdirs()
                                } catch (e: Exception) {}
                            }
                            cacheSizeBytes = withContext(Dispatchers.IO) { computeCacheSizeBytes(context) }
                            Toast.makeText(context, "تم مسح الذاكرة المؤقتة بنجاح", Toast.LENGTH_SHORT).show()
                        }
                    }) { Text("نعم", color = Pink, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showClearCacheDialog = false }) { Text("إلغاء") }
                },
                containerColor = Color(0xFF1E1E2E),
                titleContentColor = Color.White,
                textContentColor = Muted
            )
        }
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