package com.record.bridge.export

import android.content.Context
import androidx.core.content.FileProvider
import com.record.bridge.data.SiteLogEntity
import org.apache.poi.openxml4j.exceptions.InvalidFormatException
import org.apache.poi.util.Units
import org.apache.poi.xwpf.usermodel.ParagraphAlignment
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFTable
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import java.io.IOException
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WordExporter {
    private const val PAGE_WIDTH_TWIPS = 11906
    private const val MARGIN_TOP_BOTTOM_TWIPS = 1440
    private const val MARGIN_LEFT_RIGHT_TWIPS = 1080

    @Throws(IOException::class, InvalidFormatException::class)
    fun exportSiteLogsToWord(
        context: Context,
        projectName: String,
        areaName: String,
        logs: List<SiteLogEntity>,
        now: Date = Date()
    ): android.net.Uri {
        val doc = XWPFDocument()
        try {
            prepareDocument(doc)
            exportSiteLogsToWord(doc, logs)
            val fileName = buildExportBaseName(projectName, areaName, now) + ".docx"
            val file = File(File(context.cacheDir, "exports").apply { mkdirs() }, fileName)
            FileOutputStream(file).use { out -> doc.write(out) }
            return FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        } finally {
            runCatching { doc.close() }
        }
    }

    @Throws(IOException::class, InvalidFormatException::class)
    fun exportSiteLogsToWord(document: XWPFDocument, logs: List<SiteLogEntity>) {
        if (document.paragraphs.isEmpty()) {
            prepareDocument(document)
        }
        if (logs.isEmpty()) return
        val rowCount = (logs.size + 1) / 2
        val totalRows = rowCount * 2
        val availableWidth = PAGE_WIDTH_TWIPS - MARGIN_LEFT_RIGHT_TWIPS * 2
        val colWidth = availableWidth / 2

        val table = document.createTable(totalRows, 2)
        table.width = availableWidth
        makeTableBorderInvisible(table)

        for (r in 0 until rowCount) {
            val imageRowIndex = r * 2
            val descRowIndex = imageRowIndex + 1
            val left = logs.getOrNull(r * 2)
            val right = logs.getOrNull(r * 2 + 1)
            applyCellWidth(table, imageRowIndex, 0, colWidth)
            applyCellWidth(table, imageRowIndex, 1, colWidth)
            applyCellWidth(table, descRowIndex, 0, colWidth)
            applyCellWidth(table, descRowIndex, 1, colWidth)
            writeImageToCell(document, table, imageRowIndex, 0, left)
            writeImageToCell(document, table, imageRowIndex, 1, right)
            writeDescriptionToCell(table, descRowIndex, 0, left)
            writeDescriptionToCell(table, descRowIndex, 1, right)
        }
    }

    @Throws(IOException::class, InvalidFormatException::class)
    private fun writeImageToCell(
        document: XWPFDocument,
        table: XWPFTable,
        rowIndex: Int,
        colIndex: Int,
        log: SiteLogEntity?
    ) {
        val cell = table.getRow(rowIndex).getCell(colIndex)
        cell.removeParagraph(0)
        if (log == null) {
            cell.addParagraph()
            return
        }

        val imagePara = cell.addParagraph().apply { alignment = ParagraphAlignment.CENTER }
        val run = imagePara.createRun()
        val file = File(log.photoPath)
        if (file.exists()) {
            val compressed = compressForWord(file)
            compressed.inputStream().use { input ->
                run.addPicture(
                    input,
                    XWPFDocument.PICTURE_TYPE_JPEG,
                    file.nameWithoutExtension + ".jpg",
                    Units.toEMU(198.4),
                    Units.toEMU(148.8)
                )
            }
        } else {
            val lost = cell.addParagraph().apply { alignment = ParagraphAlignment.CENTER }
            lost.createRun().apply {
                color = "FF0000"
                setText("图片已丢失")
            }
        }
    }

    private fun writeDescriptionToCell(
        table: XWPFTable,
        rowIndex: Int,
        colIndex: Int,
        log: SiteLogEntity?
    ) {
        val cell = table.getRow(rowIndex).getCell(colIndex)
        cell.removeParagraph(0)
        val descPara = cell.addParagraph().apply { alignment = ParagraphAlignment.CENTER }
        descPara.createRun().apply {
            fontFamily = "宋体"
            fontSize = 10
            color = "000000"
            setText(log?.description?.takeIf { it.isNotBlank() } ?: "")
        }
        setSingleLineSpacing(descPara)
    }

    private fun applyCellWidth(table: XWPFTable, rowIndex: Int, colIndex: Int, width: Int) {
        table.getRow(rowIndex).getCell(colIndex).ctTc.addNewTcPr().addNewTcW().w = BigInteger.valueOf(width.toLong())
    }

    private fun makeTableBorderInvisible(table: XWPFTable) {
        table.ctTbl.tblPr.tblBorders?.let {
            it.left.sz = BigInteger.ZERO
            it.right.sz = BigInteger.ZERO
            it.top.sz = BigInteger.ZERO
            it.bottom.sz = BigInteger.ZERO
            it.insideH.sz = BigInteger.ZERO
            it.insideV.sz = BigInteger.ZERO
        } ?: run {
            val borders = table.ctTbl.tblPr.addNewTblBorders()
            borders.addNewLeft().sz = BigInteger.ZERO
            borders.addNewRight().sz = BigInteger.ZERO
            borders.addNewTop().sz = BigInteger.ZERO
            borders.addNewBottom().sz = BigInteger.ZERO
            borders.addNewInsideH().sz = BigInteger.ZERO
            borders.addNewInsideV().sz = BigInteger.ZERO
        }
    }

    private fun prepareDocument(document: XWPFDocument) {
        val sectPr = document.document.body.sectPr ?: document.document.body.addNewSectPr()
        val pgMar = sectPr.pgMar ?: sectPr.addNewPgMar()
        pgMar.top = BigInteger.valueOf(MARGIN_TOP_BOTTOM_TWIPS.toLong())
        pgMar.bottom = BigInteger.valueOf(MARGIN_TOP_BOTTOM_TWIPS.toLong())
        pgMar.left = BigInteger.valueOf(MARGIN_LEFT_RIGHT_TWIPS.toLong())
        pgMar.right = BigInteger.valueOf(MARGIN_LEFT_RIGHT_TWIPS.toLong())
        val pgSz = sectPr.pgSz ?: sectPr.addNewPgSz()
        pgSz.w = BigInteger.valueOf(PAGE_WIDTH_TWIPS.toLong())
        pgSz.orient = STPageOrientation.PORTRAIT

        val title = document.createParagraph().apply { alignment = ParagraphAlignment.LEFT }
        title.createRun().apply {
            fontFamily = "黑体"
            fontSize = 10
            setText("附录：现场工作及安全记录")
        }
    }

    private fun setSingleLineSpacing(paragraph: XWPFParagraph) {
        val pPr = paragraph.ctp.pPr ?: paragraph.ctp.addNewPPr()
        val spacing = pPr.spacing ?: pPr.addNewSpacing()
        spacing.lineRule = STLineSpacingRule.AUTO
        spacing.line = BigInteger.valueOf(240L)
    }

    private fun compressForWord(file: File): ByteArray {
        val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = 2 }
        val bmp = android.graphics.BitmapFactory.decodeFile(file.absolutePath, opts)
            ?: return file.readBytes()
        return java.io.ByteArrayOutputStream().use { bos ->
            bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, bos)
            bmp.recycle()
            bos.toByteArray()
        }
    }
}

