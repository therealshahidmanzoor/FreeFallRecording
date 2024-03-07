package com.example.freefallrecording;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION_CODE = 100;
    private static final int REQUEST_PERMISSION_CODE1
            = 101;

    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String FREEFALL_SWITCH_STATE_KEY = "freefallSwitchState";

//    @RequiresApi(api >= Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button sosButton = findViewById(R.id.sosButton);
        Switch freefallSwitch = findViewById(R.id.freefallSwitch);

        // Restore the state of the freefallSwitch from SharedPreferences
        boolean switchState = getFreefallSwitchState();
        freefallSwitch.setChecked(switchState);

        // Request runtime permission for recording
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
        && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO
                    ,Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_PERMISSION_CODE1);

        }



        sosButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (checkRecordingPermissions() && checkNotificationPermissions()) {
                        // Permissions are granted, start the RecordingService
                        startService(new Intent(MainActivity.this, RecordingService.class));
                    } else {
                        // Permissions are not granted, request them or show a toast
                        if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)
                        && shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                            // Request permissions if the user hasn't denied with "Don't ask again"
                            requestRecordingPermissions();
                            requestNotificationPermissions();
                        } else {
                            // Guide the user to app settings to enable permissions manually
                            Toast.makeText(MainActivity.this, "Please enable recording permissions in app settings", Toast.LENGTH_LONG).show();
                            openAppSettings();
                        }
                    }
                }

            }
        });

// Add this method to open app settings



        freefallSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Intent serviceIntent = new Intent(MainActivity.this, FreeFallDetectionService.class);
                if (isChecked) {
                    // Start the service when the switch is checked
                    Toast.makeText(MainActivity.this, String.valueOf(isChecked), Toast.LENGTH_SHORT).show();
                    startService(serviceIntent);
                } else {
                    // Stop the service when the switch is unchecked
                    Toast.makeText(MainActivity.this, String.valueOf(isChecked), Toast.LENGTH_SHORT).show();

                    stopService(serviceIntent);
                }

                // Save the state of the freefallSwitch to SharedPreferences
                saveFreefallSwitchState(isChecked);
            }
        });
    }

    private boolean getFreefallSwitchState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // Use a default value (false in this case) if the key is not found
        return prefs.getBoolean(FREEFALL_SWITCH_STATE_KEY, false);
    }

    private void saveFreefallSwitchState(boolean switchState) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(FREEFALL_SWITCH_STATE_KEY, switchState);
        editor.apply();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                Toast.makeText(this, "Recording permissions granted", Toast.LENGTH_SHORT).show();
            } else {
                // Permission denied
                Toast.makeText(this, "Recording permissions denied. Please enable them in settings.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_PERMISSION_CODE1) {
            // Handle the result for other permissions if needed
            if (grantResults.length > 0 && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                Toast.makeText(this, "Recording permissions granted", Toast.LENGTH_SHORT).show();
            } else {
                // Permission denied
                Toast.makeText(this, "Recording permissions denied. Please enable them in settings.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private boolean checkRecordingPermissions() {
        return ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }
    private boolean checkNotificationPermissions() {
        return ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestRecordingPermissions() {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                REQUEST_PERMISSION_CODE);
    }
    private void requestNotificationPermissions() {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                REQUEST_PERMISSION_CODE1);
    }
    private void openAppSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }
}
