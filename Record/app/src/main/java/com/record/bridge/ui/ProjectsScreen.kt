package com.record.bridge.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.record.bridge.data.ProjectWithCount
import com.record.bridge.vm.ProjectsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    vm: ProjectsViewModel,
    onProjectClick: (ProjectWithCount) -> Unit,
    onSettingsClick: () -> Unit
) {
    val projects by vm.projects.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var actionProject by remember { mutableStateOf<ProjectWithCount?>(null) }
    var showActionDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.createProject(name)
                        name = ""
                        showDialog = false
                    }
                ) {
                    Text("创建")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        name = ""
                        showDialog = false
                    }
                ) {
                    Text("取消")
                }
            },
            title = { Text("新建项目") },
            text = {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("项目名称") }
                )
            }
        )
    }

    if (showActionDialog) {
        val p = actionProject
        if (p != null) {
            AlertDialog(
                onDismissRequest = { showActionDialog = false },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showActionDialog = false }) {
                        Text("取消")
                    }
                },
                title = { Text(p.name) },
                text = {
                    Column {
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                showActionDialog = false
                                editName = p.name
                                showEditDialog = true
                            }
                        ) {
                            Text("编辑名称")
                        }
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                showActionDialog = false
                                showDeleteConfirm = true
                            }
                        ) {
                            Text("删除项目")
                        }
                    }
                }
            )
        }
    }

    if (showEditDialog) {
        val p = actionProject
        if (p != null) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            vm.renameProject(p.id, editName)
                            showEditDialog = false
                        }
                    ) {
                        Text("保存")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("取消")
                    }
                },
                title = { Text("编辑项目名称") },
                text = {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("项目名称") }
                    )
                }
            )
        }
    }

    if (showDeleteConfirm) {
        val p = actionProject
        if (p != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            vm.deleteProject(p.id)
                            showDeleteConfirm = false
                            actionProject = null
                        }
                    ) {
                        Text("删除")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("取消")
                    }
                },
                title = { Text("删除项目") },
                text = { Text("内部记录将全部删除，是否确定？") }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("项目") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(imageVector = Icons.Filled.Settings, contentDescription = "设置")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "新建项目")
            }
        }
    ) { padding ->
        LazyVerticalGrid(
            modifier = Modifier.padding(padding).padding(16.dp),
            columns = GridCells.Adaptive(minSize = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = projects, key = { it.id }) { p ->
                ProjectItem(
                    project = p,
                    onClick = { onProjectClick(p) },
                    onLongClick = {
                        actionProject = p
                        showActionDialog = true
                    }
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ProjectItem(
    project: ProjectWithCount,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppleFolderIcon(modifier = Modifier.size(80.dp))
        Text(
            text = project.name,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${project.recordCount}项",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * macOS Finder 风格文件夹图标
 * - 横向宽幅主体（主体 + 左上角标签页）
 * - 前层（浅色）+ 后层（深色）+ 投影层次
 * - 顶部高光弧形，底部内阴影
 * - 0.5px 白色描边，35% 透明度
 */
@Composable
private fun AppleFolderIcon(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.matchParentSize()) {
            // ---- 尺寸基准 ----
            val iconW = size.width * 0.90f
            val iconH = size.height * 0.68f
            val iconLeft = (size.width - iconW) / 2f
            val iconTop = (size.height - iconH) / 2f
            val cornerR = iconW * 0.06f   // 主体圆角
            val tabR    = iconW * 0.035f   // 标签页圆角

            // 标签页（耳朵）：顶部左侧
            val tabW  = iconW * 0.38f
            val tabH  = iconH * 0.22f
            val tabL  = iconLeft
            val tabT  = iconTop
            // 主体（宽幅矩形，从标签页底部开始）
            val bodyL = iconLeft
            val bodyT = iconTop + tabH * 0.55f
            val bodyW = iconW
            val bodyH = iconH - tabH * 0.55f

            // ---- 颜色 ----
            val frontColor  = Color(0xCC3395FF)  // 前层，80% 透明
            val backColor   = Color(0x990062CC)  // 后层，60% 透明
            val strokeColor = Color(0x59FFFFFF)  // 白色描边 35%
            val shadowColor = Color(0x40006480)  // 投影 25%

            // ---- 后层（背板 + 标签页） ----
            val tabPath = Path().apply {
                moveTo(tabL + tabR, tabT)
                lineTo(tabL + tabW - tabR, tabT)
                arcTo(
                    rect = Rect(tabL + tabW - tabR * 2, tabT, tabL + tabW, tabT + tabR * 2),
                    startAngleDegrees = -90f, sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(tabL + tabW, tabT + tabH - tabR)
                arcTo(
                    rect = Rect(tabL + tabW - tabR * 2, tabT + tabH - tabR * 2, tabL + tabW, tabT + tabH),
                    startAngleDegrees = 0f, sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(tabL + tabR, tabT + tabH)
                arcTo(
                    rect = Rect(tabL, tabT + tabH - tabR * 2, tabL + tabR * 2, tabT + tabH),
                    startAngleDegrees = 90f, sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(tabL, tabT + tabR)
                arcTo(
                    rect = Rect(tabL, tabT, tabL + tabR * 2, tabT + tabR * 2),
                    startAngleDegrees = 180f, sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                close()
            }
            val backBodyPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = bodyL, top = bodyT,
                        right = bodyL + bodyW, bottom = bodyT + bodyH,
                        cornerRadius = CornerRadius(cornerR)
                    )
                )
            }
            val backPath = Path().apply {
                addPath(tabPath)
                addPath(backBodyPath)
            }
            drawPath(path = backPath, color = backColor)

            // ---- 投影（标签页右侧延伸到底层的细线阴影） ----
            val tabShadowBrush = Brush.verticalGradient(
                colors = listOf(shadowColor, Color(0x00000000)),
                startY = bodyT,
                endY = bodyT + bodyH * 0.25f
            )
            drawLine(
                brush = tabShadowBrush,
                start = Offset(tabL + tabW, bodyT),
                end = Offset(tabL + tabW, bodyT + bodyH * 0.25f),
                strokeWidth = bodyW * 0.018f
            )

            // ---- 前层（主体面板） ----
            val frontBodyPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = bodyL, top = bodyT,
                        right = bodyL + bodyW, bottom = bodyT + bodyH,
                        cornerRadius = CornerRadius(cornerR)
                    )
                )
            }
            drawPath(path = frontBodyPath, color = frontColor)

            // ---- 顶部内高光弧形（仅前层上半部） ----
            val topHighlightBrush = Brush.verticalGradient(
                colors = listOf(Color(0x40FFFFFF), Color(0x00FFFFFF)),
                startY = bodyT,
                endY = bodyT + bodyH * 0.45f
            )
            val topHighlightPath = Path().apply {
                moveTo(bodyL, bodyT)
                arcTo(
                    rect = Rect(bodyL, bodyT, bodyL + bodyW, bodyT + bodyH * 0.45f),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )
                close()
            }
            drawPath(path = topHighlightPath, brush = topHighlightBrush)

            // ---- 底部内阴影弧形 ----
            val bottomShadowBrush = Brush.verticalGradient(
                colors = listOf(Color(0x00006090), Color(0x30006090)),
                startY = bodyT + bodyH * 0.55f,
                endY = bodyT + bodyH
            )
            val bottomShadowPath = Path().apply {
                moveTo(bodyL, bodyT + bodyH * 0.55f)
                arcTo(
                    rect = Rect(bodyL, bodyT + bodyH * 0.55f, bodyL + bodyW, bodyT + bodyH),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )
                close()
            }
            drawPath(path = bottomShadowPath, brush = bottomShadowBrush)

            // ---- 描边 ----
            drawPath(path = frontBodyPath, color = strokeColor, style = Stroke(width = 0.5.dp.toPx()))
        }
    }
}

