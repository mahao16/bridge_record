package com.record.bridge.ui

import android.content.ContentValues
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.record.bridge.domain.DefectCatalog
import com.record.bridge.photo.ProjectPhotoNumbering
import com.record.bridge.vm.AddRecordFormState
import com.record.bridge.vm.AddRecordViewModel
import kotlinx.coroutines.launch
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun sanitizeForFileName(raw: String): String {
    val s = raw.trim()
    if (s.isEmpty()) return "UNKNOWN"
    return s.map { ch ->
        if (ch.isLetterOrDigit() || ch == '_' || ch == '-') ch else '-'
    }.joinToString("")
}

private fun buildPhotoDisplayName(componentNo: String, now: Date = Date()): String {
    val comp = sanitizeForFileName(componentNo)
    val fmt = SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA)
    val ts = fmt.format(now)
    return "${comp}_${ts}"
}

private fun createImageUri(context: Context, displayName: String, projectId: Long): Uri? {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/BridgeDefects/project_$projectId")
        }
    }
    return context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecordScreen(
    vm: AddRecordViewModel,
    onBack: () -> Unit,
    title: String = "新增记录",
    submitLabel: String = "保存记录",
    showSaveAndContinue: Boolean = true
) {
    val state by vm.state.collectAsState()
    val compCodePrefixes by vm.compCodePrefixes.collectAsState()
    val defectTypeOptions by vm.defectTypePresets.collectAsState()
    val longRefOptions by vm.locationLongRefOptions.collectAsState()
    val transRefOptions by vm.locationTransRefOptions.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var pendingSeq by remember { mutableStateOf<Int?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingUri
        val seq = pendingSeq
        if (ok && uri != null) {
            val id = runCatching { ContentUris.parseId(uri) }.getOrNull()
            if (id != null && id > 0) {
                if (seq != null) {
                    ProjectPhotoNumbering.bind(context, vm.projectId, id, seq)
                }
                vm.addPhotoId(id)
            }
        }
        pendingUri = null
        pendingSeq = null
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        AddRecordForm(
            modifier = Modifier.padding(padding).padding(16.dp),
            state = state,
            compCodePrefixes = compCodePrefixes,
            defectTypeOptions = defectTypeOptions,
            longRefOptions = longRefOptions,
            transRefOptions = transRefOptions,
            onCompPrefixChange = vm::onCompPrefixChange,
            onCompNumChange = vm::onCompNumChange,
            onCompNumStep = vm::stepCompNum,
            onDefectTypeChange = vm::onSelectDefectType,
            onLocationChange = vm::onLocationChange,
            onMetricChange = vm::onMetricChange,
            onQuantitativeDescManualChange = vm::onQuantitativeDescManualChange,
            onSaveDefectTypePreset = vm::saveDefectTypePreset,
            onTakePhoto = {
                val seq = ProjectPhotoNumbering.reserveNext(context, vm.projectId)
                if (seq == null) {
                    scope.launch { snackbarHostState.showSnackbar("照片编号已达 9999，请新建项目或清理编号") }
                    return@AddRecordForm
                }
                val displayName = buildPhotoDisplayName(state.componentNo)
                val uri = createImageUri(context, displayName, vm.projectId)
                if (uri != null) {
                    pendingUri = uri
                    pendingSeq = seq
                    takePicture.launch(uri)
                }
            },
            onRemovePhoto = vm::removePhotoId,
            submitLabel = submitLabel,
            showSaveAndContinue = showSaveAndContinue,
            onSubmit = { vm.submit(onSuccess = onBack) },
            onSubmitAndContinue = {
                vm.submit(
                    onSuccess = {
                        vm.prepareNextAfterSave()
                        scope.launch {
                            snackbarHostState.showSnackbar("保存成功！请继续记录。")
                        }
                    }
                )
            }
        )
    }
}

