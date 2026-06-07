package com.example.kfmusic.model;

public class Artist {
    private long id;
    private String name;
    private int trackCount;

    public Artist(long id, String name, int trackCount) {
        this.id = id;
        this.name = name != null && !name.trim().isEmpty() ? name : "Unknown Artist";
        this.trackCount = trackCount;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getTrackCount() {
        return trackCount;
    }

    public void setTrackCount(int trackCount) {
        this.trackCount = trackCount;
    }
}
