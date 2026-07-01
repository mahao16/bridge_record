package com.record.bridge.vm

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.record.bridge.data.BridgeDefectRecordDao
import com.record.bridge.data.BridgeDefectRecordEntity
import com.record.bridge.data.DictionaryCategory
import com.record.bridge.data.DictionaryDao
import com.record.bridge.data.DictionaryEntity
import com.record.bridge.data.ProjectRecordDao
import com.record.bridge.data.ProjectRecordEntity
import com.record.bridge.domain.buildCompFullCode
import com.record.bridge.domain.DefectCatalog
import com.record.bridge.domain.parseCompFullCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class AddRecordFormState(
    val componentNo: String = "",
    val compPrefix: String = "",
    val compNum1: String = "",
    val compNum2: String = "",
    val compNum3: String = "",
    val compNum4: String = "",
    val defectType: String = "",
    val defectLocation: String = "",
    val quantitativeDesc: String = "",
    val photoIds: List<Long> = emptyList(),
    val autoConcat: Boolean = true,
    val count: String = "",
    val lengthL: String = "",
    val widthWmm: String = "",
    val widthWm: String = "",
    val depthH: String = "",
    val isSubmitting: Boolean = false,
    val submitError: String = ""
)

class AddRecordViewModel(
    private val recordDao: BridgeDefectRecordDao,
    private val dictionaryDao: DictionaryDao,
    private val projectRecordDao: ProjectRecordDao,
    val projectId: Long
) : ViewModel() {

    private val _state = MutableStateFlow(AddRecordFormState())
    val state: StateFlow<AddRecordFormState> = _state

    private var originalKey: Triple<String, String, String>? = null

    val componentPresets: StateFlow<List<String>> =
        dictionaryDao.observeActiveLabels(DictionaryCategory.COMPONENT)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val compCodePrefixes: StateFlow<List<String>> =
        dictionaryDao.observeActiveLabels(DictionaryCategory.COMPONENT)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val defectTypePresets: StateFlow<List<String>> =
        dictionaryDao.observeActiveLabels(DictionaryCategory.DEFECT_TYPE)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val locationLongRefOptions: StateFlow<List<String>> =
        dictionaryDao.observeActiveLabels(DictionaryCategory.LOCATION_LONG_REF)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val locationTransRefOptions: StateFlow<List<String>> =
        dictionaryDao.observeActiveLabels(DictionaryCategory.LOCATION_TRANS_REF)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun loadForEdit(componentNo: String, defectType: String, defectLocation: String) {
        val a = componentNo.trim()
        val b = defectType.trim()
        val c = defectLocation.trim()
        if (a.isEmpty() || b.isEmpty() || c.isEmpty()) return
        originalKey = Triple(a, b, c)
        viewModelScope.launch {
            val entity = recordDao.getOne(projectId, a, b, c) ?: return@launch
            val ids = parsePhotoIds(entity.photoIds)
            val parsed = parseCompFullCode(entity.compFullCode.ifBlank { entity.componentNo })
            _state.update {
                it.copy(
                    componentNo = entity.componentNo,
                    compPrefix = parsed.prefix,
                    compNum1 = parsed.numSegments.getOrNull(0).orEmpty(),
                    compNum2 = parsed.numSegments.getOrNull(1).orEmpty(),
                    compNum3 = parsed.numSegments.getOrNull(2).orEmpty(),
                    compNum4 = parsed.numSegments.getOrNull(3).orEmpty(),
                    defectType = entity.defectType,
                    defectLocation = entity.defectLocation,
                    quantitativeDesc = entity.quantitativeDesc,
                    photoIds = ids,
                    autoConcat = false,
                    submitError = ""
                )
            }
        }
    }

    fun onSelectComponent(componentNo: String) {
        _state.update { s ->
            val next = s.copy(componentNo = componentNo, submitError = "")
            next.copy(quantitativeDesc = if (next.autoConcat) buildQuantitativeDesc(next) else next.quantitativeDesc)
        }
    }

    fun onCompPrefixChange(prefix: String) {
        _state.update { s ->
            val next = s.copy(compPrefix = prefix, submitError = "")
            next.copy(componentNo = computeFullCode(next))
        }
    }

    fun onCompNumChange(index: Int, raw: String) {
        val digits = raw.filter { it.isDigit() }
        _state.update { s ->
            val next = when (index) {
                0 -> if (digits.isEmpty()) {
                    s.copy(compNum1 = "", compNum2 = "", compNum3 = "", compNum4 = "", submitError = "")
                } else {
                    s.copy(compNum1 = digits, submitError = "")
                }
                1 -> if (digits.isEmpty()) {
                    s.copy(compNum2 = "", compNum3 = "", compNum4 = "", submitError = "")
                } else {
                    s.copy(compNum2 = digits, submitError = "")
                }
                2 -> if (digits.isEmpty()) {
                    s.copy(compNum3 = "", compNum4 = "", submitError = "")
                } else {
                    s.copy(compNum3 = digits, submitError = "")
                }
                else -> s.copy(compNum4 = digits, submitError = "")
            }
            next.copy(componentNo = computeFullCode(next))
        }
    }

    fun stepCompNum(index: Int, delta: Int) {
        if (delta == 0) return
        _state.update { s ->
            val cur = when (index) {
                0 -> s.compNum1
                1 -> s.compNum2
                2 -> s.compNum3
                else -> s.compNum4
            }.trim()

            val nextValue = if (cur.isEmpty()) {
                if (delta > 0) "1" else ""
            } else {
                val n = cur.toIntOrNull() ?: 0
                val nn = (n + delta).coerceAtLeast(0)
                nn.toString()
            }

            val next = when (index) {
                0 -> s.copy(compNum1 = nextValue, submitError = "")
                1 -> s.copy(compNum2 = nextValue, submitError = "")
                2 -> s.copy(compNum3 = nextValue, submitError = "")
                else -> s.copy(compNum4 = nextValue, submitError = "")
            }
            next.copy(componentNo = computeFullCode(next))
        }
    }

    fun onSelectDefectType(defectType: String) {
        _state.update { s ->
            val next = s.copy(defectType = defectType, submitError = "")
            next.copy(quantitativeDesc = if (next.autoConcat) buildQuantitativeDesc(next) else next.quantitativeDesc)
        }
    }

    fun onLocationChange(location: String) {
        val loc = location.trim()
        _state.update { s ->
            val next = s.copy(defectLocation = loc, submitError = "")
            next.copy(quantitativeDesc = if (next.autoConcat) buildQuantitativeDesc(next) else next.quantitativeDesc)
        }
    }

    fun appendLocationTemplate(token: String) {
        val t = token.trim()
        if (t.isEmpty()) return
        _state.update { s ->
            val current = s.defectLocation.trim()
            val sep = if (current.isEmpty()) "" else "，"
            val next = s.copy(defectLocation = current + sep + t, submitError = "")
            next.copy(quantitativeDesc = if (next.autoConcat) buildQuantitativeDesc(next) else next.quantitativeDesc)
        }
    }

    fun onMetricChange(
        count: String? = null,
        lengthL: String? = null,
        widthWmm: String? = null,
        widthWm: String? = null,
        depthH: String? = null
    ) {
        _state.update { s ->
            val next = s.copy(
                count = count ?: s.count,
                lengthL = lengthL ?: s.lengthL,
                widthWmm = widthWmm ?: s.widthWmm,
                widthWm = widthWm ?: s.widthWm,
                depthH = depthH ?: s.depthH,
                submitError = ""
            )
            next.copy(quantitativeDesc = buildQuantitativeDesc(next))
        }
    }

    fun onPhotoBound(photoNo: String) {
        val s = photoNo.trim()
        if (s.isEmpty()) return
        val id = s.toLongOrNull() ?: return
        addPhotoId(id)
    }

    fun addPhotoId(id: Long) {
        _state.update { s ->
            if (s.photoIds.contains(id)) s else s.copy(photoIds = s.photoIds + id, submitError = "")
        }
    }

    fun removePhotoId(id: Long) {
        _state.update { s ->
            s.copy(photoIds = s.photoIds.filterNot { it == id }, submitError = "")
        }
    }

    fun saveComponentPreset(value: String) {
        val v = value.trim()
        if (v.isEmpty()) return
        viewModelScope.launch {
            val id = dictionaryDao.insert(DictionaryEntity(category = DictionaryCategory.COMPONENT, label = v, remark = "", isDefault = false, isActive = true))
            if (id == -1L) {
                dictionaryDao.updateActiveByLabel(DictionaryCategory.COMPONENT, v, true)
            }
        }
    }

    fun saveDefectTypePreset(value: String) {
        val v = value.trim()
        if (v.isEmpty()) return
        viewModelScope.launch {
            val id = dictionaryDao.insert(DictionaryEntity(category = DictionaryCategory.DEFECT_TYPE, label = v, remark = "", isDefault = false, isActive = true))
            if (id == -1L) {
                dictionaryDao.updateActiveByLabel(DictionaryCategory.DEFECT_TYPE, v, true)
            }
        }
    }

    fun onQuantitativeDescManualChange(text: String) {
        _state.update { it.copy(quantitativeDesc = text, autoConcat = false, submitError = "") }
    }

    fun submit(onSuccess: () -> Unit) {
        if (_state.value.isSubmitting) return
        val entity = buildEntityOrNull()
        if (entity == null) {
            _state.update { it.copy(submitError = "请检查描述是否完整") }
            return
        }

        _state.update { it.copy(isSubmitting = true, submitError = "") }
        viewModelScope.launch {
            try {
                val old = originalKey
                recordDao.upsert(entity)
                projectRecordDao.upsert(
                    ProjectRecordEntity(
                        projectId = projectId,
                        componentNo = entity.componentNo,
                        defectType = entity.defectType,
                        defectLocation = entity.defectLocation
                    )
                )
                if (old != null) {
                    val (oc, ot, ol) = old
                    val newKey = Triple(entity.componentNo, entity.defectType, entity.defectLocation)
                    if (old != newKey) {
                        projectRecordDao.deleteOne(projectId, oc, ot, ol)
                        recordDao.deleteOne(projectId, oc, ot, ol)
                        originalKey = newKey
                    }
                }
                _state.update { it.copy(isSubmitting = false, submitError = "") }
                onSuccess()
            } catch (e: Exception) {
                _state.update { it.copy(isSubmitting = false, submitError = e.message.orEmpty().ifEmpty { "保存失败" }) }
            }
        }
    }

    fun prepareNextAfterSave() {
        originalKey = null
        _state.update { s ->
            AddRecordFormState(
                componentNo = computeFullCode(s),
                compPrefix = s.compPrefix,
                compNum1 = s.compNum1,
                compNum2 = s.compNum2,
                compNum3 = s.compNum3,
                compNum4 = s.compNum4,
                defectType = s.defectType,             // 保留病害类型（维持定量输入框显示）
                defectLocation = s.defectLocation   // 保留位置描述
                // quantitativeDesc、count、lengthL 等度量字段均清空（默认值 ""）
            )
        }
    }

    private fun buildEntityOrNull(): BridgeDefectRecordEntity? {
        val s = _state.value
        val a = computeFullCode(s).trim()
        val b = s.defectType.trim()
        val c = s.defectLocation.trim()
        val d = s.quantitativeDesc.trim()
        val e = s.photoIds
        if (a.isEmpty() || b.isEmpty() || c.isEmpty() || d.isEmpty() || e.isEmpty()) return null
        return BridgeDefectRecordEntity(
            projectId = projectId,
            componentNo = a,
            compFullCode = a,
            defectType = b,
            defectLocation = c,
            quantitativeDesc = d,
            photoIds = e.joinToString(",")
        )
    }

    private fun buildQuantitativeDesc(s: AddRecordFormState): String {
        val dt = s.defectType.trim()
        if (dt.isEmpty()) return ""

        if (DefectCatalog.isMeshCrack(dt)) {
            val parts = mutableListOf<String>()
            s.count.trim().takeIf { it.isNotEmpty() }?.let { parts += it + "处" }
            s.widthWmm.trim().takeIf { it.isNotEmpty() }?.let { parts += "W=$it" + "mm" }
            s.depthH.trim().takeIf { it.isNotEmpty() }?.let { parts += "H=$it" + "mm" }
            val l = s.lengthL.trim().ifEmpty { "[L]" }
            val w = s.widthWm.trim().ifEmpty { "[W]" }
            parts += "S=$l×$w" + "m²"
            return parts.joinToString("，")
        }

        if (DefectCatalog.isCrackNonMesh(dt)) {
            val parts = mutableListOf<String>()
            s.count.trim().takeIf { it.isNotEmpty() }?.let { parts += "n=$it" + "条" }
            s.lengthL.trim().takeIf { it.isNotEmpty() }?.let { parts += "L=$it" + "m" }
            s.widthWmm.trim().takeIf { it.isNotEmpty() }?.let { parts += "W=$it" + "mm" }
            s.depthH.trim().takeIf { it.isNotEmpty() }?.let { parts += "H=$it" + "mm" }
            return parts.joinToString("，")
        }

        if (DefectCatalog.needsAreaCalculator(dt)) {
            val parts = mutableListOf<String>()
            s.count.trim().takeIf { it.isNotEmpty() }?.let { parts += it + "处" }
            val l = s.lengthL.trim().ifEmpty { "[L]" }
            val w = s.widthWm.trim().ifEmpty { "[W]" }
            parts += "S=$l×$w" + "m²"
            return parts.joinToString("，")
        }

        return ""
    }
}

private fun computeFullCode(s: AddRecordFormState): String =
    buildCompFullCode(
        prefix = s.compPrefix,
        numSegments = listOf(s.compNum1, s.compNum2, s.compNum3, s.compNum4)
    )

private fun parsePhotoIds(raw: String): List<Long> {
    val s = raw.trim()
    if (s.isEmpty()) return emptyList()
    return s.split(',')
        .mapNotNull { it.trim().toLongOrNull() }
        .distinct()
        .sorted()
}
