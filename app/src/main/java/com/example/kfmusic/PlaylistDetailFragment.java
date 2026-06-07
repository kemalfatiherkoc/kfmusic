package com.example.kfmusic;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kfmusic.adapter.SongAdapter;
import com.example.kfmusic.db.MusicRepository;
import com.example.kfmusic.model.Song;
import com.example.kfmusic.utils.PlaybackManager;
import com.example.kfmusic.utils.PlaylistManager;

import java.util.ArrayList;
import java.util.List;

public class PlaylistDetailFragment extends Fragment {

    private static final String ARG_PLAYLIST_NAME = "playlist_name";

    public static PlaylistDetailFragment newInstance(String playlistName) {
        PlaylistDetailFragment fragment = new PlaylistDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PLAYLIST_NAME, playlistName);
        fragment.setArguments(args);
        return fragment;
    }

    private TextView tvPlaylistTitle;
    private TextView tvPlaylistCount;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private SongAdapter adapter;
    private final List<Song> playlistSongs = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist_detail, container, false);

        tvPlaylistTitle = view.findViewById(R.id.tvPlaylistTitle);
        tvPlaylistCount = view.findViewById(R.id.tvPlaylistCount);
        recyclerView = view.findViewById(R.id.playlistSongsRecycler);
        emptyState = view.findViewById(R.id.emptyState);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        view.findViewById(R.id.btnSortPlaylist).setOnClickListener(v -> showSortPlaylistDialog());

        getParentFragmentManager().setFragmentResultListener("playlist_sort_sheet", this, (requestKey, result) -> {
            String action = result.getString(UiSheetFragment.RESULT_ACTION, "");
            if ("item".equals(action)) {
                int index = result.getInt(UiSheetFragment.RESULT_INDEX, 0);
                String playlistName = getArguments() != null ? getArguments().getString(ARG_PLAYLIST_NAME, "") : "";
                String sortBy = (index == 0) ? "title" : "date_added";
                if (isAdded()) {
                    requireContext().getSharedPreferences("kfmusic_settings", android.content.Context.MODE_PRIVATE)
                            .edit().putString("playlist_sort_" + playlistName, sortBy).apply();
                    bindPlaylist();
                }
            }
        });

        adapter = new SongAdapter(playlistSongs, this::handleItemClick);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        bindPlaylist();
    }

    private void bindPlaylist() {
        String playlistName = getArguments() != null ? getArguments().getString(ARG_PLAYLIST_NAME, "") : "";
        tvPlaylistTitle.setText(playlistName);

        playlistSongs.clear();
        playlistSongs.addAll(loadSongsForPlaylist(playlistName));
        adapter.setSongs(playlistSongs, SongAdapter.TYPE_SONG);

        int count = playlistSongs.size();
        tvPlaylistCount.setText(getString(R.string.playlist_song_count, count));

        boolean hasSongs = !playlistSongs.isEmpty();
        recyclerView.setVisibility(hasSongs ? View.VISIBLE : View.GONE);
        emptyState.setVisibility(hasSongs ? View.GONE : View.VISIBLE);
    }

    private List<Song> loadSongsForPlaylist(String playlistName) {
        if (playlistName == null || playlistName.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String sortBy = requireContext().getSharedPreferences("kfmusic_settings", android.content.Context.MODE_PRIVATE)
                .getString("playlist_sort_" + playlistName, "title");
        return PlaylistManager.getInstance(requireContext()).getSongsInPlaylistSorted(playlistName, sortBy);
    }

    private void showSortPlaylistDialog() {
        java.util.ArrayList<String> items = new java.util.ArrayList<>();
        items.add(getString(R.string.sort_alphabetical));
        items.add(getString(R.string.sort_date_added));
        UiSheetFragment sheet = UiSheetFragment.newList(
                getString(R.string.sort_playlist_title),
                getString(R.string.sort_playlist_desc),
                items,
                getString(R.string.cancel),
                null,
                "playlist_sort_sheet"
        );
        if (getParentFragmentManager() != null) {
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                    .add(R.id.fragment_container, sheet)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void handleItemClick(Song song, int position, int type) {
        if (song == null) return;
        if (type == SongAdapter.TYPE_SONG) {
            PlaybackManager.getInstance().setPlaylist(new ArrayList<>(playlistSongs), position);
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showMiniPlayer(true);
            }
        }
    }
}
