package com.record.bridge.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.offset
import com.record.bridge.data.BridgeDefectRecordEntity
import com.record.bridge.data.SiteLogType
import com.record.bridge.vm.ProjectDetailViewModel
import com.record.bridge.vm.RecordsViewModel
import com.record.bridge.vm.SiteLogRecordUi
import com.record.bridge.vm.SiteLogViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private enum class ProjectDetailTab {
    DISEASE,
    WORK,
    SAFETY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectName: String,
    detailVm: ProjectDetailViewModel,
    recordsVm: RecordsViewModel,
    siteLogVm: SiteLogViewModel,
    onBack: () -> Unit,
    onAddDisease: () -> Unit,
    onRecordClick: (BridgeDefectRecordEntity) -> Unit
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(ProjectDetailTab.DISEASE) }
    val diseaseSelected by recordsVm.selected.collectAsState()
    val siteLogState by siteLogVm.uiState.collectAsState()
    val selectionActive = when (currentTab) {
        ProjectDetailTab.DISEASE -> diseaseSelected.isNotEmpty()
        ProjectDetailTab.WORK, ProjectDetailTab.SAFETY -> siteLogState.selectedRecordIds.isNotEmpty()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(projectName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (selectionActive) {
                        TextButton(
                            onClick = {
                                if (currentTab == ProjectDetailTab.DISEASE) {
                                    recordsVm.selectAll()
                                } else {
                                    siteLogVm.selectAllCurrentRecords()
                                }
                            }
                        ) {
                            Text("全选")
                        }
                    } else {
                        IconButton(
                            onClick = {
                                if (currentTab == ProjectDetailTab.DISEASE) {
                                    recordsVm.exportAll(context, projectName)
                                } else {
                                siteLogVm.exportCurrentTabToWord(context, projectName)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "分享当前栏目"
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == ProjectDetailTab.DISEASE,
                    onClick = { currentTab = ProjectDetailTab.DISEASE },
                    icon = { Text("🔍") },
                    label = { Text("病害记录") }
                )
                NavigationBarItem(
                    selected = currentTab == ProjectDetailTab.WORK,
                    onClick = { currentTab = ProjectDetailTab.WORK },
                    icon = { Text("📷") },
                    label = { Text("工作记录") }
                )
                NavigationBarItem(
                    selected = currentTab == ProjectDetailTab.SAFETY,
                    onClick = { currentTab = ProjectDetailTab.SAFETY },
                    icon = { Text("🦺") },
                    label = { Text("安全记录") }
                )
            }
        }
    ) { padding ->
        Crossfade(
            targetState = currentTab,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            label = "project-detail-crossfade"
        ) { tab ->
            when (tab) {
                ProjectDetailTab.DISEASE -> DiseaseTabContent(
                    vm = recordsVm,
                    projectName = projectName,
                    onAddClick = onAddDisease,
                    onRecordClick = onRecordClick
                )
                ProjectDetailTab.WORK -> ProjectLogTabContent(
                    vm = siteLogVm,
                    projectName = projectName,
                    logType = SiteLogType.WORK
                )
                ProjectDetailTab.SAFETY -> ProjectLogTabContent(
                    vm = siteLogVm,
                    projectName = projectName,
                    logType = SiteLogType.SAFETY
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiseaseTabContent(
    vm: RecordsViewModel,
    projectName: String,
    onAddClick: () -> Unit,
    onRecordClick: (BridgeDefectRecordEntity) -> Unit
) {
    val context = LocalContext.current
    val records by vm.records.collectAsState()
    val selected by vm.selected.collectAsState()
    var showDeleteSelectedConfirm by remember { mutableStateOf(false) }

    if (showDeleteSelectedConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedConfirm = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteSelected()
                        showDeleteSelectedConfirm = false
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedConfirm = false }) {
                    Text("取消")
                }
            },
            title = { Text("删除确认") },
            text = { Text("已选择 ${selected.size} 条病害记录，确认删除吗？") }
        )
    }

    Scaffold(
        floatingActionButton = {
            if (selected.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FloatingActionButton(onClick = { vm.exportSelected(context, projectName) }) {
                        Icon(imageVector = Icons.Filled.Share, contentDescription = "分享已选")
                    }
                    FloatingActionButton(onClick = { showDeleteSelectedConfirm = true }) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "删除已选")
                    }
                }
            } else {
                FloatingActionButton(onClick = onAddClick) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "新增病害")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = records,
                key = { r -> "${r.projectId}|${r.componentNo}|${r.defectType}|${r.defectLocation}" }
            ) { r ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onRecordClick(r) }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = r.componentNo, style = MaterialTheme.typography.titleMedium)
                            Checkbox(
                                checked = selected.contains("${r.projectId}|${r.componentNo}|${r.defectType}|${r.defectLocation}"),
                                onCheckedChange = { vm.toggleSelected(r) }
                            )
                        }
                        Text(text = "类型：${r.defectType}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "位置：${r.defectLocation}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "定量：${r.quantitativeDesc}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectLogTabContent(
    vm: SiteLogViewModel,
    projectName: String,
    logType: String
) {
    val context = LocalContext.current
    val state by vm.uiState.collectAsState()
    var pendingPath by remember(logType) { mutableStateOf<String?>(null) }
    var showDraftDialog by remember(logType) { mutableStateOf(false) }
    var showInputSourceDialog by remember(logType) { mutableStateOf(false) }
    var showDeleteSelectedConfirm by remember(logType) { mutableStateOf(false) }
    var draftDescInput by remember(logType) { mutableStateOf("") }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val path = pendingPath
        if (ok && path != null) {
            vm.takePhoto(path)
        } else if (path != null) {
            runCatching { File(path).delete() }
        }
        pendingPath = null
    }
    val pickImages = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        uris.forEach { uri ->
            val importedPath = importProjectLogPhoto(context, uri, logType)
            if (importedPath != null) {
                vm.takePhoto(importedPath)
            }
        }
    }

    LaunchedEffect(logType) {
        vm.selectTab(logType)
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
                        showInputSourceDialog = true
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

    if (showInputSourceDialog) {
        AlertDialog(
            onDismissRequest = { showInputSourceDialog = false },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showInputSourceDialog = false }) {
                    Text("取消")
                }
            },
            title = { Text("选择图片来源") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            showInputSourceDialog = false
                            val prepared = prepareProjectLogPhoto(context, logType)
                            if (prepared != null) {
                                pendingPath = prepared.path
                                takePicture.launch(prepared.uri)
                            }
                        }
                    ) {
                        Text("拍照")
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            showInputSourceDialog = false
                            pickImages.launch("image/*")
                        }
                    ) {
                        Text("从图库选择")
                    }
                }
            }
        )
    }

    if (showDeleteSelectedConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedConfirm = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteSelectedLogs()
                        showDeleteSelectedConfirm = false
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedConfirm = false }) { Text("取消") }
            },
            title = { Text("删除确认") },
            text = { Text("已选择 ${state.selectedRecordIds.size} 条记录，确认删除吗？") }
        )
    }

    Scaffold(
        floatingActionButton = {
            if (state.selectedRecordIds.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FloatingActionButton(onClick = { vm.exportSelectedOnlyToWord(context, projectName) }) {
                        Icon(imageVector = Icons.Filled.Share, contentDescription = "分享已选")
                    }
                    FloatingActionButton(onClick = { showDeleteSelectedConfirm = true }) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "删除已选")
                    }
                }
            } else {
                FloatingActionButton(
                    onClick = {
                        if (state.draft == null) {
                            showDraftDialog = true
                        } else {
                            showInputSourceDialog = true
                        }
                    }
                ) {
                    Text("📷")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            state.draft?.let { d ->
                item {
                    ProjectLogDraftSection(
                        description = d.description,
                        photoPaths = d.photoPaths,
                        keywords = state.keywords,
                        onDescChange = vm::updateDraftDescription,
                        onBindKeyword = vm::appendDraftKeyword,
                        onRemovePhoto = vm::removeDraftPhoto,
                        onContinueShot = {
                            showInputSourceDialog = true
                        },
                        onFinish = vm::saveDraft,
                        onCancel = vm::cancelDraft
                    )
                }
            }
            val rows = state.records.chunked(2)
            items(items = rows, key = { row -> row.first().recordId }) { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProjectLogFlowItem(
                        modifier = Modifier.weight(1f),
                        record = row[0],
                        keywords = state.keywords,
                        selected = state.selectedRecordIds.contains(row[0].recordId),
                        onToggleSelected = { vm.toggleRecordSelected(row[0].recordId) },
                        onUpdateDescription = { vm.updateDescription(row[0].recordId, it) },
                        onBindKeyword = { vm.bindKeyword(row[0], it) },
                        onDeletePhoto = vm::deletePhoto
                    )
                    if (row.size > 1) {
                        ProjectLogFlowItem(
                            modifier = Modifier.weight(1f),
                            record = row[1],
                            keywords = state.keywords,
                            selected = state.selectedRecordIds.contains(row[1].recordId),
                            onToggleSelected = { vm.toggleRecordSelected(row[1].recordId) },
                            onUpdateDescription = { vm.updateDescription(row[1].recordId, it) },
                            onBindKeyword = { vm.bindKeyword(row[1], it) },
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
private fun ProjectLogFlowItem(
    modifier: Modifier,
    record: SiteLogRecordUi,
    keywords: List<String>,
    selected: Boolean,
    onToggleSelected: () -> Unit,
    onUpdateDescription: (String) -> Unit,
    onBindKeyword: (String) -> Unit,
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
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

    Card(modifier = modifier) {
        Column(
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

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Checkbox(checked = selected, onCheckedChange = { onToggleSelected() })
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
                    .clickable { showPhotosDialog = true }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectLogDraftSection(
    description: String,
    photoPaths: List<String>,
    keywords: List<String>,
    onDescChange: (String) -> Unit,
    onBindKeyword: (String) -> Unit,
    onRemovePhoto: (String) -> Unit,
    onContinueShot: () -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("批量拍照中", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                photoPaths.forEach { path ->
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
                        selected = description.contains(kw),
                        onClick = { onBindKeyword(kw) },
                        label = { Text(kw) }
                    )
                }
            }
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = description,
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

private data class PreparedProjectLogPhoto(val uri: Uri, val path: String)

private fun prepareProjectLogPhoto(context: Context, logType: String): PreparedProjectLogPhoto? {
    val dir = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "site_logs")
    if (!dir.exists()) dir.mkdirs()
    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.CHINA).format(Date())
    val prefix = if (logType == SiteLogType.WORK) "work" else "safety"
    val file = File(dir, "${prefix}_$stamp.jpg")
    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    return PreparedProjectLogPhoto(uri = uri, path = file.absolutePath)
}

private fun importProjectLogPhoto(context: Context, sourceUri: Uri, logType: String): String? {
    val dir = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "site_logs")
    if (!dir.exists()) dir.mkdirs()
    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.CHINA).format(Date())
    val prefix = if (logType == SiteLogType.WORK) "work" else "safety"
    val target = File(dir, "${prefix}_import_${stamp}_${UUID.randomUUID().toString().take(8)}.jpg")
    return runCatching {
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        target.absolutePath
    }.getOrNull()
}
