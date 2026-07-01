package com.record.bridge.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.record.bridge.vm.AddRecordViewModel

@Composable
fun EditRecordScreen(
    vm: AddRecordViewModel,
    componentNo: String,
    defectType: String,
    defectLocation: String,
    onBack: () -> Unit
) {
    LaunchedEffect(componentNo, defectType, defectLocation) {
        vm.loadForEdit(componentNo, defectType, defectLocation)
    }
    AddRecordScreen(
        vm = vm,
        onBack = onBack,
        title = "查看/修改记录",
        submitLabel = "保存修改"
    )
}

