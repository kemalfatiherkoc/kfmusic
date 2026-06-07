package com.example.kfmusic;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.example.kfmusic", appContext.getPackageName());
    }

    @Test
    public void testDatabaseInsertionSpeed() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        com.example.kfmusic.db.AppDatabase db = androidx.room.Room.inMemoryDatabaseBuilder(
                appContext, com.example.kfmusic.db.AppDatabase.class)
                .allowMainThreadQueries()
                .build();

        com.example.kfmusic.db.SongDao songDao = db.songDao();

        java.util.List<com.example.kfmusic.db.SongEntity> list = new java.util.ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            com.example.kfmusic.model.Song song = new com.example.kfmusic.model.Song(
                    i, "Song Title " + i, "Artist " + i, "Album " + i, 180000, "file:///path/" + i);
            list.add(new com.example.kfmusic.db.SongEntity(song));
        }

        long startTime = System.currentTimeMillis();
        songDao.insertSongs(list);
        long endTime = System.currentTimeMillis();

        long duration = endTime - startTime;
        android.util.Log.d("DatabaseTest", "Inserted 1000 songs in: " + duration + " ms");

        assertEquals(1000, songDao.getAllSongs().size());

        db.close();
    }
}