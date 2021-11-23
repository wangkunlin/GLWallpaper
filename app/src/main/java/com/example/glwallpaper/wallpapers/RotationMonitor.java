package com.example.glwallpaper.wallpapers;

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
    private float[] mLastRotation;

    private final SensorEventListener mListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float[] rotation = new float[9];
            SensorManager.getRotationMatrixFromVector(rotation, event.values);
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
    };

    private boolean mStarted = false;
    private final RotationChangedListener mAngleListener;
    private final int mRate;

    public RotationMonitor(Context context, int refreshRate, RotationChangedListener listener) {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mAngleListener = listener;
        mRate = refreshRate;
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
        mLastRotation = null;
    }
}
