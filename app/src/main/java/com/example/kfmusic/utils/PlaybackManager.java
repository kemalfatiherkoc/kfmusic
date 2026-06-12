package com.example.kfmusic.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.example.kfmusic.PlaybackService;
import com.example.kfmusic.db.MusicRepository;
import com.example.kfmusic.model.Song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlaybackManager {
    private static final String TAG = "PlaybackManager";
    private static PlaybackManager instance;

    private PlaybackService playbackService;
    private boolean isBound = false;
    private Context mContext;
    private MusicRepository repository;

    private List<Song> playlist = new ArrayList<>();
    private List<Song> originalPlaylist = new ArrayList<>(); // Saves pre-shuffled list order

    private int currentSongIndex = -1;

    // Repeat states
    public static final int REPEAT_OFF = 0;
    public static final int REPEAT_ALL = 1;
    public static final int REPEAT_ONE = 2;

    private int repeatMode = REPEAT_OFF;
    private boolean isShuffleEnabled = false;

    private boolean isInitialized = false;

    // Interface to listen for playback updates
    public interface PlaybackListener {
        void onTrackChanged(Song song);
        void onPlaybackStatusChanged(boolean isPlaying);
    }

    private final List<java.lang.ref.WeakReference<PlaybackListener>> listeners = new ArrayList<>();

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlaybackService.LocalBinder binder = (PlaybackService.LocalBinder) service;
            playbackService = binder.getService();
            isBound = true;
            Log.d(TAG, "PlaybackService connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            playbackService = null;
            isBound = false;
            Log.d(TAG, "PlaybackService disconnected");
        }
    };

    private PlaybackManager() {}

    public static synchronized PlaybackManager getInstance() {
        if (instance == null) {
            instance = new PlaybackManager();
        }
        return instance;
    }

    public void init(Context context) {
        if (isInitialized) {
            return;
        }
        isInitialized = true;
        this.mContext = context.getApplicationContext();
        this.repository = new MusicRepository(mContext);
        if (repository.getCachedSongs().isEmpty()) {
            repository.cacheSongs(MediaScanner.getDemoSongs());
        }

        // Load saved state
        android.content.SharedPreferences prefs = mContext.getSharedPreferences("kfmusic_playback_prefs", Context.MODE_PRIVATE);
        isShuffleEnabled = prefs.getBoolean("shuffle", false);
        repeatMode = prefs.getInt("repeat", REPEAT_OFF);
        currentSongIndex = -1;

        loadQueueFromDatabase();

        Intent intent = new Intent(mContext, PlaybackService.class);
        mContext.startService(intent);
        mContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void loadQueueFromDatabase() {
        if (repository != null) {
            List<Long> ids = repository.getSavedQueueSongIds();
            if (!ids.isEmpty()) {
                List<Song> allCached = repository.getCachedSongs();
                if (allCached.isEmpty()) {
                    allCached = MediaScanner.getDemoSongs();
                }
                List<Song> restored = new ArrayList<>();
                for (Long id : ids) {
                    for (Song s : allCached) {
                        if (s.getId() == id) {
                            restored.add(s);
                            break;
                        }
                    }
                }
                if (!restored.isEmpty()) {
                    playlist = restored;
                    originalPlaylist = new ArrayList<>(playlist);
                    if (mContext != null) {
                        int savedIndex = mContext.getSharedPreferences("kfmusic_playback_prefs", Context.MODE_PRIVATE)
                                .getInt("current_index", -1);
                        if (savedIndex >= 0 && savedIndex < playlist.size()) {
                            currentSongIndex = savedIndex;
                        } else {
                            currentSongIndex = 0;
                        }
                    } else {
                        currentSongIndex = 0;
                    }
                }
            }
        }
    }

    private void saveQueueToDatabase() {
        if (repository != null) {
            repository.saveQueue(playlist);
        }
        if (mContext != null) {
            mContext.getSharedPreferences("kfmusic_playback_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putInt("current_index", currentSongIndex)
                    .apply();
        }
    }

    public synchronized void addListener(PlaybackListener listener) {
        if (listener == null) return;
        boolean found = false;
        List<java.lang.ref.WeakReference<PlaybackListener>> toRemove = new ArrayList<>();
        for (java.lang.ref.WeakReference<PlaybackListener> ref : listeners) {
            PlaybackListener l = ref.get();
            if (l == null) {
                toRemove.add(ref);
            } else if (l == listener) {
                found = true;
            }
        }
        listeners.removeAll(toRemove);
        
        if (!found) {
            listeners.add(new java.lang.ref.WeakReference<>(listener));
        }
        
        if (getCurrentSong() != null) {
            listener.onTrackChanged(getCurrentSong());
            listener.onPlaybackStatusChanged(isPlaying());
        }
    }

    public synchronized void removeListener(PlaybackListener listener) {
        if (listener == null) return;
        List<java.lang.ref.WeakReference<PlaybackListener>> toRemove = new ArrayList<>();
        for (java.lang.ref.WeakReference<PlaybackListener> ref : listeners) {
            PlaybackListener l = ref.get();
            if (l == null || l == listener) {
                toRemove.add(ref);
            }
        }
        listeners.removeAll(toRemove);
    }

    public synchronized void syncTrackChange(Song song) {
        List<java.lang.ref.WeakReference<PlaybackListener>> toRemove = new ArrayList<>();
        for (java.lang.ref.WeakReference<PlaybackListener> ref : listeners) {
            PlaybackListener l = ref.get();
            if (l != null) {
                l.onTrackChanged(song);
            } else {
                toRemove.add(ref);
            }
        }
        listeners.removeAll(toRemove);
    }

    public synchronized void syncStatusChange(boolean isPlaying) {
        List<java.lang.ref.WeakReference<PlaybackListener>> toRemove = new ArrayList<>();
        for (java.lang.ref.WeakReference<PlaybackListener> ref : listeners) {
            PlaybackListener l = ref.get();
            if (l != null) {
                l.onPlaybackStatusChanged(isPlaying);
            } else {
                toRemove.add(ref);
            }
        }
        listeners.removeAll(toRemove);
    }

    public void setPlaylist(List<Song> songs, int startIndex) {
        this.playlist = new ArrayList<>(songs);
        this.originalPlaylist = new ArrayList<>(playlist);
        if (startIndex >= 0 && startIndex < playlist.size()) {
            this.currentSongIndex = startIndex;
            if (isShuffleEnabled) {
                Song currentSong = playlist.get(currentSongIndex);
                List<Song> shuffled = new ArrayList<>(playlist);
                shuffled.remove(currentSong);
                Collections.shuffle(shuffled);
                shuffled.add(0, currentSong);
                currentSongIndex = 0;
                this.playlist = shuffled;
            }
            if (isBound && playbackService != null) {
                playbackService.playSong(playlist.get(currentSongIndex));
            }
        }
        saveQueueToDatabase();
    }

    public List<Song> getPlaylist() {
        return playlist;
    }

    public int getCurrentSongIndex() {
        return currentSongIndex;
    }

    public Song getCurrentSong() {
        if (currentSongIndex >= 0 && currentSongIndex < playlist.size()) {
            return playlist.get(currentSongIndex);
        }
        return null;
    }

    public boolean isPlaying() {
        if (isBound && playbackService != null) {
            return playbackService.isPlaying();
        }
        return false;
    }

    public void playSong(Song song) {
        if (song == null) return;
        
        // Find song index in current playlist
        int index = playlist.indexOf(song);
        if (index >= 0) {
            currentSongIndex = index;
        } else {
            // Append and play
            playlist.add(song);
            originalPlaylist.add(song);
            currentSongIndex = playlist.size() - 1;
        }

        if (isBound && playbackService != null) {
            playbackService.playSong(song);
        }
        saveQueueToDatabase();
    }

    public void playOrPause() {
        if (isBound && playbackService != null) {
            playbackService.playOrPause();
        }
    }

    public void playNext() {
        if (playlist.isEmpty()) return;

        if (currentSongIndex == playlist.size() - 1) {
            if (repeatMode == REPEAT_OFF) {
                if (isBound && playbackService != null) {
                    playbackService.pause();
                    playbackService.seekTo(0);
                }
                return;
            } else {
                currentSongIndex = 0;
            }
        } else {
            currentSongIndex++;
        }

        if (isBound && playbackService != null) {
            playbackService.playSong(playlist.get(currentSongIndex));
        }
        saveQueueToDatabase();
    }

    public void playPrevious() {
        if (playlist.isEmpty()) return;

        currentSongIndex = currentSongIndex - 1;
        if (currentSongIndex < 0) {
            if (repeatMode == REPEAT_OFF) {
                currentSongIndex = 0;
            } else {
                currentSongIndex = playlist.size() - 1;
            }
        }

        if (isBound && playbackService != null) {
            playbackService.playSong(playlist.get(currentSongIndex));
        }
        saveQueueToDatabase();
    }

    public void seekTo(int positionMs) {
        if (isBound && playbackService != null) {
            playbackService.seekTo(positionMs);
        }
    }

    public int getCurrentPosition() {
        if (isBound && playbackService != null) {
            return playbackService.getCurrentPosition();
        }
        return 0;
    }

    public int getDuration() {
        int duration = 0;
        if (isBound && playbackService != null) {
            duration = playbackService.getDuration();
        } else {
            Song current = getCurrentSong();
            if (current != null) {
                duration = (int) current.getDuration();
            }
        }
        return duration > 0 ? duration : 0;
    }

    // Shuffle & Repeat controls
    public void setShuffleEnabled(boolean enabled) {
        if (this.isShuffleEnabled == enabled) return;
        this.isShuffleEnabled = enabled;

        if (mContext != null) {
            mContext.getSharedPreferences("kfmusic_playback_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("shuffle", enabled)
                    .apply();
        }

        if (enabled) {
            originalPlaylist = new ArrayList<>(playlist);
            Song currentSong = getCurrentSong();
            List<Song> shuffled = new ArrayList<>(playlist);
            if (currentSong != null) {
                shuffled.remove(currentSong);
            }
            Collections.shuffle(shuffled);
            if (currentSong != null) {
                shuffled.add(0, currentSong);
                currentSongIndex = 0;
            } else if (!shuffled.isEmpty()) {
                currentSongIndex = 0;
            }
            playlist = shuffled;
        } else {
            Song currentSong = getCurrentSong();
            if (!originalPlaylist.isEmpty()) {
                playlist = new ArrayList<>(originalPlaylist);
                if (currentSong != null) {
                    currentSongIndex = playlist.indexOf(currentSong);
                } else if (!playlist.isEmpty()) {
                    currentSongIndex = 0;
                }
            }
        }
        saveQueueToDatabase();
    }

    public boolean isShuffleEnabled() {
        return isShuffleEnabled;
    }

    public void setRepeatMode(int mode) {
        this.repeatMode = mode;
        if (mContext != null) {
            mContext.getSharedPreferences("kfmusic_playback_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putInt("repeat", mode)
                    .apply();
        }
    }

    public int getRepeatMode() {
        return repeatMode;
    }

    // Queue mutations
    public void addToQueue(Song song) {
        if (song == null) return;
        if (!playlist.contains(song)) {
            playlist.add(song);
            originalPlaylist.add(song);
            saveQueueToDatabase();
        }
    }

    public void playNext(Song song) {
        if (song == null) return;

        int oldIndex = playlist.indexOf(song);

        playlist.remove(song);
        originalPlaylist.remove(song);

        if (oldIndex >= 0 && oldIndex < currentSongIndex) {
            currentSongIndex--;
        } else if (oldIndex == currentSongIndex && currentSongIndex >= playlist.size() && !playlist.isEmpty()) {
            currentSongIndex = playlist.size() - 1;
        }

        int nextIndex = currentSongIndex + 1;
        if (nextIndex < 0 || nextIndex > playlist.size()) {
            nextIndex = 0;
        }
        playlist.add(nextIndex, song);
        originalPlaylist.add(nextIndex, song);
        saveQueueToDatabase();
    }

    public void removeFromQueue(int index) {
        if (index >= 0 && index < playlist.size()) {
            Song removed = playlist.remove(index);
            originalPlaylist.remove(removed);
            if (index < currentSongIndex) {
                currentSongIndex--;
            } else if (index == currentSongIndex) {
                if (playlist.isEmpty()) {
                    currentSongIndex = -1;
                    if (isBound && playbackService != null) {
                        playbackService.pause();
                    }
                } else {
                    currentSongIndex = currentSongIndex % playlist.size();
                    if (isBound && playbackService != null) {
                        playbackService.playSong(playlist.get(currentSongIndex));
                    }
                }
            }
            saveQueueToDatabase();
        }
    }

    public void clearQueue() {
        playlist.clear();
        originalPlaylist.clear();
        currentSongIndex = -1;
        if (isBound && playbackService != null) {
            playbackService.pause();
        }
        saveQueueToDatabase();
    }

    public void reorderQueue(int fromPosition, int toPosition) {
        if (fromPosition >= 0 && fromPosition < playlist.size() && toPosition >= 0 && toPosition < playlist.size()) {
            Song song = playlist.remove(fromPosition);
            playlist.add(toPosition, song);

            if (currentSongIndex == fromPosition) {
                currentSongIndex = toPosition;
            } else if (fromPosition < currentSongIndex && toPosition >= currentSongIndex) {
                currentSongIndex--;
            } else if (fromPosition > currentSongIndex && toPosition <= currentSongIndex) {
                currentSongIndex++;
            }
            saveQueueToDatabase();
        }
    }

    // Sleep Timer delegates
    public void startSleepTimer(long minutes) {
        if (isBound && playbackService != null) {
            playbackService.startSleepTimer(minutes);
        }
    }

    public void stopSleepTimer() {
        if (isBound && playbackService != null) {
            playbackService.stopSleepTimer();
        }
    }

    public long getSleepTimeRemainingMs() {
        if (isBound && playbackService != null) {
            return playbackService.getSleepTimeRemainingMs();
        }
        return 0;
    }

    public synchronized void handleDeletedTrack(long songId) {
        int indexToRemove = -1;
        for (int i = 0; i < playlist.size(); i++) {
            if (playlist.get(i).getId() == songId) {
                indexToRemove = i;
                break;
            }
        }
        
        if (indexToRemove != -1) {
            playlist.remove(indexToRemove);
            
            for (int i = 0; i < originalPlaylist.size(); i++) {
                if (originalPlaylist.get(i).getId() == songId) {
                    originalPlaylist.remove(i);
                    break;
                }
            }

            if (currentSongIndex == indexToRemove) {
                if (playlist.isEmpty()) {
                    currentSongIndex = -1;
                    if (isBound && playbackService != null) {
                        playbackService.pause();
                    }
                } else {
                    if (currentSongIndex >= playlist.size()) {
                        currentSongIndex = 0;
                    }
                    if (isBound && playbackService != null) {
                        playbackService.playSong(playlist.get(currentSongIndex));
                    }
                }
            } else if (indexToRemove < currentSongIndex) {
                currentSongIndex--;
            }
            saveQueueToDatabase();
            syncTrackChange(getCurrentSong());
        }
    }

    public void unbind(Context context) {
        if (isBound) {
            context.unbindService(serviceConnection);
            isBound = false;
        }
    }
}
