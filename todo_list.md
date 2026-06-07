# KF MUSIC - 3-Day Development TODO List (200+ Tasks)

This document maps out **205 actionable, offline-first development tasks** for the KF Music player, categorized across a tight 3-day implementation timeline. All features leverage local APIs (Content Providers, SQLite/Room, MediaPlayer, and MediaSession) without any cloud server dependency.

---

## Day 1: Core Media Engine, Media Scan, & Local DB Foundation

### 1.1 Data Models & Helpers
- [x] Task 1: Expand [Song.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/model/Song.java) constructor to support a `dateAdded` timestamp field (retrieved from MediaStore).
- [x] Task 2: Add `albumId` (long) property to [Song.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/model/Song.java) to enable loading high-fidelity album art from the local system content URI.
- [x] Task 3: Add `trackNumber` (int) field to [Song.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/model/Song.java) to sort album items correctly.
- [x] Task 4: Add `year` (int) property to [Song.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/model/Song.java) to display release years.
- [x] Task 5: Add `composer` (String) property to [Song.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/model/Song.java) to facilitate deep metadata searching.
- [x] Task 6: Add `genre` (String) property to [Song.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/model/Song.java) to enable local genre grouping.
- [x] Task 7: Add `fileSize` (long) field to [Song.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/model/Song.java) for settings statistics.
- [x] Task 8: Create an `Artist` model class inside package `com.example.kfmusic.model` to store unique artist IDs, names, and track counts.
- [x] Task 9: Create an `Album` model class inside package `com.example.kfmusic.model` to store unique album IDs, titles, artists, track counts, and release years.
- [x] Task 10: Create a `Playlist` model class to wrap playlist configurations (id, name, dateCreated, and trackCount).
- [x] Task 11: Add a constructor check in [Song.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/model/Song.java) to replace empty strings with localized "Unknown Artist" and "Unknown Album".
- [x] Task 12: Implement a custom sorting utility class `com.example.kfmusic.utils.SortUtils` to sort songs by title alphabetically.
- [x] Task 13: Add date-based sorting helper to `SortUtils` (sorting by recent date added).
- [x] Task 14: Add duration-based sorting helper to `SortUtils` (sorting from shortest to longest).
- [x] Task 15: Implement artist-name sorting comparator within `SortUtils` to sort track listings.

### 1.2 Local Media Scanner Enhancements
- [x] Task 16: Refactor [MediaScanner.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/utils/MediaScanner.java) query projection to include `MediaStore.Audio.Media.DATE_ADDED`.
- [x] Task 17: Add `MediaStore.Audio.Media.ALBUM_ID` to [MediaScanner.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/utils/MediaScanner.java) projection.
- [x] Task 18: Add `MediaStore.Audio.Media.TRACK` to [MediaScanner.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/utils/MediaScanner.java) projection.
- [x] Task 19: Add `MediaStore.Audio.Media.YEAR` to [MediaScanner.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/utils/MediaScanner.java) projection.
- [x] Task 20: Add `MediaStore.Audio.Media.SIZE` to [MediaScanner.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/utils/MediaScanner.java) projection.
- [x] Task 21: Implement path-based exclusion logic in [MediaScanner.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/utils/MediaScanner.java) to skip whatsapp audio, system UI sounds, and ringtones.
- [x] Task 22: Add filters in [MediaScanner.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/utils/MediaScanner.java) to ignore tracks shorter than 10 seconds.
- [x] Task 23: Add filters in [MediaScanner.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/utils/MediaScanner.java) to ignore tracks smaller than 100 Kilobytes.
- [x] Task 24: Construct a helper method inside [MediaScanner.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/utils/MediaScanner.java) to query and parse local audio file tags using `MediaMetadataRetriever` if MediaStore attributes return null.
- [x] Task 25: Implement error recovery in [MediaScanner.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/utils/MediaScanner.java) for handling corrupted MP3 files that crash the SQLite query helper.
- [x] Task 26: Create an asynchronous scan helper interface `ScanCallback` to notify UI about scanning progress (e.g. `onProgressUpdated(int percent)`).
- [x] Task 27: Implement an `AsyncTask` or `Thread` inside [MediaScanner.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/utils/MediaScanner.java) to run scans in the background and prevent UI lockup.

