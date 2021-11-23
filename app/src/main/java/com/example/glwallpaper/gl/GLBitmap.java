package com.example.glwallpaper.gl;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.opengl.GLES20;
import android.renderscript.Matrix4f;

import androidx.exifinterface.media.ExifInterface;

import java.nio.FloatBuffer;

/**
 * On 2021-11-23
 */
public class GLBitmap {

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

    private static int mProgram;
    private static int mAttribPosition;
    private static int mAttribTexCoords;
    private static int mUniformTexture;
    private static int mUniformProjection;

    private final Bitmap mBitmap;

    private final FloatBuffer mVertexBuffer;
    private final FloatBuffer mTextureBuffer;
    private final float[] mMatrix;

    private int mTextureId = -1;
    private final float mExtraScale;

    public static GLBitmap create(String path, float extraScale) {
        return new GLBitmap(path, extraScale);
    }

    private GLBitmap(String path, float extraScale) {
        if (extraScale > 0) {
            mExtraScale = extraScale;
        } else {
            mExtraScale = 0;
        }

        mVertexBuffer = GLUtil.makeFloatBuffer(VERTICES);
        mTextureBuffer = GLUtil.makeFloatBuffer(TEXTURES);
        mMatrix = new Matrix4f().getArray();

        int degree = 0;
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(path);
        } catch (Throwable e) {
            e.printStackTrace();
        }

        if (exif != null) {
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        }

        // todo 根据屏幕尺寸，做缩放处理，以免发生 oom
        Bitmap bitmap = BitmapFactory.decodeFile(path);

        if (degree != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(degree);
            Bitmap b = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                    bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            bitmap = b;
        }
        mBitmap = bitmap;
    }

    public static void installProgram() {

        mProgram = GLUtil.buildProgram(sVertexShader, sFragmentShader);

        mAttribPosition = GLES20.glGetAttribLocation(mProgram, "position");
        mAttribTexCoords = GLES20.glGetAttribLocation(mProgram, "texCoords");
        mUniformTexture = GLES20.glGetUniformLocation(mProgram, "texture");
        mUniformProjection = GLES20.glGetUniformLocation(mProgram, "projection");

        GLUtil.checkGlError();
    }

    public void draw(int dw, int dh, float translateX, float translateY) {
        if (mTextureId == -1) {
            if (mBitmap == null || mBitmap.isRecycled()) {
                throw new NullPointerException("bitmap == null");
            }
            mTextureId = GLUtil.loadTexture(mBitmap);
            int imageWidth = mBitmap.getWidth();
            int imageHeight = mBitmap.getHeight();

            float scale = Math.max(dw / (float) imageWidth,
                    dh / (float) imageHeight);

            scale += mExtraScale; // 额外多放大一部分
            int imageW = (int) (imageWidth * scale);
            int imageH = (int) (imageHeight * scale);

            float xScale = imageW * 1f / dw;
            float yScale = imageH * 1f / dh;

            float[] vertices = {
                    -xScale, -yScale, // 左下
                    +xScale, -yScale, // 右下
                    -xScale, +yScale, // 左上
                    +xScale, +yScale, // 右上
            };
            mVertexBuffer.position(0);
            mVertexBuffer.put(vertices);
        }

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        GLES20.glUseProgram(mProgram);

        GLES20.glUniform1i(mUniformTexture, 0);

        mMatrix[12] = translateX; // x
        mMatrix[13] = translateY; // y
        mMatrix[14] = 0;          // z
        GLES20.glUniformMatrix4fv(mUniformProjection, 1, false, mMatrix, 0);

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

    public void release() {
        if (mBitmap != null) {
            mBitmap.recycle();
        }
    }
}
