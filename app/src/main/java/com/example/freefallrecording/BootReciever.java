package com.example.freefallrecording;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReciever extends android.content.BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            // Start your service here
            Log.d("BootReceiver", "Received BOOT_COMPLETED broadcast");
            context.startService(new Intent(context, FreeFallDetectionService.class));
        }
    }
}
