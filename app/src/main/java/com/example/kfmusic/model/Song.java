package com.example.kfmusic.model;

public class Song {
    private long id;
    private String title;
    private String artist;
    private String album;
    private long duration; // in milliseconds
    private String filePath;
    private boolean isFavorite;
    
    // Day 1 Extended Metadata
    private long dateAdded;
    private long albumId;
    private int trackNumber;
    private int year;
    private String composer;
    private String genre;
    private long fileSize;
    private int playCount;
    private long lastPlayed;
    private String coverUrl;

    public Song(long id, String title, String artist, String album, long duration, String filePath) {
        this.id = id;
        this.title = title != null && !title.trim().isEmpty() ? title : "Unknown Title";
        this.artist = artist != null && !artist.trim().isEmpty() ? artist : "Unknown Artist";
        this.album = album != null && !album.trim().isEmpty() ? album : "Unknown Album";
        this.duration = duration;
        this.filePath = filePath;
        this.isFavorite = false;
        
        // Defaults
        this.dateAdded = System.currentTimeMillis() / 1000;
        this.albumId = -1;
        this.trackNumber = 0;
        this.year = 0;
        this.composer = "Unknown Composer";
        this.genre = "Unknown Genre";
        this.fileSize = 0;
        this.playCount = 0;
        this.lastPlayed = 0;
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

    public String getAlbum() {
        return album;
    }

    public void setTitle(String title) {
        this.title = title != null && !title.trim().isEmpty() ? title : "Unknown Title";
    }

    public void setArtist(String artist) {
        this.artist = artist != null && !artist.trim().isEmpty() ? artist : "Unknown Artist";
    }

    public void setAlbum(String album) {
        this.album = album != null && !album.trim().isEmpty() ? album : "Unknown Album";
    }

    public long getDuration() {
        return duration;
    }

    public String getFilePath() {
        return filePath;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        this.isFavorite = favorite;
    }

    public long getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(long dateAdded) {
        this.dateAdded = dateAdded;
    }

    public long getAlbumId() {
        return albumId;
    }

    public void setAlbumId(long albumId) {
        this.albumId = albumId;
    }

    public int getTrackNumber() {
        return trackNumber;
    }

    public void setTrackNumber(int trackNumber) {
        this.trackNumber = trackNumber;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getComposer() {
        return composer;
    }

    public void setComposer(String composer) {
        this.composer = composer;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    // Helper to format millisecond duration to MM:SS
    public String getFormattedDuration() {
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    public int getPlayCount() {
        return playCount;
    }

    public void setPlayCount(int playCount) {
        this.playCount = playCount;
    }

    public long getLastPlayed() {
        return lastPlayed;
    }

    public void setLastPlayed(long lastPlayed) {
        this.lastPlayed = lastPlayed;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }
}