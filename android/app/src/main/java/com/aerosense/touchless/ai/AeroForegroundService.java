package com.aerosense.touchless.ai;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

public class AeroForegroundService extends Service {

    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_STOP = "ACTION_STOP";
    private static final String CHANNEL_ID = "aero_gesture_service_channel";
    private static final int NOTIFICATION_ID = 2001;

    private PowerManager.WakeLock wakeLock;

    public static void start(Context context) {
        try {
            Intent intent = new Intent(context, AeroForegroundService.class);
            intent.setAction(ACTION_START);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Exception ignored) {}
    }

    public static void stop(Context context) {
        try {
            Intent intent = new Intent(context, AeroForegroundService.class);
            intent.setAction(ACTION_STOP);
            context.startService(intent);
        } catch (Exception ignored) {}
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        acquireWakeLock();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            releaseWakeLock();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        Notification notification = buildNotification();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                int serviceType = 0;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;
                }
                startForeground(NOTIFICATION_ID, notification, serviceType);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } catch (Exception e) {
            try {
                startForeground(NOTIFICATION_ID, notification);
            } catch (Exception ignored) {}
        }

        acquireWakeLock();
        return START_STICKY;
    }

    private void acquireWakeLock() {
        if (wakeLock == null) {
            try {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                if (pm != null) {
                    wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AeroSense::KeepAliveLock");
                    wakeLock.setReferenceCounted(false);
                    wakeLock.acquire(12 * 60 * 60 * 1000L); // 12 hours safety
                }
            } catch (Exception ignored) {}
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null) {
            try {
                if (wakeLock.isHeld()) {
                    wakeLock.release();
                }
            } catch (Exception ignored) {}
            wakeLock = null;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "AeroSense AI Gesture Engine",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Keeps touchless hand gestures active in background");
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);

            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        Intent launchIntent = new Intent(this, MainActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                launchIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        : PendingIntent.FLAG_UPDATE_CURRENT
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setContentTitle("AeroSense AI Hand Control Active")
                .setContentText("Touchless gesture tracking is running smoothly")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_LOW);

        return builder.build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        releaseWakeLock();
        super.onDestroy();
    }
}
