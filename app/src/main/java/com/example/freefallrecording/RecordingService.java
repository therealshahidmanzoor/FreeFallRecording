package com.example.freefallrecording;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.text.Layout;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.IOException;

public class RecordingService extends android.app.Service {

    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private String audioFilePath;
    private static final String CHANNEL_ID = "RecordingChannel";
    private static final int NOTIFICATION_ID = 1;
    private CountDownTimer countDownTimer;
    private long targetTime;
    private static final int REQUEST_PERMISSION_CODE1
            = 101;
    private static final int FOREGROUND_NOTIFICATION_ID = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification());
        }
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start recording when the service is started
        startRecording();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop recording and playback when the service is destroyed
        stopRecording();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    private Notification buildForegroundNotification() {
        // Build your foreground notification here
        // Create a notification channel (required for Android 8.0 and above)
        createNotificationChannel();

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Freefall Detection Service")
                .setContentText("Service is running")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(Color.BLUE)
                .setAutoCancel(true);

        return builder.build();
    }
    private void startRecording() {
        // Check if recording is already in progress
        if (isRecording()) {
            Toast.makeText(this, "Recording is already in progress", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if notification permissions are granted
        if (checkNotificationPermissions()) {
            // Permissions are granted, proceed with recording
            String timestamp = String.valueOf(System.currentTimeMillis());
            audioFilePath = getExternalFilesDir(Environment.DIRECTORY_MUSIC) + "/audio_record_" + timestamp + ".mp3";

            // Initialize and start the MediaRecorder
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setOutputFile(audioFilePath);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            try {
                mediaRecorder.prepare();
                mediaRecorder.start();
                Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();

                // Schedule the handler to update the notification every second
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        updateNotification();
                        handler.postDelayed(this, 1000); // Update every second
                    }
                }, 1000);

                // Start a countdown timer for 30 seconds
                countDownTimer = new CountDownTimer(30000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        // Update the target time every tick
                        targetTime = System.currentTimeMillis() + millisUntilFinished;
                        updateNotification();
                    }

                    @Override
                    public void onFinish() {
                        stopRecording();
                    }
                }.start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Permissions are not granted, request notification permissions
            requestNotificationPermissions();
        }
    }


    private void stopRecording() {
        // Your stop recording logic here
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show();
            playLastRecording(); // Play the last recording after stopping
            stopSelf();  // Stop the service when recording is complete
        }
        // Cancel the countdown timer
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        // Update notification one last time to show completion
        updateNotification();
        // Remove the notification after recording is complete
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID);
    }

    private void playLastRecording() {
        if (audioFilePath != null) {
            mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(getApplicationContext(), Uri.parse(audioFilePath));
                mediaPlayer.prepare();
                mediaPlayer.start();
                Toast.makeText(this, "Audio Started", Toast.LENGTH_SHORT).show();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isRecording() {
        return mediaRecorder != null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Recording Channel";
            String description = "Channel for recording notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }


    private void updateNotification() {
        if (isRecording()) {
            long timeLeftMillis = targetTime - System.currentTimeMillis();

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("Safe Street")
                    .setContentText("Time left: " + (timeLeftMillis / 1000) + " seconds")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(false)
                    .setOngoing(true);

            // Create an explicit intent for the stop button
            Intent stopIntent = new Intent(this, StopRecordingReceiver.class);
            PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, 0, stopIntent, PendingIntent.FLAG_MUTABLE);
            builder.addAction(R.drawable.ic_stop, "Stop Recording", stopPendingIntent);

            // Set a notification channel if necessary
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setChannelId(CHANNEL_ID);
            }

            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, builder.build());
        }
    }
    private boolean checkNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return NotificationManagerCompat.from(this).getNotificationChannel(CHANNEL_ID) != null;
        } else {
            // Notification permissions are always granted before Android O
            return true;
        }
    }

    private void requestNotificationPermissions() {
        // You can guide the user to the notification settings or use your preferred method
        Toast.makeText(this, "Please enable notification permissions in app settings", Toast.LENGTH_LONG).show();
        ActivityCompat.requestPermissions(new Activity().getParent(),
                new String[]{Manifest.permission.RECORD_AUDIO},
                REQUEST_PERMISSION_CODE1);
        openNotificationSettings();
    }

    private void openNotificationSettings() {
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        } else {
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", getPackageName(), null));
        }
        startActivity(intent);
    }

}
