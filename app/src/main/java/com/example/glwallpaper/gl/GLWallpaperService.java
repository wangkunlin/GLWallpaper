package com.example.glwallpaper.gl;

import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

import androidx.annotation.IntDef;

/**
 * On 2021-11-19
 */
public abstract class GLWallpaperService extends WallpaperService {

    public static final int RENDERMODE_WHEN_DIRTY = 0;
    public static final int RENDERMODE_CONTINUOUSLY = 1;

    @IntDef({RENDERMODE_WHEN_DIRTY, RENDERMODE_CONTINUOUSLY})
    public @interface RenderMode {
    }

    public class GLEngine extends Engine {

        private GLThread mGLThread;

        private int mEGLContextClientVersion;
        private EGLConfigChooser mEGLConfigChooser;
        private EGLContextFactory mEGLContextFactory;
        private EGLWindowSurfaceFactory mEGLWindowSurfaceFactory;
        private GLWrapper mGLWrapper;

        private void checkRenderThreadState() {
            if (mGLThread != null) {
                throw new IllegalStateException(
                        "setRenderer has already been called for this instance.");
            }
        }

        public void setEGLContextClientVersion(int version) {
            checkRenderThreadState();
            mEGLContextClientVersion = version;
        }

        public void setEGLConfigChooser(EGLConfigChooser configChooser) {
            checkRenderThreadState();
            mEGLConfigChooser = configChooser;
        }

        public void setEGLContextFactory(EGLContextFactory EGLContextFactory) {
            checkRenderThreadState();
            mEGLContextFactory = EGLContextFactory;
        }

        public void setEGLWindowSurfaceFactory(EGLWindowSurfaceFactory EGLWindowSurfaceFactory) {
            checkRenderThreadState();
            mEGLWindowSurfaceFactory = EGLWindowSurfaceFactory;
        }

        public void setGLWrapper(GLWrapper GLWrapper) {
            checkRenderThreadState();
            mGLWrapper = GLWrapper;
        }

        public void setPreserveEGLContextOnPause(boolean preserveOnPause) {
            mGLThread.setPreserveEGLContextOnPause(preserveOnPause);
        }

        public void setRenderer(Renderer renderer) {
            checkRenderThreadState();
            if (mEGLConfigChooser == null) {
                mEGLConfigChooser = new SimpleEGLConfigChooser(true, mEGLContextClientVersion);
            }
            if (mEGLContextFactory == null) {
                mEGLContextFactory = new DefaultContextFactory(mEGLContextClientVersion);
            }
            if (mEGLWindowSurfaceFactory == null) {
                mEGLWindowSurfaceFactory = new DefaultWindowSurfaceFactory();
            }
            mGLThread = new GLThread(renderer, mEGLConfigChooser,
                    mEGLContextFactory, mEGLWindowSurfaceFactory, mGLWrapper);
            mGLThread.start();
        }

        public void setRenderMode(@RenderMode int renderMode) {
            mGLThread.setRenderMode(renderMode);
        }

        public void requestRender() {
            if (!isVisible()) {
                return;
            }
            if (mGLThread != null) {
                mGLThread.requestRender();
            }
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            if (mGLThread != null) {
                mGLThread.surfaceCreated(holder);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            if (mGLThread != null) {
                mGLThread.onWindowResize(width, height);
            }
        }

        @Override
        public void onSurfaceRedrawNeeded(SurfaceHolder holder) {
            super.onSurfaceRedrawNeeded(holder);
            requestRender();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (mGLThread == null) {
                return;
            }
            if (visible) {
                mGLThread.onResume();
            } else {
                mGLThread.onPause();
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            if (mGLThread != null) {
                mGLThread.surfaceDestroyed();
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (mGLThread != null) {
                mGLThread.requestExitAndWait();
            }
        }
    }

}
