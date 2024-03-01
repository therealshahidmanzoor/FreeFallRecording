package com.example.freefallrecording;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StopRecordingReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Handle the logic to stop recording
        // This method will be called when the Stop Recording button in the notification is clicked
        // You can call your stopRecording() method or perform any necessary actions here
        // For example, you can use intent.getAction() to identify the action triggered by the button click

        // Assuming you have a method to stop recording in RecordingService, you can start it like this:
        Intent stopIntent = new Intent(context, RecordingService.class);
        context.stopService(stopIntent);
    }
}
