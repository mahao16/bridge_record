package com.record.bridge.domain

data class CompCodeParts(
    val prefix: String,
    val numSegments: List<String>
)

fun buildCompFullCode(prefix: String, numSegments: List<String>): String {
    val p = prefix.trim()
    val nums = numSegments.map { it.trim() }.filter { it.isNotEmpty() }
    if (p.isEmpty()) return nums.joinToString("-")
    if (nums.isEmpty()) return p
    return p + "-" + nums.joinToString("-")
}

fun parseCompFullCode(raw: String): CompCodeParts {
    val s = raw.trim()
    if (s.isEmpty()) return CompCodeParts(prefix = "", numSegments = emptyList())
    val parts = s.split('-').map { it.trim() }.filter { it.isNotEmpty() }
    if (parts.isEmpty()) return CompCodeParts(prefix = "", numSegments = emptyList())
    val prefix = parts.first()
    val nums = parts.drop(1).take(4).map { it.filter { ch -> ch.isDigit() } }
    return CompCodeParts(prefix = prefix, numSegments = nums)
}

