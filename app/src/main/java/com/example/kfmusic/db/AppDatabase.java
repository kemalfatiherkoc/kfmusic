package com.example.kfmusic.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {SongEntity.class, PlaylistEntity.class, PlaylistSongCrossRef.class, QueueItemEntity.class, ArtistMetaEntity.class, AlbumMetaEntity.class}, version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;

    public abstract SongDao songDao();
    public abstract PlaylistDao playlistDao();
    public abstract QueueDao queueDao();

    public static final androidx.room.migration.Migration MIGRATION_1_2 = new androidx.room.migration.Migration(1, 2) {
        @Override
        public void migrate(@androidx.annotation.NonNull androidx.sqlite.db.SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `queue_items` (`songId` INTEGER NOT NULL, `orderIndex` INTEGER NOT NULL, PRIMARY KEY(`songId`))");
        }
    };

    public static final androidx.room.migration.Migration MIGRATION_3_4 = new androidx.room.migration.Migration(3, 4) {
        @Override
        public void migrate(@androidx.annotation.NonNull androidx.sqlite.db.SupportSQLiteDatabase database) {
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_title` ON `songs` (`title`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_artist` ON `songs` (`artist`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_album` ON `songs` (`album`)");
            database.execSQL("CREATE TABLE IF NOT EXISTS `artist_metadata` (`artistName` TEXT NOT NULL, `bio` TEXT, PRIMARY KEY(`artistName`))");
            database.execSQL("CREATE TABLE IF NOT EXISTS `album_metadata` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `albumName` TEXT NOT NULL, `artistName` TEXT, `year` INTEGER NOT NULL)");
        }
    };

    private static final RoomDatabase.Callback roomCallback = new RoomDatabase.Callback() {
        @Override
        public void onOpen(@androidx.annotation.NonNull androidx.sqlite.db.SupportSQLiteDatabase db) {
            super.onOpen(db);
            try (android.database.Cursor cursor = db.query("PRAGMA integrity_check")) {
                if (cursor != null && cursor.moveToFirst()) {
                    String result = cursor.getString(0);
                    android.util.Log.d("AppDatabase", "PRAGMA integrity_check result: " + result);
                    if (!"ok".equalsIgnoreCase(result)) {
                        android.util.Log.e("AppDatabase", "Database corruption detected! Result: " + result);
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("AppDatabase", "Integrity check failed", e);
            }
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "kfmusic_local.db")
                             .addMigrations(MIGRATION_1_2, MIGRATION_3_4)
                             .addCallback(roomCallback)
                             .fallbackToDestructiveMigration()
                             .allowMainThreadQueries() // Simple, responsive offline calls
                             .build();
                }
            }
        }
        return instance;
    }
}
