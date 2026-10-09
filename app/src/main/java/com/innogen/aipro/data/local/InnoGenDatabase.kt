package com.innogen.aipro.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.innogen.aipro.data.local.dao.ProjectDao
import com.innogen.aipro.data.local.entities.ProjectConverters
import com.innogen.aipro.data.local.entities.ProjectEntity

@Database(
    entities  = [ProjectEntity::class],
    version   = 1,
    exportSchema = false
)
@TypeConverters(ProjectConverters::class)
abstract class InnoGenDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
}
