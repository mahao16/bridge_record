package com.record.bridge.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        BridgeDefectRecordEntity::class,
        DictionaryEntity::class,
        SiteLogEntity::class,
        ProjectEntity::class,
        ProjectRecordEntity::class,
        LocationDraftEntity::class
    ],
    version = 12,
    exportSchema = false
)
abstract class BridgeDatabase : RoomDatabase() {
    abstract fun recordDao(): BridgeDefectRecordDao
    abstract fun dictionaryDao(): DictionaryDao
    abstract fun siteLogDao(): SiteLogDao
    abstract fun projectDao(): ProjectDao
    abstract fun projectRecordDao(): ProjectRecordDao
    abstract fun locationDraftDao(): LocationDraftDao
}
