package com.example.glwallpaper.wallpapers.image;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * On 2021-11-22
 */
public class RotationMonitor {

    private final SensorManager mSensorManager;
    private final Sensor mSensor;

    private final LowPassSensorEventListener mListener;

    private boolean mStarted = false;
    private final int mRate;

    public RotationMonitor(Context context, int refreshRate, float lowPass, RotationChangedListener listener) {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mRate = refreshRate;
        mListener = new LowPassSensorEventListener(lowPass, listener);
    }

    public RotationMonitor(Context context, int refreshRate, RotationChangedListener listener) {
        this(context, refreshRate, LowPassSensorEventListener.ALPHA, listener);
    }

    public interface RotationChangedListener {
        void onRotationChanged(float[] angle);
    }

    public void start() {
        if (mStarted) {
            return;
        }
        mSensorManager.registerListener(mListener, mSensor, 1000000 / mRate);
        mStarted = true;
    }

    public void stop() {
        if (!mStarted) {
            return;
        }
        mSensorManager.unregisterListener(mListener);
        mStarted = false;
        mListener.onStop();
    }

    private static class LowPassSensorEventListener implements SensorEventListener {

        private static final float ALPHA = 0.25f;

        private final float mAlpha;
        private float[] mLastRotation;
        private float[] mLastValues;
        private final RotationChangedListener mAngleListener;

        private LowPassSensorEventListener(float alpha, RotationChangedListener angleListener) {
            mAlpha = alpha;
            mAngleListener = angleListener;
        }

        // 低通滤波 https://github.com/Bhide/Low-Pass-Filter-To-Android-Sensors
        private float[] lowPass(float[] input, float[] output) {
            if (output == null) {
                return input;
            }

            for (int i = 0; i < input.length; i++) {
                output[i] = output[i] + mAlpha * (input[i] - output[i]);
            }
            return output;
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (mLastValues == null) {
                mLastValues = new float[event.values.length];
            }
            mLastValues = lowPass(event.values.clone(), mLastValues);

            float[] rotation = new float[9];
            SensorManager.getRotationMatrixFromVector(rotation, mLastValues);

            if (mLastRotation == null) {
                mLastRotation = rotation;
                return;
            }
            float[] angle = new float[3];
            SensorManager.getAngleChange(angle, rotation, mLastRotation);
            if (mAngleListener != null) {
                mAngleListener.onRotationChanged(angle);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        private void onStop() {
            mLastRotation = null;
            mLastValues = null;
        }
    }
}
