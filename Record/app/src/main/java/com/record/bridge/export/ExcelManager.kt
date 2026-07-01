package com.record.bridge.export

import android.content.ContentResolver
import android.net.Uri
import com.record.bridge.data.DictionaryCategory
import com.record.bridge.data.DictionaryEntity
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook

data class DictionaryImportResult(
    val insertedCount: Int,
    val skippedDuplicateCount: Int
)

class InvalidDictionaryExcelException : IllegalArgumentException()

class ExcelManager(
    private val contentResolver: ContentResolver
) {
    fun importDictionaryFromExcel(uri: Uri): List<DictionaryEntity> {
        val inputStream = contentResolver.openInputStream(uri) ?: throw InvalidDictionaryExcelException()
        inputStream.use { input ->
            WorkbookFactory.create(input).use { workbook ->
                return parseDictionarySheet(workbook.getSheetAt(0))
            }
        }
    }

    fun writeDictionaryTemplate(uri: Uri) {
        val outputStream = contentResolver.openOutputStream(uri) ?: throw IllegalStateException()
        outputStream.use { output ->
            buildDictionaryTemplateWorkbook().use { workbook ->
                workbook.write(output)
                output.flush()
            }
        }
    }

    companion object {
        private val formatter = DataFormatter()
        private val supportedCategories = listOf(
            "构件编号" to DictionaryCategory.COMPONENT,
            "病害类型" to DictionaryCategory.DEFECT_TYPE,
            "纵向位置参照点" to DictionaryCategory.LOCATION_LONG_REF,
            "横向位置参照点" to DictionaryCategory.LOCATION_TRANS_REF
        )

        internal fun parseDictionarySheet(sheet: org.apache.poi.ss.usermodel.Sheet): List<DictionaryEntity> {
            validateHeader(sheet.getRow(0) ?: throw InvalidDictionaryExcelException())
            val entities = mutableListOf<DictionaryEntity>()
            for (rowIndex in 1..sheet.lastRowNum) {
                val row = sheet.getRow(rowIndex) ?: continue
                if (isBlankRow(row)) continue
                ensureTwoColumns(row)
                val category = normalizeCategory(readCell(row, 0))
                val label = readCell(row, 1).trim()
                if (label.isEmpty()) {
                    throw InvalidDictionaryExcelException()
                }
                entities += DictionaryEntity(
                    category = category,
                    label = label,
                    remark = "",
                    isDefault = false,
                    isActive = true
                )
            }
            return entities
        }

        internal fun buildDictionaryTemplateWorkbook(): XSSFWorkbook {
            val workbook = XSSFWorkbook()
            val templateSheet = workbook.createSheet("dictionary")
            val guideSheet = workbook.createSheet("说明")

            val headerRow = templateSheet.createRow(0)
            headerRow.createCell(0).setCellValue("category")
            headerRow.createCell(1).setCellValue("label")
            templateSheet.setColumnWidth(0, 20 * 256)
            templateSheet.setColumnWidth(1, 28 * 256)
            templateSheet.createFreezePane(0, 1)

            guideSheet.createRow(0).createCell(0).setCellValue("第一张工作表必须保留 category、label 两列标题")
            guideSheet.createRow(1).createCell(0).setCellValue("category 支持填写以下值：")
            supportedCategories.forEachIndexed { index, (displayName, value) ->
                val row = guideSheet.createRow(index + 2)
                row.createCell(0).setCellValue(displayName)
                row.createCell(1).setCellValue(value)
            }
            guideSheet.setColumnWidth(0, 24 * 256)
            guideSheet.setColumnWidth(1, 20 * 256)
            return workbook
        }

        private fun validateHeader(row: Row) {
            ensureTwoColumns(row)
            val categoryHeader = readCell(row, 0).lowercase()
            val labelHeader = readCell(row, 1).lowercase()
            if (categoryHeader !in setOf("category", "分类") || labelHeader !in setOf("label", "词条")) {
                throw InvalidDictionaryExcelException()
            }
        }

        private fun ensureTwoColumns(row: Row) {
            val lastNonBlankCellIndex = row.lastCellNum
                .takeIf { it >= 0 }
                ?.toInt()
                ?.let { upperBound ->
                    (0 until upperBound).lastOrNull { index -> readCell(row, index).isNotBlank() }
                }
                ?: -1
            if (lastNonBlankCellIndex > 1) {
                throw InvalidDictionaryExcelException()
            }
        }

        private fun normalizeCategory(value: String): String {
            val normalized = value.trim()
            return supportedCategories.firstOrNull { (displayName, rawValue) ->
                normalized.equals(displayName, ignoreCase = true) || normalized.equals(rawValue, ignoreCase = true)
            }?.second ?: throw InvalidDictionaryExcelException()
        }

        private fun readCell(row: Row, index: Int): String {
            val cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL) ?: return ""
            if (cell.cellType == CellType.BLANK) return ""
            return formatter.formatCellValue(cell).trim()
        }

        private fun isBlankRow(row: Row): Boolean =
            (0 until maxOf(row.lastCellNum.toInt(), 2)).all { index -> readCell(row, index).isBlank() }
    }
}