### 1.3 Local Database Integration (Room / SQLite)
- [x] Task 28: Add Room dependency to [app/build.gradle.kts](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/build.gradle.kts).
- [x] Task 29: Create a Room Entity class `com.example.kfmusic.db.SongEntity` to map local database songs.
- [x] Task 30: Create a Room Entity class `com.example.kfmusic.db.PlaylistEntity` to map custom offline playlists.
- [x] Task 31: Create a join entity `com.example.kfmusic.db.PlaylistSongCrossRef` to support many-to-many relationships between playlists and tracks.
- [x] Task 32: Create a DAO interface `SongDao` with CRUD queries for favorites and caches.
- [x] Task 33: Create a DAO interface `PlaylistDao` with methods to insert, delete, and fetch playlist tracks.
- [x] Task 34: Create the primary database class `com.example.kfmusic.db.AppDatabase` extending `RoomDatabase`.
- [x] Task 35: Implement a singleton provider for `AppDatabase` inside package `com.example.kfmusic.db`.
- [x] Task 36: Create database migration helper classes for version upgrades.
- [x] Task 37: Write a repository class `com.example.kfmusic.db.MusicRepository` to abstract DB calls.
- [x] Task 38: Write DB tests to verify insertion speed of 1000+ tracks.
- [x] Task 39: Integrate [FavoritesManager.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/utils/FavoritesManager.java) with Room database calls, replacing SharedPreferences.
- [x] Task 40: Integrate [PlaylistManager.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/utils/PlaylistManager.java) with Room database.

### 1.4 Playback Foundation & MediaPlayer Lifecycle
- [x] Task 41: Add error callbacks in [PlaybackManager.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/utils/PlaybackManager.java) to catch file-format errors.
- [x] Task 42: Implement volume fading helper in [PlaybackManager.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/utils/PlaybackManager.java) (fade-out on pause, fade-in on play).
- [x] Task 43: Create a `release()` method in [PlaybackManager.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/utils/PlaybackManager.java) to safely release `MediaPlayer` resources.
- [x] Task 44: Add code to catch `IllegalStateException` inside [PlaybackManager.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/utils/PlaybackManager.java) during fast-forward seeking.
- [x] Task 45: Add support in [PlaybackManager.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/utils/PlaybackManager.java) for custom file source preparation using file descriptors rather than absolute paths to support Android Scoped Storage.
- [x] Task 46: Set the audio stream type explicitly to `AudioAttributes.CONTENT_TYPE_MUSIC` in [PlaybackManager.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/utils/PlaybackManager.java) for proper volume button routing.
- [x] Task 47: Implement `WakeLock` handling in [PlaybackManager.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/utils/PlaybackManager.java) to prevent CPU sleep during playback.
- [x] Task 48: Implement Wi-Fi Lock inside the playback runner when playing local network files (DLNA/Local network shares).

### 1.5 Permission & Splash Workflow
- [x] Task 49: Modify [SplashFragment.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/SplashFragment.java) to check and show rationale if permission was previously denied.
- [x] Task 50: Add checking for Android 14+ granular storage permissions (`Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED` fallback checks if applicable).
- [x] Task 51: Add checking for Android 13+ notification permissions `Manifest.permission.POST_NOTIFICATIONS`.
- [x] Task 52: Create a setting redirect button inside the permission warning view.
- [x] Task 53: Implement a transition animation inside [MainActivity.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/MainActivity.java) when moving from Splash to Library.
- [x] Task 54: Prevent double navigation triggers in [SplashFragment.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/SplashFragment.java) when clicking the Skip button rapidly.
- [x] Task 55: Hide the status bar on the splash screen for an immersive branded entry.

