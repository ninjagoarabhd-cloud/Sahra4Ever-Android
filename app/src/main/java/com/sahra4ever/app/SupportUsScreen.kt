package com.sahra4ever.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Pink = Color(0xFFFF2FA6)
private val Purple = Color(0xFF7B2CFF)
private val Ink = Color(0xFF0F0E14)
private val GlassFill = Color.White.copy(alpha = 0.10f)
private val GlassStroke = Color.White.copy(alpha = 0.20f)
private val Muted = Color(0xFFD0CFDA)

private const val DONATION_URL = "https://example.com/support" // ضع رابط الدعم هنا
private const val TELEGRAM_URL = "https://t.me/Sahra4Ever"
private const val SHARE_TEXT =
    "🎬 جرّب تطبيق سهرة فور إيفر — أفلام ومسلسلات وأنمي بتجربة مشاهدة ممتعة ومجانية!\n" +
        "انضم لقناتنا على تيليجرام لمعرفة آخر التحديثات:\n$TELEGRAM_URL"

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "تعذّر فتح الرابط", Toast.LENGTH_SHORT).show()
    }
}

private fun shareApp(context: Context) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "سهرة فور إيفر")
        putExtra(Intent.EXTRA_TEXT, SHARE_TEXT)
    }
    context.startActivity(Intent.createChooser(send, "مشاركة التطبيق عبر"))
}

@Composable
internal fun SimpleBackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.35f))
            .border(1.dp, GlassStroke, CircleShape)
            .clickable(onClick = onBack),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "رجوع", tint = Color.White)
    }
}

@Composable
fun SupportUsScreen(onBack: () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val context = LocalContext.current

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
                Text("ادعمنا", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .padding(top = 8.dp)
                        .size(180.dp)
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(Pink.copy(alpha = 0.55f), Purple.copy(alpha = 0.28f), Color.Transparent)
                                ),
                                radius = size.minDimension / 2f
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Sahra4Ever Logo",
                        modifier = Modifier.height(64.dp)
                    )
                }

                Text(
                    "دعمك ومشاركتك تساعدنا على استمرار المنصة مجاناً",
                    color = Color.White,
                    fontSize = 20.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )

                Spacer(Modifier.height(20.dp))

                SupportCard(
                    icon = Icons.Outlined.Favorite,
                    title = "الدعم المادي",
                    subtitle = "ساهم في تغطية تكاليف السيرفرات والتطوير"
                ) {
                    Button(
                        onClick = { openUrl(context, DONATION_URL) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Pink, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) { Text("ادعمنا الآن", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                }

                Spacer(Modifier.height(14.dp))

                SupportCard(
                    icon = Icons.Outlined.Share,
                    title = "مشاركة التطبيق",
                    subtitle = "أخبر أصدقاءك عن سهرة فور إيفر"
                ) {
                    Button(
                        onClick = { shareApp(context) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) { Text("مشاركة", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                }

                Spacer(Modifier.height(14.dp))

                SupportCard(
                    icon = Icons.AutoMirrored.Outlined.Send,
                    title = "قناة التيليجرام",
                    subtitle = "انضم لتصلك آخر الأفلام والتحديثات أولاً بأول"
                ) {
                    Button(
                        onClick = { openUrl(context, TELEGRAM_URL) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF229ED9), contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) { Text("انضم الآن", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SupportCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    action: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(GlassFill)
            .border(1.dp, GlassStroke, shape)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Purple, Pink))),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = Color.White) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Muted, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        action()
    }
}