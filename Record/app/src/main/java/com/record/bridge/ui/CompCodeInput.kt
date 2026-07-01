package com.record.bridge.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.record.bridge.domain.buildCompFullCode

@Composable
fun CompCodeInput(
    prefix: String,
    numSegments: List<String>,
    prefixOptions: List<String>,
    onPrefixChange: (String) -> Unit,
    onNumChange: (index: Int, value: String) -> Unit,
    onStep: (index: Int, delta: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val segs = List(4) { idx -> numSegments.getOrNull(idx).orEmpty() }
    val focusRequesters = remember { List(4) { FocusRequester() } }
    var pendingFocusIndex by remember { mutableStateOf<Int?>(null) }
    val enabledStates = listOf(
        true,
        segs[0].isNotBlank(),
        segs[1].isNotBlank(),
        segs[2].isNotBlank()
    )
    val fullCode = remember(prefix, segs) {
        buildCompFullCode(prefix, segs)
    }

    LaunchedEffect(segs, pendingFocusIndex) {
        val nextIndex = pendingFocusIndex ?: return@LaunchedEffect
        if (enabledStates.getOrNull(nextIndex) == true) {
            focusRequesters[nextIndex].requestFocus()
            pendingFocusIndex = null
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "构件编号", style = MaterialTheme.typography.bodyMedium)
        PrefixSelector(
            prefix = prefix,
            options = prefixOptions,
            onSelect = onPrefixChange,
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SegmentEditor(
                value = segs[0],
                enabled = enabledStates[0],
                focusRequester = focusRequesters[0],
                onValueChange = {
                    onNumChange(0, it)
                    if (segs[0].isBlank() && it.isNotBlank()) pendingFocusIndex = 1
                },
                onInc = { onStep(0, +1) },
                onDec = { onStep(0, -1) },
                onNext = {
                    if (segs[0].isNotBlank()) {
                        focusRequesters[1].requestFocus()
                    }
                },
                imeAction = ImeAction.Next
            )
            Text(text = "-", modifier = Modifier.padding(top = 20.dp))
            SegmentEditor(
                value = segs[1],
                enabled = enabledStates[1],
                focusRequester = focusRequesters[1],
                onValueChange = {
                    onNumChange(1, it)
                    if (segs[1].isBlank() && it.isNotBlank()) pendingFocusIndex = 2
                },
                onInc = { onStep(1, +1) },
                onDec = { onStep(1, -1) },
                onNext = {
                    if (segs[1].isNotBlank()) {
                        focusRequesters[2].requestFocus()
                    }
                },
                imeAction = ImeAction.Next
            )
            Text(text = "-", modifier = Modifier.padding(top = 20.dp))
            SegmentEditor(
                value = segs[2],
                enabled = enabledStates[2],
                focusRequester = focusRequesters[2],
                onValueChange = {
                    onNumChange(2, it)
                    if (segs[2].isBlank() && it.isNotBlank()) pendingFocusIndex = 3
                },
                onInc = { onStep(2, +1) },
                onDec = { onStep(2, -1) },
                onNext = {
                    if (segs[2].isNotBlank()) {
                        focusRequesters[3].requestFocus()
                    }
                },
                imeAction = ImeAction.Next
            )
            Text(text = "-", modifier = Modifier.padding(top = 20.dp))
            SegmentEditor(
                value = segs[3],
                enabled = enabledStates[3],
                focusRequester = focusRequesters[3],
                onValueChange = { onNumChange(3, it) },
                onInc = { onStep(3, +1) },
                onDec = { onStep(3, -1) },
                onNext = {},
                imeAction = ImeAction.Done
            )
        }
        Text(
            text = "预览：$fullCode",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PrefixSelector(
    prefix: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var lastSelected by remember { mutableStateOf(prefix) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val filtered = remember(prefix, options, expanded, lastSelected) {
        if (!expanded) return@remember emptyList()
        val q = prefix.trim()
        if (q.isEmpty()) return@remember options
        if (q == lastSelected.trim()) return@remember options
        options.filter { it.contains(q, ignoreCase = true) }
    }
    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = {}
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .menuAnchor()
                .focusRequester(focusRequester),
            value = prefix,
            onValueChange = {
                expanded = true
                onSelect(it)
            },
            readOnly = false,
            singleLine = true,
            placeholder = { Text("前缀") },
            textStyle = MaterialTheme.typography.titleMedium,
            trailingIcon = {
                Row {
                    if (prefix.isNotBlank()) {
                        IconButton(
                            onClick = {
                                lastSelected = ""
                                expanded = false
                                onSelect("")
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
        DropdownMenu(expanded = expanded, onDismissRequest = {
            expanded = false
        }, properties = PopupProperties(focusable = false)) {
            filtered.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        expanded = false
                        lastSelected = opt
                        onSelect(opt)
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                    }
                )
            }
        }
    }
}

@Composable
private fun SegmentEditor(
    value: String,
    enabled: Boolean,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onInc: () -> Unit,
    onDec: () -> Unit,
    onNext: () -> Unit,
    imeAction: ImeAction
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        IconButton(
            modifier = Modifier.width(44.dp).height(22.dp),
            onClick = onInc,
            enabled = enabled
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "+")
        }
        OutlinedTextField(
            modifier = Modifier.width(64.dp).height(56.dp).focusRequester(focusRequester),
            value = value,
            onValueChange = { onValueChange(it.filter { ch -> ch.isDigit() }) },
            enabled = enabled,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onNext = { onNext() },
                onDone = { onNext() }
            ),
            singleLine = true
        )
        IconButton(
            modifier = Modifier.width(44.dp).height(22.dp),
            onClick = onDec,
            enabled = enabled
        ) {
            Icon(imageVector = Icons.Filled.Remove, contentDescription = "-")
        }
    }
}

