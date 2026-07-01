package com.record.bridge.vm

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.record.bridge.data.BridgeDefectRecordDao
import com.record.bridge.data.BridgeDefectRecordEntity
import com.record.bridge.data.ProjectDao
import com.record.bridge.export.ExcelExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ExportUiState(
    val projectName: String = "",
    val records: List<BridgeDefectRecordEntity> = emptyList(),
    val selected: Set<String> = emptySet(),
    val error: String = ""
)

class ExportViewModel(
    private val projectId: Long,
    private val projectDao: ProjectDao,
    private val recordDao: BridgeDefectRecordDao
) : ViewModel() {

    private val _state = MutableStateFlow(ExportUiState())
    val state: StateFlow<ExportUiState> = _state

    fun load() {
        viewModelScope.launch {
            val (projectName, records) = withContext(Dispatchers.IO) {
                val name = projectDao.getOne(projectId)?.name ?: "项目$projectId"
                val list = recordDao.listByProject(projectId)
                name to list
            }
            _state.update {
                it.copy(
                    projectName = projectName,
                    records = records,
                    selected = records.map { r -> keyOf(r) }.toSet(),
                    error = ""
                )
            }
        }
    }

    fun toggle(record: BridgeDefectRecordEntity) {
        val k = keyOf(record)
        _state.update { s ->
            val next = if (s.selected.contains(k)) s.selected - k else s.selected + k
            s.copy(selected = next)
        }
    }

    fun selectAll() {
        _state.update { s -> s.copy(selected = s.records.map { keyOf(it) }.toSet()) }
    }

    fun clearAll() {
        _state.update { it.copy(selected = emptySet()) }
    }

    fun exportAndShare(context: Context) {
        val s = _state.value
        val selectedRecords = s.records.filter { s.selected.contains(keyOf(it)) }
        if (selectedRecords.isEmpty()) {
            _state.update { it.copy(error = "请选择至少一条记录") }
            return
        }

        viewModelScope.launch {
            runCatching {
                val uri = withContext(Dispatchers.IO) {
                    ExcelExporter.exportProjectRecords(
                        context = context,
                        projectId = projectId,
                        projectName = s.projectName,
                        areaName = "病害记录",
                        records = selectedRecords
                    )
                }
                withContext(Dispatchers.Main) {
                    shareFile(context, uri, "${s.projectName}_病害记录")
                }
            }.onFailure { e ->
                _state.update { it.copy(error = e.toString()) }
            }
        }
    }
}

private fun keyOf(r: BridgeDefectRecordEntity): String =
    "${r.componentNo}|${r.defectType}|${r.defectLocation}"

private fun shareFile(context: Context, uri: android.net.Uri, title: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

