package com.example.kfmusic.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "album_metadata")
public class AlbumMetaEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    @NonNull
    public String albumName;
    public String artistName;
    public int year;

    public AlbumMetaEntity(@NonNull String albumName, String artistName, int year) {
        this.albumName = albumName;
        this.artistName = artistName;
        this.year = year;
    }
}
