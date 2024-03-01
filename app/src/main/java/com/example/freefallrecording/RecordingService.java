package com.example.freefallrecording;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
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

    @Override
    public void onCreate() {
        super.onCreate();
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

    private void startRecording() {
        // Check if recording is already in progress
        if (isRecording()) {
            Toast.makeText(this, "Recording is already in progress", Toast.LENGTH_SHORT).show();
            return;
        }

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
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
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

            RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_layout);
            remoteViews.setTextViewText(R.id.txtCountdown, "Time left: " + (timeLeftMillis / 1000) + " seconds");

            // Create an explicit intent for the stop button
            Intent stopIntent = new Intent(this, StopRecordingReceiver.class);
            PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.btnStopRecording, stopPendingIntent);

            // ... (rest of your notification code)

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setCustomContentView(remoteViews)
                    .setContentTitle("Safe Stree")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(false)
                    .setOngoing(true);

            // ... (rest of your notification code)
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, builder.build());
        }
    }
}
