package com.multibrowserbox.data.local

import androidx.room.*
import java.util.*

@Entity(tableName = "browsing_history")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: String,
    val url: String,
    val title: String,
    val visitCount: Int = 1,
    val lastVisit: Long = System.currentTimeMillis(),
    val favicon: String? = null
)

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: String,
    val url: String,
    val title: String,
    val folder: String = "default",
    val addedDate: Long = System.currentTimeMillis(),
    val favicon: String? = null
)

@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val profileId: String,
    val url: String,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val downloadedSize: Long = 0,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val mimeType: String? = null
)

enum class DownloadStatus {
    PENDING, DOWNLOADING, COMPLETED, FAILED, CANCELLED
}

@Dao
interface ProfileDao {
    @Insert
    suspend fun insertHistory(entry: HistoryEntry): Long
    
    @Query("SELECT * FROM browsing_history WHERE profileId = :profileId ORDER BY lastVisit DESC LIMIT 100")
    suspend fun getHistory(profileId: String): List<HistoryEntry>
    
    @Query("DELETE FROM browsing_history WHERE profileId = :profileId")
    suspend fun clearHistory(profileId: String)
    
    @Insert
    suspend fun insertBookmark(bookmark: Bookmark): Long
    
    @Query("SELECT * FROM bookmarks WHERE profileId = :profileId ORDER BY addedDate DESC")
    suspend fun getBookmarks(profileId: String): List<Bookmark>
    
    @Delete
    suspend fun deleteBookmark(bookmark: Bookmark)
    
    @Insert
    suspend fun insertDownload(download: DownloadItem)
    
    @Query("SELECT * FROM downloads WHERE profileId = :profileId ORDER BY startTime DESC")
    suspend fun getDownloads(profileId: String): List<DownloadItem>
    
    @Update
    suspend fun updateDownload(download: DownloadItem)
}

@Database(
    entities = [HistoryEntry::class, Bookmark::class, DownloadItem::class],
    version = 1,
    exportSchema = false
)
abstract class ProfileDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    
    companion object {
        @Volatile
        private var INSTANCE: ProfileDatabase? = null
        
        fun getDatabase(context: Context, profileId: String): ProfileDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ProfileDatabase::class.java,
                    "profile_${profileId}_db"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
