package com.example.kfmusic.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface QueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertQueueItems(List<QueueItemEntity> items);

    @Query("SELECT * FROM queue_items ORDER BY position ASC")
    List<QueueItemEntity> getQueueItems();

    @Query("DELETE FROM queue_items")
    void clearQueue();

    @Query("DELETE FROM queue_items WHERE songId = :songId")
    void removeSongFromQueue(long songId);
}
