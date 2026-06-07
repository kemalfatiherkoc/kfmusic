package com.example.kfmusic;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.kfmusic.model.Song;
import com.example.kfmusic.utils.PlaybackManager;

public class MainActivity extends AppCompatActivity implements PlaybackManager.PlaybackListener {

    private CardView miniPlayerCard;
    private TextView miniPlayerTitle;
    private TextView miniPlayerArtist;
    private ImageView miniPlayerArt;
    private ImageButton miniPlayerPlayPause;
    private ImageButton miniPlayerNext;
    private ProgressBar miniPlayerProgress;
    private final java.util.concurrent.ExecutorService miniArtExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final Runnable progressUpdater = new Runnable() {
        @Override
        public void run() {
            updateProgressBar();
            progressHandler.postDelayed(this, 500);
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Apply window insets for immersive layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize playback manager
        PlaybackManager.getInstance().init(this);

        // Bind Mini Player Views
        miniPlayerCard = findViewById(R.id.miniPlayerCard);
        miniPlayerTitle = findViewById(R.id.miniPlayerTitle);
        miniPlayerArtist = findViewById(R.id.miniPlayerArtist);
        miniPlayerArt = findViewById(R.id.miniPlayerArt);
        miniPlayerPlayPause = findViewById(R.id.miniPlayerPlayPause);
        miniPlayerNext = findViewById(R.id.miniPlayerNext);
        miniPlayerProgress = findViewById(R.id.miniPlayerProgress);

        setupMiniPlayerListeners();

        // Load initial fragment (SplashFragment)
        if (savedInstanceState == null) {
            navigateTo(new SplashFragment(), false);
        }

        // Back button dispatcher: collapse NowPlayingFragment first, then double-press exit
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            private boolean doubleBackToExitPressedOnce = false;
            private final Handler handler = new Handler(Looper.getMainLooper());
            private final Runnable resetExitStateRunnable = () -> doubleBackToExitPressedOnce = false;

            @Override
            public void handleOnBackPressed() {
                Fragment topFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (topFragment instanceof NowPlayingFragment) {
                    getSupportFragmentManager().popBackStack();
                    return;
                }

                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                    return;
                }

                if (doubleBackToExitPressedOnce) {
                    finish();
                    return;
                }

                doubleBackToExitPressedOnce = true;
                android.widget.Toast.makeText(MainActivity.this, getString(R.string.press_back_again), android.widget.Toast.LENGTH_SHORT).show();
                handler.postDelayed(resetExitStateRunnable, 2000);
            }
        });

        // Keyboard open/close listener to hide/show mini-player
        View decorView = getWindow().getDecorView();
        decorView.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            private final int keyboardMinHeight = (int) (100 * getResources().getDisplayMetrics().density);
            private boolean wasKeyboardOpen = false;

            @Override
            public void onGlobalLayout() {
                android.graphics.Rect r = new android.graphics.Rect();
                decorView.getWindowVisibleDisplayFrame(r);
                int heightDiff = decorView.getRootView().getHeight() - (r.bottom - r.top);
                boolean isKeyboardOpen = heightDiff > keyboardMinHeight;
                if (isKeyboardOpen != wasKeyboardOpen) {
                    wasKeyboardOpen = isKeyboardOpen;
                    if (isKeyboardOpen) {
                        showMiniPlayer(false);
                    } else {
                        Song currentSong = PlaybackManager.getInstance().getCurrentSong();
                        boolean isPlaying = PlaybackManager.getInstance().isPlaying();
                        if (currentSong != null && isPlaying) {
                            showMiniPlayer(true);
                        }
                    }
                }
            }
        });

        // Handle deep link audio intent
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(android.content.Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        android.net.Uri data = intent.getData();
        if (android.content.Intent.ACTION_VIEW.equals(action) && data != null) {
            try {
                Song song = com.example.kfmusic.utils.MediaScanner.createSongFromUri(this, data);
                if (song != null) {
                    java.util.List<Song> singleSongList = new java.util.ArrayList<>();
                    singleSongList.add(song);
                    PlaybackManager.getInstance().setPlaylist(singleSongList, 0);
                    showMiniPlayer(true);
                    navigateTo(new NowPlayingFragment(), true);
                }
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Error handling deep link intent", e);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        PlaybackManager.getInstance().addListener(this);
        updateMiniPlayerUI();
        startProgressUpdater();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopProgressUpdater();
        PlaybackManager.getInstance().removeListener(this);
    }

    private void setupMiniPlayerListeners() {
        miniPlayerPlayPause.setOnClickListener(v -> PlaybackManager.getInstance().playOrPause());
        miniPlayerNext.setOnClickListener(v -> PlaybackManager.getInstance().playNext());

        View.OnClickListener openNowPlaying = v -> navigateTo(new NowPlayingFragment(), true);
        findViewById(R.id.miniPlayerInfoContainer).setOnClickListener(openNowPlaying);
        findViewById(R.id.miniPlayerArt).setOnClickListener(openNowPlaying);
    }

    public void navigateTo(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        
        if (addToBackStack) {
            transaction.setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
            );
            transaction.replace(R.id.fragment_container, fragment);
            transaction.addToBackStack(null);
        } else {
            transaction.replace(R.id.fragment_container, fragment);
        }
        
        transaction.commit();
    }

    public void showMiniPlayer(boolean show) {
        if (miniPlayerCard != null) {
            miniPlayerCard.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void startProgressUpdater() {
        progressHandler.removeCallbacks(progressUpdater);
        progressHandler.post(progressUpdater);
    }

    private void stopProgressUpdater() {
        progressHandler.removeCallbacks(progressUpdater);
    }

    private void updateProgressBar() {
        if (miniPlayerProgress == null) return;
        int duration = PlaybackManager.getInstance().getDuration();
        int position = PlaybackManager.getInstance().getCurrentPosition();
        if (duration > 0) {
            miniPlayerProgress.setProgress((int) (1000L * position / duration));
        } else {
            miniPlayerProgress.setProgress(0);
        }
    }

    private void updateMiniPlayerUI() {
        Song currentSong = PlaybackManager.getInstance().getCurrentSong();
        boolean isPlaying = PlaybackManager.getInstance().isPlaying();
        if (currentSong != null) {
            miniPlayerTitle.setText(currentSong.getTitle());
            miniPlayerArtist.setText(currentSong.getArtist());
            loadMiniPlayerArtwork(currentSong);
            showMiniPlayer(true);
            updatePlayPauseIcon(isPlaying);
        } else {
            showMiniPlayer(false);
        }
    }

    private void loadMiniPlayerArtwork(Song song) {
        if (miniPlayerArt == null || song == null) return;

        miniPlayerArt.setImageResource(R.drawable.ic_music_note);
        miniPlayerArt.setImageTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.accent_blue)));
        int pad = (int) (12 * getResources().getDisplayMetrics().density);
        miniPlayerArt.setPadding(pad, pad, pad, pad);

        String coverUrl = song.getCoverUrl();
        if (coverUrl == null || coverUrl.isEmpty()) {
            return;
        }

        miniPlayerArt.setTag(song.getId());
        miniArtExecutor.execute(() -> {
            try {
                java.net.URL url = new java.net.URL(coverUrl);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                java.io.InputStream input = connection.getInputStream();
                final android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(input);
                if (bitmap != null) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (miniPlayerArt != null && miniPlayerArt.getTag() != null && (Long) miniPlayerArt.getTag() == song.getId()) {
                            miniPlayerArt.setImageBitmap(bitmap);
                            miniPlayerArt.setImageTintList(null);
                            miniPlayerArt.setPadding(0, 0, 0, 0);
                        }
                    });
                }
            } catch (Exception ignored) {
            }
        });
    }

    private void updatePlayPauseIcon(boolean isPlaying) {
        if (isPlaying) {
            miniPlayerPlayPause.setImageResource(R.drawable.ic_pause);
        } else {
            miniPlayerPlayPause.setImageResource(R.drawable.ic_play);
        }
    }

    @Override
    public void onTrackChanged(Song song) {
        updateMiniPlayerUI();
    }

    @Override
    public void onPlaybackStatusChanged(boolean isPlaying) {
        updatePlayPauseIcon(isPlaying);
        updateMiniPlayerUI();
        if (isPlaying) {
            startProgressUpdater();
        } else {
            stopProgressUpdater();
            updateProgressBar();
        }
    }
}
