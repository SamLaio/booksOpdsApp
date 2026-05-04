package com.example.booksopdsapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
                Button(onClick = onClear) { Text("清除") }
                Button(onClick = onSearch) { Text("搜索") }
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
            Button(onClick = onConfirm) { Text("確定") }
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
            Button(onClick = onConfirm) { Text("確定") }
        }
    )
}

@Composable
fun ProfilesDialog(
    profiles: List<SavedOpdsProfile>,
    onSelect: (SavedOpdsProfile) -> Unit,
    onDelete: (SavedOpdsProfile) -> Unit,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
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
                                        Text("刪除")
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

                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("關閉")
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
        confirmButton = { Button(onClick = onConfirm) { Text("確認") } },
        dismissButton = { Button(onClick = onDismiss) { Text("取消") } }
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("書籍詳情") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Text("${book.title} - ${book.author}")
                if (book.summary.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(book.summary)
                    }
                }
                if (actionLinks.isEmpty()) {
                    Text("沒有可下載的書籍連結")
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                actionLinks.take(2).forEach { action ->
                    Button(onClick = { onActionClick(action) }) {
                        Text(action.label)
                    }
                }
            }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("關閉") } }
    )
}
