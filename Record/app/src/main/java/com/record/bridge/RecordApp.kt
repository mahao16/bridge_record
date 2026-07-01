package com.record.bridge

import android.app.Application
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.record.bridge.data.BridgeDatabase
import com.record.bridge.data.DictionaryCategory
import com.record.bridge.data.DictionaryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class RecordApp : Application() {
    lateinit var db: BridgeDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(this, BridgeDatabase::class.java, "bridge_record.db")
            // WARNING: 破坏性迁移会在数据库版本升级时删除所有用户数据
            // 正式发布前应替换为显式 Migration 以保留用户数据
            .fallbackToDestructiveMigration()
            .addCallback(
                object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        prefillDictionary(db)
                    }
                }
            )
            .build()
        CoroutineScope(Dispatchers.IO).launch {
            syncDictionaryDefaults(db)
            val legacyRows = db.siteLogDao().listByProject(0L)
            db.siteLogDao().deleteByProject(0L)
            legacyRows.forEach { row ->
                runCatching { File(row.photoPath).delete() }
            }
        }
    }
}

private fun prefillDictionary(db: SupportSQLiteDatabase) {
    fun insert(category: String, label: String, remark: String, isDefault: Boolean) {
        db.execSQL(
            "INSERT OR IGNORE INTO dictionary(category,label,remark,isDefault,isActive) VALUES(?,?,?,?,?)",
            arrayOf(category, label, remark, if (isDefault) 1 else 0, 1)
        )
    }

    defaultDefectTypeLabels.forEach { insert(DictionaryCategory.DEFECT_TYPE, it, "", true) }
    defaultLongRefLabels.forEach { insert(DictionaryCategory.LOCATION_LONG_REF, it, "", true) }
    defaultTransRefLabels.forEach { insert(DictionaryCategory.LOCATION_TRANS_REF, it, "", true) }
}

private suspend fun syncDictionaryDefaults(db: BridgeDatabase) {
    val dao = db.dictionaryDao()
    dao.deleteDefaults(DictionaryCategory.COMPONENT)
    dao.deleteByCategory(DictionaryCategory.COMP_CODE)
    dao.deleteByCategory(DictionaryCategory.DESC_TEMPLATE)
    dao.deleteByCategory(DictionaryCategory.SITE_LOG_WORK)
    dao.deleteByCategory(DictionaryCategory.SITE_LOG_SAFETY)
    dao.deleteDefaultLabelsNotIn(DictionaryCategory.DEFECT_TYPE, defaultDefectTypeLabels)
    dao.deleteDefaultLabelsNotIn(DictionaryCategory.LOCATION_LONG_REF, defaultLongRefLabels)
    dao.deleteDefaultLabelsNotIn(DictionaryCategory.LOCATION_TRANS_REF, defaultTransRefLabels)
    defaultDefectTypeLabels.forEach { label ->
        dao.insert(DictionaryEntity(category = DictionaryCategory.DEFECT_TYPE, label = label, remark = "", isDefault = true, isActive = true))
    }
    defaultLongRefLabels.forEach { label ->
        dao.insert(DictionaryEntity(category = DictionaryCategory.LOCATION_LONG_REF, label = label, remark = "", isDefault = true, isActive = true))
    }
    defaultTransRefLabels.forEach { label ->
        dao.insert(DictionaryEntity(category = DictionaryCategory.LOCATION_TRANS_REF, label = label, remark = "", isDefault = true, isActive = true))
    }
}

private val defaultDefectTypeLabels = listOf(
    "横向裂缝",
    "纵向裂缝",
    "斜向裂缝",
    "网状裂缝",
    "蜂窝",
    "麻面",
    "钢筋锈蚀",
    "混凝土破损",
    "涂层劣化"
)

private val defaultLongRefLabels = listOf(
    "小里程梁端",
    "大里程梁端"
)

private val defaultTransRefLabels = listOf(
    "底板",
    "顶板",
    "左腹板",
    "右腹板"
)
