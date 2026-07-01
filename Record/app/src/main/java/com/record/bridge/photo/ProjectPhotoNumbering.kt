package com.record.bridge.photo

import android.content.Context

object ProjectPhotoNumbering {
    private const val PREFS_NAME = "project_photo_numbering"

    fun reserveNext(context: Context, projectId: Long): Int? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "next_$projectId"
        val next = prefs.getInt(key, 1).coerceAtLeast(1)
        if (next > 9999) return null
        prefs.edit().putInt(key, next + 1).apply()
        return next
    }

    fun bind(context: Context, projectId: Long, mediaStoreId: Long, seq: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(mapKey(projectId, mediaStoreId), seq).apply()
    }

    fun resolve(context: Context, projectId: Long, mediaStoreId: Long): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val v = prefs.getInt(mapKey(projectId, mediaStoreId), 0)
        if (v <= 0) return null
        return v.toString().padStart(4, '0')
    }

    private fun mapKey(projectId: Long, mediaStoreId: Long): String = "m_${projectId}_$mediaStoreId"
}
