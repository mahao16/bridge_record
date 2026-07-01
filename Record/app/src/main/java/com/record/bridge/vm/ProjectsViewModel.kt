package com.record.bridge.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.record.bridge.data.BridgeDefectRecordDao
import com.record.bridge.data.ProjectDao
import com.record.bridge.data.ProjectEntity
import com.record.bridge.data.ProjectRecordDao
import com.record.bridge.data.ProjectWithCount
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProjectsViewModel(
    private val projectDao: ProjectDao,
    private val projectRecordDao: ProjectRecordDao,
    private val recordDao: BridgeDefectRecordDao
) : ViewModel() {

    val projects: StateFlow<List<ProjectWithCount>> =
        projectDao.observeAllWithCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createProject(name: String) {
        val n = name.trim()
        if (n.isEmpty()) return
        viewModelScope.launch {
            projectDao.insert(ProjectEntity(name = n, createdAt = System.currentTimeMillis()))
        }
    }

    fun renameProject(projectId: Long, name: String) {
        val n = name.trim()
        if (projectId <= 0 || n.isEmpty()) return
        viewModelScope.launch {
            projectDao.updateName(projectId, n)
        }
    }

    fun deleteProject(projectId: Long) {
        if (projectId <= 0) return
        viewModelScope.launch {
            recordDao.deleteByProject(projectId)
            projectRecordDao.deleteByProject(projectId)
            projectDao.deleteById(projectId)
        }
    }
}

