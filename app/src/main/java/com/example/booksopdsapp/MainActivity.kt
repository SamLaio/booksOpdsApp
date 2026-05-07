package com.example.booksopdsapp

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnitType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

private const val PREF_UI = "opds_ui_prefs"
private const val KEY_DARK_MODE = "dark_mode"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiPrefs = remember { getSharedPreferences(PREF_UI, Context.MODE_PRIVATE) }
            var isDarkMode by rememberSaveable {
                mutableStateOf(uiPrefs.getBoolean(KEY_DARK_MODE, false))
            }
            LaunchedEffect(isDarkMode) {
                uiPrefs.edit().putBoolean(KEY_DARK_MODE, isDarkMode).apply()
            }
            MaterialTheme(
                colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme(),
                typography = enlargedTypography(MaterialTheme.typography, 2)
            ) {
                OpdsApp(
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { isDarkMode = !isDarkMode }
                )
            }
        }
    }
}

private fun enlargedTypography(base: Typography, deltaSp: Int): Typography {
    fun TextStyle.bump(): TextStyle {
        if (fontSize.type != TextUnitType.Sp) return this
        return copy(fontSize = (fontSize.value + deltaSp).sp)
    }

    return base.copy(
        displayLarge = base.displayLarge.bump(),
        displayMedium = base.displayMedium.bump(),
        displaySmall = base.displaySmall.bump(),
        headlineLarge = base.headlineLarge.bump(),
        headlineMedium = base.headlineMedium.bump(),
        headlineSmall = base.headlineSmall.bump(),
        titleLarge = base.titleLarge.bump(),
        titleMedium = base.titleMedium.bump(),
        titleSmall = base.titleSmall.bump(),
        bodyLarge = base.bodyLarge.bump(),
        bodyMedium = base.bodyMedium.bump(),
        bodySmall = base.bodySmall.bump(),
        labelLarge = base.labelLarge.bump(),
        labelMedium = base.labelMedium.bump(),
        labelSmall = base.labelSmall.bump()
    )
}

