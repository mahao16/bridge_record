package com.record.bridge.export

import com.record.bridge.data.DictionaryCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExcelManagerTest {
    @Test
    fun parseDictionarySheet_mapsChineseCategoryAndSkipsBlankRows() {
        val workbook = ExcelManager.buildDictionaryTemplateWorkbook()
        try {
            val sheet = workbook.getSheetAt(0)
            sheet.createRow(1).apply {
                createCell(0).setCellValue("构件编号")
                createCell(1).setCellValue("主梁")
            }
            sheet.createRow(2)
            sheet.createRow(3).apply {
                createCell(0).setCellValue("defectType")
                createCell(1).setCellValue("裂缝")
            }

            val result = ExcelManager.parseDictionarySheet(sheet)

            assertEquals(2, result.size)
            assertEquals(DictionaryCategory.COMPONENT, result[0].category)
            assertEquals("主梁", result[0].label)
            assertEquals(DictionaryCategory.DEFECT_TYPE, result[1].category)
            assertEquals("裂缝", result[1].label)
            assertTrue(result.all { it.isActive && !it.isDefault && it.remark.isEmpty() })
        } finally {
            workbook.close()
        }
    }

    @Test(expected = InvalidDictionaryExcelException::class)
    fun parseDictionarySheet_rejectsExtraColumns() {
        val workbook = ExcelManager.buildDictionaryTemplateWorkbook()
        try {
            val sheet = workbook.getSheetAt(0)
            sheet.getRow(0).createCell(2).setCellValue("remark")
            ExcelManager.parseDictionarySheet(sheet)
        } finally {
            workbook.close()
        }
    }

    @Test
    fun buildDictionaryTemplateWorkbook_createsTemplateAndGuideSheets() {
        val workbook = ExcelManager.buildDictionaryTemplateWorkbook()
        try {
            assertEquals(2, workbook.numberOfSheets)
            assertEquals("dictionary", workbook.getSheetAt(0).sheetName)
            assertEquals("category", workbook.getSheetAt(0).getRow(0).getCell(0).stringCellValue)
            assertEquals("label", workbook.getSheetAt(0).getRow(0).getCell(1).stringCellValue)
            assertEquals("说明", workbook.getSheetAt(1).sheetName)
            assertEquals("构件编号", workbook.getSheetAt(1).getRow(2).getCell(0).stringCellValue)
            assertEquals(DictionaryCategory.COMPONENT, workbook.getSheetAt(1).getRow(2).getCell(1).stringCellValue)
        } finally {
            workbook.close()
        }
    }
}
