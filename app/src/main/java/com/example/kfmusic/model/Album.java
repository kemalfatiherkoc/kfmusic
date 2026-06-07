package com.example.kfmusic.model;

public class Album {
    private long id;
    private String title;
    private String artist;
    private int trackCount;
    private int year;

    public Album(long id, String title, String artist, int trackCount, int year) {
        this.id = id;
        this.title = title != null && !title.trim().isEmpty() ? title : "Unknown Album";
        this.artist = artist != null && !artist.trim().isEmpty() ? artist : "Unknown Artist";
        this.trackCount = trackCount;
        this.year = year;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public int getTrackCount() {
        return trackCount;
    }

    public void setTrackCount(int trackCount) {
        this.trackCount = trackCount;
    }

    public int getYear() {
        return year;
    }
}
