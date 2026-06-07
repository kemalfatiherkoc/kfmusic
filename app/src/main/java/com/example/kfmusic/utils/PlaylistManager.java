package com.example.kfmusic.utils;

import android.content.Context;
import com.example.kfmusic.db.MusicRepository;
import com.example.kfmusic.db.PlaylistEntity;
import com.example.kfmusic.model.Song;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlaylistManager {
    private static PlaylistManager instance;
    private final MusicRepository repository;

    private PlaylistManager(Context context) {
        this.repository = new MusicRepository(context);
    }

    public static synchronized PlaylistManager getInstance(Context context) {
        if (instance == null) {
            instance = new PlaylistManager(context);
        }
        return instance;
    }

    public List<String> getPlaylists() {
        List<PlaylistEntity> entities = repository.getPlaylists();
        List<String> names = new ArrayList<>();
        for (PlaylistEntity entity : entities) {
            names.add(entity.name);
        }
        return names;
    }

    public void createPlaylist(String name) {
        if (name == null || name.trim().isEmpty()) return;
        String trimmed = name.trim();
        List<String> current = getPlaylists();
        if (!current.contains(trimmed)) {
            repository.createPlaylist(trimmed);
        }
    }

    public void addSongToPlaylist(String playlistName, long songId) {
        repository.addSongToPlaylist(playlistName, songId);
    }

    public Set<String> getSongsInPlaylist(String playlistName) {
        List<PlaylistEntity> entities = repository.getPlaylists();
        long playlistId = -1;
        for (PlaylistEntity entity : entities) {
            if (entity.name.equalsIgnoreCase(playlistName)) {
                playlistId = entity.id;
                break;
            }
        }
        Set<String> songIds = new HashSet<>();
        if (playlistId != -1) {
            List<Song> songs = repository.getSongsInPlaylist(playlistId);
            for (Song song : songs) {
                songIds.add(String.valueOf(song.getId()));
            }
        }
        return songIds;
    }

    public List<Song> getSongsInPlaylistSorted(String playlistName, String sortBy) {
        List<PlaylistEntity> entities = repository.getPlaylists();
        long playlistId = -1;
        for (PlaylistEntity entity : entities) {
            if (entity.name.equalsIgnoreCase(playlistName)) {
                playlistId = entity.id;
                break;
            }
        }
        if (playlistId != -1) {
            return repository.getSongsInPlaylist(playlistId, sortBy);
        }
        return new ArrayList<>();
    }

    public void renamePlaylist(String oldName, String newName) {
        if (oldName != null && newName != null && !newName.trim().isEmpty()) {
            repository.renamePlaylist(oldName.trim(), newName.trim());
        }
    }

    public void deletePlaylist(String name) {
        if (name != null) {
            repository.deletePlaylist(name.trim());
        }
    }

    public void removeSongFromPlaylist(String playlistName, long songId) {
        if (playlistName != null) {
            repository.removeSongFromPlaylist(playlistName.trim(), songId);
        }
    }
}
