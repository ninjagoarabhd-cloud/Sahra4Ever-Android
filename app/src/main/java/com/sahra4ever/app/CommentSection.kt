package com.sahra4ever.app

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Pink = Color(0xFFFF2FA6)
private val Purple = Color(0xFF7B2CFF)
private val GlassFill = Color.White.copy(alpha = 0.12f)
private val GlassStroke = Color.White.copy(alpha = 0.22f)
private val Muted = Color(0xFFD0CFDA)

private const val PAGE_SIZE = 10
private const val MAX_LEN = 500
private const val MIN_INTERVAL_MS = 3_000L

data class Comment(
    val id: String,
    val uid: String,
    val name: String,
    val photoUrl: String?,
    val text: String,
    val createdAt: Long?
)

data class CommentsState(
    val comments: List<Comment> = emptyList(),
    val loaded: Boolean = false,
    val failed: Boolean = false
)

object CommentsRepo {
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private fun col(titleKey: String) =
        db.collection("titles").document(titleKey).collection("comments")

    fun observe(titleKey: String, limit: Long = 50): Flow<CommentsState> = callbackFlow {
        val registration = col(titleKey)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) {
                    trySend(CommentsState(loaded = true, failed = true))
                    return@addSnapshotListener
                }
                val list = snap.documents.mapNotNull { d ->
                    val text = d.getString("text") ?: return@mapNotNull null
                    Comment(
                        id = d.id,
                        uid = d.getString("uid").orEmpty(),
                        name = d.getString("name")?.takeIf { it.isNotBlank() } ?: GUEST_NAME,
                        photoUrl = d.getString("photoUrl"),
                        text = text,
                        createdAt = d.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time
                    )
                }
                trySend(CommentsState(comments = list, loaded = true))
            }
        awaitClose { registration.remove() }
    }

    fun post(titleKey: String, user: AuthUser, text: String, onError: () -> Unit) {
        col(titleKey).add(
            hashMapOf(
                "uid" to user.uid,
                "name" to user.name,
                "photoUrl" to user.photoUrl,
                "text" to text.trim().take(MAX_LEN),
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).addOnFailureListener { onError() }
    }
}

private fun formatCommentDate(ms: Long?): String {
    if (ms == null) return "الآن"
    val diff = System.currentTimeMillis() - ms
    return when {
        diff < 60_000L -> "الآن"
        diff < 3_600_000L -> "منذ ${diff / 60_000L} د"
        diff < 86_400_000L -> "منذ ${diff / 3_600_000L} س"
        else -> SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(ms))
    }
}

@Composable
internal fun UserAvatar(name: String, photoUrl: String?, size: Dp, showInitial: Boolean = true) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Purple, Pink))),
        contentAlignment = Alignment.Center
    ) {
        when {
            photoUrl != null -> AsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            showInitial && name.isNotBlank() -> Text(
                name.first().uppercase(),
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.45f).sp
            )
            else -> Icon(Icons.Outlined.Person, null, tint = Color.White, modifier = Modifier.size(size * 0.6f))
        }
    }
}

@Composable
fun CommentSection(movieId: Int, mediaType: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val titleKey = remember(movieId, mediaType) { "${mediaType}_$movieId" }
    val user by AuthManager.user.collectAsState()

    var state by remember(titleKey) { mutableStateOf(CommentsState()) }
    var visible by remember(titleKey) { mutableIntStateOf(PAGE_SIZE) }
    var text by remember { mutableStateOf("") }
    var showAuth by remember { mutableStateOf(false) }
    var lastSentAt by remember { mutableLongStateOf(0L) }
    val guestClick = remember { MutableInteractionSource() }

    LaunchedEffect(titleKey) {
        CommentsRepo.observe(titleKey).collect { state = it }
    }

    fun send() {
        val u = user ?: run { showAuth = true; return }
        val t = text.trim()
        if (t.isEmpty()) return
        val now = System.currentTimeMillis()
        if (now - lastSentAt < MIN_INTERVAL_MS) {
            Toast.makeText(context, "انتظر قليلاً قبل إرسال تعليق آخر", Toast.LENGTH_SHORT).show()
            return
        }
        lastSentAt = now
        CommentsRepo.post(titleKey, u, t) {
            Toast.makeText(context, "تعذّر إرسال التعليق", Toast.LENGTH_SHORT).show()
        }
        text = ""
    }

    Column(modifier.fillMaxWidth()) {
        Text(
            if (state.loaded && !state.failed) "التعليقات (${state.comments.size})" else "التعليقات",
            color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(12.dp))

        Box(Modifier.fillMaxWidth()) {
            TextField(
                value = text,
                onValueChange = { text = it.take(MAX_LEN) },
                enabled = user != null,
                maxLines = 4,
                placeholder = {
                    Text(if (user == null) "سجّل الدخول لإضافة تعليق..." else "اكتب تعليقك...", color = Muted)
                },
                trailingIcon = {
                    IconButton(onClick = { send() }, enabled = user != null && text.isNotBlank()) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Send, "إرسال",
                            tint = if (user != null && text.isNotBlank()) Pink else Muted
                        )
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = GlassFill, unfocusedContainerColor = GlassFill, disabledContainerColor = GlassFill,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent,
                    cursorColor = Pink,
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White, disabledTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().border(1.dp, GlassStroke, RoundedCornerShape(16.dp))
            )
            if (user == null) {
                Box(
                    Modifier
                        .matchParentSize()
                        .clickable(interactionSource = guestClick, indication = null) { showAuth = true }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        when {
            !state.loaded -> Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(24.dp), color = Pink, strokeWidth = 2.dp)
            }
            state.failed -> Text("تعذّر تحميل التعليقات", color = Muted, fontSize = 14.sp)
            state.comments.isEmpty() -> Text("لا توجد تعليقات بعد. كن أول من يعلّق!", color = Muted, fontSize = 14.sp)
            else -> {
                state.comments.take(visible).forEach { c ->
                    key(c.id) { CommentItem(c) }
                }
                if (state.comments.size > visible) {
                    TextButton(onClick = { visible += PAGE_SIZE }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("عرض المزيد", color = Pink, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showAuth) AuthSheet(onDismiss = { showAuth = false })
}

@Composable
private fun CommentItem(c: Comment) {
    val shape = RoundedCornerShape(16.dp)
    val date = remember(c.createdAt) { formatCommentDate(c.createdAt) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(shape)
            .background(GlassFill)
            .border(1.dp, GlassStroke, shape)
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        UserAvatar(c.name, c.photoUrl, 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    c.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(8.dp))
                Text(date, color = Muted, fontSize = 11.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text(c.text, color = Color.White.copy(alpha = 0.92f), fontSize = 14.sp, lineHeight = 21.sp)
        }
    }
}