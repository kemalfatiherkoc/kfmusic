package com.example.kfmusic;

import android.animation.ObjectAnimator;
import com.example.kfmusic.view.AudioVisualizerView;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.kfmusic.model.Song;
import com.example.kfmusic.utils.FavoritesManager;
import com.example.kfmusic.utils.OnSwipeTouchListener;
import com.example.kfmusic.utils.PlaybackManager;
import com.example.kfmusic.utils.PlaylistManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class NowPlayingFragment extends Fragment implements PlaybackManager.PlaybackListener {

    private ImageButton btnCollapse;
    private TextView playerTitle;
    private AudioVisualizerView audioVisualizer;
    private TextView playerArtist;
    private ImageButton btnLike;
    private SeekBar seekBar;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;
    private ImageButton btnPrevious;
    private FloatingActionButton fabPlayPause;
    private ImageButton btnNext;
    private View btnAddToPlaylist;
    private android.widget.ImageView playerAlbumArt;
    private android.widget.ImageView playerBackgroundBlur;
    private static final java.util.concurrent.ExecutorService artLoaderExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
    private androidx.cardview.widget.CardView albumArtCard;
    private android.widget.ScrollView lyricsContainer;
    private TextView tvLyrics;
    private final List<com.example.kfmusic.utils.LrcParser.LrcLine> currentLyrics = new ArrayList<>();
    private int activeLyricsLineIndex = -1;

    // Shuffle, Repeat, Queue views
    private ImageButton btnShuffle;
    private ImageButton btnRepeat;
    private ImageButton btnQueue;

    // Fast-Forward & Rewind buttons
    private ImageButton btnRewind10;
    private ImageButton btnForward10;

    private ObjectAnimator rotationAnimator;

    private boolean isUserDragging = false;
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAdded() && !isUserDragging) {
                int currentPos = PlaybackManager.getInstance().getCurrentPosition();
                seekBar.setProgress(currentPos);
                tvCurrentTime.setText(formatTime(currentPos));
                updateLyricsHighlight(currentPos);
            }
            progressHandler.postDelayed(this, 250);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_now_playing, container, false);

        btnCollapse = view.findViewById(R.id.btnCollapse);
        playerTitle = view.findViewById(R.id.playerTitle);
        playerArtist = view.findViewById(R.id.playerArtist);
        btnLike = view.findViewById(R.id.btnLike);
        seekBar = view.findViewById(R.id.seekBar);
        tvCurrentTime = view.findViewById(R.id.tvCurrentTime);
        tvTotalTime = view.findViewById(R.id.tvTotalTime);
        btnPrevious = view.findViewById(R.id.btnPrevious);
        fabPlayPause = view.findViewById(R.id.fabPlayPause);
        btnNext = view.findViewById(R.id.btnNext);
        btnAddToPlaylist = view.findViewById(R.id.btnAddToPlaylist);
        playerAlbumArt = view.findViewById(R.id.playerAlbumArt);
        playerBackgroundBlur = view.findViewById(R.id.playerBackgroundBlur);
        albumArtCard = view.findViewById(R.id.albumArtCard);
        lyricsContainer = view.findViewById(R.id.lyricsContainer);
        tvLyrics = view.findViewById(R.id.tvLyrics);

        albumArtCard.setOnClickListener(v -> {
            if (lyricsContainer.getVisibility() == View.VISIBLE) {
                lyricsContainer.setVisibility(View.GONE);
                playerAlbumArt.setVisibility(View.VISIBLE);
            } else {
                lyricsContainer.setVisibility(View.VISIBLE);
                playerAlbumArt.setVisibility(View.GONE);
                updateLyricsHighlight(PlaybackManager.getInstance().getCurrentPosition());
            }
        });

        btnShuffle = view.findViewById(R.id.btnShuffle);
        btnRepeat = view.findViewById(R.id.btnRepeat);
        btnQueue = view.findViewById(R.id.btnQueue);
        audioVisualizer = view.findViewById(R.id.audioVisualizer);

        btnRewind10 = view.findViewById(R.id.btnRewind10);
        btnForward10 = view.findViewById(R.id.btnForward10);

        // Start marquee effect
        playerTitle.setSelected(true);
        playerArtist.setSelected(true);

        // Swipe gestures on album art
        playerAlbumArt.setOnTouchListener(new OnSwipeTouchListener(requireContext()) {
            @Override
            public void onSwipeLeft() {
                PlaybackManager.getInstance().playNext();
                Toast.makeText(requireContext(), getString(R.string.next_track), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSwipeRight() {
                PlaybackManager.getInstance().playPrevious();
                Toast.makeText(requireContext(), getString(R.string.previous_track), Toast.LENGTH_SHORT).show();
            }
        });

        setupListeners();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        PlaybackManager.getInstance().addListener(this);
        progressHandler.post(progressRunnable);

        // Populate initial UI
        Song current = PlaybackManager.getInstance().getCurrentSong();
        if (current != null) {
            updateUI(current);
            boolean isPlaying = PlaybackManager.getInstance().isPlaying();
            updatePlayPauseButton(isPlaying);
            if (audioVisualizer != null) {
                audioVisualizer.setPlaying(isPlaying);
            }
        }

        updateShuffleButton();
        updateRepeatButton();
    }

    @Override
    public void onStop() {
        super.onStop();
        PlaybackManager.getInstance().removeListener(this);
        progressHandler.removeCallbacks(progressRunnable);
        if (rotationAnimator != null) {
            rotationAnimator.cancel();
            rotationAnimator = null;
        }
    }

    private void setupListeners() {
        btnCollapse.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        fabPlayPause.setOnClickListener(v -> PlaybackManager.getInstance().playOrPause());
        btnNext.setOnClickListener(v -> PlaybackManager.getInstance().playNext());
        btnPrevious.setOnClickListener(v -> PlaybackManager.getInstance().playPrevious());

        btnLike.setOnClickListener(v -> {
            Song current = PlaybackManager.getInstance().getCurrentSong();
            if (current != null) {
                FavoritesManager.getInstance(requireContext()).toggleFavorite(current.getId());
                updateLikeButton(current);
            }
        });

        btnAddToPlaylist.setOnClickListener(v -> showAddToPlaylistDialog());

        btnShuffle.setOnClickListener(v -> {
            boolean shuffle = !PlaybackManager.getInstance().isShuffleEnabled();
            PlaybackManager.getInstance().setShuffleEnabled(shuffle);
            updateShuffleButton();
            Toast.makeText(requireContext(), shuffle ? getString(R.string.shuffle_on) : getString(R.string.shuffle_off), Toast.LENGTH_SHORT).show();
        });

        btnRepeat.setOnClickListener(v -> {
            int currentMode = PlaybackManager.getInstance().getRepeatMode();
            int nextMode = (currentMode + 1) % 3;
            PlaybackManager.getInstance().setRepeatMode(nextMode);
            updateRepeatButton();
            String modeStr = getString(R.string.repeat_off);
            if (nextMode == PlaybackManager.REPEAT_ALL) modeStr = getString(R.string.repeat_all);
            else if (nextMode == PlaybackManager.REPEAT_ONE) modeStr = getString(R.string.repeat_one);
            Toast.makeText(requireContext(), modeStr, Toast.LENGTH_SHORT).show();
        });

        btnQueue.setOnClickListener(v -> showQueueDialog());

        btnRewind10.setOnClickListener(v -> {
            int currentPos = PlaybackManager.getInstance().getCurrentPosition();
            int targetPos = Math.max(0, currentPos - 10000);
            PlaybackManager.getInstance().seekTo(targetPos);
            Toast.makeText(requireContext(), getString(R.string.rewound_10s), Toast.LENGTH_SHORT).show();
        });

        btnForward10.setOnClickListener(v -> {
            int currentPos = PlaybackManager.getInstance().getCurrentPosition();
            int duration = PlaybackManager.getInstance().getDuration();
            int targetPos = Math.min(duration, currentPos + 10000);
            PlaybackManager.getInstance().seekTo(targetPos);
            Toast.makeText(requireContext(), getString(R.string.forwarded_10s), Toast.LENGTH_SHORT).show();
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    tvCurrentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserDragging = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserDragging = false;
                PlaybackManager.getInstance().seekTo(seekBar.getProgress());
            }
        });
    }

    private void updateUI(Song song) {
        playerTitle.setText(song.getTitle());
        playerArtist.setText(song.getArtist());
        if (lyricsContainer != null) {
            lyricsContainer.setVisibility(View.GONE);
        }
        if (playerAlbumArt != null) {
            playerAlbumArt.setVisibility(View.VISIBLE);
        }
        loadLyrics(song);
        
        int duration = (int) song.getDuration();
        if (duration <= 0) {
            duration = 0;
        }
        seekBar.setMax(duration > 0 ? duration : 100);
        tvTotalTime.setText(formatTime(duration));
        
        updateLikeButton(song);

        // Reset to default placeholder before loading
        playerAlbumArt.setImageResource(R.drawable.ic_music_note);
        playerAlbumArt.setImageTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.accent_blue)));
        int pad = (int) (72 * getResources().getDisplayMetrics().density);
        playerAlbumArt.setPadding(pad, pad, pad, pad);
        playerBackgroundBlur.setImageBitmap(null);

        String coverUrl = song.getCoverUrl();
        if (coverUrl != null && !coverUrl.isEmpty()) {
            playerAlbumArt.setTag(song.getId());
            artLoaderExecutor.execute(() -> {
                try {
                    java.net.URL url = new java.net.URL(coverUrl);
                    java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    java.io.InputStream input = connection.getInputStream();
                    final android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(input);
                    if (bitmap != null) {
                        int scaleWidth = bitmap.getWidth() / 10;
                        int scaleHeight = bitmap.getHeight() / 10;
                        final android.graphics.Bitmap blurredBitmap;
                        if (scaleWidth > 0 && scaleHeight > 0) {
                            android.graphics.Bitmap scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, scaleWidth, scaleHeight, false);
                            blurredBitmap = blur(scaled, 8);
                        } else {
                            blurredBitmap = null;
                        }
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (isAdded() && playerAlbumArt.getTag() != null && (Long) playerAlbumArt.getTag() == song.getId()) {
                                playerAlbumArt.setImageBitmap(bitmap);
                                playerAlbumArt.setImageTintList(null);
                                playerAlbumArt.setPadding(0, 0, 0, 0);
                                if (blurredBitmap != null) {
                                    playerBackgroundBlur.setImageBitmap(blurredBitmap);
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    // ignore
                }
            });
        } else if (song.getFilePath() != null) {
            long albumId = song.getAlbumId();
            if (albumId != 0) {
                playerAlbumArt.setTag(song.getId());
                final android.content.Context ctx = getContext();
                if (ctx != null) {
                    artLoaderExecutor.execute(() -> {
                        try {
                            android.net.Uri sArtworkUri = android.net.Uri.parse("content://media/external/audio/albumart");
                            android.net.Uri uri = android.content.ContentUris.withAppendedId(sArtworkUri, albumId);
                            java.io.InputStream in = ctx.getContentResolver().openInputStream(uri);
                            final android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(in);
                            if (bitmap != null) {
                                int scaleWidth = bitmap.getWidth() / 10;
                                int scaleHeight = bitmap.getHeight() / 10;
                                final android.graphics.Bitmap blurredBitmap;
                                if (scaleWidth > 0 && scaleHeight > 0) {
                                    android.graphics.Bitmap scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, scaleWidth, scaleHeight, false);
                                    blurredBitmap = blur(scaled, 8);
                                } else {
                                    blurredBitmap = null;
                                }

                                new Handler(Looper.getMainLooper()).post(() -> {
                                    if (isAdded() && playerAlbumArt.getTag() != null && (Long) playerAlbumArt.getTag() == song.getId()) {
                                        playerAlbumArt.setImageBitmap(bitmap);
                                        playerAlbumArt.setImageTintList(null);
                                        playerAlbumArt.setPadding(0, 0, 0, 0);
                                        if (blurredBitmap != null) {
                                            playerBackgroundBlur.setImageBitmap(blurredBitmap);
                                        }
                                    }
                                });
                            }
                        } catch (Exception e) {
                            // ignore
                        }
                    });
                }
            }
        }
    }

    private void loadLyrics(Song song) {
        currentLyrics.clear();
        if (tvLyrics != null) {
            tvLyrics.setText(getString(R.string.loading_lyrics));
        }
        
        String filePath = song.getFilePath();
        if (filePath != null) {
            if (filePath.startsWith("android.resource://")) {
                generateDemoOrFallbackLyrics(song);
                return;
            }
            
            if (!filePath.startsWith("content://")) {
                try {
                    int lastDot = filePath.lastIndexOf('.');
                    if (lastDot > 0) {
                        String lrcPath = filePath.substring(0, lastDot) + ".lrc";
                        java.io.File file = new java.io.File(lrcPath);
                        if (file.exists()) {
                            try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                                currentLyrics.addAll(com.example.kfmusic.utils.LrcParser.parse(fis));
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        if (currentLyrics.isEmpty()) {
            generateDemoOrFallbackLyrics(song);
        } else {
            displayLyricsText();
        }
    }

    private void generateDemoOrFallbackLyrics(Song song) {
        currentLyrics.clear();
        String title = song.getTitle();
        long duration = song.getDuration();
        if (duration <= 0) duration = 180000;
        
        if ("Sunrise Drive".equalsIgnoreCase(title)) {
            currentLyrics.add(new com.example.kfmusic.utils.LrcParser.LrcLine(0, "[Instrumental Intro]"));
            currentLyrics.add(new com.example.kfmusic.utils.LrcParser.LrcLine(10000, "Staring at the morning light"));
            currentLyrics.add(new com.example.kfmusic.utils.LrcParser.LrcLine(15000, "Sun rises up so warm and bright"));
            currentLyrics.add(new com.example.kfmusic.utils.LrcParser.LrcLine(20000, "We hit the road, we feel the breeze"));
            currentLyrics.add(new com.example.kfmusic.utils.LrcParser.LrcLine(25000, "Driving through the forest trees"));
            currentLyrics.add(new com.example.kfmusic.utils.LrcParser.LrcLine(30000, "Sunrise Drive, under gold-blue skies"));
            currentLyrics.add(new com.example.kfmusic.utils.LrcParser.LrcLine(35000, "We leave the city and its lies"));
            currentLyrics.add(new com.example.kfmusic.utils.LrcParser.LrcLine(40000, "[Guitar Solo]"));
            currentLyrics.add(new com.example.kfmusic.utils.LrcParser.LrcLine(55000, "No destination, just the highway line"));
            currentLyrics.add(new com.example.kfmusic.utils.LrcParser.LrcLine(60000, "Everything is going to be fine"));
        } else if ("Neon Horizon".equalsIgnoreCase(title)) {
            currentLyrics.add(new com.example.kfmusic.utils.LrcParser.LrcLine(0, "[Synthwave Intro]"));
            currentLyrics.add(new com.example.kfmusic.utils.LrcParser.LrcLine(8000, "Driving through the neon lights"));
            currentLyrics.add(new com.example.kfmusic.utils.LrcParser.LrcLine(14000, "Cyberpunk dreams in retro nights"));
            currentLyrics.add(new com.example.kfmusic.utils.LrcParser.LrcLine(20000, "Look at the grid, the digital sea"));
            currentLyrics.add(new com.example.kfmusic.utils.LrcParser.LrcLine(26000, "Out on the neon horizon with me"));
        } else if ("Resmini Öptümde Yattım".equalsIgnoreCase(title)) {
            currentLyrics.add(new com.example.kfmusic.utils.LrcParser.LrcLine(0, "[Giriş]"));
            currentLyrics.add(new com.example.kfmusic.utils.LrcParser.LrcLine(10000, "Dün gece seni aradım"));
            currentLyrics.add(new com.example.kfmusic.utils.LrcParser.LrcLine(16000, "Resmini öptüm de yattım"));
            currentLyrics.add(new com.example.kfmusic.utils.LrcParser.LrcLine(22000, "Yalnızlığımı paylaştım"));
            currentLyrics.add(new com.example.kfmusic.utils.LrcParser.LrcLine(28000, "Gözyaşımı döktüm de yattım"));
        } else {
            currentLyrics.add(new com.example.kfmusic.utils.LrcParser.LrcLine(0, "[Music Playing]"));
            currentLyrics.add(new com.example.kfmusic.utils.LrcParser.LrcLine(10000, "Listening to: " + title));
            currentLyrics.add(new com.example.kfmusic.utils.LrcParser.LrcLine(duration / 4, "Enjoying the rhythm and the beat..."));
            currentLyrics.add(new com.example.kfmusic.utils.LrcParser.LrcLine(duration / 2, "This local music keeps you going!"));
            currentLyrics.add(new com.example.kfmusic.utils.LrcParser.LrcLine(duration * 3 / 4, "Feel the vibe..."));
            currentLyrics.add(new com.example.kfmusic.utils.LrcParser.LrcLine(duration - 5000, "[Outro]"));
        }
        
        displayLyricsText();
    }

    private void displayLyricsText() {
        activeLyricsLineIndex = -1;
        if (tvLyrics != null) {
            if (currentLyrics.isEmpty()) {
                tvLyrics.setText(getString(R.string.no_lyrics_found));
            } else {
                StringBuilder sb = new StringBuilder();
                for (com.example.kfmusic.utils.LrcParser.LrcLine line : currentLyrics) {
                    sb.append(line.text).append("\n");
                }
                tvLyrics.setText(sb.toString().trim());
            }
        }
    }

    private void updateLyricsHighlight(int currentPos) {
        if (currentLyrics.isEmpty() || tvLyrics == null || lyricsContainer == null || lyricsContainer.getVisibility() != View.VISIBLE) return;
        
        int activeIndex = -1;
        for (int i = 0; i < currentLyrics.size(); i++) {
            if (currentPos >= currentLyrics.get(i).timestamp) {
                activeIndex = i;
            } else {
                break;
            }
        }
        
        if (activeIndex == -1) activeIndex = 0;
        
        if (activeIndex != activeLyricsLineIndex) {
            activeLyricsLineIndex = activeIndex;
            
            android.text.SpannableStringBuilder builder = new android.text.SpannableStringBuilder();
            int highlightStart = 0;
            int highlightEnd = 0;
            
            for (int i = 0; i < currentLyrics.size(); i++) {
                String lineText = currentLyrics.get(i).text;
                int start = builder.length();
                builder.append(lineText);
                int end = builder.length();
                
                if (i == activeIndex) {
                    highlightStart = start;
                    highlightEnd = end;
                }
                
                if (i < currentLyrics.size() - 1) {
                    builder.append("\n");
                }
            }
            
            builder.setSpan(new android.text.style.ForegroundColorSpan(getResources().getColor(R.color.accent_blue)),
                    highlightStart, highlightEnd, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                    highlightStart, highlightEnd, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            tvLyrics.setText(builder);
            
            final int finalHighlightStart = highlightStart;
            tvLyrics.post(() -> {
                android.text.Layout layout = tvLyrics.getLayout();
                if (layout != null) {
                    int line = layout.getLineForOffset(finalHighlightStart);
                    int y = layout.getLineTop(line);
                    int scrollY = y - (lyricsContainer.getHeight() / 2) + (layout.getLineBottom(line) - y) / 2;
                    lyricsContainer.smoothScrollTo(0, Math.max(0, scrollY));
                }
            });
        }
    }

    private void updateLikeButton(Song song) {
        boolean isFav = FavoritesManager.getInstance(requireContext()).isFavorite(song.getId());
        if (isFav) {
            btnLike.setImageResource(R.drawable.ic_heart_filled);
            btnLike.setColorFilter(null);
        } else {
            btnLike.setImageResource(R.drawable.ic_heart_border);
            btnLike.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        }
    }

    private void updatePlayPauseButton(boolean isPlaying) {
        if (isPlaying) {
            fabPlayPause.setImageResource(R.drawable.ic_pause);
        } else {
            fabPlayPause.setImageResource(R.drawable.ic_play);
        }
        updateRotationState(isPlaying);
        if (audioVisualizer != null) {
            audioVisualizer.setPlaying(isPlaying);
        }
    }

    private void initRotationAnimation() {
        rotationAnimator = ObjectAnimator.ofFloat(playerAlbumArt, "rotation", 0f, 360f);
        rotationAnimator.setDuration(15000); // 15 seconds per full rotation
        rotationAnimator.setRepeatCount(ValueAnimator.INFINITE);
        rotationAnimator.setInterpolator(new LinearInterpolator());
    }

    private void updateRotationState(boolean isPlaying) {
        if (rotationAnimator == null) {
            initRotationAnimation();
        }
        
        if (isPlaying) {
            if (rotationAnimator.isPaused()) {
                rotationAnimator.resume();
            } else if (!rotationAnimator.isStarted()) {
                rotationAnimator.start();
            }
        } else {
            if (rotationAnimator.isRunning()) {
                rotationAnimator.pause();
            }
        }
    }

    private void updateShuffleButton() {
        boolean shuffle = PlaybackManager.getInstance().isShuffleEnabled();
        btnShuffle.setColorFilter(ContextCompat.getColor(requireContext(), 
                shuffle ? R.color.accent_blue : R.color.text_secondary));
    }

    private void updateRepeatButton() {
        int mode = PlaybackManager.getInstance().getRepeatMode();
        if (mode == PlaybackManager.REPEAT_OFF) {
            btnRepeat.setImageResource(R.drawable.ic_repeat);
            btnRepeat.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        } else if (mode == PlaybackManager.REPEAT_ALL) {
            btnRepeat.setImageResource(R.drawable.ic_repeat);
            btnRepeat.setColorFilter(ContextCompat.getColor(requireContext(), R.color.accent_blue));
        } else if (mode == PlaybackManager.REPEAT_ONE) {
            btnRepeat.setImageResource(R.drawable.ic_repeat_one);
            btnRepeat.setColorFilter(ContextCompat.getColor(requireContext(), R.color.accent_blue));
        }
    }

    private void showQueueDialog() {
        List<Song> queue = PlaybackManager.getInstance().getPlaylist();
        if (queue.isEmpty()) {
            showQueueSheet(queue);
            return;
        }

        showQueueSheet(queue);
    }

    private void showAddToPlaylistDialog() {
        Song current = PlaybackManager.getInstance().getCurrentSong();
        if (current == null) return;

        showPlaylistPicker(current.getId());
    }

    private void showCreatePlaylistDialog(long songId) {
        showCreatePlaylistDialog(songId, null);
    }

    private void showCreatePlaylistDialog(long songId, @Nullable Runnable afterCreate) {
        UiSheetFragment sheet = UiSheetFragment.newInput(
                getString(R.string.new_playlist),
                "",
                getString(R.string.playlist_name_hint),
                getString(R.string.create_and_add),
                getString(R.string.cancel),
                "playlist_create_sheet"
        );

        getParentFragmentManager().setFragmentResultListener("playlist_create_sheet", this, (requestKey, result) -> {
            String action = result.getString(UiSheetFragment.RESULT_ACTION, "");
            if ("primary".equals(action)) {
                String name = result.getString(UiSheetFragment.RESULT_TEXT, "");
                if (!name.isEmpty()) {
                    PlaylistManager pm = PlaylistManager.getInstance(requireContext());
                    pm.createPlaylist(name);
                    pm.addSongToPlaylist(name, songId);
                    Toast.makeText(requireContext(), getString(R.string.playlist_created_and_song_added, name), Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().setFragmentResult("playlist_changed", new Bundle());
                    if (afterCreate != null) {
                        afterCreate.run();
                    }
                }
            }
        });

        showModal(sheet);
    }

    private void showPlaylistPicker(long songId) {
        PlaylistManager pm = PlaylistManager.getInstance(requireContext());
        List<String> playlists = pm.getPlaylists();

        ArrayList<String> items = new ArrayList<>(); 
        items.addAll(playlists);

        UiSheetFragment sheet = UiSheetFragment.newList(
                getString(R.string.choose_playlist),
                "",
                items,
                getString(R.string.close),
                null,
                "playlist_picker_sheet"
        );

        getParentFragmentManager().setFragmentResultListener("playlist_picker_sheet", this, (requestKey, result) -> {
            String action = result.getString(UiSheetFragment.RESULT_ACTION, "");
            if ("item".equals(action)) {
                int index = result.getInt(UiSheetFragment.RESULT_INDEX, -1);
                if (index == 0) {
                    showCreatePlaylistDialog(songId, () -> reopenPlaylistPicker(songId));
                } else if (index > 0 && index - 1 < playlists.size()) {
                    String selected = playlists.get(index - 1);
                    pm.addSongToPlaylist(selected, songId);
                    Toast.makeText(requireContext(), getString(R.string.added_to_playlist, selected), Toast.LENGTH_SHORT).show();
                }
            }
        });

        showModal(sheet);
    }

    private void reopenPlaylistPicker(long songId) {
        getParentFragmentManager().popBackStack();
        new Handler(Looper.getMainLooper()).postDelayed(() -> showPlaylistPicker(songId), 120);
    }

    private void showQueueSheet(List<Song> queue) {
        ArrayList<String> items = new ArrayList<>();
        for (Song s : queue) {
            items.add(s.getTitle());
        }

        UiSheetFragment sheet = UiSheetFragment.newList(
                getString(R.string.current_queue),
                queue.isEmpty() ? getString(R.string.queue_empty) : "",
                items,
                getString(R.string.close),
                getString(R.string.clear_queue),
                "queue_sheet"
        );

        getParentFragmentManager().setFragmentResultListener("queue_sheet", this, (requestKey, result) -> {
            String action = result.getString(UiSheetFragment.RESULT_ACTION, "");
            if ("item".equals(action)) {
                int index = result.getInt(UiSheetFragment.RESULT_INDEX, -1);
                if (index >= 0 && index < queue.size()) {
                    PlaybackManager.getInstance().setPlaylist(queue, index);
                }
            } else if ("secondary".equals(action)) {
                PlaybackManager.getInstance().clearQueue();
                Toast.makeText(requireContext(), getString(R.string.queue_cleared), Toast.LENGTH_SHORT).show();
            }
        });

        showModal(sheet);
    }

    private void showModal(Fragment fragment) {
        if (getParentFragmentManager() != null) {
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                    .add(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private String formatTime(long durationMs) {
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    @Override
    public void onTrackChanged(Song song) {
        if (isAdded()) {
            updateUI(song);
        }
    }

    @Override
    public void onPlaybackStatusChanged(boolean isPlaying) {
        if (isAdded()) {
            updatePlayPauseButton(isPlaying);
        }
    }

    public static android.graphics.Bitmap blur(android.graphics.Bitmap sentBitmap, int radius) {
        android.graphics.Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);
        if (radius < 1) {
            return (null);
        }
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int[] pix = new int[w * h];
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);
        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;
        int r[] = new int[wh];
        int g[] = new int[wh];
        int b[] = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int vmin[] = new int[Math.max(w, h)];
        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int temp[] = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            temp[i] = (i / divsum);
        }
        yw = yi = 0;
        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;
        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;
            for (x = 0; x < w; x++) {
                r[yi] = temp[rsum];
                g[yi] = temp[gsum];
                b[yi] = temp[bsum];
                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;
                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];
                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];
                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];
                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;
                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];
                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];
                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];
                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;
                sir = stack[i + radius];
                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];
                rbs = r1 - Math.abs(i);
                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
                yp += w;
            }
            yp = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                pix[yp] = (0xff000000 & pix[yp]) | (temp[rsum] << 16) | (temp[gsum] << 8) | temp[bsum];
                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;
                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];
                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];
                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];
                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];
                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];
                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;
                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];
                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];
                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];
                yp += w;
            }
        }
        bitmap.setPixels(pix, 0, w, 0, 0, w, h);
        return (bitmap);
    }
}
