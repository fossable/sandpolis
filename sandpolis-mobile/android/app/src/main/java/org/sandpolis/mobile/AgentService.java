import android.app.Notification;
import android.app.Service;
import android.content.pm.PackageManager;
import android.os.Build;
import android.Manifest;
import android.content.Context;
import androidx.core.app.ServiceCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import android.app.ServiceInfo;
import android.os.ForegroundServiceStartNotAllowedException;

public class AgentService extends Service {

    private void startForeground() {
        // Before starting the service as foreground check that the app has the
        // appropriate runtime permissions. In this case, verify that the user
        // has granted the CAMERA permission.
        int cameraPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (cameraPermission == PackageManager.PERMISSION_DENIED) {
            // Without camera permissions the service cannot run in the
            // foreground. Consider informing user or updating your app UI if
            // visible.
            stopSelf();
            return;
        }

        try {
            Notification notification =
                new NotificationCompat.Builder(this, "CHANNEL_ID")
                    // Create the notification to display while the service
                    // is running
                    .build();
            int type = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                type = ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA;
            }
            ServiceCompat.startForeground(
                    /* service = */ this,
                    /* id = */ 100, // Cannot be 0
                    /* notification = */ notification,
                    /* foregroundServiceType = */ type
            );
        } catch (Exception e) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    e instanceof ForegroundServiceStartNotAllowedException
            ) {
                // App not in a valid state to start foreground service
                // (e.g started from bg)
            }
            // ...
        }
    }

    //...
}
