package com.sahra4ever.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

private val Ink = Color(0xFF0F0E14)
private val Muted = Color(0xFFD0CFDA)

@Composable
fun MyListScreen(
    onBack: () -> Unit,
    onMovieClick: (Int, String, String) -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val context = LocalContext.current

        // ✅ يضمن تحميل المفضلة من التخزين المحلي حتى لو كانت هذه أول شاشة تُفتح في التطبيق
        LaunchedEffect(Unit) { FavoritesManager.init(context) }
        val favorites by FavoritesManager.favorites.collectAsState()

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
                Text("قائمتي", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            if (favorites.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.FavoriteBorder, null,
                            tint = Muted, modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("قائمتك فارغة حالياً", color = Muted, fontSize = 16.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("أضف أفلامك ومسلسلاتك المفضلة لتظهر هنا", color = Muted.copy(alpha = 0.6f), fontSize = 13.sp)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(favorites, key = { "${it.type}-${it.id}" }) { item ->
                        FavoritePosterCard(
                            item = item,
                            onClick = { onMovieClick(item.id, item.type, item.title) },
                            onRemove = { FavoritesManager.remove(context, item.id, item.type) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoritePosterCard(item: FavoriteItem, onClick: () -> Unit, onRemove: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick)) {
        Box {
            AsyncImage(
                model = tmdbImg(item.posterPath, "w342"),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.08f))
            )
            // ✅ إزالة سريعة من المفضلة مباشرة من داخل الشبكة
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Close, "إزالة من المفضلة", tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
        Text(
            item.title,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}