package com.example.kfmusic.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "playlists")
public class PlaylistEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    
    public String name;
    public long dateCreated;

    public PlaylistEntity(String name, long dateCreated) {
        this.name = name;
        this.dateCreated = dateCreated;
    }
}
