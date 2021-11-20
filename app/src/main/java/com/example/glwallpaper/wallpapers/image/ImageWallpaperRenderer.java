package com.example.glwallpaper.wallpapers.image;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.renderscript.Matrix4f;

import com.example.glwallpaper.gl.GLUtil;
import com.example.glwallpaper.gl.Renderer;

import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


/**
 * On 2021-11-19
 */
class ImageWallpaperRenderer implements Renderer {

    private static final String TAG = "ImageWallpaperRenderer";

    private static final String sVertexSrc = "" +
            "attribute vec4 position;\n" +
            "attribute vec2 texCoords;\n" +
            "varying vec2 outTexCoords;\n" +
            "uniform mat4 projection;\n" +
            "void main(void) {\n" +
            "    outTexCoords = texCoords;\n" +
            "    gl_Position = projection * position;\n" +
            "}\n";

    private static final String sFragmentSrc = "" +
            "precision mediump float;\n" +
            "varying vec2 outTexCoords;\n" +
            "uniform sampler2D texture;\n" +
            "void main(void) {\n" +
            "    gl_FragColor = texture2D(texture, outTexCoords);\n" +
            "}\n";


    private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * GLUtil.FLOAT_SIZE_BYTES;
    private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
    private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;

    private int mSurfaceWidth;
    private int mSurfaceHeight;

    private Bitmap mBitmap;
    private int mImageWidth;
    private int mImageHeight;

    private int mTextureId = -1;
    private int mProgram;
    private int mAttribPosition;
    private int mAttribTexCoords;
    private int mUniformTexture;
    private int mUniformProjection;

    private float mScale = 1f;

    void setBitmap(Bitmap bitmap) {
        if (mBitmap != null) {
            mBitmap.recycle();
        }

        mBitmap = bitmap;

        mImageWidth = bitmap.getWidth();
        mImageHeight = bitmap.getHeight();
        mTextureId = -1;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        mProgram = GLUtil.buildProgram(sVertexSrc, sFragmentSrc);

        mAttribPosition = GLES20.glGetAttribLocation(mProgram, "position");
        mAttribTexCoords = GLES20.glGetAttribLocation(mProgram, "texCoords");
        mUniformTexture = GLES20.glGetUniformLocation(mProgram, "texture");
        mUniformProjection = GLES20.glGetUniformLocation(mProgram, "projection");

        GLUtil.checkGlError();
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

    @Override
    public void onDrawFrame(GL10 gl) {
        if (mTextureId == -1) {
            mTextureId = GLUtil.loadTexture(mBitmap);

            mScale = Math.max(1f, Math.max(mSurfaceWidth / (float) mImageWidth,
                    mSurfaceHeight / (float) mImageHeight));
        }

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        GLES20.glUseProgram(mProgram);

        GLES20.glEnableVertexAttribArray(mAttribPosition);
        GLES20.glEnableVertexAttribArray(mAttribTexCoords);

        GLES20.glUniform1i(mUniformTexture, 0);

        final Matrix4f ortho = new Matrix4f();
        ortho.loadOrtho(0.0f, mSurfaceWidth, mSurfaceHeight, 0.0f, -1.0f, 1.0f);
        GLES20.glUniformMatrix4fv(mUniformProjection, 1, false, ortho.getArray(), 0);

        GLUtil.checkGlError();

        int imageW = (int) (mImageWidth * mScale);
        int imageH = (int) (mImageHeight * mScale);

        int left = (mSurfaceWidth - imageW) / 2;
        int top = (mSurfaceHeight - imageH) / 2;
        int right = left + imageW;
        int bottom = top + imageH;

        final FloatBuffer triangleVertices = GLUtil.createMesh(left, top, right, bottom);

        // drawQuad
        triangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
        GLES20.glVertexAttribPointer(mAttribPosition, 3, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices);

        triangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
        GLES20.glVertexAttribPointer(mAttribTexCoords, 3, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(mAttribPosition);
        GLES20.glDisableVertexAttribArray(mAttribTexCoords);
    }

    @Override
    public void onDestroy() {
        if (mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }
    }
}
