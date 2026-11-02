package com.jarvis.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.mobile.data.local.MemoryEntry

@Composable
fun SettingsScreen(
    state: ChatUiState,
    onBack: () -> Unit,
    onSaveBackend: (String, String) -> Unit,
    onSignIn: (String, String) -> Unit,
    onCreateAccount: (String, String) -> Unit,
    onSignOut: () -> Unit,
    onSaveVoiceSettings: (Boolean, Float, Float, Boolean, Boolean) -> Unit,
    onAddMemory: (String) -> Unit,
    onRemoveMemory: (MemoryEntry) -> Unit,
    onClearHistory: () -> Unit,
    onDismissMessages: () -> Unit,
) {
    var backendUrl by remember(state.backendUrl) { mutableStateOf(state.backendUrl) }
    var anonymousKey by remember(state.anonymousKey) { mutableStateOf(state.anonymousKey) }
    var email by rememberSaveable { mutableStateOf(state.connectedEmail) }
    var password by rememberSaveable { mutableStateOf("") }
    var memoryDraft by rememberSaveable { mutableStateOf("") }
    var voiceEnabled by remember(state.voiceEnabled) { mutableStateOf(state.voiceEnabled) }
    var voiceSpeed by remember(state.voiceSpeed) { mutableStateOf(state.voiceSpeed) }
    var voiceVolume by remember(state.voiceVolume) { mutableStateOf(state.voiceVolume) }
    var wakeWordPrepared by remember(state.wakeWordPrepared) { mutableStateOf(state.wakeWordPrepared) }
    var historyEnabled by remember(state.historyEnabled) { mutableStateOf(state.historyEnabled) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    LaunchedEffect(state.isConnected) {
        if (state.isConnected) password = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
            }
            Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                "Configurações",
                modifier = Modifier.padding(start = 10.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (state.error != null || state.notice != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (state.error != null) Color(0xFF321A25) else Color(0xFF102B31),
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        state.error ?: state.notice.orEmpty(),
                        modifier = Modifier.weight(1f),
                        color = if (state.error != null) Color(0xFFFFA2AF) else Color(0xFFA2F1D0),
                        fontSize = 12.sp,
                    )
                    TextButton(onClick = onDismissMessages) { Text("OK") }
                }
            }
        }

        SectionCard(title = "Backend e conta") {
            Text(
                "O aplicativo usa a URL do projeto e a chave pública anon. Nunca cole aqui a chave service_role ou a chave secreta da Groq.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
            OutlinedTextField(
                value = backendUrl,
                onValueChange = { backendUrl = it.trim() },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("URL HTTPS do Supabase") },
                placeholder = { Text("https://seu-projeto.supabase.co") },
                singleLine = true,
            )
            OutlinedTextField(
                value = anonymousKey,
                onValueChange = { anonymousKey = it.trim() },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Chave pública anon / publishable") },
                singleLine = true,
            )
            Button(
                onClick = { onSaveBackend(backendUrl, anonymousKey) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Salvar backend")
            }
            HorizontalDivider(color = Color(0xFF263947))
            Text(
                if (state.isConnected) "Conectado como ${state.connectedEmail}" else "Conta Supabase",
                color = if (state.isConnected) Color(0xFF62E5A0) else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("E-mail") },
                singleLine = true,
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Senha da conta") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                OutlinedButton(
                    onClick = { onSignIn(email.trim(), password) },
                    enabled = !state.isConnected && !state.isProcessing &&
                        email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Entrar")
                }
                Button(
                    onClick = { onCreateAccount(email.trim(), password) },
                    enabled = !state.isConnected && !state.isProcessing &&
                        email.isNotBlank() && password.length >= 8,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Criar conta")
                }
            }
            if (state.isConnected) {
                OutlinedButton(
                    onClick = onSignOut,
                    enabled = !state.isProcessing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Sair da conta")
                }
            } else {
                Text(
                    "Use uma senha com pelo menos 8 caracteres. Se a confirmação por e-mail estiver ativa no Supabase, confirme o endereço antes de entrar.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                )
            }
        }

        SectionCard(title = "Voz") {
            ToggleSetting(
                title = "Ler respostas em voz alta",
                checked = voiceEnabled,
                onCheckedChange = { voiceEnabled = it },
            )
            Text("Velocidade: ${"%.1f".format(java.util.Locale("pt", "BR"), voiceSpeed)}×", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Slider(
                value = voiceSpeed,
                onValueChange = { voiceSpeed = it },
                valueRange = 0.7f..1.4f,
            )
            Text("Volume da fala: ${(voiceVolume * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Slider(
                value = voiceVolume,
                onValueChange = { voiceVolume = it },
                valueRange = 0f..1f,
            )
            ToggleSetting(
                title = "Preparar palavra-chave JARVIS",
                checked = wakeWordPrepared,
                onCheckedChange = { wakeWordPrepared = it },
            )
            Text(
                "A ativação contínua ainda não é executada. O Android restringe o uso do microfone em segundo plano; o botão de microfone funciona apenas com o app aberto.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            )
            Button(
                onClick = {
                    onSaveVoiceSettings(
                        voiceEnabled,
                        voiceSpeed,
                        voiceVolume,
                        wakeWordPrepared,
                        historyEnabled,
                    )
                },
                enabled = !state.isProcessing,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Salvar preferências") }
        }

        SectionCard(title = "Memória de longo prazo") {
            Text(
                "Anotações que você adicionar podem ser incluídas no contexto enviado à IA. Guarde apenas informações que deseja usar nas conversas.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
            OutlinedTextField(
                value = memoryDraft,
                onValueChange = { memoryDraft = it.take(400) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nova memória") },
                placeholder = { Text("Ex.: prefiro respostas objetivas") },
                minLines = 2,
                maxLines = 4,
            )
            Button(
                onClick = {
                    onAddMemory(memoryDraft)
                    memoryDraft = ""
                },
                enabled = memoryDraft.isNotBlank() && !state.isProcessing,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Salvar memória") }
            if (state.memories.isEmpty()) {
                Text("Nenhuma memória salva.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    state.memories.forEach { memory ->
                        MemoryRow(memory = memory, onRemove = { onRemoveMemory(memory) })
                    }
                }
            }
        }

        SectionCard(title = "Histórico e aparelho") {
            ToggleSetting(
                title = "Salvar histórico neste aparelho",
                checked = historyEnabled,
                onCheckedChange = { historyEnabled = it },
            )
            Text(
                "O histórico local usa SQLite. Com uma conta conectada, mensagens e memórias são sincronizadas com o Supabase e protegidas por RLS.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            )
            Text(state.deviceInfo, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
            OutlinedButton(
                onClick = { showClearConfirmation = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(17.dp))
                Text("  Apagar histórico")
            }
        }

        Spacer(Modifier.height(4.dp))
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Apagar histórico?") },
            text = { Text("As mensagens serão apagadas do aparelho e, se conectado, da nuvem.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirmation = false
                    onClearHistory()
                }) { Text("Apagar") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1823)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                title,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
            content()
        }
    }
}

@Composable
private fun ToggleSetting(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, modifier = Modifier.weight(1f), fontSize = 13.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun MemoryRow(memory: MemoryEntry, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF122231), RoundedCornerShape(11.dp))
            .padding(start = 11.dp, top = 8.dp, bottom = 8.dp, end = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            memory.content,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(38.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Remover memória", tint = Color(0xFFFF8A99))
        }
    }
}
