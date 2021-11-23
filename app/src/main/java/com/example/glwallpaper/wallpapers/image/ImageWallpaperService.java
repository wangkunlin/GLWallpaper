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
import android.preference.PreferenceManager;
import android.view.SurfaceHolder;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.glwallpaper.gl.GLBitmap;
import com.example.glwallpaper.gl.GLWallpaperService;
import com.example.glwallpaper.wallpapers.RotationMonitor;

import java.util.Collections;

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
                handleImageChange();
            }
        };

        private ImageWallpaperRenderer mRenderer;
        private RotationMonitor mRotationMonitor;

        private String getImagePath() {
            return mPreferences.getString(ACTION_SET_WALLPAPER, "");
        }

        private void handleImageChange() {
            String path = getImagePath();
            GLBitmap glBitmap = GLBitmap.create(path, 0.12f);
            mRenderer.setImages(Collections.singletonList(glBitmap));
            requestRender();
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
//            setEnableLog();
            super.onCreate(surfaceHolder);
            setOffsetNotificationsEnabled(false);

            mRotationMonitor = new RotationMonitor(getApplicationContext(), 60, this);

            setEGLContextClientVersion(2);

            mRenderer = new ImageWallpaperRenderer();
            mRenderer.setDistance(25);
            handleImageChange();

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
