package com.record.bridge.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.record.bridge.data.DictionaryCategory
import com.record.bridge.data.DictionaryEntity
import com.record.bridge.vm.DictionarySettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionarySettingsScreen(
    vm: DictionarySettingsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val tabs = listOf(
        "构件编号" to DictionaryCategory.COMPONENT,
        "病害类型" to DictionaryCategory.DEFECT_TYPE,
        "纵向位置参照点" to DictionaryCategory.LOCATION_LONG_REF,
        "横向位置参照点" to DictionaryCategory.LOCATION_TRANS_REF
    )

    var selectedTab by remember { mutableStateOf(0) }
    val category = tabs[selectedTab].second
    val component by vm.component.collectAsState()
    val defectType by vm.defectType.collectAsState()
    val longRef by vm.longRef.collectAsState()
    val transRef by vm.transRef.collectAsState()
    val importState by vm.importState.collectAsState()
    val entries = when (category) {
        DictionaryCategory.COMPONENT -> component
        DictionaryCategory.DEFECT_TYPE -> defectType
        DictionaryCategory.LOCATION_LONG_REF -> longRef
        else -> transRef
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            vm.importFromExcel(context, uri)
        }
    }
    val templateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        if (uri != null) {
            vm.downloadTemplate(context, uri)
        }
    }

    var showDialog by remember { mutableStateOf(false) }
    var newLabel by remember { mutableStateOf("") }
    var newRemark by remember { mutableStateOf("") }
    var newActive by remember { mutableStateOf(true) }
    LaunchedEffect(importState.successMessage) {
        if (importState.successMessage.isNotBlank()) {
            Toast.makeText(context, importState.successMessage, Toast.LENGTH_LONG).show()
            vm.clearFeedback()
        }
    }
    if (importState.errorMessage.isNotBlank()) {
        AlertDialog(
            onDismissRequest = vm::clearFeedback,
            confirmButton = {
                TextButton(onClick = vm::clearFeedback) {
                    Text("确定")
                }
            },
            title = { Text("导入失败") },
            text = { Text(importState.errorMessage) }
        )
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.add(category, newLabel, newRemark, newActive)
                        newLabel = ""
                        newRemark = ""
                        newActive = true
                        showDialog = false
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        newLabel = ""
                        newRemark = ""
                        newActive = true
                        showDialog = false
                    }
                ) {
                    Text("取消")
                }
            },
            title = { Text("新增词条") },
            text = {
                androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = newLabel,
                        onValueChange = { newLabel = it },
                        label = { Text("词条") }
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = newRemark,
                        onValueChange = { newRemark = it },
                        label = { Text("备注（可选）") }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = newActive,
                            onCheckedChange = { newActive = it }
                        )
                        Text(text = "添加后立即激活")
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("词库管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "新增词条")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                modifier = Modifier.weight(1f),
                                enabled = !importState.isImporting,
                                onClick = { importLauncher.launch("*/*") }
                            ) {
                                Text("导入 Excel 词库")
                            }
                            Button(
                                modifier = Modifier.weight(1f),
                                enabled = !importState.isImporting,
                                onClick = { templateLauncher.launch("dictionary_template.xlsx") }
                            ) {
                                Text("下载模板")
                            }
                        }
                    }
                }
                item {
                    TabRow(selectedTabIndex = selectedTab) {
                        tabs.forEachIndexed { index, t ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(t.first) }
                            )
                        }
                    }
                }
                items(items = entries, key = { it.id }) { e ->
                    DictionaryRow(
                        entity = e,
                        onToggle = { vm.toggleActive(e.id, !e.isActive) },
                        onDelete = { vm.delete(e) }
                    )
                }
            }
            if (importState.isImporting) {
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

@Composable
private fun DictionaryRow(
    entity: DictionaryEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(checked = entity.isActive, onCheckedChange = { onToggle() })
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(text = entity.label)
                if (entity.remark.isNotBlank()) {
                    Text(text = entity.remark)
                }
                if (entity.isDefault) {
                    Text(text = "默认")
                }
            }
            if (!entity.isDefault) {
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "删除")
                }
            }
        }
    }
}

