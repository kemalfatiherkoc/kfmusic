package com.example.kfmusic.model;

public class Playlist {
    private long id;
    private String name;
    private long dateCreated;
    private int trackCount;

    public Playlist(long id, String name, long dateCreated, int trackCount) {
        this.id = id;
        this.name = name != null && !name.trim().isEmpty() ? name : "Unnamed Playlist";
        this.dateCreated = dateCreated;
        this.trackCount = trackCount;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getDateCreated() {
        return dateCreated;
    }

    public int getTrackCount() {
        return trackCount;
    }

    public void setTrackCount(int trackCount) {
        this.trackCount = trackCount;
    }
}
