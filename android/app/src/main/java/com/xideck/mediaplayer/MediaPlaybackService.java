package com.xideck.mediaplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import java.io.File;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.IBinder;

public class MediaPlaybackService extends Service {
    public static final String CHANNEL_ID = "xi_media_playback_channel";
    public static final int NOTIFICATION_ID = 1101;

    public static final String ACTION_UPDATE = "com.xideck.mediaplayer.ACTION_UPDATE";
    public static final String ACTION_STOP_SERVICE = "com.xideck.mediaplayer.ACTION_STOP_SERVICE";
    public static final String ACTION_CMD_PLAY = "com.xideck.mediaplayer.CMD_PLAY";
    public static final String ACTION_CMD_PAUSE = "com.xideck.mediaplayer.CMD_PAUSE";
    public static final String ACTION_CMD_STOP = "com.xideck.mediaplayer.CMD_STOP";
    public static final String ACTION_CMD_RERUN = "com.xideck.mediaplayer.CMD_RERUN";
    public static final String ACTION_CMD_PREV = "com.xideck.mediaplayer.CMD_PREV";
    public static final String ACTION_CMD_NEXT = "com.xideck.mediaplayer.CMD_NEXT";
    public static final String ACTION_CMD_REWIND = "com.xideck.mediaplayer.CMD_REWIND";
    public static final String ACTION_CMD_FAST_FORWARD = "com.xideck.mediaplayer.CMD_FAST_FORWARD";

    private MediaSession mediaSession;
    private NotificationManager notificationManager;
    private Bitmap coverBitmap;

