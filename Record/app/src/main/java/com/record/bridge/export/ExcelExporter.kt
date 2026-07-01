package com.record.bridge.export

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.record.bridge.data.BridgeDefectRecordEntity
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.util.Date

object ExcelExporter {
    fun exportProjectRecords(
        context: Context,
        projectId: Long,
        projectName: String,
        areaName: String,
        records: List<BridgeDefectRecordEntity>,
        now: Date = Date()
    ): android.net.Uri {
        val fileName = buildExportBaseName(projectName, areaName, now) + ".xlsx"

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)

        val wb = buildWorkbook(records) { raw -> formatPhotoNumbers(context, projectId, raw) }
        try {
            FileOutputStream(file).use { out ->
                wb.write(out)
                out.flush()
            }
        } finally {
            runCatching { wb.close() }
        }

        val authority = context.packageName + ".fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    internal fun buildWorkbook(
        records: List<BridgeDefectRecordEntity>,
        photoFormatter: (String) -> String = ::defaultFormatPhotoIds
    ): XSSFWorkbook {
        val wb = XSSFWorkbook()
        val sheet = wb.createSheet("records")

        val df = wb.createDataFormat()
        val textFormat = df.getFormat("@")

        val font = wb.createFont().apply {
            fontName = "宋体"
            fontHeightInPoints = 11
        }
        val headerFont = wb.createFont().apply {
            fontName = "宋体"
            fontHeightInPoints = 11
            bold = true
        }

        val baseBordered = wb.createCellStyle().apply {
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            dataFormat = textFormat
            setFont(font)
            wrapText = true
        }

        val headerStyle = wb.createCellStyle().apply {
            cloneStyleFrom(baseBordered)
            setFont(headerFont)
        }

        val bodyStyle = wb.createCellStyle().apply {
            cloneStyleFrom(baseBordered)
            alignment = HorizontalAlignment.LEFT
            verticalAlignment = VerticalAlignment.TOP
        }

        val header = sheet.createRow(0).apply { heightInPoints = 20f }
        listOf("构件编号", "病害类型", "病害位置", "病害定量描述", "照片编号").forEachIndexed { i, title ->
            header.createCell(i).apply {
                setCellValue(title)
                cellStyle = headerStyle
            }
        }

        records.forEachIndexed { index, r ->
            val row = sheet.createRow(index + 1).apply { heightInPoints = 18f }
            row.createCell(0).apply {
                setCellValue(r.compFullCode.ifBlank { r.componentNo })
                cellStyle = bodyStyle
            }
            row.createCell(1).apply {
                setCellValue(r.defectType)
                cellStyle = bodyStyle
            }
            row.createCell(2).apply {
                setCellValue(r.defectLocation)
                cellStyle = bodyStyle
            }
            row.createCell(3).apply {
                setCellValue(r.quantitativeDesc)
                cellStyle = bodyStyle
            }
            row.createCell(4).apply {
                setCellValue(photoFormatter(r.photoIds))
                cellStyle = bodyStyle
            }
        }

        sheet.setColumnWidth(0, 18 * 256)
        sheet.setColumnWidth(1, 14 * 256)
        sheet.setColumnWidth(2, 24 * 256)
        sheet.setColumnWidth(3, 28 * 256)
        sheet.setColumnWidth(4, 18 * 256)

        sheet.createFreezePane(0, 1)
        sheet.setAutoFilter(CellRangeAddress(0, 0, 0, 4))
        return wb
    }
}

private fun defaultFormatPhotoIds(raw: String): String {
    val s = raw.trim()
    if (s.isEmpty()) return ""
    val tokens = s.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return ""
    return tokens.map { token ->
        token.takeLast(4).padStart(4, '0')
    }.distinct().sorted().joinToString(", ")
}

private fun formatPhotoNumbers(context: Context, projectId: Long, raw: String): String {
    val s = raw.trim()
    if (s.isEmpty()) return ""
    val tokens = s.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return ""
    return tokens.map { token ->
        val asLong = token.toLongOrNull()
        if (asLong != null) {
            getPhotoNumberFromFileName(context, asLong)
                ?: token.takeLast(4).padStart(4, '0')
        } else {
            token.takeLast(4).padStart(4, '0')
        }
    }.distinct().sorted().joinToString(", ")
}

/**
 * 从照片文件名取后4位数字作为编号
 * 文件名格式：{构件编号}_{yyyyMMddHHmmss}.jpg
 * 例如：P-01-001_20250610112130.jpg -> 取 2130
 */
private fun getPhotoNumberFromFileName(context: Context, mediaStoreId: Long): String? {
    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaStoreId)
    val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME)
    return context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            if (nameIndex >= 0) {
                val fileName = cursor.getString(nameIndex)
                // 去掉 .jpg 后缀，取最后4位数字
                fileName?.removeSuffix(".jpg")
                    ?.takeLast(4)
                    ?.filter { it.isDigit() }
                    ?.takeIf { it.length == 4 }
            } else null
        } else null
    }
}

