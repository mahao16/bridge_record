package com.record.bridge.export

import com.record.bridge.data.BridgeDefectRecordEntity
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExcelExporterTest {
    @Test
    fun buildWorkbook_writesHeadersAndRowsInExpectedColumns() {
        val r = BridgeDefectRecordEntity(
            projectId = 1L,
            componentNo = "C1",
            compFullCode = "ZZ-1-2",
            defectType = "裂缝",
            defectLocation = "距跨中1m，距底板2m",
            quantitativeDesc = "n=1条，L=2m",
            photoIds = "3,1,1,2"
        )

        val wb = ExcelExporter.buildWorkbook(listOf(r))
        try {
            val sheet = wb.getSheetAt(0)
            val header = sheet.getRow(0)
            assertEquals("构件编号", header.getCell(0).stringCellValue)
            assertEquals("病害类型", header.getCell(1).stringCellValue)
            assertEquals("病害位置", header.getCell(2).stringCellValue)
            assertEquals("病害定量描述", header.getCell(3).stringCellValue)
            assertEquals("照片编号", header.getCell(4).stringCellValue)

            val row = sheet.getRow(1)
            assertEquals("ZZ-1-2", row.getCell(0).stringCellValue)
            assertEquals("裂缝", row.getCell(1).stringCellValue)
            assertEquals("距跨中1m，距底板2m", row.getCell(2).stringCellValue)
            assertEquals("n=1条，L=2m", row.getCell(3).stringCellValue)
            assertEquals("0001, 0002, 0003", row.getCell(4).stringCellValue)

            val headerStyle = header.getCell(0).cellStyle
            assertEquals(BorderStyle.THIN, headerStyle.borderTop)
            assertEquals(BorderStyle.THIN, headerStyle.borderBottom)
            assertEquals(HorizontalAlignment.CENTER, headerStyle.alignment)

            val font = wb.getFontAt(headerStyle.fontIndex)
            assertTrue(font.bold)
        } finally {
            wb.close()
        }
    }

    @Test
    fun buildWorkbook_nonNumericPhotoIds_takesLast4Chars() {
        val r = BridgeDefectRecordEntity(
            projectId = 1L,
            componentNo = "C1",
            compFullCode = "ZZ-1-2",
            defectType = "裂缝",
            defectLocation = "跨中",
            quantitativeDesc = "n=1条",
            photoIds = "ZZ_20260510095801,ZZ_20260510095802"
        )

        val wb = ExcelExporter.buildWorkbook(listOf(r))
        try {
            val sheet = wb.getSheetAt(0)
            val row = sheet.getRow(1)
            assertEquals("5801, 5802", row.getCell(4).stringCellValue)
        } finally {
            wb.close()
        }
    }

    @Test
    fun buildWorkbook_mixedPhotoIds_formatsAllCorrectly() {
        val r = BridgeDefectRecordEntity(
            projectId = 1L,
            componentNo = "C1",
            compFullCode = "ZZ-1-2",
            defectType = "裂缝",
            defectLocation = "跨中",
            quantitativeDesc = "n=1条",
            photoIds = "3,ZZ_20260510095801"
        )

        val wb = ExcelExporter.buildWorkbook(listOf(r))
        try {
            val sheet = wb.getSheetAt(0)
            val row = sheet.getRow(1)
            assertEquals("0003, 5801", row.getCell(4).stringCellValue)
        } finally {
            wb.close()
        }
    }
}

