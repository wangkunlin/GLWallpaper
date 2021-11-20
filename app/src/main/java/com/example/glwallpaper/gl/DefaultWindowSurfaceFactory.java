package com.example.glwallpaper.gl;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

/**
 * On 2021-11-19
 */
public class DefaultWindowSurfaceFactory implements EGLWindowSurfaceFactory {

    private static final String TAG = "DefaultWindowSurfaceFac";

    @Override
    public EGLSurface createWindowSurface(EGL10 egl, EGLDisplay display,
                                          EGLConfig config, Object nativeWindow) {
        EGLSurface result = null;
        while (result == null) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
            }

            try {
                result = egl.eglCreateWindowSurface(display, config, nativeWindow, null);
            } catch (Throwable ignored) {
            }
        }
        return result;
    }

    @Override
    public void destroySurface(EGL10 egl, EGLDisplay display,
                               EGLSurface surface) {
        egl.eglDestroySurface(display, surface);
    }
}
