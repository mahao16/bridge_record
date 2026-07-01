package com.record.bridge.export

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun buildExportBaseName(
    projectName: String,
    areaName: String,
    now: Date = Date()
): String {
    val date = SimpleDateFormat("yyyy.M.d", Locale.CHINA).format(now)
    return listOf(
        sanitizeExportSegment(projectName, "项目"),
        sanitizeExportSegment(areaName, "记录"),
        date
    ).joinToString("_")
}

private fun sanitizeExportSegment(raw: String, fallback: String): String {
    val s = raw.trim().ifEmpty { fallback }
    return s.map { ch ->
        if (ch.isLetterOrDigit() || ch == '_' || ch == '-' || ch == ' ' || ch == '.') ch else '-'
    }.joinToString("").trim().ifEmpty { fallback }
}

