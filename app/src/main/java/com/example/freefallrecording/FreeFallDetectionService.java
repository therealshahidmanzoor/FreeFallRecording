package com.example.freefallrecording;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
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
    private static final int FREEFALL_NOTIFICATION_ID = 3;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;

    private float[] gravityValues;
    private float[] gyroscopeValues;

    private static final float FREEFALL_THRESHOLD = 1.0f; // Adjust this threshold based on your requirements

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("FreeFallDetectionService", "Service created");

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            }
            if (gyroscope != null) {
                sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }

        gravityValues = new float[3];
        gyroscopeValues = new float[3];

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
            gravityValues = event.values.clone();
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyroscopeValues = event.values.clone();
        }

        float x = gravityValues[0];
        float y = gravityValues[1];
        float z = gravityValues[2];

        float acceleration = Math.abs(x) + Math.abs(y) + Math.abs(z);

        float pitch = gyroscopeValues[1];

        if (acceleration < FREEFALL_THRESHOLD && Math.abs(pitch) < 1.0f) {
            Toast.makeText(this, "Freefall Detected", Toast.LENGTH_SHORT).show();
            sendFreefallNotification();
        }
    }

    private void sendFreefallNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_MUTABLE);

        createNotificationChannel();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Freefall Detected")
                .setContentText("Your device is in freefall!")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(Color.BLUE)
                .setAutoCancel(true);

        NotificationManagerCompat.from(this).notify(FREEFALL_NOTIFICATION_ID, builder.build());
    }

    private Notification buildForegroundNotification() {
        createNotificationChannel();

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
