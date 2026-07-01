package com.record.bridge.export

import android.content.Context
import androidx.core.content.FileProvider
import com.record.bridge.data.BridgeDefectRecordEntity
import com.record.bridge.data.SiteLogEntity
import org.apache.poi.xwpf.usermodel.ParagraphAlignment
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ProjectReportWordExporter {
    fun exportProjectReport(
        context: Context,
        projectName: String,
        defects: List<BridgeDefectRecordEntity>,
        workLogs: List<SiteLogEntity>,
        safetyLogs: List<SiteLogEntity>,
        now: Date = Date()
    ): android.net.Uri {
        val doc = XWPFDocument()
        try {
            writeTitle(doc, "${projectName} 总报告")
            appendDefectSummary(doc, defects)
            appendDefectDetails(doc, defects)
            appendWorkLogs(doc, workLogs)
            appendSafetyLedger(doc, safetyLogs)
            val fileName = buildExportBaseName(projectName, "总报告", now) + ".docx"
            val file = File(File(context.cacheDir, "exports").apply { mkdirs() }, fileName)
            FileOutputStream(file).use { out -> doc.write(out) }
            return FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        } finally {
            runCatching { doc.close() }
        }
    }

    private fun writeTitle(doc: XWPFDocument, text: String) {
        val p = doc.createParagraph().apply { alignment = ParagraphAlignment.CENTER }
        p.createRun().apply {
            fontFamily = "黑体"
            fontSize = 14
            setText(text)
        }
    }

    private fun appendDefectSummary(doc: XWPFDocument, defects: List<BridgeDefectRecordEntity>) {
        val p = doc.createParagraph().apply { alignment = ParagraphAlignment.LEFT }
        p.createRun().apply {
            fontFamily = "黑体"
            fontSize = 12
            setText("一、病害统计")
        }
        val grouped = defects.groupBy { it.defectType }.toList().sortedBy { it.first }
        val table = doc.createTable(grouped.size + 1, 2)
        table.getRow(0).getCell(0).text = "病害类型"
        table.getRow(0).getCell(1).text = "数量"
        grouped.forEachIndexed { index, entry ->
            table.getRow(index + 1).getCell(0).text = entry.first
            table.getRow(index + 1).getCell(1).text = entry.second.size.toString()
        }
    }

    private fun appendDefectDetails(doc: XWPFDocument, defects: List<BridgeDefectRecordEntity>) {
        val p = doc.createParagraph().apply { alignment = ParagraphAlignment.LEFT }
        p.createRun().apply {
            fontFamily = "黑体"
            fontSize = 12
            setText("二、病害明细")
        }
        val table = doc.createTable(defects.size + 1, 5)
        table.getRow(0).getCell(0).text = "构件编号"
        table.getRow(0).getCell(1).text = "病害类型"
        table.getRow(0).getCell(2).text = "病害位置"
        table.getRow(0).getCell(3).text = "定量描述"
        table.getRow(0).getCell(4).text = "照片编号"
        defects.sortedWith(compareBy({ it.componentNo }, { it.defectType }, { it.defectLocation })).forEachIndexed { index, row ->
            val r = table.getRow(index + 1)
            r.getCell(0).text = row.compFullCode.ifBlank { row.componentNo }
            r.getCell(1).text = row.defectType
            r.getCell(2).text = row.defectLocation
            r.getCell(3).text = row.quantitativeDesc
            r.getCell(4).text = row.photoIds
        }
    }

    private fun appendWorkLogs(doc: XWPFDocument, workLogs: List<SiteLogEntity>) {
        val p = doc.createParagraph().apply { alignment = ParagraphAlignment.LEFT }
        p.createRun().apply {
            fontFamily = "黑体"
            fontSize = 12
            setText("三、工作记录")
        }
        if (workLogs.isNotEmpty()) {
            WordExporter.exportSiteLogsToWord(doc, workLogs.sortedBy { it.timestamp })
        }
    }

    private fun appendSafetyLedger(doc: XWPFDocument, safetyLogs: List<SiteLogEntity>) {
        val p = doc.createParagraph().apply { alignment = ParagraphAlignment.LEFT }
        p.createRun().apply {
            fontFamily = "黑体"
            fontSize = 12
            setText("四、安全台账")
        }
        val sorted = safetyLogs.sortedBy { it.timestamp }
        val table = doc.createTable(sorted.size + 1, 4)
        table.getRow(0).getCell(0).text = "序号"
        table.getRow(0).getCell(1).text = "时间"
        table.getRow(0).getCell(2).text = "描述"
        table.getRow(0).getCell(3).text = "图片路径"
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        sorted.forEachIndexed { index, row ->
            val r = table.getRow(index + 1)
            r.getCell(0).text = (index + 1).toString()
            r.getCell(1).text = fmt.format(Date(row.timestamp))
            r.getCell(2).text = row.description
            r.getCell(3).text = row.photoPath
        }
    }
}
