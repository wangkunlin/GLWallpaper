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

import java.util.ArrayList;
import java.util.List;

/**
 * On 2021-11-19
 */
public class ImageWallpaperService extends GLWallpaperService {

    private static final String ACTION_SET_WALLPAPER = ImageWallpaperService.class.getName();
    private LocalBroadcastManager mBroadcastManager;
    private SharedPreferences mPreferences;

    public static void setWallpaper(Context context, ImageWallpaperMeta bean) {
        if (bean.isInvalid()) {
            return;
        }
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putString(ACTION_SET_WALLPAPER, bean.toJson()).apply();

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

        private final ImageWallpaperRenderer mRenderer = new ImageWallpaperRenderer();
        private RotationMonitor mRotationMonitor;

        private ImageWallpaperMeta getWallpaperMeta() {
            String json = mPreferences.getString(ACTION_SET_WALLPAPER, "");
            return ImageWallpaperMeta.fromJson(json);
        }

        private void handleImageChange() {
            ImageWallpaperMeta meta = getWallpaperMeta();
            if (meta == null || meta.isInvalid()) {
                return;
            }
            int distance = ImageWallpaperMeta.DEFAULT_MOVE_DISTANCE;
            if (meta.moveDistance > 0) {
                distance = meta.moveDistance;
            }
            mRenderer.setDistance(distance);

            float extraScale = ImageWallpaperMeta.DEFAULT_EXTRA_SCALE;
            if (meta.extraScale >= 0) {
                extraScale = meta.extraScale;
            }

            List<GLBitmap> glBitmaps = new ArrayList<>();
            List<Float> moveFactors = new ArrayList<>();

            for (int i = 0; i < meta.images.size(); i++) {
                float factor = meta.getMovieFactor(i);
                moveFactors.add(factor);

                String path = meta.images.get(i);
                GLBitmap glBitmap = GLBitmap.create(path, extraScale);
                glBitmaps.add(glBitmap);
            }

            mRenderer.setImages(glBitmaps);
            mRenderer.setMoveFactors(moveFactors);
            requestRender();
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
//            setEnableLog();
            super.onCreate(surfaceHolder);
            setOffsetNotificationsEnabled(false);

            mRotationMonitor = new RotationMonitor(getApplicationContext(), 60, this);

            setEGLContextClientVersion(2);

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
            mRenderer.release();
            super.onDestroy();
        }
    }

    @Override
    public Engine onCreateEngine() {
        return new ImageEngine();
    }
}
