package com.example.booksopdsapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter

@Composable
fun SearchDialog(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("搜索") },
        text = {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                label = { Text("輸入書名關鍵字") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            )
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onClear) { Icon(Icons.Filled.Clear, contentDescription = "清除") }
                Button(onClick = onSearch) { Icon(Icons.Filled.Search, contentDescription = "搜索") }
            }
        }
    )
}

@Composable
fun LoadErrorDialog(
    message: String,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("讀取失敗") },
        text = { Text("無法讀取 OPDS。\n$message") },
        confirmButton = {
            Button(onClick = onConfirm) { Icon(Icons.Filled.Check, contentDescription = "確定") }
        }
    )
}

@Composable
fun DebugDialog(
    message: String,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onConfirm,
        title = { Text("Debug") },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onConfirm) { Icon(Icons.Filled.Check, contentDescription = "確定") }
        }
    )
}

@Composable
fun ProfilesDialog(
    profiles: List<SavedOpdsProfile>,
    onSelect: (SavedOpdsProfile) -> Unit,
    onDelete: (SavedOpdsProfile) -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("已記憶 OPDS", style = MaterialTheme.typography.titleLarge)
                val profileListState = rememberLazyListState()

                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        state = profileListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(profiles) { profile ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { onSelect(profile) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(profile.name)
                                    }
                                    Button(
                                        onClick = { onDelete(profile) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = "刪除")
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                    ProfileListScrollbar(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxSize()
                            .width(6.dp),
                        listState = profileListState,
                        totalItems = profiles.size
                    )
                }
                if (profiles.isEmpty()) {
                    Text("目前沒有記憶資料")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onImport) { Icon(Icons.Filled.FileUpload, contentDescription = "匯入") }
                        Button(onClick = onExport) { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "匯出") }
                    }
                    Button(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "關閉") }
                }
            }
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    targetName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("確認刪除") },
        text = { Text("要刪除記錄「$targetName」嗎？") },
        confirmButton = { Button(onClick = onConfirm) { Icon(Icons.Filled.Check, contentDescription = "確認") } },
        dismissButton = { Button(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "取消") } }
    )
}

@Composable
fun BookDetailDialog(
    book: OpdsBook,
    actionLinks: List<OpdsViewModel.BookActionLink>,
    coverUrl: String?,
    onActionClick: (OpdsViewModel.BookActionLink) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(16.dp)
        ) {
            val bodyStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = (MaterialTheme.typography.bodyLarge.fontSize.value + 2).sp
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("書籍詳情", style = MaterialTheme.typography.titleLarge)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (coverUrl != null) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(model = coverUrl),
                                contentDescription = "Book cover",
                                modifier = Modifier
                                    .height(216.dp)
                                    .width(144.dp)
                            )
                        }
                    }
                    Text("${book.title} - ${book.author}", style = bodyStyle)
                    if (book.summary.isNotBlank()) {
                        Text(book.summary, style = bodyStyle)
                    }
                    if (actionLinks.isEmpty()) {
                        Text("沒有可下載的書籍連結", style = bodyStyle)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    Button(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "關閉")
                    }
                    actionLinks.take(2).forEach { action ->
                        Button(onClick = { onActionClick(action) }) {
                            Text(actionTypeLabel(action))
                        }
                    }
                }
            }
        }
    }
}

private fun actionTypeLabel(action: OpdsViewModel.BookActionLink): String {
    if (action.extensionHint.isNotBlank()) return action.extensionHint.uppercase()
    val mime = action.mimeType.substringBefore(";").trim().lowercase()
    if (mime.isBlank()) return "下載"
    val subtype = mime.substringAfter("/", "").substringBefore("+").ifBlank { "下載" }
    return subtype.uppercase()
}
