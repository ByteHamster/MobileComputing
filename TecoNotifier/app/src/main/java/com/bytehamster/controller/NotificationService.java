package com.bytehamster.controller;

import android.content.Intent;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

/**
 * @author Hans-Peter Lehmann
 * @version 1.0
 */
public class NotificationService extends NotificationListenerService {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String pack = sbn.getPackageName();
        String title = sbn.getNotification().extras.getString("android.title");
        Log.i("Msg","Notification Posted: " + pack);

        if (!pack.equals(getPackageName()) && !pack.equals("android")) {
            Intent mServiceIntent = new Intent(this, VibratorService.class);
            mServiceIntent.setAction("NOTIFICATION");
            mServiceIntent.putExtra("PACKAGE", pack);
            mServiceIntent.putExtra("TITLE", title);
            startService(mServiceIntent);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.i("Msg","Notification Removed");

    }
}
