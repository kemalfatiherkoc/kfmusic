package com.example.kfmusic.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertPlaylist(PlaylistEntity playlist);

    @Delete
    void deletePlaylist(PlaylistEntity playlist);

    @Query("SELECT * FROM playlists ORDER BY name ASC")
    List<PlaylistEntity> getAllPlaylists();

    @Query("SELECT * FROM playlists WHERE name = :name LIMIT 1")
    PlaylistEntity getPlaylistByName(String name);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void addSongToPlaylist(PlaylistSongCrossRef crossRef);

    @Query("DELETE FROM playlist_song_join WHERE playlistId = :playlistId AND songId = :songId")
    void removeSongFromPlaylist(long playlistId, long songId);

    @Query("SELECT songs.* FROM songs INNER JOIN playlist_song_join ON songs.id = playlist_song_join.songId WHERE playlist_song_join.playlistId = :playlistId ORDER BY songs.title ASC")
    List<SongEntity> getSongsInPlaylist(long playlistId);

    @Query("SELECT songs.* FROM songs INNER JOIN playlist_song_join ON songs.id = playlist_song_join.songId WHERE playlist_song_join.playlistId = :playlistId ORDER BY songs.title ASC")
    List<SongEntity> getSongsInPlaylistSortedAlphabetical(long playlistId);

    @Query("SELECT songs.* FROM songs INNER JOIN playlist_song_join ON songs.id = playlist_song_join.songId WHERE playlist_song_join.playlistId = :playlistId ORDER BY songs.dateAdded DESC")
    List<SongEntity> getSongsInPlaylistSortedDateAdded(long playlistId);

    @Query("DELETE FROM playlist_song_join WHERE songId = :songId")
    void removeSongFromAllPlaylists(long songId);

    @Query("DELETE FROM playlist_song_join WHERE playlistId NOT IN (SELECT id FROM playlists) OR songId NOT IN (SELECT id FROM songs)")
    void purgeOrphanCrossRefs();

    @Query("DELETE FROM playlists")
    void clearAllPlaylists();

    @Query("DELETE FROM playlist_song_join")
    void clearAllCrossRefs();

    @Query("UPDATE playlists SET name = :newName WHERE id = :id")
    void updatePlaylistName(long id, String newName);
}
