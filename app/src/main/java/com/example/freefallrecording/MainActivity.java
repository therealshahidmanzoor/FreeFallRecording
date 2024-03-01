package com.example.freefallrecording;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION_CODE = 100;
    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String FREEFALL_SWITCH_STATE_KEY = "freefallSwitchState";

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
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO},
                    REQUEST_PERMISSION_CODE);
        }

        sosButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the RecordingService
                startService(new Intent(MainActivity.this, RecordingService.class));
            }
        });

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
}