### 1.6 Vector Asset Integration
- [x] Task 56: Fix `ic_play` viewport parameters for pixel perfect alignments.
- [x] Task 57: Fix `ic_pause` layout sizing.
- [x] Task 58: Standardize viewport scale across all control vector drawables (24dp x 24dp).
- [x] Task 59: Add `ic_shuffle.xml` to drawable resources.
- [x] Task 60: Add `ic_repeat.xml` to drawable resources.
- [x] Task 61: Add `ic_repeat_one.xml` to drawable resources.
- [x] Task 62: Add `ic_queue.xml` to drawable resources.
- [x] Task 63: Add `ic_folder.xml` to drawable resources.
- [x] Task 64: Add `ic_sort.xml` to drawable resources.
- [x] Task 65: Add `ic_sleep_timer.xml` to drawable resources.
- [x] Task 66: Add `ic_delete.xml` to drawable resources.
- [x] Task 67: Add `ic_edit.xml` to drawable resources.
- [x] Task 68: Add `ic_volume_up.xml` to drawable resources.
- [x] Task 69: Add `ic_volume_mute.xml` to drawable resources.
- [x] Task 70: Create a custom selector drawable `tab_item_color.xml` to tint tabs depending on active states.

---

## Day 2: Foreground Service, Advanced Queue, Audio Focus & DB Integration

### 2.1 Playback Service & Notification Controls
- [x] Task 71: Create a foreground service class `com.example.kfmusic.utils.PlaybackService` extending `Service`.
- [x] Task 72: Register `PlaybackService` in [AndroidManifest.xml](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/AndroidManifest.xml).
- [x] Task 73: Move `MediaPlayer` lifecycle management from [PlaybackManager.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/utils/PlaybackManager.java) into `PlaybackService`.
- [x] Task 74: Set up Binder class inside `PlaybackService` to allow `MainActivity` to bind and communicate.
- [x] Task 75: Create notification channels for music playback (importance default/low).
- [x] Task 76: Build custom media style notifications using `androidx.media.app.NotificationCompat.MediaStyle`.
- [x] Task 77: Implement Play/Pause action receiver in `PlaybackService`.
- [x] Task 78: Implement Skip Next action receiver in `PlaybackService`.
- [x] Task 79: Implement Skip Previous action receiver in `PlaybackService`.
- [x] Task 80: Implement Close/Stop service button in the notification.
- [x] Task 81: Bind the notification artwork dynamically using the current song's album art URI.
- [x] Task 82: Implement a default placeholder image fallback inside notifications if the song has no album art.
- [x] Task 83: Hook up the lockscreen player controls using Android's MediaSession APIs.
- [x] Task 84: Make notification seekable using MediaStyle session tokens (on Android 10+).
- [x] Task 85: Make notifications disappear when paused and allow swipe to dismiss.

### 2.2 Media Session & Headset Integrations
- [x] Task 86: Initialize `MediaSessionCompat` inside `PlaybackService`.
- [x] Task 87: Set session flags `MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS` and `FLAG_HANDLES_TRANSPORT_CONTROLS`.
- [x] Task 88: Implement `MediaSessionCompat.Callback` methods to receive hardware button clicks.
- [x] Task 89: Create a `HeadsetReceiver` broadcast receiver class to capture `Intent.ACTION_AUDIO_BECOMING_NOISY`.
- [x] Task 90: Implement logic to pause music automatically when headphones are unplugged.
- [x] Task 91: Implement media key listeners to capture single, double, and triple clicks on wired/bluetooth headset controls (Play, Skip, Previous).
- [x] Task 92: Add metadata synchronizer to pass song tags to the system Bluetooth stack (AVRCP).
- [x] Task 93: Fix Bluetooth track information sync delays.

