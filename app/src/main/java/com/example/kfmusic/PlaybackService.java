package com.example.kfmusic;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.kfmusic.model.Song;
import com.example.kfmusic.utils.PlaybackManager;

import java.io.IOException;
import java.util.List;

public class PlaybackService extends Service implements AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = "PlaybackService";
    private static final String CHANNEL_ID = "kfmusic_playback_channel";
    private static final int NOTIFICATION_ID = 808;

    // Actions
    public static final String ACTION_PLAY = "com.example.kfmusic.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.example.kfmusic.ACTION_PAUSE";
    public static final String ACTION_TOGGLE = "com.example.kfmusic.ACTION_TOGGLE";
    public static final String ACTION_NEXT = "com.example.kfmusic.ACTION_NEXT";
    public static final String ACTION_PREVIOUS = "com.example.kfmusic.ACTION_PREVIOUS";
    public static final String ACTION_STOP = "com.example.kfmusic.ACTION_STOP";

    private final IBinder binder = new LocalBinder();
    private MediaPlayer mediaPlayer;
    private MediaSession mediaSession;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private android.net.wifi.WifiManager.WifiLock wifiLock;

    private boolean isBecomingNoisyRegistered = false;
    private boolean isPrepared = false;
    private boolean wasPlayingBeforeFocusLoss = false;
    private boolean wasDucking = false;
    private int consecutiveErrors = 0;

    private int headsetClickCount = 0;
    private final Handler headsetHandler = new Handler(Looper.getMainLooper());
    private final Runnable headsetRunnable = new Runnable() {
        @Override
        public void run() {
            if (headsetClickCount == 1) {
                playOrPause();
            } else if (headsetClickCount == 2) {
                playNext();
            } else if (headsetClickCount >= 3) {
                playPrevious();
            }
            headsetClickCount = 0;
        }
    };

    private void handleHeadsetButtonClick() {
        headsetClickCount++;
        headsetHandler.removeCallbacks(headsetRunnable);
        headsetHandler.postDelayed(headsetRunnable, 300);
    }
    // Sleep Timer properties
    private final Handler sleepHandler = new Handler(Looper.getMainLooper());
    private long sleepTimeRemainingMs = 0;
    private final Runnable sleepRunnable = new Runnable() {
        @Override
        public void run() {
            if (sleepTimeRemainingMs > 0) {
                sleepTimeRemainingMs -= 1000;
                if (sleepTimeRemainingMs <= 0) {
                    pause();
                    sleepTimeRemainingMs = 0;
                } else {
                    sleepHandler.postDelayed(this, 1000);
                }
            }
        }
    };

    public void startSleepTimer(long minutes) {
        stopSleepTimer();
        sleepTimeRemainingMs = minutes * 60 * 1000;
        if (sleepTimeRemainingMs > 0) {
            sleepHandler.postDelayed(sleepRunnable, 1000);
        }
    }

    public void stopSleepTimer() {
        sleepHandler.removeCallbacks(sleepRunnable);
        sleepTimeRemainingMs = 0;
    }

    public long getSleepTimeRemainingMs() {
        return sleepTimeRemainingMs;
    }

    // BroadcastReceiver to pause when headphones are unplugged
    private final BroadcastReceiver noisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                pause();
            }
        }
    };

    // BroadcastReceiver to pause when storage is disconnected
    private final BroadcastReceiver storageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_MEDIA_EJECT.equals(action) || Intent.ACTION_MEDIA_UNMOUNTED.equals(action)) {
                pause();
            }
        }
    };

    public class LocalBinder extends Binder {
        public PlaybackService getService() {
            return PlaybackService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // Initialize WifiLock
        android.net.wifi.WifiManager wifiManager = (android.net.wifi.WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            wifiLock = wifiManager.createWifiLock(android.net.wifi.WifiManager.WIFI_MODE_FULL_HIGH_PERF, "KFMusicWifiLock");
        }

        // Initialize Media Player
        mediaPlayer = new MediaPlayer();
        
        // Register storage disconnect receiver
        IntentFilter storageFilter = new IntentFilter();
        storageFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        storageFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        storageFilter.addDataScheme("file");
        registerReceiver(storageReceiver, storageFilter);
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build());

        mediaPlayer.setOnCompletionListener(mp -> {
            if (PlaybackManager.getInstance().getRepeatMode() == PlaybackManager.REPEAT_ONE) {
                Song currentSong = PlaybackManager.getInstance().getCurrentSong();
                if (currentSong != null) {
                    playSong(currentSong);
                } else {
                    playNext();
                }
            } else {
                playNext();
            }
        });
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "MediaPlayer error: " + what + ", " + extra);
            isPrepared = false;
            consecutiveErrors++;
            if (consecutiveErrors >= 3) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getApplicationContext(), "Too many playback errors, stopping.", Toast.LENGTH_SHORT).show();
                });
                stopForeground(true);
                stopSelf();
            } else {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getApplicationContext(), "Unable to play this track, skipping...", Toast.LENGTH_SHORT).show();
                });
                playNext();
            }
            return true;
        });

        // Initialize Media Session
        mediaSession = new MediaSession(this, "KFMusicSession");
        mediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                playOrPause();
            }

            @Override
            public void onPause() {
                playOrPause();
            }

            @Override
            public void onSkipToNext() {
                playNext();
            }

            @Override
            public void onSkipToPrevious() {
                playPrevious();
            }

            @Override
            public void onSeekTo(long pos) {
                seekTo((int) pos);
            }

            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
                if (Intent.ACTION_MEDIA_BUTTON.equals(mediaButtonIntent.getAction())) {
                    android.view.KeyEvent event = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                    if (event != null && event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
                        int keyCode = event.getKeyCode();
                        if (keyCode == android.view.KeyEvent.KEYCODE_HEADSETHOOK || 
                            keyCode == android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                            handleHeadsetButtonClick();
                            return true;
                        }
                    }
                }
                return super.onMediaButtonEvent(mediaButtonIntent);
            }
        });
        mediaSession.setActive(true);

        createNotificationChannel();
        registerNoisyReceiver();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            handleAction(intent.getAction());
        }
        return START_NOT_STICKY;
    }

    private void handleAction(String action) {
        switch (action) {
            case ACTION_PLAY:
                resume();
                break;
            case ACTION_PAUSE:
                pause();
                break;
            case ACTION_TOGGLE:
                playOrPause();
                break;
            case ACTION_NEXT:
                playNext();
                break;
            case ACTION_PREVIOUS:
                playPrevious();
                break;
            case ACTION_STOP:
                stopForeground(true);
                stopSelf();
                break;
        }
    }

    // Playback APIs
    public void playSong(Song song) {
        if (song == null) return;
        
        if (!requestAudioFocus()) {
            return; // Could not gain audio focus
        }

        try {
            isPrepared = false;
            mediaPlayer.reset();
            
            String path = song.getFilePath();
            if (path != null && (path.startsWith("content://") || path.startsWith("android.resource://") || path.startsWith("http://") || path.startsWith("https://"))) {
                mediaPlayer.setDataSource(this, Uri.parse(path));
            } else {
                mediaPlayer.setDataSource(path);
            }
            
            if (path != null && (path.startsWith("http://") || path.startsWith("https://"))) {
                acquireWifiLock();
            } else {
                releaseWifiLock();
            }

            mediaPlayer.setOnPreparedListener(mp -> {
                isPrepared = true;
                consecutiveErrors = 0;
                mediaPlayer.start();

                // Task 137: Increment play count in database
                new com.example.kfmusic.db.MusicRepository(this).incrementPlayCount(song.getId());

                // Initialize Audiofx Equalizer
                com.example.kfmusic.utils.AudioEffectManager.getInstance(this)
                        .initEffects(mediaPlayer.getAudioSessionId());

                updatePlaybackState(true);
                loadArtworkAsync(song, true);
                
                // Sync with PlaybackManager callbacks
                PlaybackManager.getInstance().syncTrackChange(song);
                PlaybackManager.getInstance().syncStatusChange(true);
            });

            mediaPlayer.prepareAsync();

        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            Log.e(TAG, "Error preparing audio source: " + song.getTitle(), e);
        }
    }

    public void playOrPause() {
        if (mediaPlayer.isPlaying()) {
            pause();
        } else {
            resume();
        }
    }

    private final Handler fadeHandler = new Handler(Looper.getMainLooper());
    private Runnable fadeRunnable;

    private void fadeVolume(final float startVol, final float endVol, final long durationMs, final Runnable onComplete) {
        fadeHandler.removeCallbacks(fadeRunnable);
        final long startTime = System.currentTimeMillis();
        fadeRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startTime;
                float fraction = Math.min(1.0f, (float) elapsed / durationMs);
                float currentVol = startVol + (endVol - startVol) * fraction;
                if (mediaPlayer != null) {
                    try {
                        mediaPlayer.setVolume(currentVol, currentVol);
                    } catch (IllegalStateException e) {
                        // ignore
                    }
                }
                if (fraction < 1.0f) {
                    fadeHandler.postDelayed(this, 30);
                } else {
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            }
        };
        fadeHandler.post(fadeRunnable);
    }

    public void pause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            releaseWifiLock();
            updatePlaybackState(false);
            if (PlaybackManager.getInstance().getCurrentSong() != null) {
                loadArtworkAsync(PlaybackManager.getInstance().getCurrentSong(), false);
            }
            PlaybackManager.getInstance().syncStatusChange(false);
        }
    }

    public void resume() {
        if (!requestAudioFocus()) {
            return;
        }
        if (isPrepared && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            String path = PlaybackManager.getInstance().getCurrentSong() != null ? PlaybackManager.getInstance().getCurrentSong().getFilePath() : null;
            if (path != null && (path.startsWith("http://") || path.startsWith("https://"))) {
                acquireWifiLock();
            }
            updatePlaybackState(true);
            if (PlaybackManager.getInstance().getCurrentSong() != null) {
                loadArtworkAsync(PlaybackManager.getInstance().getCurrentSong(), true);
            }
            PlaybackManager.getInstance().syncStatusChange(true);
        }
    }

    public void playNext() {
        PlaybackManager.getInstance().playNext();
    }

    public void playPrevious() {
        PlaybackManager.getInstance().playPrevious();
    }

    public void seekTo(int posMs) {
        if (isPrepared) {
            mediaPlayer.seekTo(posMs);
            updatePlaybackState(mediaPlayer.isPlaying());
        }
    }

    public boolean isPlaying() {
        return isPrepared && mediaPlayer.isPlaying();
    }

    public int getCurrentPosition() {
        return isPrepared ? mediaPlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        if (isPrepared && mediaPlayer != null) {
            try {
                int duration = mediaPlayer.getDuration();
                return duration > 0 ? duration : 0;
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    // Audio Focus API handling
    private boolean requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(audioAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(this)
                    .build();
            return audioManager.requestAudioFocus(audioFocusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        } else {
            return audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
    }

    private void abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
            }
        } else {
            audioManager.abandonAudioFocus(this);
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if (wasDucking) {
                    fadeVolume(0.2f, 1.0f, 500, null);
                    wasDucking = false;
                }
                if (wasPlayingBeforeFocusLoss) {
                    resume();
                    wasPlayingBeforeFocusLoss = false;
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                wasPlayingBeforeFocusLoss = mediaPlayer.isPlaying();
                pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                wasPlayingBeforeFocusLoss = mediaPlayer.isPlaying();
                pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (mediaPlayer.isPlaying()) {
                    wasDucking = true;
                    mediaPlayer.setVolume(0.2f, 0.2f);
                }
                break;
        }
    }

    private void showNotification(Song song, boolean isPlaying) {
        loadArtworkAsync(song, isPlaying);
    }

    private final java.util.concurrent.ExecutorService artExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();

    private void loadArtworkAsync(Song song, boolean isPlaying) {
        artExecutor.execute(() -> {
            Bitmap albumArt = getAlbumArt(song);
            new Handler(Looper.getMainLooper()).post(() -> {
                MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder()
                        .putString(MediaMetadata.METADATA_KEY_TITLE, song.getTitle())
                        .putString(MediaMetadata.METADATA_KEY_ARTIST, song.getArtist())
                        .putString(MediaMetadata.METADATA_KEY_ALBUM, song.getAlbum())
                        .putLong(MediaMetadata.METADATA_KEY_DURATION, song.getDuration());
                if (albumArt != null) {
                    metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, albumArt);
                    metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, albumArt);
                }
                mediaSession.setMetadata(metadataBuilder.build());

                showNotificationWithArt(song, isPlaying, albumArt);
            });
        });
    }

    private Bitmap getAlbumArt(Song song) {
        Bitmap art = null;
        if (song.getAlbumId() > 0) {
            art = getAlbumArtBitmap(song.getAlbumId());
        }
        if (art == null) {
            art = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        }
        return art;
    }

    private Bitmap getAlbumArtBitmap(long albumId) {
        java.io.InputStream in = null;
        try {
            Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
            Uri uri = android.content.ContentUris.withAppendedId(sArtworkUri, albumId);
            in = getContentResolver().openInputStream(uri);
            return BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            return null;
        } finally {
            if (in != null) {
                try { in.close(); } catch (Exception ignored) {}
            }
        }
    }

    private void showNotificationWithArt(Song song, boolean isPlaying, Bitmap albumArt) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        // Control intents
        PendingIntent playPauseIntent = PendingIntent.getService(this, 1, 
                new Intent(this, PlaybackService.class).setAction(ACTION_TOGGLE), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent nextIntent = PendingIntent.getService(this, 2, 
                new Intent(this, PlaybackService.class).setAction(ACTION_NEXT), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent prevIntent = PendingIntent.getService(this, 3, 
                new Intent(this, PlaybackService.class).setAction(ACTION_PREVIOUS), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stopIntent = PendingIntent.getService(this, 4, 
                new Intent(this, PlaybackService.class).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID)
                .setStyle(new Notification.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentTitle(song.getTitle())
                .setContentText(song.getArtist())
                .setContentIntent(pendingIntent)
                .setColor(0xFF5E7CE2)
                .setLargeIcon(albumArt)
                .addAction(new Notification.Action.Builder(
                        R.drawable.ic_skip_previous, "Previous", prevIntent).build())
                .addAction(new Notification.Action.Builder(
                        isPlaying ? R.drawable.ic_pause : R.drawable.ic_play, 
                        isPlaying ? "Pause" : "Play", playPauseIntent).build())
                .addAction(new Notification.Action.Builder(
                        R.drawable.ic_skip_next, "Next", nextIntent).build())
                .addAction(new Notification.Action.Builder(
                        R.drawable.ic_close, "Stop", stopIntent).build())
                .setOngoing(isPlaying);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        }

        Notification notification = builder.build();
        startForeground(NOTIFICATION_ID, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "KF Music Playback",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("KF Music Player controller notification");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private void updatePlaybackState(boolean isPlaying) {
        PlaybackState.Builder stateBuilder = new PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE |
                        PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_SKIP_TO_NEXT |
                        PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_SEEK_TO);
        
        stateBuilder.setState(
                isPlaying ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED,
                mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0,
                1.0f
        );
        mediaSession.setPlaybackState(stateBuilder.build());
    }

    private void updateMediaSessionMetadata(Song song) {
        // Obsoleted by loadArtworkAsync
    }

    private void registerNoisyReceiver() {
        if (!isBecomingNoisyRegistered) {
            registerReceiver(noisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
            isBecomingNoisyRegistered = true;
        }
    }

    private void unregisterNoisyReceiver() {
        if (isBecomingNoisyRegistered) {
            unregisterReceiver(noisyReceiver);
            isBecomingNoisyRegistered = false;
        }
    }

    private void acquireWifiLock() {
        if (wifiLock != null && !wifiLock.isHeld()) {
            wifiLock.acquire();
        }
    }

    private void releaseWifiLock() {
        if (wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(storageReceiver);
        } catch (Exception ignored) {}
        unregisterNoisyReceiver();
        abandonAudioFocus();
        releaseWifiLock();
        sleepHandler.removeCallbacks(sleepRunnable);
        com.example.kfmusic.utils.AudioEffectManager.getInstance(this).releaseEffects();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
    }
}
