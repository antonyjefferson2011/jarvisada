package com.jarvis.mobile.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.mobile.data.local.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Cyan = Color(0xFF42E8F5)
private val Muted = Color(0xFF9DB6C1)
private val Panel = Color(0xFF0C1823)

@Composable
fun JarvisScreen(
    state: ChatUiState,
    onSend: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onMicToggle: () -> Unit,
    onStopSpeaking: () -> Unit,
    onClearHistory: () -> Unit,
    onDismissMessages: () -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }
    var showClearConfirmation by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding()
            .padding(horizontal = 20.dp),
    ) {
        Header(
            state = state,
            onOpenSettings = onOpenSettings,
            onClearHistory = { showClearConfirmation = true },
        )
        HolographicCore(
            status = state.status,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 12.dp),
        )

        if (state.error != null || state.notice != null) {
            MessageBanner(
                message = state.error ?: state.notice.orEmpty(),
                isError = state.error != null,
                onDismiss = onDismissMessages,
            )
            Spacer(Modifier.height(8.dp))
        }

        if (state.messages.isEmpty()) {
            EmptyConversation(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.messages, key = ChatMessage::id) { message ->
                    MessageBubble(message)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(
                onClick = onMicToggle,
                enabled = !state.isProcessing,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (state.status == AssistantStatus.LISTENING) {
                            Color(0xFF1B4650)
                        } else {
                            Panel
                        },
                    )
                    .border(
                        1.dp,
                        if (state.status == AssistantStatus.LISTENING) Cyan else Color(0xFF213744),
                        CircleShape,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = if (state.status == AssistantStatus.LISTENING) {
                        "Parar escuta"
                    } else {
                        "Falar com JARVIS"
                    },
                    tint = if (state.status == AssistantStatus.LISTENING) Cyan else Muted,
                )
            }

            BasicTextField(
                value = input,
                onValueChange = { input = it.take(2000) },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Panel)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    submitMessage(input, onSend, focusManager) { input = "" }
                }),
                maxLines = 4,
                decorationBox = { innerTextField ->
                    Box {
                        if (input.isBlank()) {
                            Text(
                                "Digite uma mensagem…",
                                color = Muted.copy(alpha = 0.8f),
                                fontSize = 15.sp,
                            )
                        }
                        innerTextField()
                    }
                },
            )

            IconButton(
                onClick = onStopSpeaking,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Panel),
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Interromper a fala",
                    tint = Muted,
                    modifier = Modifier.size(20.dp),
                )
            }

            IconButton(
                onClick = {
                    submitMessage(input, onSend, focusManager) { input = "" }
                },
                enabled = input.isNotBlank() && !state.isProcessing,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (input.isNotBlank()) Cyan else Panel),
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Enviar mensagem",
                    tint = if (input.isNotBlank()) Color(0xFF002126) else Muted,
                )
            }
        }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Limpar histórico?") },
            text = { Text("As mensagens serão removidas deste aparelho e, se conectado, da nuvem.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirmation = false
                    onClearHistory()
                }) { Text("Limpar") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun Header(
    state: ChatUiState,
    onOpenSettings: () -> Unit,
    onClearHistory: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "J.A.R.V.I.S.",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.4.sp,
            )
            Text(
                "ASSISTENTE PESSOAL",
                color = Muted,
                fontSize = 9.sp,
                letterSpacing = 2.sp,
            )
        }
    if (state.messages.isNotEmpty()) {
        IconButton(onClick = onClearHistory) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Limpar histórico",
                tint = Muted,
            )
        }
    }
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Abrir configurações",
                tint = Muted,
            )
        }
    }
    StatusPill(status = state.status)
}

