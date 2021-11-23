package com.example.glwallpaper.gl;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.glGetError;

/**
 * On 2021-11-20
 */
public class GLUtil {

    private static final String GL_LOG_TAG = "GLUtil";

    public static void checkGlError() {
        int error = glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            Log.w(GL_LOG_TAG, "GL error = 0x" + Integer.toHexString(error), new Throwable());
        }
    }

    public static int loadTexture(Bitmap bitmap) {
        int[] textures = new int[1];

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glGenTextures(1, textures, 0);
        checkGlError();

        int texture = textures[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        checkGlError();

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, bitmap, GLES20.GL_UNSIGNED_BYTE, 0);
        checkGlError();

        return texture;
    }

    public static int buildProgram(String vertex, String fragment) {
        int vertexShader = buildShader(vertex, GLES20.GL_VERTEX_SHADER);
        if (vertexShader == 0) return 0;

        int fragmentShader = buildShader(fragment, GLES20.GL_FRAGMENT_SHADER);
        if (fragmentShader == 0) return 0;

        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
        checkGlError();

        GLES20.glDeleteShader(vertexShader);
        GLES20.glDeleteShader(fragmentShader);

        int[] status = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] != GLES20.GL_TRUE) {
            String error = GLES20.glGetProgramInfoLog(program);
            Log.d(GL_LOG_TAG, "Error while linking program:\n" + error);
            GLES20.glDeleteProgram(program);
            return 0;
        }

        return program;
    }

    public static int buildShader(String source, int type) {
        int shader = GLES20.glCreateShader(type);

        GLES20.glShaderSource(shader, source);
        checkGlError();

        GLES20.glCompileShader(shader);
        checkGlError();

        int[] status = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] != GLES20.GL_TRUE) {
            String error = GLES20.glGetShaderInfoLog(shader);
            Log.d(GL_LOG_TAG, "Error while compiling shader:\n" + error);
            GLES20.glDeleteShader(shader);
            return 0;
        }

        return shader;
    }

    public static FloatBuffer makeFloatBuffer(int len) {
        FloatBuffer buffer = ByteBuffer.allocateDirect(len * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buffer.position(0);
        return buffer;
    }

    public static FloatBuffer makeFloatBuffer(float[] arr) {
        FloatBuffer buffer = ByteBuffer.allocateDirect(arr.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buffer.put(arr);
        buffer.position(0);
        return buffer;
    }

    public static final int FLOAT_SIZE_BYTES = 4;

}
