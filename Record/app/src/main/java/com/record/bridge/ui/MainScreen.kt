package com.record.bridge.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.record.bridge.data.BridgeDefectRecordEntity
import com.record.bridge.vm.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    vm: MainViewModel,
    onAddClick: () -> Unit,
    onRecordClick: (BridgeDefectRecordEntity) -> Unit = {}
) {
    val records by vm.records.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("桥梁病害记录") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "新增记录")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = records,
                key = { r -> "${r.componentNo}|${r.defectType}|${r.defectLocation}" }
            ) { r ->
                RecordItem(record = r, onClick = { onRecordClick(r) })
            }
        }
    }
}

@Composable
private fun RecordItem(
    record: BridgeDefectRecordEntity,
    onClick: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = record.componentNo, style = MaterialTheme.typography.titleMedium)
                Text(text = record.photoIds, style = MaterialTheme.typography.bodyMedium)
            }
            Text(text = "类型：${record.defectType}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "位置：${record.defectLocation}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "定量：${record.quantitativeDesc}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

