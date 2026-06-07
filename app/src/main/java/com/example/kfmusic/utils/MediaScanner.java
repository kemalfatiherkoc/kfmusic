package com.example.kfmusic.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;

import com.example.kfmusic.db.MusicRepository;
import com.example.kfmusic.model.Song;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MediaScanner {
    private static final String TAG = "MediaScanner";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static List<Song> demoSongsCache;
    private static List<Song> cachedLibrarySongs = new ArrayList<>();

    public interface ScanCallback {
        void onProgress(int progress, int total);
        void onComplete(List<Song> songs);
    }

    public static void scanLocalMusicAsync(Context context, ScanCallback callback) {
        scanAvailableMusicAsync(context, true, callback);
    }

    public static void scanAvailableMusicAsync(Context context, boolean forceLocalScan, ScanCallback callback) {
        Context appContext = context != null ? context.getApplicationContext() : null;
        executor.execute(() -> {
            List<Song> songs = resolveAvailableSongs(appContext, forceLocalScan, callback);
            cacheLibrarySongs(appContext, songs);
            if (callback != null) {
                mainHandler.post(() -> callback.onComplete(new ArrayList<>(songs)));
            }
        });
    }

    public static List<Song> scanLocalMusic(Context context) {
        return scanLocalMusic(context, null);
    }

    public static Song createSongFromUri(Context context, Uri uri) {
        if (context == null || uri == null) return null;

        String uriString = uri.toString();
        long id = 0x4000000000000000L | (uriString.hashCode() & 0xffffffffL);
        String title = queryDisplayName(context, uri);
        long size = queryFileSize(context, uri);
        long duration = 0;
        String artist = null;
        String album = null;
        int year = 0;
        String composer = null;
        String genre = null;

        android.media.MediaMetadataRetriever retriever = null;
        try {
            retriever = new android.media.MediaMetadataRetriever();
            retriever.setDataSource(context, uri);
            String metadataTitle = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE);
            artist = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST);
            album = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM);
            composer = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_COMPOSER);
            genre = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_GENRE);

            String durationText = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (durationText != null && !durationText.trim().isEmpty()) {
                duration = Math.max(0, Long.parseLong(durationText.trim()));
            }

            String yearText = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_YEAR);
            if (yearText != null && !yearText.trim().isEmpty()) {
                year = Integer.parseInt(yearText.trim());
            }

            if (metadataTitle != null && !metadataTitle.trim().isEmpty()) {
                title = metadataTitle;
            }
        } catch (Exception e) {
            Log.w(TAG, "Unable to read selected audio metadata: " + uri, e);
        } finally {
            if (retriever != null) {
                try {
                    retriever.release();
                } catch (Exception ignored) {
                }
            }
        }

        Song song = new Song(id, stripAudioExtension(title), artist, album, duration, uriString);
        song.setDateAdded(System.currentTimeMillis() / 1000);
        song.setFileSize(size);
        song.setYear(year);
        if (composer != null && !composer.trim().isEmpty()) {
            song.setComposer(composer);
        }
        if (genre != null && !genre.trim().isEmpty()) {
            song.setGenre(genre);
        }
        return song;
    }

    public static boolean hasStoragePermission(Context context) {
        if (context == null) return false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_AUDIO)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED;
        }
        return androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    public static List<Song> getCachedLibrarySongs() {
        if (cachedLibrarySongs != null && !cachedLibrarySongs.isEmpty()) {
            return new ArrayList<>(cachedLibrarySongs);
        }
        return getDemoSongs();
    }

    public static boolean isUsingLocalLibrary() {
        return cachedLibrarySongs != null && !cachedLibrarySongs.isEmpty() && !isDemoOnlyCache(cachedLibrarySongs);
    }

    private static List<Song> scanLocalMusic(Context context, ScanCallback callback) {
        List<Song> songs = new ArrayList<>();
        if (context == null || !hasStoragePermission(context)) {
            return songs;
        }

        ContentResolver contentResolver = context.getContentResolver();
        
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.SIZE
        };
        
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        Cursor cursor = null;
        try {
            cursor = contentResolver.query(uri, projection, selection, null, sortOrder);
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                int durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                
                int dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED);
                int albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
                int trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK);
                int yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR);
                int sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);

                int totalCount = cursor.getCount();
                int processed = 0;

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idCol);
                    String title = cursor.getString(titleCol);
                    String artist = cursor.getString(artistCol);
                    String album = cursor.getString(albumCol);
                    long duration = cursor.getLong(durationCol);
                    
                    long dateAdded = cursor.getLong(dateCol);
                    long albumId = cursor.getLong(albumIdCol);
                    int trackNumber = cursor.getInt(trackCol);
                    int year = cursor.getInt(yearCol);
                    long size = cursor.getLong(sizeCol);

                    Uri songUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                    String uriString = songUri.toString();

                    if (uriString.toLowerCase().contains("whatsapp")) {
                        continue;
                    }

                    // Filters: ignore tracks < 10 seconds or < 100 KB
                    if (duration < 10000 || size < 102400) {
                        continue;
                    }

                    Song song = new Song(id, title, artist, album, duration, uriString);
                    
                    // Assign extended properties
                    song.setDateAdded(dateAdded);
                    song.setAlbumId(albumId);
                    song.setTrackNumber(trackNumber);
                    song.setYear(year);
                    song.setFileSize(size);

                    // Task 24: Fallback to MediaMetadataRetriever if basic metadata is missing
                    if ((title == null || title.trim().isEmpty() || "Unknown Title".equals(song.getTitle()) ||
                            artist == null || artist.trim().isEmpty() || "Unknown Artist".equals(song.getArtist()))) {
                        fallbackRetrieveTags(context, songUri, song);
                    }

                    songs.add(song);
                    processed++;

                    if (callback != null && processed % 10 == 0) {
                        final int finalProcessed = processed;
                        mainHandler.post(() -> callback.onProgress(finalProcessed, totalCount));
                    }
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException while scanning local music.", e);
        } catch (Exception e) {
            Log.e(TAG, "Error querying MediaStore database", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return songs;
    }

    private static void fallbackRetrieveTags(Context context, Uri uri, Song song) {
        if (context == null || uri == null) return;
        android.media.MediaMetadataRetriever retriever = null;
        try {
            retriever = new android.media.MediaMetadataRetriever();
            retriever.setDataSource(context, uri);
            
            String title = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE);
            String artist = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String album = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM);
            String composer = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_COMPOSER);
            String genre = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_GENRE);
            String yearStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_YEAR);

            if (title != null && !title.trim().isEmpty() && (song.getTitle() == null || song.getTitle().equals("Unknown Title"))) {
                song.setTitle(title);
            }
            if (artist != null && !artist.trim().isEmpty() && (song.getArtist() == null || song.getArtist().equals("Unknown Artist"))) {
                song.setArtist(artist);
            }
            if (album != null && !album.trim().isEmpty() && (song.getAlbum() == null || song.getAlbum().equals("Unknown Album"))) {
                song.setAlbum(album);
            }
            if (composer != null && !composer.trim().isEmpty()) {
                song.setComposer(composer);
            }
            if (genre != null && !genre.trim().isEmpty()) {
                song.setGenre(genre);
            }
            if (yearStr != null && !yearStr.trim().isEmpty() && song.getYear() <= 0) {
                try {
                    song.setYear(Integer.parseInt(yearStr.trim()));
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to read file tags via MediaMetadataRetriever: " + uri, e);
        } finally {
            if (retriever != null) {
                try {
                    retriever.release();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }

    private static String queryDisplayName(Context context, Uri uri) {
        try (Cursor cursor = context.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (column >= 0) {
                    String displayName = cursor.getString(column);
                    if (displayName != null && !displayName.trim().isEmpty()) {
                        return displayName;
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Unable to query selected audio name: " + uri, e);
        }
        return "Selected Audio";
    }

    private static long queryFileSize(Context context, Uri uri) {
        try (Cursor cursor = context.getContentResolver().query(uri, new String[]{OpenableColumns.SIZE}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (column >= 0 && !cursor.isNull(column)) {
                    return Math.max(0, cursor.getLong(column));
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Unable to query selected audio size: " + uri, e);
        }
        return 0;
    }

    private static String stripAudioExtension(String title) {
        if (title == null) return null;
        return title.replaceFirst("(?i)\\.(mp3|m4a|aac|wav|flac|ogg|opus|webm|mid|midi)$", "");
    }

    private static List<Song> resolveAvailableSongs(Context context, boolean forceLocalScan, ScanCallback callback) {
        if (context == null) {
            return getDemoSongs();
        }

        if (!forceLocalScan) {
            List<Song> cached = getCachedLibrarySongs();
            if (!cached.isEmpty()) {
                boolean shouldRescanForLocal = hasStoragePermission(context) && isDemoOnlyCache(cached);
                if (!shouldRescanForLocal) {
                    return cached;
                }
            }
        }

        List<Song> combined = new ArrayList<>();
        if (hasStoragePermission(context)) {
            List<Song> localSongs = scanLocalMusic(context, callback);
            combined.addAll(localSongs);
        }

        List<Song> demos = getDemoSongs();
        for (Song demo : demos) {
            boolean exists = false;
            for (Song s : combined) {
                if (s.getTitle().equalsIgnoreCase(demo.getTitle())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                combined.add(demo);
            }
        }

        return combined;
    }

    private static void cacheLibrarySongs(Context context, List<Song> songs) {
        cachedLibrarySongs = songs != null ? new ArrayList<>(songs) : new ArrayList<>();
        if (context == null || songs == null || songs.isEmpty()) {
            return;
        }

        try {
            new MusicRepository(context).cacheSongs(songs);
        } catch (Exception e) {
            Log.w(TAG, "Unable to cache media library", e);
        }
    }

    private static boolean isDemoOnlyCache(List<Song> songs) {
        if (songs == null || songs.isEmpty()) return true;
        for (Song song : songs) {
            if (song != null && song.getFilePath() != null && song.getFilePath().startsWith("content://")) {
                return false;
            }
        }
        return true;
    }

    public static List<Song> getDemoSongs() {
        if (demoSongsCache == null) {
            List<Song> songs = new ArrayList<>();
            Song s1 = new Song(10001, "Sunrise Drive", "SoundHelix", "Built-in Demo Pack", 372000, "android.resource://com.example.kfmusic/raw/sunrise_drive");
            s1.setCoverUrl("https://picsum.photos/seed/sunrise-drive/200/200");
            songs.add(s1);

            Song s2 = new Song(10002, "Neon Horizon", "SoundHelix", "Built-in Demo Pack", 423000, "android.resource://com.example.kfmusic/raw/neon_horizon");
            s2.setCoverUrl("https://picsum.photos/seed/neon-horizon/200/200");
            songs.add(s2);

            Song s3 = new Song(10003, "Midnight Pulse", "SoundHelix", "Built-in Demo Pack", 302000, "android.resource://com.example.kfmusic/raw/midnight_pulse");
            s3.setCoverUrl("https://picsum.photos/seed/midnight-pulse/200/200");
            songs.add(s3);

            Song s4 = new Song(10004, "Golden Skies", "SoundHelix", "Built-in Demo Pack", 302000, "android.resource://com.example.kfmusic/raw/golden_skies");
            s4.setCoverUrl("https://picsum.photos/seed/golden-skies/200/200");
            songs.add(s4);

            Song s5 = new Song(10005, "Electric Bloom", "SoundHelix", "Built-in Demo Pack", 363000, "android.resource://com.example.kfmusic/raw/electric_bloom");
            s5.setCoverUrl("https://picsum.photos/seed/electric-bloom/200/200");
            songs.add(s5);
            
            Song s6 = new Song(10006, "Resmini Öptümde Yattım", "Cengiz Kurtoglu", "Built-in Demo Pack", 363000, "android.resource://com.example.kfmusic/raw/resmini_optumde_yattim_cengiz_kurtoglu");
            s6.setCoverUrl("https://i.scdn.co/image/ab67616d0000b2738111068fefe0295d58330be7");
            songs.add(s6);
            demoSongsCache = java.util.Collections.unmodifiableList(songs);
        }
        return new ArrayList<>(demoSongsCache);
    }
}
