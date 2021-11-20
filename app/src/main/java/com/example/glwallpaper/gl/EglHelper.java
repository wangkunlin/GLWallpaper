package com.example.glwallpaper.gl;

import android.util.Log;
import android.view.SurfaceHolder;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL;

/**
 * On 2021-11-19
 */
class EglHelper {

    private EGL10 mEgl;
    EGLConfig mEglConfig;
    private EGLContext mEglContext;

    private EGLDisplay mEglDisplay;
    private EGLSurface mEglSurface;

    private final EGLConfigChooser mEGLConfigChooser;
    private final EGLContextFactory mEGLContextFactory;
    private final EGLWindowSurfaceFactory mEGLWindowSurfaceFactory;
    private final GLWrapper mGLWrapper;

    static boolean mEnableLog = false;

    public EglHelper(EGLConfigChooser eglConfigChooser,
                     EGLContextFactory eglContextFactory,
                     EGLWindowSurfaceFactory eglWindowSurfaceFactory,
                     GLWrapper glWrapper) {
        mEGLConfigChooser = eglConfigChooser;
        mEGLContextFactory = eglContextFactory;
        mEGLWindowSurfaceFactory = eglWindowSurfaceFactory;
        mGLWrapper = glWrapper;
    }

    /**
     * Initialize EGL for a given configuration spec.
     */
    public void start() {
        if (mEnableLog) {
            Log.w("EglHelper", "start() tid=" + Thread.currentThread().getId());
        }
        /*
         * Get an EGL instance
         */
        if (mEgl == null) {
            mEgl = (EGL10) EGLContext.getEGL();
        }

        /*
         * Get to the default display.
         */
        if (mEglDisplay == null) {
            mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        }

        if (mEglDisplay == EGL10.EGL_NO_DISPLAY) {
            throw new RuntimeException("eglGetDisplay failed");
        }

        /*
         * We can now initialize EGL for that display
         */
        if (mEglConfig == null) {
            int[] version = new int[2];
            if (!mEgl.eglInitialize(mEglDisplay, version)) {
                throw new RuntimeException("eglInitialize failed");
            }
            mEglConfig = mEGLConfigChooser.chooseConfig(mEgl, mEglDisplay);
        }

        /*
         * Create an EGL context. We want to do this as rarely as we can, because an
         * EGL context is a somewhat heavy object.
         */
        if (mEglContext == null) {
            mEglContext = mEGLContextFactory.createContext(mEgl, mEglDisplay, mEglConfig);
            if (mEglContext == null || mEglContext == EGL10.EGL_NO_CONTEXT) {
                mEglContext = null;
                throw new RuntimeException("createContext failed");
            }
            if (mEnableLog) {
                Log.w("EglHelper", "createContext " + mEglContext + " tid=" + Thread.currentThread().getId());
            }
        }

        mEglSurface = null;
    }

    /**
     * Create an egl surface for the current SurfaceHolder surface. If a surface
     * already exists, destroy it before creating the new surface.
     *
     * @return true if the surface was created successfully.
     */
    public boolean createSurface(SurfaceHolder surfaceHolder) {
        if (mEnableLog) {
            Log.w("EglHelper", "createSurface()  tid=" + Thread.currentThread().getId());
        }
        /*
         * Check preconditions.
         */
        if (mEgl == null) {
            throw new RuntimeException("egl not initialized");
        }
        if (mEglDisplay == null) {
            throw new RuntimeException("eglDisplay not initialized");
        }
        if (mEglConfig == null) {
            throw new RuntimeException("mEglConfig not initialized");
        }

        /*
         *  The window size has changed, so we need to create a new
         *  surface.
         */
        destroySurfaceImp();

        /*
         * Create an EGL surface we can render into.
         */
        mEglSurface = mEGLWindowSurfaceFactory.createWindowSurface(mEgl,
                mEglDisplay, mEglConfig, surfaceHolder);

        if (mEglSurface == null || mEglSurface == EGL10.EGL_NO_SURFACE) {
            int error = mEgl.eglGetError();
            if (error == EGL10.EGL_BAD_NATIVE_WINDOW) {
                if (mEnableLog) {
                    Log.e("EglHelper", "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.");
                }
            }
            return false;
        }

        /*
         * Before we can issue GL commands, we need to make sure
         * the context is current and bound to a surface.
         */
        if (!mEgl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
            /*
             * Could not make the context current, probably because the underlying
             * SurfaceView surface has been destroyed.
             */
            logEglErrorAsWarning("EGLHelper", "eglMakeCurrent", mEgl.eglGetError());
            return false;
        }

        return true;
    }

    /**
     * Create a GL object for the current EGL context.
     *
     * @return
     */
    GL createGL() {
        GL gl = mEglContext.getGL();
        if (mGLWrapper != null) {
            gl = mGLWrapper.wrap(gl);
        }
        return gl;
    }

    /**
     * Display the current render surface.
     *
     * @return the EGL error code from eglSwapBuffers.
     */
    public int swap() {
        if (!mEgl.eglSwapBuffers(mEglDisplay, mEglSurface)) {
            return mEgl.eglGetError();
        }
        return EGL10.EGL_SUCCESS;
    }

    public void destroySurface() {
        if (mEnableLog) {
            Log.w("EglHelper", "destroySurface()  tid=" + Thread.currentThread().getId());
        }
        destroySurfaceImp();
    }

    private void destroySurfaceImp() {
        if (mEglSurface != null && mEglSurface != EGL10.EGL_NO_SURFACE) {
            mEgl.eglMakeCurrent(mEglDisplay, EGL10.EGL_NO_SURFACE,
                    EGL10.EGL_NO_SURFACE,
                    EGL10.EGL_NO_CONTEXT);
            mEGLWindowSurfaceFactory.destroySurface(mEgl, mEglDisplay, mEglSurface);
            mEglSurface = null;
        }
    }

    public void finish() {
        if (mEnableLog) {
            Log.w("EglHelper", "finish() tid=" + Thread.currentThread().getId());
        }
        if (mEglContext != null) {
            mEGLContextFactory.destroyContext(mEgl, mEglDisplay, mEglContext);
            mEglContext = null;
        }
        if (mEglDisplay != null) {
            mEgl.eglTerminate(mEglDisplay);
            mEglDisplay = null;
        }
    }

    public static void throwEglException(String function, int error) {
        String message = formatEglError(function, error);
        if (mEnableLog) {
            Log.e("EglHelper", "throwEglException tid=" + Thread.currentThread().getId() + " "
                    + message);
        }
        throw new RuntimeException(message);
    }

    public static void logEglErrorAsWarning(String tag, String function, int error) {
        if (mEnableLog) {
            Log.w(tag, formatEglError(function, error));
        }
    }

    public static String formatEglError(String function, int error) {
        return function + " failed: " + error;
    }
}
