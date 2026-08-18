package org.sandpolis.mobile;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import com.google.androidgamesdk.GameActivity;

public class MainActivity extends GameActivity {

    /** Arbitrary; we never inspect the result, so nothing else uses it. */
    private static final int REQUEST_POST_NOTIFICATIONS = 1;

    /** SAF document picker for a realm certificate. */
    private static final int REQUEST_PICK_REALM_CERT = 2;

    static {
        Log.d("main", "Preparing to load native library");
        System.loadLibrary("sandpolis_mobile");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // From API 33 posting a notification needs a runtime grant as well as
        // the manifest entry; without it NotificationManager.notify is silently
        // dropped. Denial is fine — notifications just don't appear.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_POST_NOTIFICATIONS);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            hideSystemUi();
        }
    }

    private void hideSystemUi() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
        );
    }

    /**
     * Launch the Storage Access Framework document picker so the user can
     * choose a realm certificate (.realm.pem). Called from Rust via JNI, so it
     * hops to the UI thread itself. The result is delivered back through
     * {@link #nativeOnRealmCertPicked}.
     */
    public void openRealmCertPicker() {
        runOnUiThread(() -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            // A .pem file has no reliable MIME type, so accept anything and
            // let the native side validate the contents.
            intent.setType("*/*");
            startActivityForResult(intent, REQUEST_PICK_REALM_CERT);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PICK_REALM_CERT) {
            return;
        }

        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            nativeOnRealmCertPicked(null, null);
            return;
        }

        Uri uri = data.getData();
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            nativeOnRealmCertPicked(displayName(uri), out.toByteArray());
        } catch (Exception e) {
            Log.e("main", "Failed to read picked realm cert", e);
            nativeOnRealmCertPicked(null, null);
        }
    }

    /** The picked document's display name, for native error messages. */
    private String displayName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    return cursor.getString(index);
                }
            }
        } catch (Exception e) {
            Log.w("main", "Failed to resolve picked file name", e);
        }
        return uri.getLastPathSegment();
    }

    /**
     * Hand the picked realm cert to the native library. Both arguments are
     * null when the user backed out of the picker (or the file was unreadable).
     */
    private static native void nativeOnRealmCertPicked(String name, byte[] contents);
}
