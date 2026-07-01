package com.record.bridge.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.record.bridge.data.BridgeDefectRecordDao
import com.record.bridge.data.DictionaryDao
import com.record.bridge.data.ProjectDao
import com.record.bridge.data.ProjectRecordDao
import com.record.bridge.data.SiteLogDao

class MainViewModelFactory(
    private val dao: BridgeDefectRecordDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(dao) as T
    }
}

class AddRecordViewModelFactory(
    private val recordDao: BridgeDefectRecordDao,
    private val dictionaryDao: DictionaryDao,
    private val projectRecordDao: ProjectRecordDao,
    private val projectId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AddRecordViewModel(recordDao, dictionaryDao, projectRecordDao, projectId) as T
    }
}

class ProjectsViewModelFactory(
    private val projectDao: ProjectDao,
    private val projectRecordDao: ProjectRecordDao,
    private val recordDao: BridgeDefectRecordDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ProjectsViewModel(projectDao, projectRecordDao, recordDao) as T
    }
}

class RecordsViewModelFactory(
    private val projectId: Long,
    private val recordDao: BridgeDefectRecordDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RecordsViewModel(projectId, recordDao) as T
    }
}

class ExportViewModelFactory(
    private val projectId: Long,
    private val projectDao: ProjectDao,
    private val recordDao: BridgeDefectRecordDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ExportViewModel(projectId, projectDao, recordDao) as T
    }
}

class ProjectDetailViewModelFactory(
    private val projectId: Long,
    private val projectDao: ProjectDao,
    private val recordDao: BridgeDefectRecordDao,
    private val siteLogDao: SiteLogDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ProjectDetailViewModel(projectId, projectDao, recordDao, siteLogDao) as T
    }
}

class DictionarySettingsViewModelFactory(
    private val dao: DictionaryDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DictionarySettingsViewModel(dao) as T
    }
}

class SiteLogViewModelFactory(
    private val siteLogDao: SiteLogDao,
    private val dictionaryDao: DictionaryDao,
    private val projectId: Long = 0L
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SiteLogViewModel(siteLogDao, dictionaryDao, projectId) as T
    }
}
