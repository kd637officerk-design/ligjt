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
        String alias = requiredAlias();
        boolean granted = getPermissionState(alias) == PermissionState.GRANTED;
        JSObject res = new JSObject();
        res.put("granted", granted);
        call.resolve(res);
    }

    @PluginMethod
    public void scanMedia(PluginCall call) {
        JSArray files = new JSArray();
        scanStore(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "audio", files);
        scanStore(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video", files);
        JSObject res = new JSObject();
        res.put("files", files);
        call.resolve(res);
    }

    private void scanStore(Uri contentUri, String kind, JSArray out) {
        ContentResolver cr = getContext().getContentResolver();
        String[] projection = {
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.TITLE,
            "duration",
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
        };

        try (Cursor cursor = cr.query(contentUri, projection, null, null, MediaStore.MediaColumns.DATE_ADDED + " DESC")) {
            if (cursor == null) return;
            int idCol = cursor.getColumnIndex(MediaStore.MediaColumns._ID);
            int nameCol = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
            int titleCol = cursor.getColumnIndex(MediaStore.MediaColumns.TITLE);
            int durCol = cursor.getColumnIndex("duration");
            int sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE);
            int mimeCol = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE);
            int dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
            int bucketCol = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME);

            int artistCol = kind.equals("audio") ? cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST) : -1;
            int albumCol = kind.equals("audio") ? cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM) : -1;

            int count = 0;
            while (cursor.moveToNext() && count < 500) {
                long id = idCol >= 0 ? cursor.getLong(idCol) : 0;
                String displayName = nameCol >= 0 ? cursor.getString(nameCol) : null;
                String title = titleCol >= 0 ? cursor.getString(titleCol) : null;
                String cleanTitle = (title != null && !title.isEmpty()) ? title : (displayName != null ? displayName.replaceFirst("\\.[^.]+$", "") : "Track");
                long durMs = durCol >= 0 ? cursor.getLong(durCol) : 0;
                long size = sizeCol >= 0 ? cursor.getLong(sizeCol) : 0;
                String mime = mimeCol >= 0 ? cursor.getString(mimeCol) : "";
                String dataPath = dataCol >= 0 ? cursor.getString(dataCol) : null;
                String bucket = bucketCol >= 0 ? cursor.getString(bucketCol) : null;
                String artist = artistCol >= 0 ? cursor.getString(artistCol) : null;
                String album = albumCol >= 0 ? cursor.getString(albumCol) : null;

                String playableUri;
                if (dataPath != null && new File(dataPath).canRead()) {
                    playableUri = "file://" + dataPath;
                } else {
                    playableUri = ContentUris.withAppendedId(contentUri, id).toString();
                }

                String folder = "Internal Storage/" + (kind.equals("video") ? "Videos" : "Music") + (bucket != null && !bucket.isEmpty() ? "/" + bucket : "");

                JSObject item = new JSObject();
                item.put("name", cleanTitle);
                if (artist != null && !artist.equals("<unknown>")) item.put("artist", artist);
                if (album != null) item.put("album", album);
                item.put("folder", folder);
                item.put("kind", kind);
                item.put("uri", playableUri);
                item.put("size", size);
                item.put("duration", durMs / 1000.0);
                item.put("mimeType", mime != null ? mime : "");
                out.put(item);
                count++;
            }
        } catch (Exception ignored) {
            // Skip inaccessible MediaStore collections
        }
    }
}
