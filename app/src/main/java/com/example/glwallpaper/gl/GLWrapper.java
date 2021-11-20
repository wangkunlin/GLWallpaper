package com.example.glwallpaper.gl;

import javax.microedition.khronos.opengles.GL;

/**
 * On 2021-11-20
 */
public interface GLWrapper {
    /**
     * Wraps a gl interface in another gl interface.
     *
     * @param gl a GL interface that is to be wrapped.
     * @return either the input argument or another GL object that wraps the input argument.
     */
    GL wrap(GL gl);
}
