package com.planetofoses989.kwideo.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.planetofoses989.kwideo.database.converters.NetworkProtocolConverter
import com.planetofoses989.kwideo.database.dao.NetworkConnectionDao
import com.planetofoses989.kwideo.database.dao.PlaybackStateDao
import com.planetofoses989.kwideo.database.dao.PlaylistDao
import com.planetofoses989.kwideo.database.dao.RecentlyPlayedDao
import com.planetofoses989.kwideo.database.dao.VideoMetadataDao
import com.planetofoses989.kwideo.database.entities.PlaybackStateEntity
import com.planetofoses989.kwideo.database.entities.PlaylistEntity
import com.planetofoses989.kwideo.database.entities.PlaylistItemEntity
import com.planetofoses989.kwideo.database.entities.RecentlyPlayedEntity
import com.planetofoses989.kwideo.database.entities.VideoMetadataEntity
import com.planetofoses989.kwideo.domain.network.NetworkConnection

@Database(
  entities = [
    PlaybackStateEntity::class,
    RecentlyPlayedEntity::class,
    VideoMetadataEntity::class,
    NetworkConnection::class,
    PlaylistEntity::class,
    PlaylistItemEntity::class,
  ],
  version = 8,
  exportSchema = true,
)
@TypeConverters(NetworkProtocolConverter::class)
abstract class MpviumDatabase : RoomDatabase() {
  abstract fun videoDataDao(): PlaybackStateDao

  abstract fun recentlyPlayedDao(): RecentlyPlayedDao

  abstract fun videoMetadataDao(): VideoMetadataDao

  abstract fun networkConnectionDao(): NetworkConnectionDao

  abstract fun playlistDao(): PlaylistDao
}
