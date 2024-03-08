package com.example.freefallrecording;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class FreeFallDetectionService extends android.app.Service implements SensorEventListener {

    private static final String CHANNEL_ID = "FreefallChannel";
    private static final int FOREGROUND_NOTIFICATION_ID = 1;
    private static final int FREEFALL_NOTIFICATION_ID = 3; // Use a unique ID for each notification

    private SensorManager sensorManager;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("FreeFallDetectionService", "Service created");
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }

        // Start the service as a foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            // Check if the service is started by the BootReceiver
            if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
                // Handle the case when the system is restarted
                // You may want to check the saved state or perform any necessary initialization
            }
        }
        return START_STICKY;
    }
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for this example
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            double x = event.values[0];
            double y = event.values[1];
            double z = event.values[2];

            double accelerationReader = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));

            if (accelerationReader > 0.3d && accelerationReader < 0.5d) {
                Toast.makeText(this, "FreeFall Detected", Toast.LENGTH_SHORT).show();
                sendFreefallNotification();
            }
        }
    }

    private void sendFreefallNotification() {
        // Create an explicit intent for the notification
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_MUTABLE);

        // Create a notification channel (required for Android 8.0 and above)
        createNotificationChannel();

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Freefall Detected")
                .setContentText("Your device is in freefall!")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(Color.BLUE)
                .setAutoCancel(true);

        // Show the freefall notification with a unique ID
        NotificationManagerCompat.from(this).notify(FREEFALL_NOTIFICATION_ID, builder.build());
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

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Freefall Channel";
            String description = "Channel for freefall notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
