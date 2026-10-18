package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "battery_alert_logs")
data class BatteryAlertLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val alertType: String, // "THERMAL", "CHARGE_LIMIT_80", "FAST_CHARGE"
    val temperatureCelsius: Float,
    val batteryLevel: Int,
    val timestampMillis: Long = System.currentTimeMillis()
)

@Dao
interface BatteryAlertLogDao {
    @Query("SELECT * FROM battery_alert_logs ORDER BY timestampMillis DESC LIMIT 50")
    fun observeRecentAlerts(): Flow<List<BatteryAlertLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: BatteryAlertLogEntity)

    @Query("DELETE FROM battery_alert_logs")
    suspend fun clearAllAlerts()
}

@Database(
    entities = [BatteryAlertLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class BatteryCareDatabase : RoomDatabase() {
    abstract fun alertLogDao(): BatteryAlertLogDao

    companion object {
        @Volatile
        private var INSTANCE: BatteryCareDatabase? = null

        fun getInstance(context: Context): BatteryCareDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BatteryCareDatabase::class.java,
                    "battery_care_pro.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
