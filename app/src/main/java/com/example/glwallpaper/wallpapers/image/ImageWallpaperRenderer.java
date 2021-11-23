package com.example.glwallpaper.wallpapers.image;

import android.opengl.GLES20;

import com.example.glwallpaper.gl.GLBitmap;
import com.example.glwallpaper.gl.Renderer;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * On 2021-11-19
 */
public class ImageWallpaperRenderer implements Renderer {

    private final Object mTranslateLock = new Object();

    private final List<GLBitmap> mImages = new ArrayList<>();
    private final List<Float> mMoveFactors = new ArrayList<>();

    private int mSurfaceWidth;
    private int mSurfaceHeight;

    private float mTranslateX = 0;
    private float mTranslateY = 0;

    private int mImageCount = 0;
    private int mMoveFactorCount = 0;

    public ImageWallpaperRenderer() {
        setDistance(ImageWallpaperMeta.DEFAULT_MOVE_DISTANCE);
    }

    public void setImages(List<GLBitmap> images) {
        releaseImages();
        mImages.addAll(images);
        mImageCount = mImages.size();
    }

    public void setMoveFactors(List<Float> factors) {
        mMoveFactors.clear();
        mMoveFactors.addAll(factors);
        mMoveFactorCount = mMoveFactors.size();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFuncSeparate(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA, 1, 1);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        GLBitmap.installProgram();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (height == 0) {
            height = 1;
        }
        GLES20.glViewport(0, 0, width, height);
        mSurfaceWidth = width;
        mSurfaceHeight = height;
    }

    private float mBaseMoveFactor = 0.06f;

    public void setDistance(int distance) {
        mBaseMoveFactor = distance * 0.003f + 0.03f;
    }

    public void angleChanged(float x, float y) {
        synchronized (mTranslateLock) { // 图片会抖动，猜测可能是，传感器的速率和 绘制速率不同导致，暂时加个锁
            double xSin = Math.sin(x);
            mTranslateX = (float) (xSin * mBaseMoveFactor);

            double ySin = Math.sin(y);
            mTranslateY = (float) (ySin * mBaseMoveFactor);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        synchronized (mTranslateLock) {
            int N = mImageCount;
            int factorN = mMoveFactorCount;
            for (int i = 0; i < N; i++) {
                GLBitmap image = mImages.get(i);
                float factor = 1;
                if (i < factorN) {
                    factor = mMoveFactors.get(i);
                }
                float tx = mTranslateX / factor;
                float ty = mTranslateY / factor;
                image.draw(mSurfaceWidth, mSurfaceHeight, tx, ty);
            }
        }
    }

    @Override
    public void release() {
        releaseImages();
        mMoveFactors.clear();
    }

    private void releaseImages() {
        for (GLBitmap image : mImages) {
            if (image != null) {
                image.release();
            }
        }
        mImages.clear();
    }
}
