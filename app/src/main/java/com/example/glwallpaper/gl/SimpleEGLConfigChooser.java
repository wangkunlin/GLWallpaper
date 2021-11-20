package com.example.glwallpaper.gl;

/**
 * On 2021-11-20
 */
public class SimpleEGLConfigChooser extends BaseConfigChooser.ComponentSizeChooser {
    public SimpleEGLConfigChooser(boolean withDepthBuffer, int EGLContextClientVersion) {
        super(8, 8, 8, 0, withDepthBuffer ? 16 : 0, 0,
                EGLContextClientVersion);
    }
}