    private String curTitle = "SOWRA";
    private String curArtist = "Ready";
    private String curAlbum = "SOWRA";
    private boolean curPlaying = false;
    private long curDurationMs = 0L;
    private long curPositionMs = 0L;
    private String curArtworkPath = "";
    private String loadedArtworkPath = null;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = getSystemService(NotificationManager.class);
        ensureChannel();
        coverBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.default_album_cover);

        mediaSession = new MediaSession(this, "XIMediaSession");
        mediaSession.setFlags(
            MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS
        );
        mediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                AndroidMediaPlugin.emitMediaAction("play", -1);
            }

            @Override
            public void onPause() {
                AndroidMediaPlugin.emitMediaAction("pause", -1);
            }

            @Override
            public void onStop() {
                AndroidMediaPlugin.emitMediaAction("stop", -1);
                stopSelfAndNotification();
            }

            @Override
            public void onSkipToPrevious() {
                AndroidMediaPlugin.emitMediaAction("prev", -1);
            }

            @Override
            public void onSkipToNext() {
                AndroidMediaPlugin.emitMediaAction("next", -1);
            }

            @Override
            public void onRewind() {
                AndroidMediaPlugin.emitMediaAction("seekback", -1);
            }

            @Override
            public void onFastForward() {
                AndroidMediaPlugin.emitMediaAction("seekforward", -1);
            }

            @Override
            public void onSeekTo(long pos) {
                AndroidMediaPlugin.emitMediaAction("seekto", pos);
            }
        });
        mediaSession.setActive(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }
        String action = intent.getAction();
        if (ACTION_STOP_SERVICE.equals(action)) {
            stopSelfAndNotification();
            return START_NOT_STICKY;
        } else if (ACTION_CMD_PLAY.equals(action)) {
            AndroidMediaPlugin.emitMediaAction("play", -1);
        } else if (ACTION_CMD_PAUSE.equals(action)) {
            AndroidMediaPlugin.emitMediaAction("pause", -1);
        } else if (ACTION_CMD_STOP.equals(action)) {
            AndroidMediaPlugin.emitMediaAction("stop", -1);
            stopSelfAndNotification();
            return START_NOT_STICKY;
        } else if (ACTION_CMD_RERUN.equals(action)) {
            AndroidMediaPlugin.emitMediaAction("rerun", 0);
        } else if (ACTION_CMD_PREV.equals(action)) {
            AndroidMediaPlugin.emitMediaAction("prev", -1);
        } else if (ACTION_CMD_NEXT.equals(action)) {
            AndroidMediaPlugin.emitMediaAction("next", -1);
        } else if (ACTION_CMD_REWIND.equals(action)) {
            AndroidMediaPlugin.emitMediaAction("seekback", -1);
        } else if (ACTION_CMD_FAST_FORWARD.equals(action)) {
            AndroidMediaPlugin.emitMediaAction("seekforward", -1);
        } else if (ACTION_UPDATE.equals(action)) {
            curTitle = intent.getStringExtra("title") != null ? intent.getStringExtra("title") : "SOWRA";
            curArtist = intent.getStringExtra("artist") != null ? intent.getStringExtra("artist") : "Local Media";
            curAlbum = intent.getStringExtra("album") != null ? intent.getStringExtra("album") : "SOWRA";
            curPlaying = intent.getBooleanExtra("isPlaying", false);
            curDurationMs = Math.max(0L, intent.getLongExtra("durationMs",
                (long)(intent.getDoubleExtra("durationSec", 0.0) * 1000.0)));
            curPositionMs = Math.max(0L, intent.getLongExtra("positionMs",
                (long)(intent.getDoubleExtra("positionSec", 0.0) * 1000.0)));
            curArtworkPath = intent.getStringExtra("artworkPath") != null
                ? intent.getStringExtra("artworkPath") : curArtworkPath;
            updateSessionAndNotification();
        }
        return START_STICKY;
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID,
                "SOWRA Media Controls",
                NotificationManager.IMPORTANCE_LOW
            );
            ch.setDescription("SOWRA lock-screen and notification playback controls");
            ch.setShowBadge(false);
            notificationManager.createNotificationChannel(ch);
        }
    }

    private PendingIntent actionPendingIntent(String action, int reqCode) {
        Intent i = new Intent(this, MediaPlaybackService.class);
        i.setAction(action);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getService(this, reqCode, i, flags);
    }

    private void updateSessionAndNotification() {
        MediaMetadata.Builder meta = new MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, curTitle)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, curArtist)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, curAlbum);
        if (curDurationMs > 0) {
            meta.putLong(MediaMetadata.METADATA_KEY_DURATION, curDurationMs);
        }
        if (coverBitmap != null) {
            meta.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, coverBitmap);
        }
        if (loadedArtworkPath == null || !loadedArtworkPath.equals(curArtworkPath)) {
            Bitmap newCover = loadArtwork(curArtworkPath);
            if (newCover == null) newCover = BitmapFactory.decodeResource(getResources(), R.drawable.default_album_cover);
            if (newCover != null) {
                if (coverBitmap != null && coverBitmap != newCover) coverBitmap.recycle();
                coverBitmap = newCover;
            }
            loadedArtworkPath = curArtworkPath;
        }
        if (coverBitmap != null) {
            meta.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, coverBitmap);
            meta.putBitmap(MediaMetadata.METADATA_KEY_ART, coverBitmap);
            meta.putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, coverBitmap);
        }
        mediaSession.setMetadata(meta.build());

        long allowedActions =
            PlaybackState.ACTION_PLAY |
            PlaybackState.ACTION_PAUSE |
            PlaybackState.ACTION_PLAY_PAUSE |
            PlaybackState.ACTION_STOP |
            PlaybackState.ACTION_SKIP_TO_PREVIOUS |
            PlaybackState.ACTION_SKIP_TO_NEXT |
            PlaybackState.ACTION_REWIND |
            PlaybackState.ACTION_FAST_FORWARD |
            PlaybackState.ACTION_SEEK_TO;

        PlaybackState state = new PlaybackState.Builder()
            .setActions(allowedActions)
            .setState(
                curPlaying ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED,
                curPositionMs,
                curPlaying ? 1.0f : 0.0f
            )
            .build();
        mediaSession.setPlaybackState(state);

        Intent launchIntent = new Intent(this, MainActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Action prevAct = new Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_media_previous),
            "Backward",
            actionPendingIntent(ACTION_CMD_PREV, 10)
        ).build();

        Notification.Action playPauseAct = new Notification.Action.Builder(
            Icon.createWithResource(this, curPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play),
            curPlaying ? "Pause" : "Play",
            actionPendingIntent(curPlaying ? ACTION_CMD_PAUSE : ACTION_CMD_PLAY, 11)
        ).build();

        Notification.Action rewindAct = new Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_media_rew),
            "Rewind 10 seconds",
            actionPendingIntent(ACTION_CMD_REWIND, 12)
        ).build();

        Notification.Action nextAct = new Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_media_next),
            "Next track",
            actionPendingIntent(ACTION_CMD_NEXT, 13)
        ).build();

        Notification.Action fastForwardAct = new Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_media_ff),
            "Forward 10 seconds",
            actionPendingIntent(ACTION_CMD_FAST_FORWARD, 14)
        ).build();

        Notification.Action rerunAct = new Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_menu_revert),
            "Re-run",
            actionPendingIntent(ACTION_CMD_RERUN, 15)
        ).build();

        Notification.Action stopAct = new Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
            "Stop",
            actionPendingIntent(ACTION_CMD_STOP, 16)
        ).build();

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? new Notification.Builder(this, CHANNEL_ID)
            : new Notification.Builder(this);

        builder
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setLargeIcon(coverBitmap)
            .setContentTitle(curTitle)
            .setContentText(curArtist)
            .setSubText(curAlbum)
            .setContentIntent(contentIntent)
            .setOngoing(curPlaying)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(prevAct)
            .addAction(playPauseAct)
            .addAction(rewindAct)
            .addAction(nextAct)
            .addAction(fastForwardAct)
            .addAction(rerunAct)
            .addAction(stopAct)
            .setStyle(new Notification.MediaStyle()
                .setMediaSession(mediaSession.getSessionToken())
                .setShowActionsInCompactView(0, 1, 3));

        Notification notification = builder.build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private Bitmap loadArtwork(String path) {
        if (path == null || path.trim().isEmpty()) return null;
        try {
            String clean = path.startsWith("file://") ? path.substring(7) : path;
            File file = new File(clean);
            if (file.exists() && file.isFile()) return BitmapFactory.decodeFile(file.getAbsolutePath());
        } catch (Exception ignored) {}
        return null;
    }

    private void stopSelfAndNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
        stopSelf();
    }

    @Override
    public void onDestroy() {
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
