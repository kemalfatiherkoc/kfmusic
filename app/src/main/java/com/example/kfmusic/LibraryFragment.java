package com.example.kfmusic;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kfmusic.adapter.SongAdapter;
import com.example.kfmusic.db.MusicRepository;
import com.example.kfmusic.model.Song;
import com.example.kfmusic.utils.FavoritesManager;
import com.example.kfmusic.utils.MediaScanner;
import com.example.kfmusic.utils.PlaybackManager;
import com.example.kfmusic.utils.PlaylistManager;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LibraryFragment extends Fragment {

    private static final int TAB_SONGS = 0;
    private static final int TAB_LIKED = 1;
    private static final int TAB_PLAYLISTS = 2;

    private EditText searchInput;
    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private SongAdapter songAdapter;
    private LinearLayout scanProgressBarContainer;
    private TextView tvScanProgress;
    private View emptyState;
    private Button btnRefresh;
    private Button btnCreatePlaylist;
    private Button btnPickMusic;

    private final List<Song> allSongs = new ArrayList<>();
    private final List<Song> displayedItems = new ArrayList<>();
    private String currentSearchQuery = "";
    private int currentTab = TAB_SONGS;
    private boolean hasLoadedOnce = false;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private final ActivityResultLauncher<Intent> pickMusicLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != android.app.Activity.RESULT_OK || result.getData() == null) {
                    return;
                }
                importSelectedMusic(result.getData());
            });


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);

        searchInput = view.findViewById(R.id.searchInput);
        tabLayout = view.findViewById(R.id.tabLayout);
        recyclerView = view.findViewById(R.id.songRecyclerView);
        emptyState = view.findViewById(R.id.emptyState);
        btnCreatePlaylist = view.findViewById(R.id.btnCreatePlaylist);
        btnPickMusic = view.findViewById(R.id.btnPickMusic);
        scanProgressBarContainer = view.findViewById(R.id.scanProgressBarContainer);
        tvScanProgress = view.findViewById(R.id.tvScanProgress);

        view.findViewById(R.id.btnSettings).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateTo(new SettingsFragment(), true);
            }
        });

        songAdapter = new SongAdapter(displayedItems, this::handleItemClick);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(songAdapter);

        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.songs_tab)));
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.liked_songs)));
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.playlists_tab)));

        setupListeners();

        int savedTab = requireContext().getSharedPreferences("kfmusic_settings", android.content.Context.MODE_PRIVATE).getInt("last_selected_tab", 0);
        TabLayout.Tab tab = tabLayout.getTabAt(savedTab);
        if (tab != null) {
            tab.select();
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!hasLoadedOnce) {
            loadMusicData(false);
        }
    }

    private void setupListeners() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                if (isAdded()) {
                    requireContext().getSharedPreferences("kfmusic_settings", android.content.Context.MODE_PRIVATE)
                            .edit().putInt("last_selected_tab", currentTab).apply();
                }
                updateDisplayedList();
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim().toLowerCase(Locale.US);
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                searchRunnable = LibraryFragment.this::updateDisplayedList;
                searchHandler.postDelayed(searchRunnable, 250);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnCreatePlaylist.setOnClickListener(v -> showCreatePlaylistDialog());
        btnPickMusic.setOnClickListener(v -> openMusicPicker());

        getParentFragmentManager().setFragmentResultListener("playlist_changed", this, (requestKey, result) -> loadMusicData(false));
    }

    public void loadMusicData(boolean forceScan) {
        scanProgressBarContainer.setVisibility(View.VISIBLE);
        tvScanProgress.setText(getString(R.string.loading_music));

        boolean canScanLocal = MediaScanner.hasStoragePermission(requireContext());

        if (!forceScan && !canScanLocal) {
            List<Song> cachedSongs = new MusicRepository(requireContext()).getCachedSongs();
            if (!cachedSongs.isEmpty()) {
                allSongs.clear();
                allSongs.addAll(cachedSongs);
                hasLoadedOnce = true;
                scanProgressBarContainer.setVisibility(View.GONE);
                updateDisplayedList();
                return;
            }
        }

        MediaScanner.scanAvailableMusicAsync(requireContext(), canScanLocal || forceScan, new MediaScanner.ScanCallback() {
            @Override
            public void onProgress(int progress, int total) {
                if (isAdded()) {
                    tvScanProgress.setText(getString(R.string.loading_music) + " " + progress + "/" + total);
                }
            }

            @Override
            public void onComplete(List<Song> songs) {
                if (!isAdded()) return;
                allSongs.clear();
                allSongs.addAll(songs);
                hasLoadedOnce = true;
                scanProgressBarContainer.setVisibility(View.GONE);
                updateDisplayedList();
            }
        });
    }

    private void openMusicPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        pickMusicLauncher.launch(intent);
    }

    private void importSelectedMusic(Intent data) {
        List<Uri> uris = new ArrayList<>();
        if (data.getClipData() != null) {
            for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                uris.add(data.getClipData().getItemAt(i).getUri());
            }
        } else if (data.getData() != null) {
            uris.add(data.getData());
        }

        if (uris.isEmpty()) {
            return;
        }

        List<Song> selectedSongs = new ArrayList<>();
        for (Uri uri : uris) {
            persistReadPermission(uri);
            Song song = MediaScanner.createSongFromUri(requireContext(), uri);
            if (song != null && song.getDuration() >= 0) {
                selectedSongs.add(song);
            }
        }

        if (selectedSongs.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.import_music_failed), Toast.LENGTH_SHORT).show();
            return;
        }

        new MusicRepository(requireContext()).cacheSongs(selectedSongs);
        mergeSongs(selectedSongs);
        currentTab = TAB_SONGS;
        TabLayout.Tab songsTab = tabLayout.getTabAt(TAB_SONGS);
        if (songsTab != null) {
            songsTab.select();
        } else {
            updateDisplayedList();
        }
        Toast.makeText(requireContext(), getString(R.string.import_music_success, selectedSongs.size()), Toast.LENGTH_SHORT).show();
    }

    private void persistReadPermission(Uri uri) {
        try {
            requireContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {
            // Some providers grant only a temporary read permission; playback still works in-session.
        }
    }

    private void mergeSongs(List<Song> songs) {
        Map<Long, Song> merged = new LinkedHashMap<>();
        for (Song song : allSongs) {
            merged.put(song.getId(), song);
        }
        for (Song song : songs) {
            merged.put(song.getId(), song);
        }
        allSongs.clear();
        allSongs.addAll(merged.values());
        hasLoadedOnce = true;
        updateDisplayedList();
    }

    private void updateDisplayedList() {
        displayedItems.clear();

        if (currentTab == TAB_SONGS) {
            btnCreatePlaylist.setVisibility(View.GONE);
            for (Song song : allSongs) {
                if (matchesSearch(song)) {
                    displayedItems.add(song);
                }
            }
            songAdapter.setSongs(displayedItems, SongAdapter.TYPE_SONG);
        } else if (currentTab == TAB_LIKED) {
            btnCreatePlaylist.setVisibility(View.GONE);
            List<Song> favorites = FavoritesManager.getInstance(requireContext()).getFavoriteSongs();
            for (Song song : favorites) {
                if (matchesSearch(song)) {
                    displayedItems.add(song);
                }
            }
            songAdapter.setSongs(displayedItems, SongAdapter.TYPE_SONG);
        } else {
            btnCreatePlaylist.setVisibility(View.VISIBLE);
            List<String> playlists = PlaylistManager.getInstance(requireContext()).getPlaylists();
            for (int i = 0; i < playlists.size(); i++) {
                String name = playlists.get(i);
                if (!currentSearchQuery.isEmpty() && !name.toLowerCase(Locale.US).contains(currentSearchQuery)) {
                    continue;
                }
                List<Song> songsInPlaylist = getSongsForPlaylist(name);
                Song playlistSong = new Song(-5000 - i, name, getString(R.string.playlist_tab_playlist), getString(R.string.playlist_tab_playlist), 0,
                        songsInPlaylist.isEmpty() ? "" : songsInPlaylist.get(0).getFilePath());
                if (!songsInPlaylist.isEmpty()) {
                    playlistSong.setCoverUrl(songsInPlaylist.get(0).getCoverUrl());
                }
                displayedItems.add(playlistSong);
            }
            songAdapter.setSongs(displayedItems, SongAdapter.TYPE_PLAYLIST);
        }

        boolean hasSongs = !displayedItems.isEmpty();
        recyclerView.setVisibility(hasSongs ? View.VISIBLE : View.GONE);

        if (!hasSongs) {
            if (emptyState instanceof android.view.ViewStub) {
                emptyState = ((android.view.ViewStub) emptyState).inflate();
            }
            if (emptyState != null) {
                emptyState.setVisibility(View.VISIBLE);
                TextView emptyTitle = emptyState.findViewById(R.id.tvEmptyTitle);
                TextView emptyDesc = emptyState.findViewById(R.id.tvEmptyDesc);
                ImageView emptyIcon = emptyState.findViewById(R.id.ivEmptyIcon);
                btnRefresh = emptyState.findViewById(R.id.btnRefresh);
                if (btnRefresh != null) {
                    btnRefresh.setOnClickListener(v -> loadMusicData(true));
                }

                if (emptyTitle != null && emptyDesc != null && emptyIcon != null) {
                    if (currentTab == TAB_PLAYLISTS) {
                        emptyTitle.setText(getString(R.string.playlists_empty));
                        emptyDesc.setText(getString(R.string.playlists_empty_desc));
                        emptyIcon.setImageResource(R.drawable.ic_queue);
                        if (btnRefresh != null) btnRefresh.setVisibility(View.GONE);
                    } else if (currentTab == TAB_LIKED) {
                        emptyTitle.setText(getString(R.string.liked_empty));
                        emptyDesc.setText(getString(R.string.liked_empty_desc));
                        emptyIcon.setImageResource(R.drawable.ic_heart_border);
                        if (btnRefresh != null) btnRefresh.setVisibility(View.GONE);
                    } else if (!currentSearchQuery.isEmpty()) {
                        emptyTitle.setText(getString(R.string.no_search_results));
                        emptyDesc.setText(getString(R.string.no_search_results_desc, currentSearchQuery));
                        emptyIcon.setImageResource(R.drawable.ic_search);
                        if (btnRefresh != null) btnRefresh.setVisibility(View.GONE);
                    } else {
                        emptyTitle.setText(getString(R.string.no_music_available));
                        emptyDesc.setText(getString(R.string.no_music_desc));
                        emptyIcon.setImageResource(R.drawable.ic_cloud);
                        if (btnRefresh != null) btnRefresh.setVisibility(View.VISIBLE);
                    }
                }
            }
        } else {
            if (emptyState != null && !(emptyState instanceof android.view.ViewStub)) {
                emptyState.setVisibility(View.GONE);
            }
        }
    }

    private boolean matchesSearch(Song song) {
        if (currentSearchQuery.isEmpty()) return true;
        String title = song.getTitle() != null ? song.getTitle().toLowerCase(Locale.US) : "";
        String artist = song.getArtist() != null ? song.getArtist().toLowerCase(Locale.US) : "";
        String album = song.getAlbum() != null ? song.getAlbum().toLowerCase(Locale.US) : "";
        return title.contains(currentSearchQuery) || artist.contains(currentSearchQuery) || album.contains(currentSearchQuery);
    }

    private List<Song> getSongsForPlaylist(String playlistName) {
        List<Song> result = new ArrayList<>();
        if (playlistName == null) return result;

        java.util.Set<String> songIds = PlaylistManager.getInstance(requireContext()).getSongsInPlaylist(playlistName);
        for (Song song : new MusicRepository(requireContext()).getCachedSongs()) {
            if (songIds.contains(String.valueOf(song.getId()))) {
                result.add(song);
            }
        }
        return result;
    }

    private void showCreatePlaylistDialog() {
        UiSheetFragment sheet = UiSheetFragment.newInput(
                getString(R.string.new_playlist),
                "",
                getString(R.string.playlist_name_hint),
                getString(R.string.create_and_add),
                getString(R.string.cancel),
                "library_playlist_create_sheet"
        );

        getParentFragmentManager().setFragmentResultListener("library_playlist_create_sheet", this, (requestKey, result) -> {
            String action = result.getString(UiSheetFragment.RESULT_ACTION, "");
            if ("primary".equals(action)) {
                String name = result.getString(UiSheetFragment.RESULT_TEXT, "");
                if (!name.isEmpty()) {
                    PlaylistManager pm = PlaylistManager.getInstance(requireContext());
                    pm.createPlaylist(name);
                    getParentFragmentManager().setFragmentResult("playlist_changed", new Bundle());
                    loadMusicData(false);
                }
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

    private void handleItemClick(Song song, int position, int type) {
        if (song == null) return;

        if (type == SongAdapter.TYPE_SONG) {
            PlaybackManager.getInstance().setPlaylist(new ArrayList<>(displayedItems), position);
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showMiniPlayer(true);
            }
        } else if (type == SongAdapter.TYPE_PLAYLIST) {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateTo(PlaylistDetailFragment.newInstance(song.getTitle()), true);
            }
        }
    }
}
