package com.example.kfmusic.utils;

import android.content.Context;
import com.example.kfmusic.db.MusicRepository;

public class FavoritesManager {
    private static FavoritesManager instance;
    private final MusicRepository repository;

    private FavoritesManager(Context context) {
        this.repository = new MusicRepository(context);
    }

    public static synchronized FavoritesManager getInstance(Context context) {
        if (instance == null) {
            instance = new FavoritesManager(context);
        }
        return instance;
    }

    public boolean isFavorite(long songId) {
        return repository.isFavorite(songId);
    }

    public void toggleFavorite(long songId) {
        synchronized (this) {
            boolean fav = isFavorite(songId);
            repository.updateFavorite(songId, !fav);
        }
    }

    public java.util.List<com.example.kfmusic.model.Song> getFavoriteSongs() {
        return repository.getFavoriteSongs();
    }
}
