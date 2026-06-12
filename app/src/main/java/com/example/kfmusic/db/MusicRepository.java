package com.example.kfmusic.db;

import android.content.Context;

import com.example.kfmusic.model.Song;

import java.util.ArrayList;
import java.util.List;

public class MusicRepository {
    private final SongDao songDao;
    private final PlaylistDao playlistDao;
    private final QueueDao queueDao;
    public MusicRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.songDao = db.songDao();
        this.playlistDao = db.playlistDao();
        this.queueDao = db.queueDao();
    }

    @androidx.room.Transaction
    public void saveQueue(List<Song> songs) {
        queueDao.clearQueue();
        List<QueueItemEntity> entities = new ArrayList<>();
        for (int i = 0; i < songs.size(); i++) {
            entities.add(new QueueItemEntity(songs.get(i).getId(), i));
        }
        queueDao.insertQueueItems(entities);
    }

    public List<Long> getSavedQueueSongIds() {
        List<QueueItemEntity> entities = queueDao.getQueueItems();
        List<Long> ids = new ArrayList<>();
        for (QueueItemEntity entity : entities) {
            ids.add(entity.songId);
        }
        return ids;
    }

    private static List<Song> songsCache = null;

    private void invalidateCache() {
        synchronized (MusicRepository.class) {
            songsCache = null;
        }
    }

    public void cacheSongs(List<Song> songs) {
        synchronized (MusicRepository.class) {
        List<SongEntity> entities = new ArrayList<>();
        for (Song song : songs) {
            SongEntity entity = new SongEntity(song);
            SongEntity existing = songDao.getSongById(song.getId());
            if (existing != null) {
                entity.isFavorite = existing.isFavorite;
                entity.playCount = existing.playCount;
                entity.lastPlayed = existing.lastPlayed;
            }
            entities.add(entity);
        }
        songDao.insertSongs(entities);
            invalidateCache();
        }
    }

    public List<Song> getCachedSongs() {
        synchronized (MusicRepository.class) {
            if (songsCache == null) {
                List<SongEntity> entities = songDao.getAllSongs();
                songsCache = new ArrayList<>();
                for (SongEntity entity : entities) {
                    songsCache.add(entity.toSong());
                }
            }
            return new ArrayList<>(songsCache);
        }
    }

    public void updateFavorite(long songId, boolean isFavorite) {
        synchronized (MusicRepository.class) {
            songDao.updateFavoriteStatus(songId, isFavorite);
            invalidateCache();
        }
    }

    public boolean isFavorite(long songId) {
        Boolean result = songDao.isFavorite(songId);
        return result != null && result;
    }

    public List<Song> getFavoriteSongs() {
        List<SongEntity> entities = songDao.getFavoriteSongs();
        List<Song> songs = new ArrayList<>();
        for (SongEntity entity : entities) {
            songs.add(entity.toSong());
        }
        return songs;
    }

    public void syncFavorites(List<Long> favoriteSongIds) {
        synchronized (MusicRepository.class) {
            for (SongEntity entity : songDao.getAllSongs()) {
                boolean isFav = favoriteSongIds.contains(entity.id);
                if (entity.isFavorite != isFav) {
                    songDao.updateFavoriteStatus(entity.id, isFav);
                }
            }
            invalidateCache();
        }
    }

    @androidx.room.Transaction
    public void deleteSong(long songId) {
        songDao.deleteSongById(songId);
        playlistDao.removeSongFromAllPlaylists(songId);
        queueDao.removeSongFromQueue(songId);
        synchronized (MusicRepository.class) {
            invalidateCache();
        }
    }

    public void validateAndCleanSongs(Context context) {
        List<SongEntity> entities = songDao.getAllSongs();
        List<SongEntity> toDelete = new ArrayList<>();
        for (SongEntity entity : entities) {
            if (entity.filePath != null) {
                if (entity.filePath.startsWith("content://")) {
                    try {
                        android.net.Uri uri = android.net.Uri.parse(entity.filePath);
                        try (android.os.ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r")) {
                            if (pfd == null) {
                                toDelete.add(entity);
                            }
                        }
                    } catch (Exception e) {
                        toDelete.add(entity);
                    }
                } else if (entity.filePath.startsWith("android.resource://")) {
                    // Resource is fine
                } else {
                    java.io.File file = new java.io.File(entity.filePath);
                    if (!file.exists()) {
                        toDelete.add(entity);
                    }
                }
            } else {
                toDelete.add(entity);
            }
        }
        if (!toDelete.isEmpty()) {
            for (SongEntity entity : toDelete) {
                deleteSong(entity.id);
            }
            synchronized (MusicRepository.class) {
                invalidateCache();
            }
        }
    }

    public void purgeOrphanPlaylistConnections() {
        playlistDao.purgeOrphanCrossRefs();
    }

    public long getTotalLibrarySize() {
        return songDao.getTotalLibrarySize();
    }

    public String getTotalLibrarySizeDisplay() {
        long bytes = getTotalLibrarySize();
        if (bytes <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(bytes)/Math.log10(1024));
        return new java.text.DecimalFormat("#,##0.#").format(bytes/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    // Playlist Helpers
    public void createPlaylist(String name) {
        PlaylistEntity entity = new PlaylistEntity(name, System.currentTimeMillis());
        playlistDao.insertPlaylist(entity);
    }

    public List<PlaylistEntity> getPlaylists() {
        return playlistDao.getAllPlaylists();
    }

    public void addSongToPlaylist(String playlistName, long songId) {
        PlaylistEntity playlist = playlistDao.getPlaylistByName(playlistName);
        if (playlist != null) {
            playlistDao.addSongToPlaylist(new PlaylistSongCrossRef(playlist.id, songId));
        }
    }

    public List<Song> getSongsInPlaylist(long playlistId) {
        return getSongsInPlaylist(playlistId, "title");
    }

    public List<Song> getSongsInPlaylist(long playlistId, String sortBy) {
        List<SongEntity> entities;
        if ("date_added".equalsIgnoreCase(sortBy)) {
            entities = playlistDao.getSongsInPlaylistSortedDateAdded(playlistId);
        } else {
            entities = playlistDao.getSongsInPlaylistSortedAlphabetical(playlistId);
        }
        List<Song> songs = new ArrayList<>();
        for (SongEntity entity : entities) {
            songs.add(entity.toSong());
        }
        return songs;
    }

    public void renamePlaylist(String oldName, String newName) {
        PlaylistEntity playlist = playlistDao.getPlaylistByName(oldName);
        if (playlist != null) {
            playlistDao.updatePlaylistName(playlist.id, newName);
        }
    }

    public void deletePlaylist(String name) {
        PlaylistEntity playlist = playlistDao.getPlaylistByName(name);
        if (playlist != null) {
            playlistDao.deletePlaylist(playlist);
        }
    }

    public void removeSongFromPlaylist(String playlistName, long songId) {
        PlaylistEntity playlist = playlistDao.getPlaylistByName(playlistName);
        if (playlist != null) {
            playlistDao.removeSongFromPlaylist(playlist.id, songId);
        }
    }

    public void incrementPlayCount(long songId) {
        synchronized (MusicRepository.class) {
            songDao.incrementPlayCount(songId, System.currentTimeMillis());
            invalidateCache();
        }
    }

    public List<Song> getMostPlayedSongs() {
        List<SongEntity> entities = songDao.getMostPlayedSongs();
        List<Song> songs = new ArrayList<>();
        for (SongEntity entity : entities) {
            songs.add(entity.toSong());
        }
        return songs;
    }

    public List<Song> getRecentlyPlayedSongs() {
        List<SongEntity> entities = songDao.getRecentlyPlayedSongs();
        List<Song> songs = new ArrayList<>();
        for (SongEntity entity : entities) {
            songs.add(entity.toSong());
        }
        return songs;
    }

    public String exportDatabaseToJson() {
        try {
            org.json.JSONObject backup = new org.json.JSONObject();
            
            org.json.JSONArray songsArray = new org.json.JSONArray();
            for (SongEntity s : songDao.getAllSongs()) {
                org.json.JSONObject sj = new org.json.JSONObject();
                sj.put("id", s.id);
                sj.put("title", s.title);
                sj.put("artist", s.artist);
                sj.put("album", s.album);
                sj.put("duration", s.duration);
                sj.put("filePath", s.filePath);
                sj.put("isFavorite", s.isFavorite);
                sj.put("dateAdded", s.dateAdded);
                sj.put("albumId", s.albumId);
                sj.put("trackNumber", s.trackNumber);
                sj.put("year", s.year);
                sj.put("composer", s.composer);
                sj.put("genre", s.genre);
                sj.put("fileSize", s.fileSize);
                sj.put("playCount", s.playCount);
                sj.put("lastPlayed", s.lastPlayed);
                sj.put("coverUrl", s.coverUrl);
                songsArray.put(sj);
            }
            backup.put("songs", songsArray);

            org.json.JSONArray playlistsArray = new org.json.JSONArray();
            for (PlaylistEntity p : playlistDao.getAllPlaylists()) {
                org.json.JSONObject pj = new org.json.JSONObject();
                pj.put("id", p.id);
                pj.put("name", p.name);
                pj.put("dateCreated", p.dateCreated);
                
                org.json.JSONArray refsArray = new org.json.JSONArray();
                for (SongEntity s : playlistDao.getSongsInPlaylist(p.id)) {
                    refsArray.put(s.id);
                }
                pj.put("songIds", refsArray);
                playlistsArray.put(pj);
            }
            backup.put("playlists", playlistsArray);

            return backup.toString(4);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @androidx.room.Transaction
    public boolean importDatabaseFromJson(String jsonStr) {
        try {
            org.json.JSONObject backup = new org.json.JSONObject(jsonStr);
            
            org.json.JSONArray songsArray = backup.optJSONArray("songs");
            if (songsArray != null) {
                List<SongEntity> songs = new ArrayList<>();
                for (int i = 0; i < songsArray.length(); i++) {
                    org.json.JSONObject sj = songsArray.getJSONObject(i);
                    SongEntity s = new SongEntity();
                    s.id = sj.getLong("id");
                    s.title = sj.getString("title");
                    s.artist = sj.getString("artist");
                    s.album = sj.getString("album");
                    s.duration = sj.getLong("duration");
                    s.filePath = sj.getString("filePath");
                    s.isFavorite = sj.getBoolean("isFavorite");
                    s.dateAdded = sj.optLong("dateAdded", 0);
                    s.albumId = sj.optLong("albumId", 0);
                    s.trackNumber = sj.optInt("trackNumber", 0);
                    s.year = sj.optInt("year", 0);
                    s.composer = sj.optString("composer", "");
                    s.genre = sj.optString("genre", "");
                    s.fileSize = sj.optLong("fileSize", 0);
                    s.playCount = sj.optInt("playCount", 0);
                    s.lastPlayed = sj.optLong("lastPlayed", 0);
                    s.coverUrl = sj.optString("coverUrl", "");
                    songs.add(s);
                }
                if (!songs.isEmpty()) {
                    songDao.clearAllSongs();
                    songDao.insertSongs(songs);
                }
            }

            org.json.JSONArray playlistsArray = backup.optJSONArray("playlists");
            if (playlistsArray != null) {
                playlistDao.clearAllPlaylists();
                playlistDao.clearAllCrossRefs();

                for (int i = 0; i < playlistsArray.length(); i++) {
                    org.json.JSONObject pj = playlistsArray.getJSONObject(i);
                    PlaylistEntity p = new PlaylistEntity(pj.getString("name"), pj.optLong("dateCreated", System.currentTimeMillis()));
                    long newId = playlistDao.insertPlaylist(p);
                    
                    org.json.JSONArray songIds = pj.optJSONArray("songIds");
                    if (songIds != null) {
                        for (int j = 0; j < songIds.length(); j++) {
                            playlistDao.addSongToPlaylist(new PlaylistSongCrossRef(newId, songIds.getLong(j)));
                        }
                    }
                }
            }
            invalidateCache();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
