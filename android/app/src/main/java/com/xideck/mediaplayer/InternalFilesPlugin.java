package com.xideck.mediaplayer;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

@CapacitorPlugin(
    name = "InternalFiles",
    permissions = {
        @Permission(
            alias = "media33",
            strings = {
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO
            }
        ),
        @Permission(
            alias = "legacyStorage",
            strings = {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
        ),
        @Permission(
            alias = "notifications",
            strings = {
                Manifest.permission.POST_NOTIFICATIONS
            }
        )
    }
)
public class InternalFilesPlugin extends Plugin {

    private String requiredAlias() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? "media33" : "legacyStorage";
    }

    @PluginMethod
    public void requestStoragePermission(PluginCall call) {
        String alias = requiredAlias();
        if (getPermissionState(alias) == PermissionState.GRANTED) {
            JSObject res = new JSObject();
            res.put("granted", true);
            call.resolve(res);
            return;
        }
        requestPermissionForAlias(alias, call, "storagePermissionCallback");
    }

    @PermissionCallback
    private void storagePermissionCallback(PluginCall call) {
        boolean granted = getPermissionState(requiredAlias()) == PermissionState.GRANTED;
        JSObject res = new JSObject();
        res.put("granted", granted);
        call.resolve(res);
    }

    @PluginMethod
    public void requestNotificationPermission(PluginCall call) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            getPermissionState("notifications") == PermissionState.GRANTED) {
            JSObject res = new JSObject();
            res.put("granted", true);
            call.resolve(res);
            return;
        }
        requestPermissionForAlias("notifications", call, "notificationPermissionCallback");
    }

    @PermissionCallback
    private void notificationPermissionCallback(PluginCall call) {
        JSObject res = new JSObject();
        res.put("granted", Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            getPermissionState("notifications") == PermissionState.GRANTED);
        call.resolve(res);
    }

    @PluginMethod
    public void scanMedia(PluginCall call) {
        JSArray files = new JSArray();
        scanAudio(files);
        scanVideos(files);
        JSObject res = new JSObject();
        res.put("files", files);
        res.put("count", files.length());
        call.resolve(res);
    }

    private void scanAudio(JSArray out) {
        ContentResolver cr = getContext().getContentResolver();
        String[] projection = new String[] {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.DATA
        };

        try (Cursor cursor = cr.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            MediaStore.Audio.Media.IS_MUSIC + "!=0 OR " + MediaStore.Audio.Media.IS_MUSIC + " IS NULL",
            null,
            MediaStore.Audio.Media.DATE_ADDED + " DESC"
        )) {
            if (cursor == null) return;
            int idCol = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int nameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);
            int titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE);
            int mimeCol = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE);
            int artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            int yearCol = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR);
            int trackCol = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK);
            int addedCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED);
            int modifiedCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED);
            int dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);

            while (cursor.moveToNext()) {
                long id = idCol >= 0 ? cursor.getLong(idCol) : 0L;
                String displayName = value(cursor, nameCol);
                String title = value(cursor, titleCol);
                String cleanTitle = (title != null && !title.trim().isEmpty())
                    ? title : stripExtension(displayName != null ? displayName : "Track");
                long durationMs = number(cursor, durationCol);
                long size = number(cursor, sizeCol);
                String artist = value(cursor, artistCol);
                String album = value(cursor, albumCol);
                long albumId = number(cursor, albumIdCol);
                long year = number(cursor, yearCol);
                long track = number(cursor, trackCol);
                long dateAdded = number(cursor, addedCol);
                long dateModified = number(cursor, modifiedCol);

                Uri mediaUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                String dataPath = value(cursor, dataCol);
                if (dataPath != null && new File(dataPath).canRead()) mediaUri = Uri.fromFile(new File(dataPath));
                String artworkPath = albumId > 0 ? copyAlbumArtwork(albumId) : null;

                JSObject item = new JSObject();
                item.put("id", "audio:" + id);
                item.put("name", cleanTitle);
                item.put("artist", cleanArtist(artist));
                item.put("album", cleanArtist(album));
                item.put("folder", "Internal Storage/Music");
                item.put("kind", "audio");
                item.put("uri", mediaUri.toString());
                item.put("artworkPath", artworkPath != null ? artworkPath : "");
                item.put("size", size);
                item.put("duration", durationMs / 1000.0);
                item.put("mimeType", value(cursor, mimeCol) != null ? value(cursor, mimeCol) : "");
                item.put("year", year);
                item.put("trackNumber", track);
                item.put("dateAdded", dateAdded);
                item.put("dateModified", dateModified);
                item.put("albumId", albumId);
                out.put(item);
            }
        } catch (Exception ignored) {
            // An unavailable MediaStore column/provider should not crash the scan.
        }
    }

    private void scanVideos(JSArray out) {
        ContentResolver cr = getContext().getContentResolver();
        String[] projection = new String[] {
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.DATA
        };
        try (Cursor cursor = cr.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, MediaStore.Video.Media.DATE_ADDED + " DESC"
        )) {
            if (cursor == null) return;
            int idCol = cursor.getColumnIndex(MediaStore.Video.Media._ID);
            int nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME);
            int titleCol = cursor.getColumnIndex(MediaStore.Video.Media.TITLE);
            int durationCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION);
            int sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE);
            int mimeCol = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE);
            int addedCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED);
            int modifiedCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED);
            int dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA);

            while (cursor.moveToNext()) {
                long id = idCol >= 0 ? cursor.getLong(idCol) : 0L;
                String displayName = value(cursor, nameCol);
                String title = value(cursor, titleCol);
                String cleanTitle = (title != null && !title.trim().isEmpty())
                    ? title : stripExtension(displayName != null ? displayName : "Video");
                Uri mediaUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
                String dataPath = value(cursor, dataCol);
                if (dataPath != null && new File(dataPath).canRead()) mediaUri = Uri.fromFile(new File(dataPath));

                JSObject item = new JSObject();
                item.put("id", "video:" + id);
                item.put("name", cleanTitle);
                item.put("artist", "");
                item.put("album", "");
                item.put("folder", "Internal Storage/Videos");
                item.put("kind", "video");
                item.put("uri", mediaUri.toString());
                item.put("artworkPath", "");
                item.put("size", number(cursor, sizeCol));
                item.put("duration", number(cursor, durationCol) / 1000.0);
                item.put("mimeType", value(cursor, mimeCol) != null ? value(cursor, mimeCol) : "");
                item.put("dateAdded", number(cursor, addedCol));
                item.put("dateModified", number(cursor, modifiedCol));
                out.put(item);
            }
        } catch (Exception ignored) {
            // Skip inaccessible video collection.
        }
    }

    private String copyAlbumArtwork(long albumId) {
        File dir = new File(getContext().getCacheDir(), "sowra-album-art");
        if (!dir.exists() && !dir.mkdirs()) return null;
        File target = new File(dir, albumId + ".jpg");
        if (target.exists() && target.length() > 0) return target.getAbsolutePath();

        Uri artUri = new Uri.Builder()
            .scheme("content")
            .authority("media")
            .appendPath("external")
            .appendPath("audio")
            .appendPath("albumart")
            .appendPath(String.valueOf(albumId))
            .build();

        try (InputStream in = getContext().getContentResolver().openInputStream(artUri);
             FileOutputStream out = new FileOutputStream(target)) {
            if (in == null) return null;
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            out.flush();
            return target.length() > 0 ? target.getAbsolutePath() : null;
        } catch (Exception ignored) {
            if (target.exists()) target.delete();
            return null;
        }
    }

    private String value(Cursor c, int index) {
        if (index < 0 || c.isNull(index)) return null;
        return c.getString(index);
    }

    private long number(Cursor c, int index) {
        if (index < 0 || c.isNull(index)) return 0L;
        try { return c.getLong(index); } catch (Exception ignored) { return 0L; }
    }

    private String stripExtension(String name) {
        return name == null ? "Track" : name.replaceFirst("\\.[^.]+$", "");
    }

    private String cleanArtist(String value) {
        return value == null || value.equals("<unknown>") ? "" : value;
    }
}
