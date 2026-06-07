package com.example.kfmusic.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface SongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSong(SongEntity song);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSongs(List<SongEntity> songs);

    @Query("SELECT * FROM songs ORDER BY title ASC")
    List<SongEntity> getAllSongs();

    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY title ASC")
    List<SongEntity> getFavoriteSongs();

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :songId")
    void updateFavoriteStatus(long songId, boolean isFavorite);

    @Query("SELECT isFavorite FROM songs WHERE id = :songId")
    boolean isFavorite(long songId);

    @Query("SELECT * FROM songs WHERE id = :songId")
    SongEntity getSongById(long songId);

    @Delete
    void deleteSong(SongEntity song);

    @Query("DELETE FROM songs")
    void clearAllSongs();

    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayed = :timestamp WHERE id = :songId")
    void incrementPlayCount(long songId, long timestamp);

    @Query("SELECT * FROM songs WHERE playCount > 0 ORDER BY playCount DESC LIMIT 50")
    List<SongEntity> getMostPlayedSongs();

    @Query("SELECT * FROM songs WHERE lastPlayed > 0 ORDER BY lastPlayed DESC LIMIT 50")
    List<SongEntity> getRecentlyPlayedSongs();

    @Query("DELETE FROM songs WHERE id = :songId")
    void deleteSongById(long songId);

    @Query("SELECT IFNULL(SUM(fileSize), 0) FROM songs")
    long getTotalLibrarySize();
}
