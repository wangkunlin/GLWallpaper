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
public class ImageWallpaperRenderer implements Renderer {

    private static final String TAG = "ImageWallpaperRenderer";

    private static final String sVertexShader = "" +
            "attribute vec4 position;\n" +
            "attribute vec2 texCoords;\n" +
            "varying vec2 outTexCoords;\n" +
            "uniform mat4 projection;\n" +
            "void main(void) {\n" +
            "    outTexCoords = texCoords;\n" +
            "    gl_Position = projection * position;\n" +
            "}\n";

    private static final String sFragmentShader = "" +
            "precision mediump float;\n" +
            "varying vec2 outTexCoords;\n" +
            "uniform sampler2D texture;\n" +
            "void main(void) {\n" +
            "    gl_FragColor = texture2D(texture, outTexCoords);\n" +
            "}\n";

    // 顶点坐标数组, 坐标原点为 open gl 的正中心, 向右为 x 正方形，向上为 y 正方向
    // 顶点坐标定义 绘制区域, 每个区域是一个三角形, 顶点三角形组合, 有多个方式
    // 这个需要动态计算，这里只是展示
    private static final float[] VERTICES = {
            -1.0f, -1.0f, // 左下
            +1.0f, -1.0f, // 右下
            -1.0f, +1.0f, // 左上
            +1.0f, +1.0f, // 右上
    };

    // 纹理坐标数组, 坐标原点为 纹理的左下角, 向右为 x 正方形，向上为 y 正方向
    // 纹理坐标，定义 纹理应该如何映射到 顶点坐标所描述的三角形上
    // 上下颠倒 是因为 open gl 的坐标与 android 的坐标系 y 轴相反，所以要反过来映射
    private static final float[] TEXTURES = {
            0f, 1f, // 左上
            1f, 1f, // 右上
            0f, 0f, // 左下
            1f, 0f, // 右下
    };

//    private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * GLUtil.FLOAT_SIZE_BYTES;
//    private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
//    private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;

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

    private final FloatBuffer mVertexBuffer;
    private final FloatBuffer mTextureBuffer;
    private final Matrix4f mMatrix = new Matrix4f();
    private final float[] mTmpMatrix = new float[16];

    public ImageWallpaperRenderer() {
        mVertexBuffer = GLUtil.makeFloatBuffer(VERTICES);
        mTextureBuffer = GLUtil.makeFloatBuffer(TEXTURES);
    }

    public void setBitmap(Bitmap bitmap) {
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
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFuncSeparate(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA, 1, 1);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        mProgram = GLUtil.buildProgram(sVertexShader, sFragmentShader);

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

    private float mMoveFactor = 0.06f;

    public void setDistance(int distance) {
        mMoveFactor = distance * 0.003f + 0.03f;
    }

    public void angleChanged(float x, float y) {
        double xSin = Math.sin(x);
        float xValue = (float) (xSin * mMoveFactor);

        double ySin = Math.sin(y);
        float yValue = (float) (ySin * mMoveFactor);

        synchronized (mMatrix) { // 图片会抖动，猜测可能是，传感器的速率和 绘制速率不同导致，暂时加个锁
            mMatrix.loadIdentity();
            mMatrix.translate(xValue, yValue, 0);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (mTextureId == -1) {
            if (mBitmap == null) {
                throw new NullPointerException("bitmap == null");
            }
            mTextureId = GLUtil.loadTexture(mBitmap);

            float scale = Math.max(mSurfaceWidth / (float) mImageWidth,
                    mSurfaceHeight / (float) mImageHeight);

            scale += 0.12f;
            int imageW = (int) (mImageWidth * scale);
            int imageH = (int) (mImageHeight * scale);

            float xScale = imageW * 1f / mSurfaceWidth;
            float yScale = imageH * 1f / mSurfaceHeight;

            float[] vertices = {
                    -xScale, -yScale, // 左下
                    +xScale, -yScale, // 右下
                    -xScale, +yScale, // 左上
                    +xScale, +yScale, // 右上
            };
            mVertexBuffer.position(0);
            mVertexBuffer.put(vertices);
        }

//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        GLES20.glUseProgram(mProgram);

        GLES20.glUniform1i(mUniformTexture, 0);

        synchronized (mMatrix) {
            System.arraycopy(mMatrix.getArray(), 0, mTmpMatrix, 0, 16);
            GLES20.glUniformMatrix4fv(mUniformProjection, 1, false, mTmpMatrix, 0);
        }

        GLUtil.checkGlError();

        mVertexBuffer.position(0);
        int vertextSize = 2;
        GLES20.glVertexAttribPointer(mAttribPosition, vertextSize, GLES20.GL_FLOAT, false,
                vertextSize * GLUtil.FLOAT_SIZE_BYTES, mVertexBuffer);
        GLES20.glEnableVertexAttribArray(mAttribPosition);

        mTextureBuffer.position(0);
        int textureSize = 2;
        GLES20.glVertexAttribPointer(mAttribTexCoords, textureSize, GLES20.GL_FLOAT, false,
                textureSize * GLUtil.FLOAT_SIZE_BYTES, mTextureBuffer);
        GLES20.glEnableVertexAttribArray(mAttribTexCoords);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTICES.length / 2);

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
