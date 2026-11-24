package com.xideck.mediaplayer;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "AndroidMedia")
public class AndroidMediaPlugin extends Plugin {
    private static AndroidMediaPlugin activeInstance;

    @Override
    public void load() {
        activeInstance = this;
    }

    @Override
    protected void handleOnDestroy() {
        if (activeInstance == this) activeInstance = null;
    }

    public static void emitMediaAction(String action, double positionMs) {
        if (activeInstance != null) {
            JSObject data = new JSObject();
            data.put("action", action);
            if ("seekto".equals(action)) data.put("positionMs", positionMs);
            if ("seekback".equals(action) || "seekforward".equals(action)) data.put("offsetSec", 10);
            activeInstance.notifyListeners("mediaAction", data);
        }
    }

    @PluginMethod
    public void updateMetadata(PluginCall call) {
        Context ctx = getContext();
        Intent intent = new Intent(ctx, MediaPlaybackService.class);
        intent.setAction(MediaPlaybackService.ACTION_UPDATE);
        intent.putExtra("title", call.getString("title", "SOWRA"));
        intent.putExtra("artist", call.getString("artist", "Local Media"));
        intent.putExtra("album", call.getString("album", "SOWRA"));
        intent.putExtra("isPlaying", Boolean.TRUE.equals(call.getBoolean("isPlaying", false)));

        Double durationMs = call.getDouble("durationMs");
        Double positionMs = call.getDouble("positionMs");
        Double durationSec = call.getDouble("durationSec");
        Double positionSec = call.getDouble("positionSec");
        intent.putExtra("durationMs",
            durationMs != null ? durationMs.longValue() : (durationSec != null ? (long)(durationSec * 1000) : 0L));
        intent.putExtra("positionMs",
            positionMs != null ? positionMs.longValue() : (positionSec != null ? (long)(positionSec * 1000) : 0L));
        intent.putExtra("artworkPath", call.getString("artworkPath", ""));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(intent);
        else ctx.startService(intent);
        call.resolve();
    }

    @PluginMethod
    public void stopNotification(PluginCall call) {
        Context ctx = getContext();
        ctx.stopService(new Intent(ctx, MediaPlaybackService.class));
        call.resolve();
    }
}
