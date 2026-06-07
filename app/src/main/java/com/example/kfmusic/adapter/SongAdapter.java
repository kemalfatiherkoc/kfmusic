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

    private List<Song> songList;
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
        this.songList = songList;
    }

    public SongAdapter(List<Song> songList, OnItemClickListener listener) {
        this.songList = songList;
        this.listener = listener;
    }

    public void setSongs(List<Song> newSongList, int type) {
        List<Song> newListCopy = newSongList != null ? new java.util.ArrayList<>(newSongList) : new java.util.ArrayList<>();
        if (this.currentType != type) {
            this.songList = newListCopy;
            this.currentType = type;
            notifyDataSetChanged();
            return;
        }

        final List<Song> oldList = this.songList != null ? new java.util.ArrayList<>(this.songList) : new java.util.ArrayList<>();
        this.currentType = type;

        androidx.recyclerview.widget.DiffUtil.DiffResult diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(new androidx.recyclerview.widget.DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldList != null ? oldList.size() : 0;
            }

            @Override
            public int getNewListSize() {
                return newSongList != null ? newSongList.size() : 0;
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                Song oldSong = oldList.get(oldItemPosition);
                Song newSong = newListCopy.get(newItemPosition);
                return oldSong.getId() == newSong.getId();
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Song oldSong = oldList.get(oldItemPosition);
                Song newSong = newListCopy.get(newItemPosition);
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
                selected.add(songList.get(pos));
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
            if ("HEADER".equals(songList.get(position).getArtist())) {
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
            holder.headerText.setText(songList.get(position).getTitle());
            return;
        }

        SongViewHolder holder = (SongViewHolder) generalHolder;
        Song song = songList.get(position);

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
                    try {
                        java.net.URL url = new java.net.URL(coverUrl);
                        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                        connection.setDoInput(true);
                        connection.connect();
                        java.io.InputStream input = connection.getInputStream();
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
                    }
                });
            } else {
                long albumId = song.getAlbumId();
                if (albumId > 0) {
                    final android.content.Context ctx = holder.itemView.getContext();
                    holder.itemView.setTag(song.getId());

                    artThreadPool.execute(() -> {
                        try {
                            Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
                            Uri uri = android.content.ContentUris.withAppendedId(sArtworkUri, albumId);
                            java.io.InputStream in = ctx.getContentResolver().openInputStream(uri);
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
                    try {
                        java.net.URL url = new java.net.URL(coverUrl);
                        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                        connection.setDoInput(true);
                        connection.connect();
                        java.io.InputStream input = connection.getInputStream();
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
                    }
                });
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode && currentType == TYPE_SONG) {
                toggleSelection(holder.getAdapterPosition());
            } else {
                if (listener != null) {
                    listener.onItemClick(song, holder.getAdapterPosition(), currentType);
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
