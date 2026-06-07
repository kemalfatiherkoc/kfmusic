package com.example.kfmusic.db;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "queue_items")
public class QueueItemEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public long songId;
    public int position;

    public QueueItemEntity() {}

    @Ignore
    public QueueItemEntity(long songId, int position) {
        this.songId = songId;
        this.position = position;
    }
}
