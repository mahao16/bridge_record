package com.record.bridge.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.record.bridge.data.SiteLogType
import com.record.bridge.vm.SiteLogDraftUi
import com.record.bridge.vm.SiteLogRecordUi
import com.record.bridge.vm.SiteLogViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SiteLogScreen(
    vm: SiteLogViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state by vm.uiState.collectAsState()
    val listState = rememberLazyListState()
    var pendingPath by remember { mutableStateOf<String?>(null) }
    var showDraftDialog by remember { mutableStateOf(false) }
    var draftDescInput by remember { mutableStateOf("") }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val path = pendingPath
        if (ok && path != null) {
            vm.takePhoto(path)
        } else if (path != null) {
            runCatching { File(path).delete() }
        }
        pendingPath = null
    }

    LaunchedEffect(state.scrollToLatestToken, state.records.size, state.selectedTab) {
        if (state.records.isNotEmpty()) {
            val rowIndex = (state.records.size - 1) / 2 + 1
            listState.animateScrollToItem(rowIndex)
        }
    }

    if (showDraftDialog) {
        AlertDialog(
            onDismissRequest = { showDraftDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.beginDraft(draftDescInput)
                        showDraftDialog = false
                        draftDescInput = ""
                        val prepared = prepareSiteLogPhoto(context, state.selectedTab)
                        if (prepared != null) {
                            pendingPath = prepared.path
                            takePicture.launch(prepared.uri)
                        }
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDraftDialog = false }) { Text("取消") }
            },
            title = { Text("输入工作内容") },
            text = {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = draftDescInput,
                    onValueChange = { draftDescInput = it },
                    label = { Text("内容描述") }
                )
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("现场日志") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.exportSelectedToWord(context) }) {
                        Icon(imageVector = Icons.Filled.Share, contentDescription = "导出Word")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (state.draft == null) {
                        showDraftDialog = true
                    } else {
                        val prepared = prepareSiteLogPhoto(context, state.selectedTab)
                        if (prepared != null) {
                            pendingPath = prepared.path
                            takePicture.launch(prepared.uri)
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = if (state.draft == null) Icons.Filled.CameraAlt else Icons.Filled.Done,
                    contentDescription = if (state.draft == null) "拍照" else "继续拍照"
                )
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TabRow(selectedTabIndex = if (state.selectedTab == SiteLogType.WORK) 0 else 1) {
                    Tab(
                        selected = state.selectedTab == SiteLogType.WORK,
                        onClick = { vm.selectTab(SiteLogType.WORK) },
                        text = { Text("工作记录") }
                    )
                    Tab(
                        selected = state.selectedTab == SiteLogType.SAFETY,
                        onClick = { vm.selectTab(SiteLogType.SAFETY) },
                        text = { Text("安全记录") }
                    )
                }
            }

            if (state.draft != null) {
                item {
                    DraftSection(
                        draft = state.draft!!,
                        keywords = state.keywords,
                        onDescChange = vm::updateDraftDescription,
                        onBindKeyword = vm::appendDraftKeyword,
                        onRemovePhoto = vm::removeDraftPhoto,
                        onContinueShot = {
                            val prepared = prepareSiteLogPhoto(context, state.selectedTab)
                            if (prepared != null) {
                                pendingPath = prepared.path
                                takePicture.launch(prepared.uri)
                            }
                        },
                        onFinish = vm::saveDraft,
                        onCancel = vm::cancelDraft
                    )
                }
            }

            val rows = state.records.chunked(2)
            items(items = rows, key = { row -> row.first().recordId }) { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SiteLogItem(
                        modifier = Modifier.weight(1f),
                        record = row[0],
                        keywords = state.keywords,
                        onDelete = { vm.deleteLog(row[0].recordId) },
                        onUpdateDescription = { vm.updateDescription(row[0].recordId, it) },
                        onBindKeyword = { vm.bindKeyword(row[0], it) },
                        selected = state.selectedRecordIds.contains(row[0].recordId),
                        onToggleSelected = { vm.toggleRecordSelected(row[0].recordId) },
                        onDeletePhoto = vm::deletePhoto
                    )
                    if (row.size > 1) {
                        SiteLogItem(
                            modifier = Modifier.weight(1f),
                            record = row[1],
                            keywords = state.keywords,
                            onDelete = { vm.deleteLog(row[1].recordId) },
                            onUpdateDescription = { vm.updateDescription(row[1].recordId, it) },
                            onBindKeyword = { vm.bindKeyword(row[1], it) },
                            selected = state.selectedRecordIds.contains(row[1].recordId),
                            onToggleSelected = { vm.toggleRecordSelected(row[1].recordId) },
                            onDeletePhoto = vm::deletePhoto
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SiteLogItem(
    modifier: Modifier,
    record: SiteLogRecordUi,
    keywords: List<String>,
    onDelete: () -> Unit,
    onUpdateDescription: (String) -> Unit,
    onBindKeyword: (String) -> Unit,
    selected: Boolean,
    onToggleSelected: () -> Unit,
    onDeletePhoto: (Long) -> Unit
) {
    var desc by remember(record.recordId, record.description) { mutableStateOf(record.description) }
    var showPhotosDialog by remember(record.recordId) { mutableStateOf(false) }

    if (showPhotosDialog) {
        AlertDialog(
            onDismissRequest = { showPhotosDialog = false },
            confirmButton = {
                TextButton(onClick = { showPhotosDialog = false }) {
                    Text("关闭")
                }
            },
            title = { Text("编辑描述与图片") },
            text = {
                androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = desc,
                        onValueChange = {
                            desc = it
                            onUpdateDescription(it)
                        },
                        label = { Text("描述") }
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(record.photos, key = { it.id }) { p ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = File(p.path),
                                    contentDescription = null,
                                    modifier = Modifier.weight(1f).height(120.dp),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(onClick = { onDeletePhoto(p.id) }) {
                                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "删除图片")
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    androidx.compose.material3.Card(modifier = modifier) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clickable { showPhotosDialog = true }
            ) {
                val front = record.photos.lastOrNull()
                val backCount = kotlin.math.min(2, (record.photos.size - 1).coerceAtLeast(0))
                for (i in backCount downTo 1) {
                    val back = record.photos.getOrNull(record.photos.size - 1 - i) ?: continue
                    AsyncImage(
                        model = File(back.path),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(124.dp)
                            .offset(x = (i * 6).dp, y = (i * 6).dp)
                            .alpha(0.65f),
                        contentScale = ContentScale.Crop
                    )
                }
                if (front != null) {
                    AsyncImage(
                        model = File(front.path),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(124.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Checkbox(checked = selected, onCheckedChange = { onToggleSelected() })
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "删除")
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                keywords.forEach { kw ->
                    FilterChip(
                        selected = desc.contains(kw),
                        onClick = { onBindKeyword(kw) },
                        label = { Text(kw) }
                    )
                }
            }

            Text(
                text = if (desc.isBlank()) "点击描述查看并管理图片" else desc,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPhotosDialog = true },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DraftSection(
    draft: SiteLogDraftUi,
    keywords: List<String>,
    onDescChange: (String) -> Unit,
    onBindKeyword: (String) -> Unit,
    onRemovePhoto: (String) -> Unit,
    onContinueShot: () -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit
) {
    androidx.compose.material3.Card(modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("批量拍照中", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                draft.photoPaths.forEach { path ->
                    Box {
                        AsyncImage(
                            model = File(path),
                            contentDescription = null,
                            modifier = Modifier.width(96.dp).height(96.dp),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(modifier = Modifier.align(Alignment.TopEnd), onClick = { onRemovePhoto(path) }) {
                            Icon(imageVector = Icons.Filled.Delete, contentDescription = "删除")
                        }
                    }
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                keywords.forEach { kw ->
                    FilterChip(
                        selected = draft.description.contains(kw),
                        onClick = { onBindKeyword(kw) },
                        label = { Text(kw) }
                    )
                }
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = draft.description,
                onValueChange = onDescChange,
                label = { Text("工作内容") }
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(modifier = Modifier.weight(1f), onClick = onContinueShot) { Text("继续拍照") }
                Button(modifier = Modifier.weight(1f), onClick = onFinish) { Text("完成") }
                Button(modifier = Modifier.weight(1f), onClick = onCancel) { Text("取消") }
            }
        }
    }
}

private data class PreparedSiteLogPhoto(val uri: Uri, val path: String)

private fun prepareSiteLogPhoto(context: Context, logType: String): PreparedSiteLogPhoto? {
    val dir = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "site_logs")
    if (!dir.exists()) dir.mkdirs()
    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.CHINA).format(Date())
    val prefix = if (logType == SiteLogType.WORK) "work" else "safety"
    val file = File(dir, "${prefix}_$stamp.jpg")
    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    return PreparedSiteLogPhoto(uri = uri, path = file.absolutePath)
}