@Composable
private fun AddRecordForm(
    modifier: Modifier,
    state: AddRecordFormState,
    compCodePrefixes: List<String>,
    defectTypeOptions: List<String>,
    longRefOptions: List<String>,
    transRefOptions: List<String>,
    onCompPrefixChange: (String) -> Unit,
    onCompNumChange: (index: Int, value: String) -> Unit,
    onCompNumStep: (index: Int, delta: Int) -> Unit,
    onDefectTypeChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onMetricChange: (count: String?, lengthL: String?, widthWmm: String?, widthWm: String?, depthH: String?) -> Unit,
    onQuantitativeDescManualChange: (String) -> Unit,
    onSaveDefectTypePreset: (String) -> Unit,
    onTakePhoto: () -> Unit,
    onRemovePhoto: (Long) -> Unit,
    submitLabel: String,
    showSaveAndContinue: Boolean,
    onSubmit: () -> Unit,
    onSubmitAndContinue: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var previewPhotoId by remember { mutableStateOf<Long?>(null) }
    Column(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                })
            }
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CompCodeInput(
            prefix = state.compPrefix,
            numSegments = listOf(state.compNum1, state.compNum2, state.compNum3, state.compNum4),
            prefixOptions = compCodePrefixes,
            onPrefixChange = onCompPrefixChange,
            onNumChange = onCompNumChange,
            onStep = onCompNumStep,
            modifier = Modifier.fillMaxWidth()
        )

        SearchablePresetField(
            label = "病害类型",
            value = state.defectType,
            options = defectTypeOptions,
            onValueChange = onDefectTypeChange,
            onSelect = onDefectTypeChange,
            onSavePreset = onSaveDefectTypePreset
        )

        PositionRecordEditor(
            position = state.defectLocation,
            onPositionChange = onLocationChange,
            longRefOptions = longRefOptions,
            transRefOptions = transRefOptions,
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("照片")
            IconButton(onClick = onTakePhoto, enabled = !state.isSubmitting) {
                Icon(imageVector = Icons.Filled.PhotoCamera, contentDescription = "拍照")
            }
        }

        Text(
            text = if (state.photoIds.isEmpty()) "未关联照片" else "已关联照片编号：${state.photoIds.joinToString(", ")}",
            style = MaterialTheme.typography.bodyMedium
        )

        if (state.photoIds.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items = state.photoIds, key = { it }) { id ->
                    val bmp = rememberThumbnailBitmap(context, id)
                    if (bmp != null) {
                        IconButton(onClick = { previewPhotoId = id }) {
                            Image(bitmap = bmp.asImageBitmap(), contentDescription = id.toString())
                        }
                    } else {
                        OutlinedTextField(
                            value = id.toString(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("照片") }
                        )
                    }
                }
            }
        }

        val pid = previewPhotoId
        if (pid != null) {
            val full = rememberFullBitmap(context, pid)
            AlertDialog(
                onDismissRequest = { previewPhotoId = null },
                confirmButton = {
                    TextButton(onClick = { previewPhotoId = null }) { Text("关闭") }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            onRemovePhoto(pid)
                            previewPhotoId = null
                        }
                    ) { Text("移除") }
                },
                title = { Text("照片 $pid") },
                text = {
                    if (full != null) {
                        Image(bitmap = full.asImageBitmap(), contentDescription = pid.toString())
                    } else {
                        Text("无法加载照片")
                    }
                }
            )
        }

        HorizontalDivider()

        val isCrackNonMesh = DefectCatalog.isCrackNonMesh(state.defectType)
        val isMeshCrack = DefectCatalog.isMeshCrack(state.defectType)
        val needsArea = DefectCatalog.needsAreaCalculator(state.defectType)

        if (isCrackNonMesh) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.count,
                onValueChange = { onMetricChange(it, null, null, null, null) },
                label = { Text("n(条)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = state.lengthL,
                    onValueChange = { onMetricChange(null, it, null, null, null) },
                    label = { Text("长度L(m)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = state.widthWmm,
                    onValueChange = { onMetricChange(null, null, it, null, null) },
                    label = { Text("宽度W(mm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.depthH,
                onValueChange = { onMetricChange(null, null, null, null, it) },
                label = { Text("深度H(mm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        } else if (isMeshCrack) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = state.count,
                    onValueChange = { onMetricChange(it, null, null, null, null) },
                    label = { Text("n(处)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = state.widthWmm,
                    onValueChange = { onMetricChange(null, null, it, null, null) },
                    label = { Text("宽度W(mm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.depthH,
                onValueChange = { onMetricChange(null, null, null, null, it) },
                label = { Text("深度H(mm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = state.lengthL,
                    onValueChange = { onMetricChange(null, it, null, null, null) },
                    label = { Text("面积长度L(m)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = state.widthWm,
                    onValueChange = { onMetricChange(null, null, null, it, null) },
                    label = { Text("面积宽度W(m)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        } else if (needsArea) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.count,
                onValueChange = { onMetricChange(it, null, null, null, null) },
                label = { Text("n(处)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = state.lengthL,
                    onValueChange = { onMetricChange(null, it, null, null, null) },
                    label = { Text("长度L(m)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = state.widthWm,
                    onValueChange = { onMetricChange(null, null, null, it, null) },
                    label = { Text("宽度W(m)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.quantitativeDesc,
            onValueChange = onQuantitativeDescManualChange,
            label = { Text("病害定量描述") },
            readOnly = false
        )

        if (state.submitError.isNotBlank()) {
            Text(text = state.submitError)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (showSaveAndContinue) {
                Button(modifier = Modifier.weight(1f), onClick = onSubmitAndContinue, enabled = !state.isSubmitting) {
                    Text("保存并继续")
                }
                Button(modifier = Modifier.weight(1f), onClick = onSubmit, enabled = !state.isSubmitting) {
                    Text("保存返回")
                }
            } else {
                Button(modifier = Modifier.fillMaxWidth(), onClick = onSubmit, enabled = !state.isSubmitting) {
                    Text(submitLabel)
                }
            }
        }
    }
}

private fun imageUriFromId(id: Long): Uri =
    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

@Composable
private fun rememberThumbnailBitmap(context: Context, id: Long): android.graphics.Bitmap? {
    return remember(id) {
        val uri = imageUriFromId(id)
        val input: InputStream? = runCatching { context.contentResolver.openInputStream(uri) }.getOrNull()
        input.use {
            val bytes = it?.readBytes() ?: return@remember null
            android.graphics.BitmapFactory.Options().run {
                inJustDecodeBounds = true
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, this)
                val sample = maxOf(1, minOf(outWidth / 256, outHeight / 256))
                inJustDecodeBounds = false
                inSampleSize = sample
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, this)
            }
        }
    }
}

@Composable
private fun rememberFullBitmap(context: Context, id: Long): android.graphics.Bitmap? {
    return remember(id) {
        val uri = imageUriFromId(id)
        val input: InputStream? = runCatching { context.contentResolver.openInputStream(uri) }.getOrNull()
        input.use {
            val bytes = it?.readBytes() ?: return@remember null
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SearchablePresetField(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    onSelect: (String) -> Unit,
    onSavePreset: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var lastSelected by remember { mutableStateOf(value) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val query = value
    val filtered = remember(query, options, expanded, lastSelected) {
        if (!expanded) return@remember emptyList()
        val q = query.trim()
        if (q.isEmpty()) return@remember options
        if (q == lastSelected.trim()) return@remember options
        options.filter { it.contains(q, ignoreCase = true) }
    }
    val canSave = query.trim().isNotEmpty() && options.none { it == query.trim() }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {}
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .focusRequester(focusRequester),
            value = value,
            onValueChange = {
                expanded = true
                onValueChange(it)
            },
            label = { Text(label) },
            trailingIcon = {
                Row {
                    if (value.isNotBlank()) {
                        IconButton(
                            onClick = {
                                lastSelected = ""
                                expanded = false
                                onValueChange("")
                                focusRequester.requestFocus()
                            }
                        ) {
                            Icon(imageVector = Icons.Filled.Clear, contentDescription = "清除")
                        }
                    }
                    IconButton(onClick = {
                        expanded = !expanded
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                    }) {
                        Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = "展开")
                    }
                }
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = false)
        ) {
            if (canSave) {
                DropdownMenuItem(
                    text = { Text("保存为预设：${query.trim()}") },
                    onClick = {
                        onSavePreset(query.trim())
                        expanded = false
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                    }
                )
            }
            filtered.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        expanded = false
                        lastSelected = opt
                        onSelect(opt)
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                    }
                )
            }
        }
    }
}


