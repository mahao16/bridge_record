package com.record.bridge.vm

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.record.bridge.data.BridgeDefectRecordDao
import com.record.bridge.data.ProjectDao
import com.record.bridge.data.SiteLogDao
import com.record.bridge.data.SiteLogType
import com.record.bridge.export.ProjectReportWordExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProjectDetailViewModel(
    private val projectId: Long,
    private val projectDao: ProjectDao,
    private val recordDao: BridgeDefectRecordDao,
    private val siteLogDao: SiteLogDao
) : ViewModel() {
    private val _projectName = MutableStateFlow("项目 $projectId")
    val projectName: StateFlow<String> = _projectName.asStateFlow()

    init {
        viewModelScope.launch {
            val fallback = "项目 $projectId"
            val name = withContext(Dispatchers.IO) { projectDao.getOne(projectId)?.name?.trim().orEmpty() }
            _projectName.value = name.ifEmpty { fallback }
        }
    }

    fun exportProjectReport(context: Context) {
        viewModelScope.launch {
            try {
                val pname = projectName.value
                val (defects, workLogs, safetyLogs) = withContext(Dispatchers.IO) {
                    Triple(
                        recordDao.listByProject(projectId),
                        siteLogDao.getLogsByProjectAndType(projectId, SiteLogType.WORK),
                        siteLogDao.getLogsByProjectAndType(projectId, SiteLogType.SAFETY)
                    )
                }
                if (defects.isEmpty() && workLogs.isEmpty() && safetyLogs.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "当前项目无可导出数据", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                val uri = withContext(Dispatchers.IO) {
                    ProjectReportWordExporter.exportProjectReport(
                        context = context,
                        projectName = pname,
                        defects = defects,
                        workLogs = workLogs,
                        safetyLogs = safetyLogs
                    )
                }
                withContext(Dispatchers.Main) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "${pname}_总报告").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
