package com.unesell.comfysorter

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unesell.comfysorter.ui.theme.GlassBorder

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ServerListScreen(
    repository: ServerRepository,
    onNavigateToScanner: () -> Unit,
    onServerClick: (String) -> Unit
) {
    val servers by repository.servers.collectAsState()
    var showRenameDialog by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBlobsBackground()

        Column(modifier = Modifier.fillMaxSize()) {

            // Шапка — опущена ниже (80dp вместо 64dp)
            Column(
                modifier = Modifier
                    .padding(top = 80.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)
            ) {
                Text(
                    text = "Выбор сервера",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Основные параметры ComfyFileSorter",
                    color = Color(0xFF9A9A9A),
                    fontSize = 15.sp
                )
            }

            // Список серверов
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                if (servers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Нет сохранённых серверов.\nДобавьте новое подключение.",
                            color = Color(0xFF9A9A9A),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 15.sp
                        )
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(servers, key = { it.url }) { server ->
                            ServerCard(
                                server = server,
                                onClick = { onServerClick(server.url) },
                                onDelete = { repository.removeServer(server.url) },
                                onRename = { showRenameDialog = server.url }
                            )
                        }
                    }
                }
            }

            // Кнопка "Новое подключение"
            Box(
                modifier = Modifier
                    .padding(start = 24.dp, end = 24.dp, bottom = 40.dp, top = 16.dp)
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color(0xFF2B2930).copy(alpha = 0.95f))
                    .border(1.dp, GlassBorder, RoundedCornerShape(30.dp))
                    .clickable { onNavigateToScanner() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Новое подключение",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp
                    )
                }
            }
        }
    }

    // Диалог переименования
    showRenameDialog?.let { url ->
        val server = servers.find { it.url == url }
        if (server != null) {
            RenameServerDialog(
                initialName = server.name,
                onConfirm = { newName ->
                    repository.renameServer(url, newName)
                    showRenameDialog = null
                },
                onDismiss = { showRenameDialog = null }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ServerCard(
    server: SavedServer,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF28242E).copy(alpha = 0.80f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { onRename() }
            )
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = server.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    // Иконка подсказки редактирования
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Переименовать",
                        tint = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.size(14.dp).padding(start = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = server.url,
                    color = Color(0xFF6B9BF2),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete",
                        tint = Color(0xFFCD3737)
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun RenameServerDialog(
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1C24),
        title = { Text("Переименовать сервер", color = Color.White) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    singleLine = true,
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF6B9BF2))
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank()
            ) { Text("Сохранить", color = Color(0xFF6B9BF2)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = Color.White.copy(alpha = 0.6f)) }
        }
    )
}
