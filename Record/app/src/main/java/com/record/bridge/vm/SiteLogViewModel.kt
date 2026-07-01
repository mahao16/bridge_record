package com.record.bridge.vm

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.record.bridge.data.DictionaryCategory
import com.record.bridge.data.DictionaryDao
import com.record.bridge.data.SiteLogDao
import com.record.bridge.data.SiteLogEntity
import com.record.bridge.data.SiteLogType
import com.record.bridge.export.WordExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class SiteLogPhotoUi(
    val id: Long,
    val path: String
)

data class SiteLogRecordUi(
    val recordId: String,
    val logType: String,
    val description: String,
    val timestamp: Long,
    val photos: List<SiteLogPhotoUi>
)

data class SiteLogDraftUi(
    val description: String = "",
    val photoPaths: List<String> = emptyList()
)

data class SiteLogUiState(
    val selectedTab: String = SiteLogType.WORK,
    val records: List<SiteLogRecordUi> = emptyList(),
    val keywords: List<String> = emptyList(),
    val draft: SiteLogDraftUi? = null,
    val selectedRecordIds: Set<String> = emptySet(),
    val scrollToLatestToken: Long = 0L
)

class SiteLogViewModel(
    private val siteLogDao: SiteLogDao,
    private val dictionaryDao: DictionaryDao,
    private val projectId: Long = 0L
) : ViewModel() {

    private val selectedTab = MutableStateFlow(SiteLogType.WORK)
    private val scrollToken = MutableStateFlow(0L)
    private val draft = MutableStateFlow<SiteLogDraftUi?>(null)
    private val selectedRecordIds = MutableStateFlow<Set<String>>(emptySet())
    private val recordsFlow = selectedTab.flatMapLatest {
        if (projectId > 0) {
            siteLogDao.observeByProjectAndType(projectId, it)
        } else {
            siteLogDao.observeByType(it)
        }
    }.map { rows -> rows.toRecordList() }
    private val keywordsFlow = selectedTab.flatMapLatest { tab ->
        dictionaryDao.observeActiveLabels(
            if (tab == SiteLogType.WORK) DictionaryCategory.SITE_LOG_WORK else DictionaryCategory.SITE_LOG_SAFETY
        )
    }
    private val baseStateFlow = combine(
        selectedTab,
        recordsFlow,
        keywordsFlow,
        draft,
        selectedRecordIds
    ) { tab, records, keywords, d, selected ->
        SiteLogUiState(
            selectedTab = tab,
            records = records,
            keywords = keywords,
            draft = d,
            selectedRecordIds = selected
        )
    }

    val uiState: StateFlow<SiteLogUiState> =
        combine(
            baseStateFlow,
            scrollToken
        ) { base, token ->
            base.copy(scrollToLatestToken = token)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SiteLogUiState())

    fun selectTab(tab: String) {
        selectedTab.value = tab
        selectedRecordIds.value = emptySet()
        cancelDraft()
    }

    fun beginDraft(description: String) {
        val d = description.trim()
        if (d.isEmpty()) return
        draft.value = SiteLogDraftUi(description = d, photoPaths = emptyList())
    }

    fun takePhoto(photoPath: String) {
        val path = photoPath.trim()
        if (path.isEmpty()) return
        val d = draft.value ?: return
        draft.value = d.copy(photoPaths = d.photoPaths + path)
    }

    fun updateDescription(recordId: String, newDesc: String) {
        viewModelScope.launch {
            siteLogDao.updateDescriptionByRecordId(recordId, newDesc.trim())
        }
    }

    fun bindKeyword(record: SiteLogRecordUi, keyword: String) {
        val key = keyword.trim()
        if (key.isEmpty()) return
        val current = record.description.trim()
        val next = if (current.isEmpty()) key else if (current.contains(key)) current else "$current，$key"
        updateDescription(record.recordId, next)
    }

    fun deleteLog(recordId: String) {
        viewModelScope.launch {
            val rows = siteLogDao.listByRecordId(recordId)
            siteLogDao.deleteByRecordId(recordId)
            selectedRecordIds.update { it - recordId }
            rows.forEach { row -> runCatching { File(row.photoPath).delete() } }
        }
    }

    fun deletePhoto(photoId: Long) {
        viewModelScope.launch {
            val row = siteLogDao.getOne(photoId) ?: return@launch
            siteLogDao.deleteById(photoId)
            runCatching { File(row.photoPath).delete() }
            val left = siteLogDao.listByRecordId(row.recordId)
            if (left.isEmpty()) {
                selectedRecordIds.update { it - row.recordId }
            }
        }
    }

    fun toggleRecordSelected(recordId: String) {
        selectedRecordIds.update { set ->
            if (set.contains(recordId)) set - recordId else set + recordId
        }
    }

    fun clearSelectedRecords() {
        selectedRecordIds.value = emptySet()
    }

    fun selectAllCurrentRecords() {
        selectedRecordIds.value = uiState.value.records.map { it.recordId }.toSet()
    }

    fun invertSelectedRecords() {
        val all = uiState.value.records.map { it.recordId }.toSet()
        val current = selectedRecordIds.value
        selectedRecordIds.value = all - current
    }

    fun exportCurrentTabToWord(context: Context, projectName: String = "项目$projectId") {
        viewModelScope.launch {
            try {
                val areaName = if (selectedTab.value == SiteLogType.WORK) "工作记录" else "安全记录"
                val rows = withContext(Dispatchers.IO) {
                    if (projectId > 0) {
                        siteLogDao.getLogsByProjectAndType(projectId, selectedTab.value)
                    } else {
                        siteLogDao.listByType(selectedTab.value)
                    }
                }
                if (rows.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "当前无可导出记录", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                val uri = withContext(Dispatchers.IO) { WordExporter.exportSiteLogsToWord(context, projectName, areaName, rows) }
                withContext(Dispatchers.Main) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "导出现场日志").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun exportSelectedToWord(context: Context, projectName: String = "项目$projectId") {
        viewModelScope.launch {
            val recordIds = selectedRecordIds.value.toList()
            try {
                val areaName = if (selectedTab.value == SiteLogType.WORK) "工作记录" else "安全记录"
                val rows = withContext(Dispatchers.IO) {
                    if (recordIds.isEmpty()) {
                        if (projectId > 0) {
                            siteLogDao.getLogsByProjectAndType(projectId, selectedTab.value)
                        } else {
                            siteLogDao.listByType(selectedTab.value)
                        }
                    } else {
                        siteLogDao.listByRecordIds(recordIds)
                    }
                }
                if (rows.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "当前无可导出记录", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                val uri = withContext(Dispatchers.IO) { WordExporter.exportSiteLogsToWord(context, projectName, areaName, rows) }
                withContext(Dispatchers.Main) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "导出现场日志").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                clearSelectedRecords()
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun exportSelectedOnlyToWord(context: Context, projectName: String = "项目$projectId") {
        viewModelScope.launch {
            val recordIds = selectedRecordIds.value.toList()
            if (recordIds.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "请先选择记录", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }
            try {
                val areaName = if (selectedTab.value == SiteLogType.WORK) "工作记录" else "安全记录"
                val rows = withContext(Dispatchers.IO) {
                    siteLogDao.listByRecordIds(recordIds)
                }
                if (rows.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "当前无可导出记录", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                val uri = withContext(Dispatchers.IO) { WordExporter.exportSiteLogsToWord(context, projectName, areaName, rows) }
                withContext(Dispatchers.Main) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "导出现场日志").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                clearSelectedRecords()
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun deleteSelectedLogs() {
        viewModelScope.launch {
            val ids = selectedRecordIds.value.toList()
            if (ids.isEmpty()) return@launch
            ids.forEach { recordId ->
                val rows = siteLogDao.listByRecordId(recordId)
                siteLogDao.deleteByRecordId(recordId)
                rows.forEach { row -> runCatching { File(row.photoPath).delete() } }
            }
            clearSelectedRecords()
        }
    }

    fun saveDraft() {
        val d = draft.value ?: return
        if (d.photoPaths.isEmpty()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val recordId = UUID.randomUUID().toString()
            d.photoPaths.forEach { path ->
                siteLogDao.insert(
                    SiteLogEntity(
                        projectId = projectId,
                        recordId = recordId,
                        photoPath = path,
                        logType = selectedTab.value,
                        description = d.description,
                        timestamp = now
                    )
                )
            }
            draft.value = null
            scrollToken.update { it + 1L }
        }
    }

    fun cancelDraft() {
        val d = draft.value ?: return
        d.photoPaths.forEach { path -> runCatching { File(path).delete() } }
        draft.value = null
    }

    fun continueDraft() {
        if (draft.value == null) return
    }

    fun appendDraftKeyword(keyword: String) {
        val d = draft.value ?: return
        val key = keyword.trim()
        if (key.isEmpty()) return
        val current = d.description.trim()
        val next = if (current.isEmpty()) key else if (current.contains(key)) current else "$current，$key"
        draft.value = d.copy(description = next)
    }

    fun updateDraftDescription(newDesc: String) {
        val d = draft.value ?: return
        draft.value = d.copy(description = newDesc)
    }

    fun removeDraftPhoto(path: String) {
        val d = draft.value ?: return
        draft.value = d.copy(photoPaths = d.photoPaths.filterNot { it == path })
        runCatching { File(path).delete() }
        if (draft.value?.photoPaths?.isEmpty() == true) {
            draft.value = d.copy(photoPaths = emptyList())
        }
    }

    fun clearSupplementKeywords() {
        viewModelScope.launch {
            dictionaryDao.deleteAllInTwoCategories(
                DictionaryCategory.SITE_LOG_WORK,
                DictionaryCategory.SITE_LOG_SAFETY
            )
        }
    }
}

private fun List<SiteLogEntity>.toRecordList(): List<SiteLogRecordUi> {
    return this.groupBy { it.recordId.ifBlank { it.id.toString() } }
        .values
        .map { rows ->
            val first = rows.first()
            SiteLogRecordUi(
                recordId = first.recordId.ifBlank { first.id.toString() },
                logType = first.logType,
                description = first.description,
                timestamp = first.timestamp,
                photos = rows.sortedBy { it.id }.map { SiteLogPhotoUi(id = it.id, path = it.photoPath) }
            )
        }
        .sortedBy { it.timestamp }
}

