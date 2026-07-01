package com.record.bridge.vm

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.record.bridge.data.BridgeDefectRecordDao
import com.record.bridge.data.BridgeDefectRecordEntity
import com.record.bridge.export.ExcelExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecordsViewModel(
    private val projectId: Long,
    private val dao: BridgeDefectRecordDao
) : ViewModel() {
    private val selectedKeys = MutableStateFlow<Set<String>>(emptySet())

    val records: StateFlow<List<BridgeDefectRecordEntity>> =
        dao.observeByProject(projectId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selected: StateFlow<Set<String>> =
        combine(records, selectedKeys) { rows, keys ->
            keys.intersect(rows.map { keyOf(it) }.toSet())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun toggleSelected(record: BridgeDefectRecordEntity) {
        val key = keyOf(record)
        selectedKeys.update { current ->
            if (current.contains(key)) current - key else current + key
        }
    }

    fun selectAll() {
        selectedKeys.value = records.value.map { keyOf(it) }.toSet()
    }

    fun invertSelection() {
        val all = records.value.map { keyOf(it) }.toSet()
        val current = selected.value
        selectedKeys.value = all - current
    }

    fun clearSelection() {
        selectedKeys.value = emptySet()
    }

    fun exportAll(context: Context, projectName: String) {
        exportRecords(context, projectName, records.value)
    }

    fun exportSelected(context: Context, projectName: String) {
        val selectedRows = records.value.filter { selected.value.contains(keyOf(it)) }
        if (selectedRows.isEmpty()) {
            Toast.makeText(context, "请先选择记录", Toast.LENGTH_SHORT).show()
            return
        }
        exportRecords(context, projectName, selectedRows)
    }

    fun deleteSelected() {
        val selectedRows = records.value.filter { selected.value.contains(keyOf(it)) }
        if (selectedRows.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            selectedRows.forEach { row ->
                dao.deleteOne(projectId, row.componentNo, row.defectType, row.defectLocation)
            }
            selectedKeys.value = emptySet()
        }
    }

    private fun exportRecords(context: Context, projectName: String, rows: List<BridgeDefectRecordEntity>) {
        if (rows.isEmpty()) {
            Toast.makeText(context, "当前无可导出记录", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            runCatching {
                val uri = withContext(Dispatchers.IO) {
                    ExcelExporter.exportProjectRecords(
                        context = context,
                        projectId = projectId,
                        projectName = projectName,
                        areaName = "病害记录",
                        records = rows
                    )
                }
                withContext(Dispatchers.Main) {
                    val title = "${projectName}_病害记录"
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    selectedKeys.value = emptySet()
                }
            }.onFailure {
                Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private fun keyOf(r: BridgeDefectRecordEntity): String =
    "${r.projectId}|${r.componentNo}|${r.defectType}|${r.defectLocation}"

