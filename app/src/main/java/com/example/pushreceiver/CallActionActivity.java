package com.example.pushreceiver;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CallActionActivity extends AppCompatActivity {
    private static final String TAG = "Call Action Activity";
    public Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        context = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_action);

        // Keep screen on and show above lock screen
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );

        Button ignoreButton = findViewById(R.id.ignoreButton);
        Button openAppButton = findViewById(R.id.openAppButton);

        ignoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Stop the sound
                MyFirebaseMessagingService.stopSound();
                // Finish the activity
                finish();
            }
        });

        openAppButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Stop the sound
                MyFirebaseMessagingService.stopSound();

                Log.w(TAG, "Opening DA APP");

//                // Launch main activity with the data
//                Intent mainIntent = new Intent(CallActionActivity.this, MainActivity.class);
//                mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//
//                // Pass along any extras from the notification
//                Bundle extras = getIntent().getExtras();
//                if (extras != null) {
//                    mainIntent.putExtras(extras);
//                }
//
//                startActivity(mainIntent);

//                launchExternalApp(context, "com.google.android.youtube");
//                findAllInstalledApps(context);

//                List<String> apps = getAllInstalledApps(context);
//                for (String app : apps) {
//                    System.out.println(app);
//                }

                launchApp(context, "com.da.sathei_achi");

                finish();
            }
        });
    }


    public List<String> getAllInstalledApps(Context context) {
        List<String> installedApps = new ArrayList<>();
        PackageManager packageManager = context.getPackageManager();

        // Get all installed packages
        List<PackageInfo> packages = packageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES);

        for (PackageInfo packageInfo : packages) {
            // Only list third-party apps or all apps based on your requirement
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                // Third-party apps only
                installedApps.add(packageInfo.packageName);
            } else {
                // All apps including system apps
                installedApps.add(packageInfo.packageName);
            }

            // Optional: Log or print package names
            Log.d("InstalledApps", packageInfo.packageName);
        }

        return installedApps;
    }


    public void launchApp(Context context, String packageName) {
        try {
            PackageManager packageManager = context.getPackageManager();
            Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);

            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launchIntent);
            } else {
                Log.e("AppLauncher", "No launch intent found for " + packageName);
            }
        } catch (Exception e) {
            Log.e("AppLauncher", "Error launching app", e);
        }
    }






//    @Override
//    public void onBackPressed() {
//        // Prevent back button from dismissing the activity
//        // You can remove this if you want to allow back button to work
//    }
}