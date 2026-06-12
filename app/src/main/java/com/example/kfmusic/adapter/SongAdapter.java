package com.example.kfmusic.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.CheckBox;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kfmusic.R;
import com.example.kfmusic.model.Song;

import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_SONG = 0;
    public static final int TYPE_ARTIST = 1;
    public static final int TYPE_ALBUM = 2;
    public static final int TYPE_PLAYLIST = 3;
    public static final int TYPE_LETTER_HEADER = 5;

    public static class ListItem {
        public static final int TYPE_SONG = 0;
        public static final int TYPE_HEADER = 1;
        public int type;
        public Song song;
        public String header;
        public ListItem(Song song) { this.type = TYPE_SONG; this.song = song; }
        public ListItem(String header) { this.type = TYPE_HEADER; this.header = header; }
    }

    private List<ListItem> songList;
    private int currentType = TYPE_SONG;
    private OnItemClickListener listener;
    private OnOptionsClickListener optionsListener;
    private boolean isSelectionMode = false;
    private java.util.Set<Integer> selectedPositions = new java.util.HashSet<>();
    private OnSelectionModeListener selectionModeListener;
    private static final java.util.concurrent.ExecutorService artThreadPool = java.util.concurrent.Executors.newFixedThreadPool(4);

    public interface OnSelectionModeListener {
        void onSelectionModeChanged(boolean isSelectionMode);
        void onSelectionCountChanged(int count);
    }

    public interface OnItemClickListener {
        void onItemClick(Song song, int position, int type);
    }

    public interface OnOptionsClickListener {
        void onOptionsClick(Song song, View anchorView, int position);
    }

    public SongAdapter(List<Song> songList) {
        this.songList = convertToListItems(songList);
    }

    public SongAdapter(List<Song> songList, OnItemClickListener listener) {
        this.songList = convertToListItems(songList);
        this.listener = listener;
    }

    private static List<ListItem> convertToListItems(List<Song> songs) {
        List<ListItem> items = new java.util.ArrayList<>();
        if (songs != null) {
            for (Song song : songs) {
                items.add(new ListItem(song));
            }
        }
        return items;
    }

    public void setSongs(List<Song> newSongList, int type) {
        List<ListItem> newListCopy = newSongList != null ? convertToListItems(newSongList) : new java.util.ArrayList<>();
        if (this.currentType != type) {
            this.songList = newListCopy;
            this.currentType = type;
            notifyDataSetChanged();
            return;
        }

        final List<ListItem> oldList = this.songList != null ? new java.util.ArrayList<>(this.songList) : new java.util.ArrayList<>();
        this.currentType = type;

        androidx.recyclerview.widget.DiffUtil.DiffResult diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(new androidx.recyclerview.widget.DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldList != null ? oldList.size() : 0;
            }

            @Override
            public int getNewListSize() {
                return newListCopy != null ? newListCopy.size() : 0;
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                ListItem oldItem = oldList.get(oldItemPosition);
                ListItem newItem = newListCopy.get(newItemPosition);
                if (oldItem.type != newItem.type) return false;
                if (oldItem.type == ListItem.TYPE_HEADER) {
                    return oldItem.header != null && oldItem.header.equals(newItem.header);
                }
                return oldItem.song != null && newItem.song != null && oldItem.song.getId() == newItem.song.getId();
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                ListItem oldItem = oldList.get(oldItemPosition);
                ListItem newItem = newListCopy.get(newItemPosition);
                if (oldItem.type != newItem.type) return false;
                if (oldItem.type == ListItem.TYPE_HEADER) {
                    return oldItem.header != null ? oldItem.header.equals(newItem.header) : newItem.header == null;
                }
                Song oldSong = oldItem.song;
                Song newSong = newItem.song;
                return oldSong.getTitle().equals(newSong.getTitle()) &&
                        oldSong.getArtist().equals(newSong.getArtist()) &&
                        oldSong.getAlbum().equals(newSong.getAlbum()) &&
                        oldSong.getDuration() == newSong.getDuration();
            }
        });

        this.songList = newListCopy;
        diffResult.dispatchUpdatesTo(this);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnOptionsClickListener(OnOptionsClickListener optionsListener) {
        this.optionsListener = optionsListener;
    }

    public void setOnSelectionModeListener(OnSelectionModeListener listener) {
        this.selectionModeListener = listener;
    }

    public boolean isSelectionMode() {
        return isSelectionMode;
    }

    public void startSelectionMode(int initialPosition) {
        if (!isSelectionMode) {
            isSelectionMode = true;
            selectedPositions.clear();
            selectedPositions.add(initialPosition);
            notifyDataSetChanged();
            if (selectionModeListener != null) {
                selectionModeListener.onSelectionModeChanged(true);
                selectionModeListener.onSelectionCountChanged(selectedPositions.size());
            }
        }
    }

    public void toggleSelection(int position) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position);
        } else {
            selectedPositions.add(position);
        }
        notifyItemChanged(position);
        if (selectionModeListener != null) {
            selectionModeListener.onSelectionCountChanged(selectedPositions.size());
        }
        if (selectedPositions.isEmpty() && isSelectionMode) {
            exitSelectionMode();
        }
    }

    public void exitSelectionMode() {
        if (isSelectionMode) {
            isSelectionMode = false;
            selectedPositions.clear();
            notifyDataSetChanged();
            if (selectionModeListener != null) {
                selectionModeListener.onSelectionModeChanged(false);
                selectionModeListener.onSelectionCountChanged(0);
            }
        }
    }

    public void selectAll() {
        if (isSelectionMode) {
            selectedPositions.clear();
            for (int i = 0; i < getItemCount(); i++) {
                selectedPositions.add(i);
            }
            notifyDataSetChanged();
            if (selectionModeListener != null) {
                selectionModeListener.onSelectionCountChanged(selectedPositions.size());
            }
        }
    }

    public java.util.List<Song> getSelectedSongs() {
        java.util.List<Song> selected = new java.util.ArrayList<>();
        for (int pos : selectedPositions) {
            if (pos >= 0 && pos < songList.size()) {
                ListItem item = songList.get(pos);
                if (item.type == ListItem.TYPE_SONG) {
                    selected.add(item.song);
                }
            }
        }
        return selected;
    }

    public int getSelectedCount() {
        return selectedPositions.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (songList != null && position >= 0 && position < songList.size()) {
            if (songList.get(position).type == ListItem.TYPE_HEADER) {
                return TYPE_LETTER_HEADER;
            }
        }
        return currentType;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_LETTER_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.header_item, parent, false);
            return new HeaderViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.song_item, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder generalHolder, int position) {
        if (getItemViewType(position) == TYPE_LETTER_HEADER) {
            HeaderViewHolder holder = (HeaderViewHolder) generalHolder;
            holder.headerText.setText(songList.get(position).header);
            return;
        }

        SongViewHolder holder = (SongViewHolder) generalHolder;
        Song song = songList.get(position).song;

        com.example.kfmusic.model.Song currentPlaying = com.example.kfmusic.utils.PlaybackManager.getInstance().getCurrentSong();
        boolean isCurrent = currentPlaying != null && currentPlaying.getId() == song.getId() && currentType == TYPE_SONG;

        if (isCurrent) {
            holder.songTitle.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.accent_blue));
        } else {
            holder.songTitle.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_main));
        }

        holder.checkboxSelect.setVisibility(View.GONE);
        holder.btnSongOptions.setVisibility(View.GONE);

        if (currentType == TYPE_SONG) {
            holder.songTitle.setText(song.getTitle());
            holder.songArtist.setText(song.getArtist());
            holder.songDuration.setText(song.getFormattedDuration());
            holder.songDuration.setVisibility(View.VISIBLE);

            // Task 121: Load Album Art asynchronously using background thread pools
            holder.imgAlbumArt.setImageResource(R.drawable.ic_music_note);
            holder.imgAlbumArt.setImageTintList(android.content.res.ColorStateList.valueOf(holder.itemView.getContext().getResources().getColor(R.color.accent_muted)));
            int pad = (int) (12 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
            holder.imgAlbumArt.setPadding(pad, pad, pad, pad);

            String coverUrl = song.getCoverUrl();
            if (coverUrl != null && !coverUrl.isEmpty()) {
                final android.content.Context ctx = holder.itemView.getContext();
                holder.itemView.setTag(song.getId());

                artThreadPool.execute(() -> {
                    java.net.HttpURLConnection connection = null;
                    java.io.InputStream input = null;
                    try {
                        java.net.URL url = new java.net.URL(coverUrl);
                        connection = (java.net.HttpURLConnection) url.openConnection();
                        connection.setDoInput(true);
                        connection.connect();
                        input = connection.getInputStream();
                        final Bitmap bitmap = BitmapFactory.decodeStream(input);
                        if (bitmap != null) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                if (holder.itemView.getTag() != null && (Long) holder.itemView.getTag() == song.getId()) {
                                    holder.imgAlbumArt.setImageBitmap(bitmap);
                                    holder.imgAlbumArt.setImageTintList(null);
                                    holder.imgAlbumArt.setPadding(0, 0, 0, 0);
                                }
                            });
                        }
                    } catch (Exception e) {
                        // ignore
                    } finally {
                        if (input != null) {
                            try { input.close(); } catch (Exception ignored) {}
                        }
                        if (connection != null) {
                            connection.disconnect();
                        }
                    }
                });
            } else {
                long albumId = song.getAlbumId();
                if (albumId > 0) {
                    final android.content.Context ctx = holder.itemView.getContext();
                    holder.itemView.setTag(song.getId());

                    artThreadPool.execute(() -> {
                        java.io.InputStream in = null;
                        try {
                            Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
                            Uri uri = android.content.ContentUris.withAppendedId(sArtworkUri, albumId);
                            in = ctx.getContentResolver().openInputStream(uri);
                            final Bitmap bitmap = BitmapFactory.decodeStream(in);
                            if (bitmap != null) {
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    if (holder.itemView.getTag() != null && (Long) holder.itemView.getTag() == song.getId()) {
                                        holder.imgAlbumArt.setImageBitmap(bitmap);
                                        holder.imgAlbumArt.setImageTintList(null);
                                        holder.imgAlbumArt.setPadding(0, 0, 0, 0);
                                    }
                                });
                            }
                        } catch (Exception e) {
                            // ignore
                        } finally {
                            if (in != null) {
                                try { in.close(); } catch (Exception ignored) {}
                            }
                        }
                    });
                }
            }
        } else if (currentType == TYPE_PLAYLIST) {
            holder.songTitle.setText(song.getTitle());
            holder.songArtist.setText(song.getArtist());
            holder.songDuration.setVisibility(View.GONE);

            holder.imgAlbumArt.setImageResource(R.drawable.ic_music_note);
            holder.imgAlbumArt.setImageTintList(android.content.res.ColorStateList.valueOf(holder.itemView.getContext().getResources().getColor(R.color.accent_muted)));
            int pad = (int) (12 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
            holder.imgAlbumArt.setPadding(pad, pad, pad, pad);

            String coverUrl = song.getCoverUrl();
            if (coverUrl != null && !coverUrl.isEmpty()) {
                holder.itemView.setTag(song.getId());
                final android.content.Context ctx = holder.itemView.getContext();
                artThreadPool.execute(() -> {
                    java.net.HttpURLConnection connection = null;
                    java.io.InputStream input = null;
                    try {
                        java.net.URL url = new java.net.URL(coverUrl);
                        connection = (java.net.HttpURLConnection) url.openConnection();
                        connection.setDoInput(true);
                        connection.connect();
                        input = connection.getInputStream();
                        final Bitmap bitmap = BitmapFactory.decodeStream(input);
                        if (bitmap != null) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                if (holder.itemView.getTag() != null && (Long) holder.itemView.getTag() == song.getId()) {
                                    holder.imgAlbumArt.setImageBitmap(bitmap);
                                    holder.imgAlbumArt.setImageTintList(null);
                                    holder.imgAlbumArt.setPadding(0, 0, 0, 0);
                                }
                            });
                        }
                    } catch (Exception e) {
                        // ignore
                    } finally {
                        if (input != null) {
                            try { input.close(); } catch (Exception ignored) {}
                        }
                        if (connection != null) {
                            connection.disconnect();
                        }
                    }
                });
            }
        }

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            if (isSelectionMode && currentType == TYPE_SONG) {
                toggleSelection(pos);
            } else {
                if (listener != null) {
                    listener.onItemClick(song, pos, currentType);
                }
            }
        });

        holder.itemView.setOnLongClickListener(v -> false);
    }

    @Override
    public int getItemCount() {
        return songList != null ? songList.size() : 0;
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView songTitle;
        TextView songArtist;
        TextView songDuration;
        ImageButton btnSongOptions;
        CheckBox checkboxSelect;
        android.widget.ImageView imgAlbumArt;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            songTitle = itemView.findViewById(R.id.songTitle);
            songArtist = itemView.findViewById(R.id.songArtist);
            songDuration = itemView.findViewById(R.id.songDuration);
            btnSongOptions = itemView.findViewById(R.id.btnSongOptions);
            checkboxSelect = itemView.findViewById(R.id.checkboxSelect);
            imgAlbumArt = itemView.findViewById(R.id.imgAlbumArt);
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView headerText;
        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            headerText = itemView.findViewById(R.id.headerText);
        }
    }
}
