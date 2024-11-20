package com.example.pushreceiver;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 101;

    private TextView tokenTextView;
    private TextView notificationPermissionStatus;
    private TextView batteryOptimizationStatus;
    private TextView overlayPermissionStatus;
    private Button copyTokenButton;
    private Button requestPermissionButton;
    private Button openSettingsButton;
    private Button resetPermissionsButton;
    private Button requestOverlayPermissionButton;

    private Button testSoundButton;
    private Button stopSoundButton;
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private MaterialCardView tokenCard;
    private MaterialCardView permissionCard;

    private TextInputEditText phoneInput;
    private Button submitPhoneButton;
    private String currentFirebaseToken;
    private MaterialCardView phoneCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupClickListeners();
        refreshTokenAndStatus();
        initializeSoundControls();
        checkOverlayPermission();

        initializePhoneViews();
        setupPhoneSubmission();
    }

    private void initializePhoneViews() {
        phoneInput = findViewById(R.id.phoneInput);
        submitPhoneButton = findViewById(R.id.submitPhoneButton);
    }

    private void setupPhoneSubmission() {
        submitPhoneButton.setOnClickListener(v -> submitPhoneNumber());
    }

    private void submitPhoneNumber() {
        String phoneNumber = phoneInput.getText().toString().trim();

        if (phoneNumber.isEmpty()) {
            phoneInput.setError("Phone number is required");
            return;
        }

        String fullPhoneNumber = "+880"+phoneNumber;

        // Show progress
        submitPhoneButton.setEnabled(false);
        submitPhoneButton.setText("Registering...");

        // Get the current Firebase token
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        showError("Failed to get Firebase token");
                        resetSubmitButton();
                        return;
                    }

                    String token = task.getResult();

                    ApiClient.getInstance().registerOrUpdatePushToken(fullPhoneNumber, token, new ApiClient.ApiCallback() {
                        @Override
                        public void onSuccess() {
                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this,
                                        "Phone number registered successfully", Toast.LENGTH_SHORT).show();
                                resetSubmitButton();
                                savePhoneNumber(phoneNumber);
                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
                                showError("Failed to register: " + error);
                                resetSubmitButton();
                            });
                        }
                    });
                });
    }

    private void resetSubmitButton() {
        submitPhoneButton.setEnabled(true);
        submitPhoneButton.setText("Register Phone Number");
    }

    private void showError(String message) {
        Log.w(TAG, message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void savePhoneNumber(String phoneNumber) {
        SharedPreferences prefs = getSharedPreferences("PushReceiverPrefs", MODE_PRIVATE);
        prefs.edit().putString("phone_number", phoneNumber).apply();
    }

    private void initializeViews() {
        tokenTextView = findViewById(R.id.tokenTextView);
        notificationPermissionStatus = findViewById(R.id.notificationPermissionStatus);
        batteryOptimizationStatus = findViewById(R.id.batteryOptimizationStatus);
        overlayPermissionStatus = findViewById(R.id.overlayPermissionStatus);
        copyTokenButton = findViewById(R.id.copyTokenButton);
        requestPermissionButton = findViewById(R.id.requestPermissionButton);
        openSettingsButton = findViewById(R.id.openSettingsButton);
        resetPermissionsButton = findViewById(R.id.resetPermissionsButton);
        requestOverlayPermissionButton = findViewById(R.id.requestOverlayPermissionButton);
        tokenCard = findViewById(R.id.tokenCard);
        permissionCard = findViewById(R.id.permissionCard);
        phoneCard = findViewById(R.id.phoneCard);
    }

    private void setupClickListeners() {
        copyTokenButton.setOnClickListener(v -> copyTokenToClipboard());
        requestPermissionButton.setOnClickListener(v -> requestNotificationPermission());
        openSettingsButton.setOnClickListener(v -> openAppSettings());
        resetPermissionsButton.setOnClickListener(v -> resetAllPermissions());
        requestOverlayPermissionButton.setOnClickListener(v -> requestOverlayPermission());
    }

    private void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasOverlayPermission = Settings.canDrawOverlays(this);
            updateOverlayPermissionStatus(hasOverlayPermission);
            requestOverlayPermissionButton.setVisibility(
                    hasOverlayPermission ? View.GONE : View.VISIBLE
            );
        }
    }

    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
        }
    }

    private void updateOverlayPermissionStatus(boolean hasPermission) {
        if (overlayPermissionStatus != null) {
            overlayPermissionStatus.setText("Overlay Permission: " +
                    (hasPermission ? "Granted ✓" : "Not Granted ✗"));
            overlayPermissionStatus.setTextColor(getColor(hasPermission ?
                    android.R.color.holo_green_dark : android.R.color.holo_red_dark));
        }
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

        // Check overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasOverlayPermission = Settings.canDrawOverlays(this);
            updateOverlayPermissionStatus(hasOverlayPermission);
            requestOverlayPermissionButton.setVisibility(
                    hasOverlayPermission ? View.GONE : View.VISIBLE
            );
        }

        // Update button visibility
        requestPermissionButton.setVisibility(!hasNotificationPermission ?
                View.VISIBLE : View.GONE);

        if(hasNotificationPermission && Settings.canDrawOverlays(this)) {
            phoneCard.setVisibility(View.VISIBLE);
        }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                boolean hasOverlayPermission = Settings.canDrawOverlays(this);
                updateOverlayPermissionStatus(hasOverlayPermission);
                requestOverlayPermissionButton.setVisibility(
                        hasOverlayPermission ? View.GONE : View.VISIBLE
                );
            }
        }
    }
}