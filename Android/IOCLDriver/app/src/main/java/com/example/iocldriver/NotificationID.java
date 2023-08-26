package com.example.iocldriver;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;


public class NotificationID extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "Channel_ID",
                    "Current Location",
                    NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

    }
}