### 2.3 Audio Focus Management
- [x] Task 94: Implement `AudioManager.OnAudioFocusChangeListener` in `PlaybackService`.
- [x] Task 95: Request audio focus dynamically before starting playback (`AudioManager.requestAudioFocus`).
- [x] Task 96: Handle `AudioManager.AUDIOFOCUS_LOSS`: pause playback and release resources.
- [x] Task 97: Handle `AudioManager.AUDIOFOCUS_LOSS_TRANSIENT`: pause playback but keep player prepared.
- [x] Task 98: Handle `AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK`: lower the volume instead of pausing.
- [x] Task 99: Handle `AudioManager.AUDIOFOCUS_GAIN`: restore full volume or resume playback.
- [x] Task 100: Add backward-compatible audio focus request builders (`AudioFocusRequestCompat` for Android Oreo+).
- [x] Task 101: Abandon audio focus immediately when playback is manually paused or stopped.

### 2.4 Queue & Shuffle/Repeat Mechanics
- [x] Task 102: Implement a custom shuffle system in [PlaybackManager.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/utils/PlaybackManager.java) that shuffles tracks without repeating.
- [x] Task 103: Save the original pre-shuffled list order to revert back when shuffle is toggled off.
- [x] Task 104: Add repeat states: `REPEAT_OFF`, `REPEAT_ALL`, `REPEAT_ONE`.
- [x] Task 105: Update completion listener in `MediaPlayer` to check repeat states.
- [x] Task 106: Create a playlist queue cache database table to reload active queues on app relaunch.
- [x] Task 107: Implement "Play Next" feature (adds song directly after current song in queue).
- [x] Task 108: Implement "Add to Queue" feature (appends song to the very end of queue).
- [x] Task 109: Implement drag-and-drop queue item reordering helper.
- [x] Task 110: Add a clear queue function to [PlaybackManager.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/utils/PlaybackManager.java).
- [x] Task 111: Implement "Remove from queue" by song index.

### 2.5 Library UI Tab & RecyclerView Rebuilding
- [x] Task 112: Design a dynamic tab selection pill layout in [fragment_library.xml](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/res/layout/fragment_library.xml) using Material Cards.
- [x] Task 113: Refactor [SongAdapter.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/adapter/SongAdapter.java) to highlight the currently playing song in the list with a colored title or playing icon.
- [x] Task 114: Add multi-select support to [SongAdapter.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/adapter/SongAdapter.java) for batch deleting and adding to playlists.
- [x] Task 115: Add context menu actions (three dots) to song rows.
- [x] Task 116: Integrate fast scrollbars on the RecyclerView for swift alphabet navigation.
- [x] Task 117: Implement letter headers in the list when sorting alphabetically.
- [x] Task 118: Add loading progress spinners in the RecyclerView during background scanning.
- [x] Task 119: Build a custom layout for empty search results.
- [x] Task 120: Implement list animations (adding, removing, filtering items).
- [x] Task 121: Optimize image thumbnail loading using custom background task pools (avoiding image decoding lags).

### 2.6 Local Playlist Operations
- [x] Task 122: Wire up playlist creation logic with database insertion in [LibraryFragment.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/LibraryFragment.java).
- [x] Task 123: Add "Delete Playlist" dialog in the UI.
- [x] Task 124: Add "Rename Playlist" edit field in the UI.
- [x] Task 125: Implement sorting criteria inside playlists (custom, alphabetical, or date-added).
- [x] Task 126: Add "Recently Added" smart playlist (auto-generated by dates).
- [x] Task 127: Add "Most Played" smart playlist (tracks count using database logs).
- [x] Task 128: Add "Recently Played" smart playlist.
- [x] Task 129: Implement batch addition of tracks to a playlist.
- [x] Task 130: Build drag-and-drop row reordering callbacks specifically inside playlist details view.

