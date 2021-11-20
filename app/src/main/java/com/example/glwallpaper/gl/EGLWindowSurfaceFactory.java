package com.example.glwallpaper.gl;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

/**
 * On 2021-11-20
 */
public interface EGLWindowSurfaceFactory {
    /**
     * @return null if the surface cannot be constructed.
     */
    EGLSurface createWindowSurface(EGL10 egl, EGLDisplay display, EGLConfig config,
                                   Object nativeWindow);

    void destroySurface(EGL10 egl, EGLDisplay display, EGLSurface surface);
}