@Composable
private fun OpdsApp(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    vm: OpdsViewModel = viewModel()
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedBook by remember { mutableStateOf<OpdsBook?>(null) }
    var inputExpanded by rememberSaveable { mutableStateOf(true) }
    var rememberProfile by rememberSaveable { mutableStateOf(true) }
    var showProfiles by remember { mutableStateOf(false) }
    var pendingSaveProfile by remember { mutableStateOf(false) }
    var rememberedProfiles by remember { mutableStateOf(loadProfiles(context)) }
    var pendingDeleteProfile by remember { mutableStateOf<SavedOpdsProfile?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var showLoadErrorDialog by remember { mutableStateOf(false) }
    var loadErrorMessage by remember { mutableStateOf("") }
    val trackedDownloads = remember { mutableMapOf<Long, String>() }
    val contentListState = rememberLazyListState()
    val uiScope = rememberCoroutineScope()
    val exportProfileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val exported = exportProfilesJson(rememberedProfiles)
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(exported.toByteArray(Charsets.UTF_8))
            } ?: error("無法開啟輸出檔案")
        }.onSuccess {
            Toast.makeText(context, "已匯出檔案", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "匯出失敗：${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
    val importProfileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val raw = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                reader.readText()
            } ?: error("無法讀取匯入檔案")
            val imported = importProfilesJson(raw)
            if (imported.isEmpty()) error("內容不是有效的 OPDS 設定檔")
            var merged = rememberedProfiles
            imported.forEach { profile ->
                merged = upsertProfile(merged, profile)
            }
            rememberedProfiles = merged
            saveProfiles(context, merged)
            imported.size
        }.onSuccess { importedCount ->
            Toast.makeText(context, "已匯入 $importedCount 筆", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "匯入失敗：${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.loading, uiState.error, uiState.feedTitle) {
        if (pendingSaveProfile && !uiState.loading) {
            if (uiState.error == null && rememberProfile) {
                val name = uiState.profileName.ifBlank { uiState.feedTitle.ifBlank { uiState.url } }
                val updated = upsertProfile(
                    profiles = rememberedProfiles,
                    profile = SavedOpdsProfile(
                        name = name,
                        url = uiState.url,
                        username = uiState.username,
                        password = uiState.password
                    )
                )
                rememberedProfiles = updated
                saveProfiles(context, updated)
                vm.updateProfileName(name)
            }
            pendingSaveProfile = false
        }
    }
    LaunchedEffect(uiState.error) {
        if (!uiState.error.isNullOrBlank()) {
            val raw = uiState.error ?: "無法讀取 OPDS"
            loadErrorMessage = if (
                raw.contains("failed to connect", ignoreCase = true) ||
                raw.contains("timeout", ignoreCase = true) ||
                raw.contains("timed out", ignoreCase = true)
            ) {
                "無法連接，請確認連線狀況"
            } else {
                raw
            }
            showLoadErrorDialog = true
        }
    }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
                val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (downloadId <= 0L) return
                val fileName = trackedDownloads.remove(downloadId) ?: return
                val dm = ctx.getSystemService(DownloadManager::class.java)
                val query = DownloadManager.Query().setFilterById(downloadId)
                dm.query(query).use { cursor ->
                    if (!cursor.moveToFirst()) return
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    if (statusIndex == -1) return
                    when (cursor.getInt(statusIndex)) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            cleanupStaleDownloadNotifications(dm, downloadId, fileName)
                            Toast.makeText(ctx, "下載完成：$fileName", Toast.LENGTH_SHORT).show()
                        }
                        DownloadManager.STATUS_FAILED -> {
                            Toast.makeText(ctx, "下載失敗：$fileName", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            if (uiState.feedTitle.isBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("OPDS Reader", style = MaterialTheme.typography.headlineMedium)
                    Button(onClick = onToggleDarkMode) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "切換日夜模式"
                        )
                    } 
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val fixedName = uiState.profileName.ifBlank { uiState.feedTitle }
                        Text(
                            text = fixedName,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee()
                        )
                        if (uiState.contextLabel.isNotBlank()) {
                            Text(
                                text = uiState.contextLabel,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .basicMarquee()
                            )
                        }
                    }
                    Button(onClick = {
                        inputExpanded = !inputExpanded
                        searchQuery = ""
                    }, enabled = !uiState.loading) {
                        Icon(
                            imageVector = if (inputExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = "切換"
                        )
                    }
                    if (inputExpanded) {
                        Button(onClick = onToggleDarkMode, enabled = !uiState.loading) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = "切換日夜模式"
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                vm.goBack()
                                uiScope.launch { contentListState.scrollToItem(0) }
                            },
                            enabled = !uiState.loading && uiState.canGoBack
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "返回"
                            )
                        }
                    }
                }
            }
            if (inputExpanded) {
                OutlinedTextField(
                    value = uiState.profileName,
                    onValueChange = vm::updateProfileName,
                    label = { Text("名稱（記錄用）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.url,
                    onValueChange = vm::updateUrl,
                    label = { Text("OPDS Feed URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.username,
                    onValueChange = vm::updateUsername,
                    label = { Text("帳號（可選）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = vm::updatePassword,
                    label = { Text("密碼（可選）") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberProfile,
                            onCheckedChange = { rememberProfile = it },
                            enabled = !uiState.loading
                        )
                        Text("記憶")
                    }
                    Button(onClick = {
                        pendingSaveProfile = rememberProfile
                        vm.loadFeed()
                        inputExpanded = false
                    }, enabled = !uiState.loading) {
                        Icon(
                            imageVector = Icons.Filled.FolderOpen,
                            contentDescription = "讀取"
                        )
                    }
                    Button(
                        onClick = { showProfiles = true },
                        enabled = rememberedProfiles.isNotEmpty() && !uiState.loading
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "列表"
                        )
                    }
                }
            }

            val visibleBooks = uiState.books

            Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
                Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = contentListState,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(visibleBooks) { book ->
                                val canNavigate = vm.hasNavigableLink(book)
                                val canRead = vm.hasReadableLink(book)
                                BookCard(
                                    book = book,
                                    canNavigate = canNavigate,
                                    canRead = canRead,
                                    onOpen = {
                                        when {
                                            canNavigate -> vm.openBook(book)
                                            canRead -> selectedBook = book
                                        }
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                    LazyListScrollbar(
                        state = contentListState,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        vm.goPreviousPage()
                        uiScope.launch { contentListState.scrollToItem(0) }
                    },
                    enabled = !uiState.loading && uiState.canGoPreviousPage,
                    modifier = Modifier.weight(0.4f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "上頁"
                    )
                }
                Button(
                    onClick = { showSearch = true },
                    enabled = !uiState.loading && uiState.canSearch,
                    modifier = Modifier.weight(0.2f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "搜索"
                    )
                }
                Button(
                    onClick = {
                        vm.goNextPage()
                        uiScope.launch { contentListState.scrollToItem(0) }
                    },
                    enabled = !uiState.loading && uiState.canGoNextPage,
                    modifier = Modifier.weight(0.4f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "下頁"
                    )
                }
            }

            if (uiState.loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
        }
    }

    if (showSearch) {
        SearchDialog(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onClear = { searchQuery = "" },
            onSearch = {
                vm.searchInOpds(searchQuery)
                uiScope.launch { contentListState.scrollToItem(0) }
                showSearch = false
            },
            onDismiss = { showSearch = false }
        )
    }

    if (showLoadErrorDialog) {
        LoadErrorDialog(
            message = loadErrorMessage,
            onConfirm = {
                showLoadErrorDialog = false
                searchQuery = ""
                inputExpanded = true
                vm.returnToHomeAfterError()
            }
        )
    }

    if (vm.isDebugPopupEnabled()) {
        uiState.debugUrlMessage?.let { msg ->
            DebugDialog(
                message = msg,
                onConfirm = { vm.clearDebugUrlMessage() }
            )
        }
    }

    if (showProfiles) {
        ProfilesDialog(
            profiles = rememberedProfiles,
            onSelect = { profile ->
                vm.updateProfileName(profile.name)
                vm.updateUrl(profile.url)
                vm.updateUsername(profile.username)
                vm.updatePassword(profile.password)
                inputExpanded = true
                showProfiles = false
            },
            onDelete = { profile ->
                pendingDeleteProfile = profile
            },
            onImport = {
                importProfileLauncher.launch(arrayOf("application/json", "text/plain"))
            },
            onExport = {
                exportProfileLauncher.launch("opds_profiles.json")
            },
            onClose = { showProfiles = false }
        )
    }

    pendingDeleteProfile?.let { target ->
        ConfirmDeleteDialog(
            targetName = target.name,
            onConfirm = {
                val updated = rememberedProfiles.filterNot {
                    it.name.equals(target.name, ignoreCase = true)
                }
                rememberedProfiles = updated
                saveProfiles(context, updated)
                pendingDeleteProfile = null
            },
            onDismiss = { pendingDeleteProfile = null }
        )
    }

    selectedBook?.let { book ->
        val actionLinks = vm.resolveReadableLinks(book)
        val coverUrl = vm.resolveCoverUrl(book)
        BookDetailDialog(
            book = book,
            actionLinks = actionLinks,
            coverUrl = coverUrl,
            onActionClick = { action ->
                vm.showDebugMessage(vm.buildDownloadDebugMessage(action))
                val enqueued = enqueueBookDownload(
                    context = context,
                    action = action,
                    username = uiState.username,
                    password = uiState.password
                )
                if (enqueued.isNew) {
                    trackedDownloads[enqueued.id] = enqueued.fileName
                }
                selectedBook = null
            },
            onDismiss = { selectedBook = null }
        )
    }
}

@Composable
private fun LazyListScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier
) {
    val layoutInfo = state.layoutInfo
    val totalItems = layoutInfo.totalItemsCount
    val visibleItems = layoutInfo.visibleItemsInfo
    if (totalItems <= 0 || visibleItems.isEmpty()) return

    val firstVisible = visibleItems.first().index
    val lastVisible = visibleItems.last().index
    val canScroll = firstVisible > 0 || lastVisible < totalItems - 1
    if (!canScroll) return

    val progress = firstVisible.toFloat() / totalItems.toFloat()
    val viewportFraction = (visibleItems.size.toFloat() / totalItems.toFloat()).coerceIn(0.08f, 1f)

    BoxWithConstraints(
        modifier = modifier
            .width(4.dp)
            .fillMaxSize()
    ) {
        val trackHeight = maxHeight
        val thumbHeight = trackHeight * viewportFraction
        val maxOffset = trackHeight - thumbHeight
        val thumbOffset = maxOffset * progress.coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = thumbOffset)
                .height(thumbHeight)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                .alpha(0.8f)
                .background(Color.Gray.copy(alpha = 0.65f))
        )
    }
}

private fun cleanupStaleDownloadNotifications(
    dm: DownloadManager,
    completedId: Long,
    fileName: String
) {
    val query = DownloadManager.Query().setFilterByStatus(
        DownloadManager.STATUS_PENDING or
            DownloadManager.STATUS_PAUSED or
            DownloadManager.STATUS_RUNNING
    )
    val staleIds = mutableListOf<Long>()
    dm.query(query).use { cursor ->
        val idIndex = cursor.getColumnIndex(DownloadManager.COLUMN_ID)
        val titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
        if (idIndex == -1 || titleIndex == -1) return
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idIndex)
            if (id == completedId) continue
            val title = cursor.getString(titleIndex).orEmpty()
            if (title == fileName) {
                staleIds += id
            }
        }
    }
    if (staleIds.isNotEmpty()) {
        dm.remove(*staleIds.toLongArray())
    }
}
