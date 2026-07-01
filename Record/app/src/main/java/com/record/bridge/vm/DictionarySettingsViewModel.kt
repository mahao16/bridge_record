package com.record.bridge.vm

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.record.bridge.data.DictionaryCategory
import com.record.bridge.data.DictionaryDao
import com.record.bridge.data.DictionaryEntity
import com.record.bridge.export.ExcelManager
import com.record.bridge.export.InvalidDictionaryExcelException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DictionaryImportUiState(
    val isImporting: Boolean = false,
    val successMessage: String = "",
    val errorMessage: String = ""
)

class DictionarySettingsViewModel(
    private val dao: DictionaryDao
) : ViewModel() {

    val component: StateFlow<List<DictionaryEntity>> =
        dao.observeEntries(DictionaryCategory.COMPONENT).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val defectType: StateFlow<List<DictionaryEntity>> =
        dao.observeEntries(DictionaryCategory.DEFECT_TYPE).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val longRef: StateFlow<List<DictionaryEntity>> =
        dao.observeEntries(DictionaryCategory.LOCATION_LONG_REF).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val transRef: StateFlow<List<DictionaryEntity>> =
        dao.observeEntries(DictionaryCategory.LOCATION_TRANS_REF).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _importState = MutableStateFlow(DictionaryImportUiState())
    val importState: StateFlow<DictionaryImportUiState> = _importState

    fun add(category: String, label: String, remark: String, isActive: Boolean) {
        val l = label.trim()
        if (l.isEmpty()) return
        val r = remark.trim()
        viewModelScope.launch {
            val id = dao.insert(DictionaryEntity(category = category, label = l, remark = r, isDefault = false, isActive = isActive))
            if (id == -1L) {
                dao.updateActiveByLabel(category, l, isActive)
                if (r.isNotEmpty()) {
                    dao.updateRemarkByLabel(category, l, r)
                }
            }
        }
    }

    fun toggleActive(id: Long, active: Boolean) {
        viewModelScope.launch {
            dao.updateActive(id, active)
        }
    }

    fun delete(entity: DictionaryEntity) {
        if (entity.isDefault) return
        viewModelScope.launch {
            dao.deleteById(entity.id)
        }
    }

    fun importFromExcel(context: Context, uri: Uri) {
        if (_importState.value.isImporting) return
        val manager = ExcelManager(context.applicationContext.contentResolver)
        viewModelScope.launch {
            _importState.update { it.copy(isImporting = true, successMessage = "", errorMessage = "") }
            runCatching {
                withContext(Dispatchers.IO) {
                    val imported = manager.importDictionaryFromExcel(uri)
                    val uniqueImported = imported.distinctBy { entity -> entity.category to entity.label }
                    val duplicateRowsInExcel = imported.size - uniqueImported.size
                    val results = if (uniqueImported.isEmpty()) {
                        emptyList()
                    } else {
                        dao.insertAll(uniqueImported)
                    }
                    val insertedCount = results.count { it != -1L }
                    val duplicateRowsInDatabase = results.count { it == -1L }
                    insertedCount to (duplicateRowsInExcel + duplicateRowsInDatabase)
                }
            }.onSuccess { (insertedCount, duplicateCount) ->
                _importState.value = DictionaryImportUiState(
                    isImporting = false,
                    successMessage = "导入成功！新增 $insertedCount 条词汇，跳过 $duplicateCount 条重复词汇。"
                )
            }.onFailure { throwable ->
                _importState.value = DictionaryImportUiState(
                    isImporting = false,
                    errorMessage = if (throwable is InvalidDictionaryExcelException) {
                        "Excel 格式不符合模板要求"
                    } else {
                        throwable.message ?: "导入失败，请重试"
                    }
                )
            }
        }
    }

    fun downloadTemplate(context: Context, uri: Uri) {
        val manager = ExcelManager(context.applicationContext.contentResolver)
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    manager.writeDictionaryTemplate(uri)
                }
            }.onSuccess {
                _importState.update { it.copy(successMessage = "模板已保存") }
            }.onFailure { throwable ->
                _importState.update { it.copy(errorMessage = throwable.message ?: "模板保存失败，请重试") }
            }
        }
    }

    fun clearFeedback() {
        _importState.update { it.copy(successMessage = "", errorMessage = "") }
    }
}

