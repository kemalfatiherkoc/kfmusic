package com.example.kfmusic.utils;

import com.example.kfmusic.model.Song;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SortUtils {

    public static void sortByTitle(List<Song> songs) {
        Collections.sort(songs, new Comparator<Song>() {
            @Override
            public int compare(Song o1, Song o2) {
                String title1 = o1.getTitle() != null ? o1.getTitle() : "";
                String title2 = o2.getTitle() != null ? o2.getTitle() : "";
                return title1.compareToIgnoreCase(title2);
            }
        });
    }

    public static void sortByArtist(List<Song> songs) {
        Collections.sort(songs, new Comparator<Song>() {
            @Override
            public int compare(Song o1, Song o2) {
                String artist1 = o1.getArtist() != null ? o1.getArtist() : "";
                String artist2 = o2.getArtist() != null ? o2.getArtist() : "";
                return artist1.compareToIgnoreCase(artist2);
            }
        });
    }

    public static void sortByAlbum(List<Song> songs) {
        Collections.sort(songs, new Comparator<Song>() {
            @Override
            public int compare(Song o1, Song o2) {
                String album1 = o1.getAlbum() != null ? o1.getAlbum() : "";
                String album2 = o2.getAlbum() != null ? o2.getAlbum() : "";
                return album1.compareToIgnoreCase(album2);
            }
        });
    }

    public static void sortByDuration(List<Song> songs) {
        Collections.sort(songs, new Comparator<Song>() {
            @Override
            public int compare(Song o1, Song o2) {
                return Long.compare(o1.getDuration(), o2.getDuration());
            }
        });
    }

    public static void sortByDateAdded(List<Song> songs) {
        Collections.sort(songs, new Comparator<Song>() {
            @Override
            public int compare(Song o1, Song o2) {
                return Long.compare(o2.getDateAdded(), o1.getDateAdded()); // Descending (recent first)
            }
        });
    }
}
