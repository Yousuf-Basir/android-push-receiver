package com.example.pushreceiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "MyFirebaseMsgService";
    private static final String CHANNEL_ID = "push_notification_channel";
    private static MediaPlayer mediaPlayer;

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New token: " + token);
        sendRegistrationToServer(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Store data from payload
        Map<String, String> data = remoteMessage.getData();

        // Wake screen
        wakeScreen();

        // Play custom sound
        playNotificationSound();

        // Show notification
        showNotification();

        // Launch CallActionActivity
        launchCallActionScreen(data);
    }

    private void wakeScreen() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "MyApp:NotificationWakeLock"
        );
        wakeLock.acquire(10000); // Release after 10 seconds
    }

    public static void stopSound() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void playNotificationSound() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            mediaPlayer = MediaPlayer.create(this, R.raw.callring);
            mediaPlayer.setLooping(true); // Make the sound loop until stopped
            mediaPlayer.start();
        } catch (Exception e) {
            Log.e(TAG, "Error playing sound", e);
        }
    }

    private void showNotification() {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create notification channel for Android 8.0 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Push Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );

            channel.setDescription("Receives push notifications");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();

            channel.setSound(
                    Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.callring),
                    audioAttributes
            );

            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("New Call Request")
                        .setContentText("Tap to open the app")
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        notificationManager.notify(0, notificationBuilder.build());
    }

    private void launchCallActionScreen(Map<String, String> data) {
        Intent intent = new Intent(this, CallActionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Add data to intent
        for (Map.Entry<String, String> entry : data.entrySet()) {
            intent.putExtra(entry.getKey(), entry.getValue());
        }

        startActivity(intent);
    }

    private void sendRegistrationToServer(String token) {
        // TODO: Implement method to send token to your Node.js server
    }
}