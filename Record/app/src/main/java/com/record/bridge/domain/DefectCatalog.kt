package com.record.bridge.domain

object DefectCatalog {
    fun isCrackNonMesh(defectType: String): Boolean {
        val t = defectType.trim()
        if (t.isEmpty()) return false
        val isCrack = t.contains("裂缝")
        val isMesh = isMeshCrack(t)
        return isCrack && !isMesh
    }

    fun isMeshCrack(defectType: String): Boolean {
        val t = defectType.trim()
        if (t.isEmpty()) return false
        return t.contains("网状裂缝") || t.contains("龟裂")
    }

    fun needsAreaCalculator(defectType: String): Boolean {
        val t = defectType.trim()
        if (t.isEmpty()) return false
        val isCrack = t.contains("裂缝")
        val isMesh = isMeshCrack(t)
        return isMesh || !isCrack
    }
}
