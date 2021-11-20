package com.example.glwallpaper.wallpapers.image;

import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.SurfaceHolder;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.glwallpaper.gl.GLWallpaperService;

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

    private class ImageEngine extends GLEngine {

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
            return BitmapFactory.decodeFile(path);
        }

        private void handleReceiverEvent() {
            Bitmap bitmap = loadBitmap();
            if (bitmap == null) {
                return;
            }
            changeBitmap(bitmap);
        }

        private ImageWallpaperRenderer mRenderer;

        private void changeBitmap(Bitmap bitmap) {
            mRenderer.setBitmap(bitmap);
            requestRender();
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            setOffsetNotificationsEnabled(false);

            Bitmap bitmap = loadBitmap();
            if (bitmap == null) {
                return;
            }

            setEGLContextClientVersion(2);

            mRenderer = new ImageWallpaperRenderer();
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


        private void uninstallReceiver() {
            mBroadcastManager.unregisterReceiver(mReceiver);
        }

        @Override
        public void onDestroy() {
            uninstallReceiver();
            super.onDestroy();
        }
    }

    @Override
    public Engine onCreateEngine() {
        return new ImageEngine();
    }
}
