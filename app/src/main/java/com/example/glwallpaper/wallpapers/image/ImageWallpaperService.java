package com.example.glwallpaper.wallpapers.image;

import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.SurfaceHolder;

import androidx.exifinterface.media.ExifInterface;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.glwallpaper.gl.GLWallpaperService;
import com.example.glwallpaper.wallpapers.RotationMonitor;

/**
 * On 2021-11-19
 */
public class ImageWallpaperService extends GLWallpaperService {

    private static final String ACTION_SET_WALLPAPER = ImageWallpaperService.class.getName();
    private LocalBroadcastManager mBroadcastManager;
    private SharedPreferences mPreferences;

    public static void setWallpaper(Context context, String path) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putString(ACTION_SET_WALLPAPER, path).apply();

        WallpaperManager manager = WallpaperManager.getInstance(context);

        ComponentName cn = new ComponentName(context, ImageWallpaperService.class);
        WallpaperInfo info = manager.getWallpaperInfo();
        if (info == null || !info.getComponent().equals(cn)) {
            Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, cn);
            context.startActivity(intent);
        } else {
            LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(context);
            broadcastManager.sendBroadcast(new Intent(ACTION_SET_WALLPAPER));
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mBroadcastManager = LocalBroadcastManager.getInstance(this);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    private class ImageEngine extends GLEngine implements RotationMonitor.RotationChangedListener {

        private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleReceiverEvent();
            }
        };

        private Bitmap loadBitmap() {
            String path = mPreferences.getString(ACTION_SET_WALLPAPER, "");
            if (TextUtils.isEmpty(path)) {
                return null;
            }

            int degree = 0;
            ExifInterface exif = null;
            try {
                exif = new ExifInterface(path);
            } catch (Throwable e) {
                e.printStackTrace();
            }

            if (exif != null) {
                int orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL);
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        degree = 90;
                        break;

                    case ExifInterface.ORIENTATION_ROTATE_180:
                        degree = 180;
                        break;

                    case ExifInterface.ORIENTATION_ROTATE_270:
                        degree = 270;
                        break;
                }
            }

            Bitmap bitmap = BitmapFactory.decodeFile(path);

            if (degree != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(degree);
                Bitmap b = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                        bitmap.getHeight(), matrix, true);
                bitmap.recycle();
                bitmap = b;
            }

            return bitmap;
        }

        private void handleReceiverEvent() {
            Bitmap bitmap = loadBitmap();
            if (bitmap == null) {
                return;
            }
            changeBitmap(bitmap);
        }

        private ImageWallpaperRenderer mRenderer;
        private RotationMonitor mRotationMonitor;

        private void changeBitmap(Bitmap bitmap) {
            mRenderer.setBitmap(bitmap);
            requestRender();
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
//            setEnableLog();
            super.onCreate(surfaceHolder);
            setOffsetNotificationsEnabled(false);

            Bitmap bitmap = loadBitmap();
            if (bitmap == null) {
                return;
            }
            mRotationMonitor = new RotationMonitor(getApplicationContext(), 60, this);

            setEGLContextClientVersion(2);

            mRenderer = new ImageWallpaperRenderer();
            mRenderer.setDistance(25);
            mRenderer.setBitmap(bitmap);

            setRenderer(mRenderer);
            setPreserveEGLContextOnPause(true);
            setRenderMode(RENDERMODE_WHEN_DIRTY);

            installReceiver();
        }

        private void installReceiver() {
            IntentFilter filter = new IntentFilter(ACTION_SET_WALLPAPER);

            mBroadcastManager.registerReceiver(mReceiver, filter);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (mRotationMonitor == null) {
                return;
            }
            if (visible) {
                mRotationMonitor.start();
            } else {
                mRotationMonitor.stop();
            }
        }

        @Override
        public void onRotationChanged(float[] angle) {
            if (mRenderer == null) {
                return;
            }
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mRenderer.angleChanged(angle[1], angle[2]);
            } else {
                mRenderer.angleChanged(angle[2], angle[1]);
            }
            requestRender();
        }

        private void uninstallReceiver() {
            mBroadcastManager.unregisterReceiver(mReceiver);
        }

        @Override
        public void onDestroy() {
            uninstallReceiver();
            if (mRotationMonitor != null) {
                mRotationMonitor.stop();
            }
            super.onDestroy();
        }
    }

    @Override
    public Engine onCreateEngine() {
        return new ImageEngine();
    }
}
