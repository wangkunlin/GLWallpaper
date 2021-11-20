package com.example.glwallpaper.util;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * On 2021-11-19
 * 竞品 代码
 */
public class DrawBitmapUtil {

    public static final float[] f1928l = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            1.0f, 0.0f
    };

    private static int glProgram;
    private static int aPosition;
    private static int aTexCoords;
    private static int uMVPMatrix;
    private static int uTexture;
    private static int maxTextureSize;


    public final float[] a;
    public boolean b = false;
    public FloatBuffer c;

    public FloatBuffer f1935d;

    public int f1936e = 1;

    public int f1937f = 1;

    public int mImageWidth = 0;

    public int mImageHeight = 0;

    public float mImageRatio;

    public int mMaxTextureSize;

    public int[] f1942k;

    public DrawBitmapUtil(Bitmap bitmap) {
        float[] fArr = new float[18];
        a = fArr;
        mMaxTextureSize = maxTextureSize;
        if (bitmap != null) {
            b = true;
            c = makeBuffer(fArr.length);
            float[] fArr2 = f1928l;
            FloatBuffer b0 = makeBuffer(fArr2.length);
            b0.put(fArr2);
            b0.position(0);
            f1935d = b0;
            mImageWidth = bitmap.getWidth();
            mImageHeight = bitmap.getHeight();
            mImageRatio = ((float) mImageWidth) / ((float) mImageHeight);
            int i5 = mImageHeight % mMaxTextureSize;
            int i6 = (mImageWidth / (mMaxTextureSize + 1)) + 1;
            f1936e = i6;
            int i7 = (mImageHeight / (mMaxTextureSize + 1)) + 1;
            f1937f = i7;
            int[] iArr = new int[(i6 * i7)];
            f1942k = iArr;
            if (i6 == 1 && i7 == 1) {
                iArr[0] = generateTexture(bitmap);
            } else {
                Rect rect = new Rect();
                for (int i8 = 0; i8 < f1937f; i8++) {
                    int i9 = 0;
                    while (i9 < f1936e) {
                        int i10 = mMaxTextureSize;
                        int i11 = f1937f;
                        int i12 = i9 + 1;
                        rect.set(i9 * i10, ((i11 - i8) - 1) * i10, i12 * i10, (i11 - i8) * i10);
                        if (i5 > 0) {
                            rect.offset(0, (-mMaxTextureSize) + i5);
                        }
                        Bitmap createBitmap = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height());
                        f1942k[(f1936e * i8) + i9] = generateTexture(createBitmap);
                        createBitmap.recycle();
                        i9 = i12;
                    }
                }
            }
            bitmap.recycle();
        }
    }

    public void release() {
        int[] iArr = f1942k;
        if (iArr != null) {
            GLES20.glDeleteTextures(iArr.length, iArr, 0);
            checkGlError("Destroy picture");
        }
    }

    public void draw(float[] fArr) {
        if (b) {
            GLES20.glUseProgram(glProgram);
            char c2 = 1;
            GLES20.glUniformMatrix4fv(uMVPMatrix, 1, false, fArr, 0);
            checkGlError("glUniformMatrix4fv");
            GLES20.glEnableVertexAttribArray(aPosition);
            GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, 12, (Buffer) c);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glUniform1i(uTexture, 0);
            GLES20.glVertexAttribPointer(aTexCoords, 2, GLES20.GL_FLOAT, false, 8, (Buffer) f1935d);
            GLES20.glEnableVertexAttribArray(aTexCoords);
            int i2 = 0;
            while (i2 < f1937f) {
                int i3 = 0;
                while (i3 < f1936e) {
                    float[] fArr2 = a;
                    float min = Math.min(i3 * 2.0f * mMaxTextureSize / mImageWidth - 4.0f, 1.0f) * (-mImageRatio);
                    fArr2[9] = min;
                    fArr2[3] = min;
                    fArr2[0] = min;
                    float[] fArr3 = a;
                    float min2 = Math.min((((((float) (i2 + 1)) * 2.0f) * ((float) mMaxTextureSize)) / ((float) mImageHeight)) - 4.0f, 1.0f);
                    fArr3[16] = min2;
                    fArr3[10] = min2;
                    fArr3[c2] = min2;
                    float[] fArr4 = a;
                    int i4 = i3 + 1;
                    float min3 = Math.min((((((float) i4) * 2.0f) * ((float) mMaxTextureSize)) / ((float) mImageWidth)) - 4.0f, 1.0f) * (-mImageRatio);
                    fArr4[15] = min3;
                    fArr4[12] = min3;
                    fArr4[6] = min3;
                    float[] fArr5 = a;
                    float min4 = Math.min((((((float) i2) * 2.0f) * ((float) mMaxTextureSize)) / ((float) mImageHeight)) - 4.0f, 1.0f);
                    fArr5[13] = min4;
                    fArr5[7] = min4;
                    fArr5[4] = min4;
                    c.put(a);
                    c.position(0);
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, f1942k[(f1936e * i2) + i3]);
                    checkGlError("glBindTexture");
                    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, a.length / 3);
                    i3 = i4;
                    c2 = 1;
                }
                i2++;
                c2 = 1;
            }
            GLES20.glDisableVertexAttribArray(aPosition);
            GLES20.glDisableVertexAttribArray(aTexCoords);
        }
    }


    private static int generateTexture(Bitmap bitmap) {
        int[] iArr = new int[1];
        GLES20.glGenTextures(1, iArr, 0);
        checkGlError("glGenTextures");
        if (iArr[0] != 0) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, iArr[0]);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            checkGlError("texImage2D");
        }
        if (iArr[0] != 0) {
            return iArr[0];
        }
        Log.e("GLUtil", "Error loading texture (empty texture handle)");
        throw new RuntimeException("Error loading texture (empty texture handle).");
    }

    private static FloatBuffer makeBuffer(int len) {
        FloatBuffer buffer = ByteBuffer.allocateDirect(len * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        buffer.position(0);
        return buffer;
    }

    private static final String VertexShaderSource = "" +
            "uniform mat4 uMVPMatrix;" +
            "attribute vec4 aPosition;" +
            "attribute vec2 aTexCoords;" +
            "varying vec2 vTexCoords;" +
            "void main(){" +
            "  vTexCoords = aTexCoords;" +
            "  gl_Position = uMVPMatrix * aPosition;" +
            "}";

    private static final String FragmentShaderSource = "" +
            "precision mediump float;" +
            "uniform sampler2D uTexture;" +
            "varying vec2 vTexCoords;" +
            "void main(){" +
            "  gl_FragColor = texture2D(uTexture, vTexCoords);" +
            "}";

    public static void init() {
        int glVertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(glVertexShader, VertexShaderSource);
        GLES20.glCompileShader(glVertexShader);
        checkGlError("glCompileShader");

        int glFragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(glFragmentShader, FragmentShaderSource);
        GLES20.glCompileShader(glFragmentShader);
        checkGlError("glCompileShader");

        int program = GLES20.glCreateProgram();
        checkGlError("glCreateProgram");

        GLES20.glAttachShader(program, glVertexShader);
        GLES20.glAttachShader(program, glFragmentShader);
        GLES20.glLinkProgram(program);
        checkGlError("glLinkProgram");

        GLES20.glDeleteShader(glVertexShader);
        GLES20.glDeleteShader(glFragmentShader);
        glProgram = program;

        aPosition = GLES20.glGetAttribLocation(program, "aPosition");
        aTexCoords = GLES20.glGetAttribLocation(program, "aTexCoords");
        uMVPMatrix = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        uTexture = GLES20.glGetUniformLocation(program, "uTexture");

        int[] iArr = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, iArr, 0);
        maxTextureSize = iArr[0];
    }

    private static void checkGlError(String str) {
        int glGetError = GLES20.glGetError();
        if (glGetError != 0) {
            Log.e("GLUtil", str + ": glError " + glGetError);
            throw new RuntimeException(str + ": glError " + glGetError);
        }
    }
}
