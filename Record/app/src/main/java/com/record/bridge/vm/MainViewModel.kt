package com.record.bridge.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.record.bridge.data.BridgeDefectRecordDao
import com.record.bridge.data.BridgeDefectRecordEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MainViewModel(
    dao: BridgeDefectRecordDao
) : ViewModel() {
    val records: StateFlow<List<BridgeDefectRecordEntity>> =
        dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
