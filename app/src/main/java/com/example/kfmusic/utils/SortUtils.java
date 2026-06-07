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
                return o1.getTitle().compareToIgnoreCase(o2.getTitle());
            }
        });
    }

    public static void sortByArtist(List<Song> songs) {
        Collections.sort(songs, new Comparator<Song>() {
            @Override
            public int compare(Song o1, Song o2) {
                return o1.getArtist().compareToIgnoreCase(o2.getArtist());
            }
        });
    }

    public static void sortByAlbum(List<Song> songs) {
        Collections.sort(songs, new Comparator<Song>() {
            @Override
            public int compare(Song o1, Song o2) {
                return o1.getAlbum().compareToIgnoreCase(o2.getAlbum());
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