@Composable
private fun StatusPill(status: AssistantStatus) {
    val color = when (status) {
        AssistantStatus.ONLINE -> Color(0xFF62E5A0)
        AssistantStatus.OFFLINE -> Muted
        AssistantStatus.LISTENING -> Cyan
        AssistantStatus.PROCESSING -> Color(0xFFFFC66B)
    }
    Row(
        modifier = Modifier
            .padding(top = 2.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.22f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Text(
            status.label,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
        )
    }
}

@Composable
private fun HolographicCore(status: AssistantStatus, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "jarvis-core")
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = if (status == AssistantStatus.PROCESSING) 1.08f else 1.02f,
        animationSpec = infiniteRepeatable(tween(1900), RepeatMode.Reverse),
        label = "core-pulse",
    )
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.size(138.dp)) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val base = size.minDimension * 0.25f * pulse
            drawCircle(Cyan.copy(alpha = 0.045f), radius = base * 2.35f, center = androidx.compose.ui.geometry.Offset(centerX, centerY))
            drawCircle(Cyan.copy(alpha = 0.08f), radius = base * 1.6f, center = androidx.compose.ui.geometry.Offset(centerX, centerY))
            drawCircle(
                color = Cyan.copy(alpha = 0.56f),
                radius = base * 1.28f,
                center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                style = Stroke(width = 1.dp.toPx()),
            )
            drawCircle(
                color = Color(0xFF9BF9FF).copy(alpha = 0.84f),
                radius = base * 0.84f,
                center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                style = Stroke(width = 2.dp.toPx()),
            )
            drawCircle(
                color = Cyan.copy(alpha = 0.78f),
                radius = base * 0.48f,
                center = androidx.compose.ui.geometry.Offset(centerX, centerY),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.92f),
                radius = base * 0.13f,
                center = androidx.compose.ui.geometry.Offset(centerX, centerY),
            )

            val orbitRadius = base * 1.48f
            for (tick in 0 until 24) {
                val angle = Math.toRadians((tick * 15.0))
                val inner = orbitRadius + if (tick % 3 == 0) 2.dp.toPx() else 5.dp.toPx()
                val outer = orbitRadius + 8.dp.toPx()
                val start = androidx.compose.ui.geometry.Offset(
                    centerX + kotlin.math.cos(angle).toFloat() * inner,
                    centerY + kotlin.math.sin(angle).toFloat() * inner,
                )
                val end = androidx.compose.ui.geometry.Offset(
                    centerX + kotlin.math.cos(angle).toFloat() * outer,
                    centerY + kotlin.math.sin(angle).toFloat() * outer,
                )
                drawLine(
                    color = Cyan.copy(alpha = if (tick % 3 == 0) 0.62f else 0.22f),
                    start = start,
                    end = end,
                    strokeWidth = 1.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
        Text(
            text = when (status) {
                AssistantStatus.ONLINE -> "SISTEMA CONECTADO"
                AssistantStatus.OFFLINE -> "COMANDOS LOCAIS DISPONÍVEIS"
                AssistantStatus.LISTENING -> "ESTOU OUVINDO"
                AssistantStatus.PROCESSING -> "PROCESSANDO SOLICITAÇÃO"
            },
            color = Muted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.5.sp,
        )
    }
}

@Composable
private fun EmptyConversation(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "À disposição.",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(7.dp))
        Text(
            "Pergunte algo ou use o microfone para executar comandos locais.",
            color = Muted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "Experimente: “Que horas são?” • “Calcule 18 × 7” • “Abrir câmera”",
            color = Muted.copy(alpha = 0.8f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            lineHeight = 17.sp,
        )
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(
                    RoundedCornerShape(
                        topStart = 17.dp,
                        topEnd = 17.dp,
                        bottomStart = if (isUser) 17.dp else 5.dp,
                        bottomEnd = if (isUser) 5.dp else 17.dp,
                    ),
                )
                .background(if (isUser) Color(0xFF10303A) else Panel)
                .border(
                    1.dp,
                    if (isUser) Cyan.copy(alpha = 0.17f) else Color(0xFF20323E),
                    RoundedCornerShape(17.dp),
                )
                .padding(horizontal = 14.dp, vertical = 11.dp)
                .animateContentSize(),
        ) {
            Text(
                if (isUser) "VOCÊ" else "J.A.R.V.I.S.",
                color = if (isUser) Cyan else Muted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                message.content,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                lineHeight = 21.sp,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(Date(message.createdAt)),
                color = Muted.copy(alpha = 0.7f),
                fontSize = 9.sp,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

@Composable
private fun MessageBanner(message: String, isError: Boolean, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isError) Color(0xFF321A25) else Color(0xFF102B31),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                message,
                color = if (isError) Color(0xFFFFA2AF) else Color(0xFFA2F1D0),
                fontSize = 12.sp,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    }
}

private fun submitMessage(
    text: String,
    onSend: (String) -> Unit,
    focusManager: FocusManager,
    clearInput: () -> Unit,
) {
    if (text.isBlank()) return
    onSend(text)
    clearInput()
    focusManager.clearFocus()
}
