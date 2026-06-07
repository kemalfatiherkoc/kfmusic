package com.example.kfmusic.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.kfmusic.model.Song;

@Entity(tableName = "songs", indices = {
    @androidx.room.Index(value = {"title"}),
    @androidx.room.Index(value = {"artist"}),
    @androidx.room.Index(value = {"album"})
})
public class SongEntity {
    @PrimaryKey
    public long id;
    
    public String title;
    public String artist;
    public String album;
    public long duration;
    public String filePath;
    public boolean isFavorite;
    
    // Extended Metadata
    public long dateAdded;
    public long albumId;
    public int trackNumber;
    public int year;
    public String composer;
    public String genre;
    public long fileSize;
    public int playCount;
    public long lastPlayed;
    public String coverUrl;

    public SongEntity() {}

    public SongEntity(Song song) {
        this.id = song.getId();
        this.title = song.getTitle();
        this.artist = song.getArtist();
        this.album = song.getAlbum();
        this.duration = song.getDuration();
        this.filePath = song.getFilePath();
        this.isFavorite = song.isFavorite();
        
        this.dateAdded = song.getDateAdded();
        this.albumId = song.getAlbumId();
        this.trackNumber = song.getTrackNumber();
        this.year = song.getYear();
        this.composer = song.getComposer();
        this.genre = song.getGenre();
        this.fileSize = song.getFileSize();
        this.playCount = song.getPlayCount();
        this.lastPlayed = song.getLastPlayed();
        this.coverUrl = song.getCoverUrl();
    }

    public Song toSong() {
        Song song = new Song(id, title, artist, album, duration, filePath);
        song.setFavorite(isFavorite);
        song.setDateAdded(dateAdded);
        song.setAlbumId(albumId);
        song.setTrackNumber(trackNumber);
        song.setYear(year);
        song.setComposer(composer);
        song.setGenre(genre);
        song.setFileSize(fileSize);
        song.setPlayCount(playCount);
        song.setLastPlayed(lastPlayed);
        song.setCoverUrl(coverUrl);
        return song;
    }
}