### 2.7 Database Operations & Repository Wiring
- [x] Task 131: Map database inserts to background threads inside `MusicRepository`.
- [x] Task 132: Write favorite song sync methods to save likes directly in database.
- [x] Task 133: Create database transaction hooks for deleting tracks.
- [x] Task 134: Implement database backup/restore methods (saving DB files locally as JSON or XML).
- [x] Task 135: Optimize query indices for fast searches on SQLite titles, artists, and albums.
- [x] Task 136: Implement file validation during loading (remove from DB if file no longer exists).
- [x] Task 137: Add track playcount incrementer methods inside `MusicRepository`.
- [x] Task 138: Write DB cleanup queries to purge orphan playlist connections.
- [x] Task 139: Expose database track sizes to calculations in [SettingsFragment.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/SettingsFragment.java).
- [x] Task 140: Create database tables caching artist and album bios or metadata tags parsed from local tags.

---

## Day 3: Immersive Player Polish, Equalizer, Settings, & Testing

### 3.1 Immersive Player UI Improvements
- [x] Task 141: Add swipe gestures on the album art in [NowPlayingFragment.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/NowPlayingFragment.java) to skip tracks (swipe left = next, swipe right = previous).
- [x] Task 142: Style progress SeekBar thumb size and state (large when dragging, thin when playing).
- [x] Task 143: Implement high-fidelity album art image rendering using standard bitmap background threads to avoid UI thread lag.
- [x] Task 144: Generate a blurred album art background behind the player using RenderScript/ScriptIntrinsicBlur.
- [x] Task 145: Add rotation animations to the music note symbol when tracks are playing.
- [x] Task 146: Implement custom font sizes dynamically adapting for long song titles to prevent text clipping.
- [x] Task 147: Create a marquee text effect on title and artist fields for extra-long labels.
- [x] Task 148: Add an animated audio visualizer bar at the bottom of [NowPlayingFragment.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/NowPlayingFragment.java).
- [x] Task 149: Implement a localized lyrics view (LRC file reader) that parses and highlights lines in sync with playback.
- [x] Task 150: Add a toggle between lyrics and album art.
- [x] Task 151: Add a fast forward/rewind button on the controls (jump by 10 seconds).

### 3.2 Native Equalizer & Audio Effects
- [x] Task 152: Initialize Android's native `Equalizer` effect using the `MediaPlayer` audio session ID.
- [x] Task 153: Create a settings entry inside [SettingsFragment.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/SettingsFragment.java) to configure the Equalizer.
- [x] Task 154: Build a basic 5-band slider interface for custom audio profiles.
- [x] Task 155: Implement Equalizer presets (Normal, Classical, Dance, Flat, Folk, Metal, Hip Hop, Jazz, Pop, Rock).
- [x] Task 156: Add bass boost slider using `BassBoost` audio effect API.
- [x] Task 157: Add virtualizer slider using `Virtualizer` audio effect API.
- [x] Task 158: Handle release of audio effects when `MediaPlayer` is reset or recreated.
- [x] Task 159: Save equalizer slider presets to SharedPreferences.
- [x] Task 160: Create a quick toggle to enable/disable equalizer without losing configuration.

### 3.3 Settings & File Manager Customizations
- [ ] Task 161: Implement a folder blacklist page inside Settings to ignore specific storage folders.
- [ ] Task 162: Add custom storage scanner target paths (letting users pick folders to scan).
- [x] Task 163: Add a "Rescan Library" button that clears caches and runs a fresh media query.
- [x] Task 164: Create a "Sleep Timer" dialog inside settings (set audio auto-stop after 15, 30, 45, or 60 minutes).
- [x] Task 165: Build a countdown display for the active sleep timer.
- [ ] Task 166: Add a toggle to customize player theme (Soft Navy Dark Mode, Pitch Black Mode, or System Default).
- [x] Task 167: Implement a database cache cleaner button.
- [ ] Task 168: Add a "File Info" details dialog in the library to show path, codec, bitrate, and size of local tracks.
- [ ] Task 169: Implement an option to delete the physical audio file from device storage directly from the app (using Android SAF on 10+).

