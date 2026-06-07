package com.example.kfmusic.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "artist_metadata")
public class ArtistMetaEntity {
    @PrimaryKey
    @NonNull
    public String artistName;
    public String bio;

    public ArtistMetaEntity(@NonNull String artistName, String bio) {
        this.artistName = artistName;
        this.bio = bio;
    }
}
