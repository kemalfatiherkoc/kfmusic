package com.example.kfmusic.db;

import androidx.room.Entity;

@Entity(primaryKeys = {"playlistId", "songId"}, tableName = "playlist_song_join")
public class PlaylistSongCrossRef {
    public long playlistId;
    public long songId;

    public PlaylistSongCrossRef(long playlistId, long songId) {
        this.playlistId = playlistId;
        this.songId = songId;
    }
}
