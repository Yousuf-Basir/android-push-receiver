package com.example.pushreceiver;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;

    private TextView tokenTextView;
    private TextView notificationPermissionStatus;
    private TextView batteryOptimizationStatus;
    private Button copyTokenButton;
    private Button requestPermissionButton;
    private Button openSettingsButton;
    private Button resetPermissionsButton;

    private Button testSoundButton;
    private Button stopSoundButton;
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private MaterialCardView tokenCard;
    private MaterialCardView permissionCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupClickListeners();
        refreshTokenAndStatus();
        initializeSoundControls();
    }

    private void initializeViews() {
        tokenTextView = findViewById(R.id.tokenTextView);
        notificationPermissionStatus = findViewById(R.id.notificationPermissionStatus);
        batteryOptimizationStatus = findViewById(R.id.batteryOptimizationStatus);
        copyTokenButton = findViewById(R.id.copyTokenButton);
        requestPermissionButton = findViewById(R.id.requestPermissionButton);
        openSettingsButton = findViewById(R.id.openSettingsButton);
        resetPermissionsButton = findViewById(R.id.resetPermissionsButton);
        tokenCard = findViewById(R.id.tokenCard);
        permissionCard = findViewById(R.id.permissionCard);
    }

    private void setupClickListeners() {
        copyTokenButton.setOnClickListener(v -> copyTokenToClipboard());
        requestPermissionButton.setOnClickListener(v -> requestNotificationPermission());
        openSettingsButton.setOnClickListener(v -> openAppSettings());
        resetPermissionsButton.setOnClickListener(v -> resetAllPermissions());
    }

    private void refreshTokenAndStatus() {
        // Get FCM token
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        tokenTextView.setText("Failed to get token");
                        return;
                    }

                    String token = task.getResult();
                    tokenTextView.setText(token);
                    Log.d(TAG, "FCM Token: " + token);
                });

        // Update permission statuses
        updatePermissionStatuses();
    }

    private void updatePermissionStatuses() {
        // Check notification permission
        boolean hasNotificationPermission = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        } else {
            hasNotificationPermission = true; // Not required for older versions
        }

        notificationPermissionStatus.setText("Notification Permission: " +
                (hasNotificationPermission ? "Granted ✓" : "Not Granted ✗"));
        notificationPermissionStatus.setTextColor(getColor(hasNotificationPermission ?
                android.R.color.holo_green_dark : android.R.color.holo_red_dark));

        // Check battery optimization status
        boolean isBatteryOptimizationDisabled = true; // Default for older versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.os.PowerManager pm = (android.os.PowerManager) getSystemService(POWER_SERVICE);
            isBatteryOptimizationDisabled = pm.isIgnoringBatteryOptimizations(getPackageName());
        }

        batteryOptimizationStatus.setText("Battery Optimization: " +
                (isBatteryOptimizationDisabled ? "Disabled ✓" : "Enabled ✗"));
        batteryOptimizationStatus.setTextColor(getColor(isBatteryOptimizationDisabled ?
                android.R.color.holo_green_dark : android.R.color.holo_red_dark));

        // Update button visibility
        requestPermissionButton.setVisibility(!hasNotificationPermission ?
                View.VISIBLE : View.GONE);
    }

    private void copyTokenToClipboard() {
        String token = tokenTextView.getText().toString();
        android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        android.content.ClipData clip =
                android.content.ClipData.newPlainText("FCM Token", token);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Token copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_CODE);
            }
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    private void resetAllPermissions() {
        // This will clear app data, effectively resetting all permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13+, use the new runtime permission model
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    PERMISSION_REQUEST_CODE);
        }

        // Open app settings for manual reset
        openAppSettings();
        Toast.makeText(this, "Please manually reset permissions in Settings",
                Toast.LENGTH_LONG).show();
    }

    private void initializeSoundControls() {
        testSoundButton = findViewById(R.id.testSoundButton);
        stopSoundButton = findViewById(R.id.stopSoundButton);

        testSoundButton.setOnClickListener(v -> testNotificationSound());
        stopSoundButton.setOnClickListener(v -> stopNotificationSound());

        // Initially hide stop button
        updateSoundControlButtons();
    }

    private void testNotificationSound() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            mediaPlayer = MediaPlayer.create(this, R.raw.callring);
            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                updateSoundControlButtons();
                mp.release();
            });
            mediaPlayer.start();
            isPlaying = true;
            updateSoundControlButtons();
            Toast.makeText(this, "Playing notification sound", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error playing sound", e);
            Toast.makeText(this, "Error playing sound", Toast.LENGTH_SHORT).show();
            isPlaying = false;
            updateSoundControlButtons();
        }
    }

    private void stopNotificationSound() {
        if (mediaPlayer != null && isPlaying) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
                isPlaying = false;
                updateSoundControlButtons();
                Toast.makeText(this, "Sound stopped", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping sound", e);
            }
        }
    }
    private void updateSoundControlButtons() {
        testSoundButton.setEnabled(!isPlaying);
        stopSoundButton.setVisibility(isPlaying ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionStatuses();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            updatePermissionStatuses();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop sound if app goes to background
        stopNotificationSound();
    }
}