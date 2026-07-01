package com.record.bridge.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.unit.dp

@Composable
fun PositionRecordEditor(
    position: String,
    onPositionChange: (String) -> Unit,
    longRefOptions: List<String> = listOf("小里程梁端", "大里程梁端"),
    transRefOptions: List<String> = listOf("底板", "顶板", "左腹板", "右腹板"),
    modifier: Modifier = Modifier
) {
    var longRef by remember { mutableStateOf("") }
    var longValue by remember { mutableStateOf("") }
    var longUnit by remember { mutableStateOf("m") }

    var transRef by remember { mutableStateOf("") }
    var transValue by remember { mutableStateOf("") }
    var transUnit by remember { mutableStateOf("m") }

    var manualFinal by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(position) {
        val p = position.trim()
        val m = manualFinal
        if (m != null && m != p) {
            manualFinal = p
        }
        if (manualFinal == null && p.isNotEmpty() && longRef.isBlank() && longValue.isBlank() && transRef.isBlank() && transValue.isBlank()) {
            val parts = p.split('，')
            parsePart(parts.getOrNull(0))?.let { (r, v, u) ->
                longRef = r
                longValue = v
                longUnit = u
            }
            parsePart(parts.getOrNull(1))?.let { (r, v, u) ->
                transRef = r
                transValue = v
                transUnit = u
            }
        }
    }

    val computedPreview = remember(longRef, longValue, longUnit, transRef, transValue, transUnit) {
        buildString {
            val a = buildPart(longRef, longValue, longUnit)
            val b = buildPart(transRef, transValue, transUnit)
            if (a.isNotEmpty()) append(a)
            if (b.isNotEmpty()) {
                if (isNotEmpty()) append("，")
                append(b)
            }
        }
    }

    val finalText = manualFinal ?: computedPreview

    LaunchedEffect(computedPreview, position, manualFinal) {
        if (manualFinal == null && computedPreview != position) onPositionChange(computedPreview)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            PositionBlock(
                title = "纵向位置",
                ref = longRef,
                onRefChange = { longRef = it },
                refOptions = longRefOptions,
                number = longValue,
                onNumberChange = { longValue = it },
                unit = longUnit,
                onUnitChange = { longUnit = it }
            )
            PositionBlock(
                title = "横向位置",
                ref = transRef,
                onRefChange = { transRef = it },
                refOptions = transRefOptions,
                number = transValue,
                onNumberChange = { transValue = it },
                unit = transUnit,
                onUnitChange = { transUnit = it }
            )
        }

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "最终位置描述", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = finalText,
                onValueChange = {
                    manualFinal = it
                    onPositionChange(it)
                },
                singleLine = false,
                minLines = 2,
                trailingIcon = {
                    if (manualFinal != null) {
                        IconButton(
                            onClick = {
                                manualFinal = null
                                onPositionChange(computedPreview)
                            }
                        ) {
                            Icon(imageVector = Icons.Filled.Clear, contentDescription = "恢复自动")
                        }
                    }
                }
            )
        }
    }
}

private fun buildPart(ref: String, value: String, unit: String): String {
    val r = ref.trim()
    val v = value.trim()
    if (r.isEmpty() || v.isEmpty()) return ""
    return "距$r$v$unit"
}

private fun parsePart(part: String?): Triple<String, String, String>? {
    val raw = part?.trim().orEmpty()
    if (!raw.startsWith("距")) return null
    val body = raw.removePrefix("距").trim()
    val idx = body.indexOfFirst { it.isDigit() || it == '.' || it == '-' }
    if (idx <= 0) return null
    val ref = body.substring(0, idx).trim()
    val valueUnit = body.substring(idx).trim()
    return when {
        valueUnit.endsWith("cm") -> Triple(ref, valueUnit.removeSuffix("cm").trim(), "cm")
        valueUnit.endsWith("m") -> Triple(ref, valueUnit.removeSuffix("m").trim(), "m")
        else -> Triple(ref, valueUnit, "m")
    }
}

@Composable
private fun PositionBlock(
    title: String,
    ref: String,
    onRefChange: (String) -> Unit,
    refOptions: List<String>,
    number: String,
    onNumberChange: (String) -> Unit,
    unit: String,
    onUnitChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "距", modifier = Modifier.width(24.dp).padding(top = 12.dp))
            EditableDropdown(
                modifier = Modifier.weight(1f),
                value = ref,
                onValueChange = onRefChange,
                options = refOptions
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Spacer(modifier = Modifier.width(24.dp))
            CompactNumberField(
                value = number,
                onValueChange = onNumberChange,
                modifier = Modifier.weight(1f)
            )
            UnitSegmented(
                unit = unit,
                onUnitChange = onUnitChange,
                modifier = Modifier.width(104.dp)
            )
        }
    }
}

@Composable
private fun CompactNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp
) {
    OutlinedTextField(
        modifier = modifier.height(height),
        value = value,
        onValueChange = { onValueChange(it) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        trailingIcon = {
            if (value.isNotBlank()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(imageVector = Icons.Filled.Clear, contentDescription = "清除")
                }
            }
        }
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun UnitSegmented(
    unit: String,
    onUnitChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.height(40.dp)) {
        SegmentedButton(
            selected = unit == "m",
            onClick = { onUnitChange("m") },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
        ) {
            Text("m")
        }
        SegmentedButton(
            selected = unit == "cm",
            onClick = { onUnitChange("cm") },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
        ) {
            Text("cm")
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EditableDropdown(
    modifier: Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    options: List<String>
) {
    var expanded by remember { mutableStateOf(false) }
    var lastSelected by remember { mutableStateOf(value) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val filtered = remember(value, options, expanded, lastSelected) {
        if (!expanded) return@remember emptyList()
        val q = value.trim()
        if (q.isEmpty()) return@remember options
        if (q == lastSelected.trim()) return@remember options
        options.filter { it.contains(q, ignoreCase = true) }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {},
        modifier = modifier
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .focusRequester(focusRequester),
            value = value,
            onValueChange = {
                expanded = true
                onValueChange(it)
            },
            singleLine = true,
            placeholder = { Text("参照点") },
            trailingIcon = {
                Row {
                    if (value.isNotBlank()) {
                        IconButton(
                            onClick = {
                                lastSelected = ""
                                expanded = false
                                onValueChange("")
                                focusRequester.requestFocus()
                            }
                        ) {
                            Icon(imageVector = Icons.Filled.Clear, contentDescription = "清除")
                        }
                    }
                    IconButton(onClick = {
                        expanded = !expanded
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                    }) {
                        Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = "展开")
                    }
                }
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = false)
        ) {
            filtered.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        expanded = false
                        lastSelected = opt
                        onValueChange(opt)
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                    }
                )
            }
        }
    }
}

