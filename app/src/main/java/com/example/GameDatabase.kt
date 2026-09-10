package com.example

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Entity(tableName = "save_data")
data class SaveData(
    @PrimaryKey val id: Int = 0,
    val posX: Float,
    val posY: Float,
    val posZ: Float,
    val rotY: Float,
    val hour: Int,
    val minute: Int,
    val stamina: Float,
    val isTentDeployed: Boolean
)

@Dao
interface SaveDao {
    @Query("SELECT * FROM save_data WHERE id = 0")
    suspend fun getSaveData(): SaveData?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaveData(saveData: SaveData)
}

@Database(entities = [SaveData::class], version = 1)
abstract class GameDatabase : RoomDatabase() {
    abstract fun saveDao(): SaveDao
}
