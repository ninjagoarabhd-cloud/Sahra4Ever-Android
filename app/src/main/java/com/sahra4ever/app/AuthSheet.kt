package com.sahra4ever.app

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val Pink = Color(0xFFFF2FA6)
private val Muted = Color(0xFFD0CFDA)
private val FieldBg = Color(0xFF2A2A2A)

private enum class AuthMode { Login, Register }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var mode by remember { mutableStateOf(AuthMode.Login) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun launchAuth(block: suspend () -> Unit) {
        if (busy) return
        scope.launch {
            busy = true
            error = null
            try {
                block()
                onDismiss()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = friendlyAuthError(e)
            }
            busy = false
        }
    }

    fun submit() {
        val mail = email.trim()
        when {
            mode == AuthMode.Register && name.isBlank() -> { error = "أدخل اسمك"; return }
            mail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(mail).matches() -> { error = "أدخل بريداً إلكترونياً صحيحاً"; return }
            password.length < 6 -> { error = "كلمة المرور 6 أحرف على الأقل"; return }
        }
        launchAuth {
            if (mode == AuthMode.Register) AuthManager.registerWithEmail(name, mail, password)
            else AuthManager.signInWithEmail(mail, password)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1E1E1E)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("سجّل الدخول للمشاركة", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "التصفح متاح للجميع، أما التعليق فيحتاج حساباً",
                color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))

            if (mode == AuthMode.Register) {
                AuthField(name, { name = it }, "الاسم الظاهر في التعليقات", enabled = !busy, ltr = false)
                Spacer(Modifier.height(10.dp))
            }
            AuthField(
                email, { email = it }, "البريد الإلكتروني", enabled = !busy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
            )
            Spacer(Modifier.height(10.dp))
            AuthField(
                password, { password = it }, "كلمة المرور", enabled = !busy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailing = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (showPassword) "إخفاء" else "إظهار",
                            tint = Muted
                        )
                    }
                }
            )

            error?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = Color(0xFFFF6B6B), fontSize = 13.sp, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { submit() },
                enabled = !busy,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Pink, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(
                        if (mode == AuthMode.Login) "تسجيل الدخول" else "إنشاء حساب",
                        fontWeight = FontWeight.Bold, fontSize = 16.sp
                    )
                }
            }

            TextButton(
                onClick = {
                    mode = if (mode == AuthMode.Login) AuthMode.Register else AuthMode.Login
                    error = null
                },
                enabled = !busy
            ) {
                Text(
                    if (mode == AuthMode.Login) "ليس لديك حساب؟ أنشئ حساباً" else "لديك حساب؟ سجّل الدخول",
                    color = Pink, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AuthField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    ltr: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: @Composable (() -> Unit)? = null
) {
    TextField(
        value = value,
        onValueChange = onChange,
        enabled = enabled,
        singleLine = true,
        placeholder = { Text(placeholder, color = Muted) },
        trailingIcon = trailing,
        textStyle = LocalTextStyle.current.copy(textDirection = if (ltr) TextDirection.Ltr else TextDirection.Content),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = FieldBg, unfocusedContainerColor = FieldBg, disabledContainerColor = FieldBg,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent,
            cursorColor = Pink,
            focusedTextColor = Color.White, unfocusedTextColor = Color.White, disabledTextColor = Color.White.copy(alpha = 0.6f)
        ),
        modifier = Modifier.fillMaxWidth()
    )
}