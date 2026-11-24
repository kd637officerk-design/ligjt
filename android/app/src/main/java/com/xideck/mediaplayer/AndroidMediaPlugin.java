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
        if (activeInstance == this) {
            activeInstance = null;
        }
    }

    public static void emitMediaAction(String action, double positionSec) {
        if (activeInstance != null) {
            JSObject data = new JSObject();
            data.put("action", action);
            if (positionSec >= 0) {
                data.put("position", positionSec);
            }
            activeInstance.notifyListeners("mediaAction", data);
        }
    }

    @PluginMethod
    public void updateMetadata(PluginCall call) {
        Context ctx = getContext();
        Intent intent = new Intent(ctx, MediaPlaybackService.class);
        intent.setAction(MediaPlaybackService.ACTION_UPDATE);
        intent.putExtra("title", call.getString("title", "XI Media Deck"));
        intent.putExtra("artist", call.getString("artist", "Local Media"));
        intent.putExtra("album", call.getString("album", "Eternia Deck"));
        intent.putExtra("isPlaying", Boolean.TRUE.equals(call.getBoolean("isPlaying", false)));
        Double dur = call.getDouble("durationSec", 0.0);
        Double pos = call.getDouble("positionSec", 0.0);
        intent.putExtra("durationSec", dur != null ? dur : 0.0);
        intent.putExtra("positionSec", pos != null ? pos : 0.0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(intent);
        } else {
            ctx.startService(intent);
        }
        call.resolve();
    }

    @PluginMethod
    public void stopNotification(PluginCall call) {
        Context ctx = getContext();
        Intent intent = new Intent(ctx, MediaPlaybackService.class);
        intent.setAction(MediaPlaybackService.ACTION_STOP_SERVICE);
        ctx.startService(intent);
        call.resolve();
    }
}
