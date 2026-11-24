package com.xideck.mediaplayer;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(AndroidMediaPlugin.class);
        registerPlugin(InternalFilesPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
