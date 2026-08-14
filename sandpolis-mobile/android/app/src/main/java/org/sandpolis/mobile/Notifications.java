package org.sandpolis.mobile;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;

import androidx.core.app.NotificationCompat;

/**
 * Posts notifications raised by the Rust side.
 *
 * <p>The native code calls {@link #show} through JNI (see
 * {@code sandpolis-instance/src/notification/native/android.rs}); building a
 * {@code NotificationCompat} by JNI signature instead would be a lot of fragile
 * call-by-string code, so everything Android-specific lives here and the FFI
 * surface stays at one static method.
 *
 * <p>There is one channel per severity so the user can silence routine chatter
 * from the system settings without also losing alerts.
 */
public final class Notifications {

    private static final String TAG = "sandpolis";

    /** Indexed by severity, matching {@code Severity::rank()} on the Rust side. */
    private static final String[] CHANNEL_IDS = {
            "sandpolis-info", "sandpolis-warn", "sandpolis-error"
    };

    private static final String[] CHANNEL_NAMES = {"Info", "Warnings", "Errors"};

    private static final int[] IMPORTANCE = {
            NotificationManager.IMPORTANCE_LOW,
            NotificationManager.IMPORTANCE_DEFAULT,
            NotificationManager.IMPORTANCE_HIGH
    };

    /** Distinct per notification so a new one never replaces an unread one. */
    private static int nextId = 1;

    private Notifications() {
    }

    /**
     * Post one notification.
     *
     * @param context  the application or activity context
     * @param title    one line; shown as the notification's title
     * @param body     optional detail, empty when there is none
     * @param severity 0 = info, 1 = warning, 2 = error
     */
    public static void show(Context context, String title, String body, int severity) {
        if (context == null) {
            return;
        }

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            Log.w(TAG, "No NotificationManager available");
            return;
        }

        // Clamp rather than throw: the severity crosses an FFI boundary, and a
        // notification is not worth crashing the app over.
        int level = Math.max(0, Math.min(severity, CHANNEL_IDS.length - 1));

        // Creating a channel that already exists only updates its name, so this
        // is safe to do on every notification and saves an init hook.
        manager.createNotificationChannel(
                new NotificationChannel(CHANNEL_IDS[level], CHANNEL_NAMES[level], IMPORTANCE[level]));

        // Framework status-bar icons: the app ships no drawables of its own, and
        // a small icon must be a white silhouette to tint correctly.
        int icon = level == 0
                ? android.R.drawable.stat_notify_sync
                : android.R.drawable.stat_sys_warning;

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_IDS[level])
                        .setSmallIcon(icon)
                        .setContentTitle(title)
                        .setAutoCancel(true);

        if (body != null && !body.isEmpty()) {
            builder.setContentText(body)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(body));
        }

        try {
            manager.notify(nextId(), builder.build());
        } catch (SecurityException e) {
            // POST_NOTIFICATIONS was denied (API 33+). The user said no, so
            // this is not an error worth more than a log line.
            Log.w(TAG, "Not permitted to post notifications", e);
        }
    }

    private static synchronized int nextId() {
        return nextId++;
    }
}