### 3.4 Library Navigation, Swapping, & View Controllers
- [x] Task 170: Enable sliding transitions for navigating between Library and Settings.
- [x] Task 171: Implement back button overriding in [MainActivity.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/MainActivity.java) (collapse Now Playing before exiting app).
- [x] Task 172: Remember the user's last selected tab in [LibraryFragment.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/LibraryFragment.java) and restore it on restart.
- [x] Task 173: Implement a double-tap back action to exit the app.
- [x] Task 174: Implement deep link handlers to launch the app directly when an audio file is clicked in a file explorer.
- [x] Task 175: Hide the mini-player dynamically in [MainActivity.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/MainActivity.java) when the keyboard is open in the library search input.

### 3.5 Performance Optimizations
- [x] Task 176: Implement layout stubbing for the empty state to reduce initial inflation time of [fragment_library.xml](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/res/layout/fragment_library.xml).
- [x] Task 177: Configure recycler view properties (`setHasFixedSize(true)`) to prevent layout recalculation passes.
- [x] Task 178: Use `DiffUtil` inside [SongAdapter.java](file:///C:/Users/agnes/Desktop/projects/kfmusic/app/src/main/java/com/example/kfmusic/adapter/SongAdapter.java) to refresh modified rows instead of calling expensive `notifyDataSetChanged()`.
- [ ] Task 179: Clean up static context leaks in `PlaybackManager`.
- [ ] Task 180: Benchmark app startup time, keeping it under 300 milliseconds.
- [x] Task 181: Reduce memory footprint of blurred background graphics by scaling bitmaps down (1/10th size) before processing.
- [ ] Task 182: Handle battery optimization requests dynamically (allow background service to bypass aggressive power limits).
- [ ] Task 183: Avoid nested layout weights in XML designs to speed up view drawing.

### 3.6 Edge Cases & Error Handling
- [ ] Task 184: Gracefully handle playback failures when files are deleted externally by another app.
- [ ] Task 185: Manage storage disconnects (e.g. SD Card removal or USB transfer mode).
- [ ] Task 186: Prevent player crashes when audio track duration is reported as negative or zero.
- [ ] Task 187: Add automatic queue shifting when a played track is deleted.
- [ ] Task 188: Catch and log security exceptions when querying internal storage files on restricted Android versions.
- [x] Task 189: Implement database state corruption checks.
- [ ] Task 190: Gracefully handle headphone volume spikes when output changes.

### 3.7 Quality Control, Testing, & Final Audits
- [ ] Task 191: Write Unit tests for `PlaybackManager` track indices shifting.
- [ ] Task 192: Write Unit tests for `MediaScanner` parser filters.
- [ ] Task 193: Create Espresso UI test cases for search input matches.
- [ ] Task 194: Create Espresso UI test cases checking settings page accessibility.
- [ ] Task 195: Test scanning speed with simulated libraries of 100, 500, 2000, and 5000+ local files.
- [ ] Task 196: Verify memory usage profiles under Profiler to identify memory leaks.
- [ ] Task 197: Audit background battery drain of `PlaybackService`.
- [x] Task 198: Ensure complete strict-mode compliance (no network/file actions on the UI thread).
- [ ] Task 199: Conduct accessibility testing using TalkBack screen reader compatibility profiles.
- [ ] Task 200: Verify multi-window and split-screen mode layout responsiveness.
- [ ] Task 201: Check layout alignments on tablets and folding displays.
- [ ] Task 202: Verify dynamic orientation switches (landscape player layout adaptation).
- [ ] Task 203: Audit colors for color-blindness accessibility standards.
- [ ] Task 204: Test Bluetooth AVRCP song information updates on at least 3 model targets.
- [ ] Task 205: Clean up all debugging log messages and verify final release build obfuscation rules (Proguard/R8).
