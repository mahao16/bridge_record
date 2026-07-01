package com.record.bridge.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.record.bridge.data.BridgeDefectRecordEntity
import com.record.bridge.vm.ExportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    vm: ExportViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) {
        vm.load()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导出 Excel") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(text = "项目：${state.projectName}", style = MaterialTheme.typography.titleMedium)
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(modifier = Modifier.weight(1f), onClick = vm::selectAll) { Text("全选") }
                    Button(modifier = Modifier.weight(1f), onClick = vm::clearAll) { Text("清空") }
                    Button(modifier = Modifier.weight(1f), onClick = { vm.exportAndShare(context) }) { Text("导出分享") }
                }
            }
            if (state.error.isNotBlank()) {
                item { Text(text = state.error) }
            }
            items(
                items = state.records,
                key = { keyOf(it) }
            ) { r ->
                RecordSelectItem(
                    record = r,
                    checked = state.selected.contains(keyOf(r)),
                    onToggle = { vm.toggle(r) }
                )
            }
        }
    }
}

@Composable
private fun RecordSelectItem(
    record: BridgeDefectRecordEntity,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Card(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
            androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "${record.componentNo} | ${record.defectType}", style = MaterialTheme.typography.bodyMedium)
                Text(text = record.defectLocation, style = MaterialTheme.typography.bodySmall)
                Text(text = "照片：${record.photoIds}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun keyOf(r: BridgeDefectRecordEntity): String =
    "${r.componentNo}|${r.defectType}|${r.defectLocation}"

