package com.example.glwallpaper.gl;

import android.util.Log;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

/**
 * On 2021-11-19
 */
public class DefaultContextFactory implements EGLContextFactory {
    private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

    private final int mEGLContextClientVersion;

    public DefaultContextFactory(int EGLContextClientVersion) {
        mEGLContextClientVersion = EGLContextClientVersion;
    }

    @Override
    public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig config) {
        int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, mEGLContextClientVersion,
                EGL10.EGL_NONE};

        return egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT,
                mEGLContextClientVersion != 0 ? attrib_list : null);
    }

    @Override
    public void destroyContext(EGL10 egl, EGLDisplay display,
                               EGLContext context) {
        if (!egl.eglDestroyContext(display, context)) {
            Log.e("DefaultContextFactory", "display:" + display + " context: " + context);
        }
    }
}
