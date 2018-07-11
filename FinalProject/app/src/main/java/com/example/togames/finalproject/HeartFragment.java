package com.example.togames.finalproject;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link HeartFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link HeartFragment#getInstance} singleton factory
 * method to get an instance of this fragment.
 */
public class HeartFragment extends Fragment implements SensorEventListener {

    private static final int STORAGE_PERMISSION_CODE = 23;
    private OnFragmentInteractionListener mListener;

    private static HeartFragment instance;

    private TextView textView_heart_rate, textView_heart_rate_text;
    private ImageView imageView_heart;
    private SensorManager mSensorManager;
    private Sensor heartSensor;
    private int heartRate;
    private Animation anim_zoomInOut;

    public HeartFragment() {
        // Required empty public constructor
    }

    // Singleton getInstance() method
    public static HeartFragment getInstance() {
        if (instance == null) instance = new HeartFragment();
        return instance;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize temperature and step counter sensors
        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        heartSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        anim_zoomInOut = AnimationUtils.loadAnimation(getContext(), R.anim.scale_up_down);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_heart, container, false);

        textView_heart_rate = view.findViewById(R.id.textView_heart_rate);
        textView_heart_rate_text = view.findViewById(R.id.textView_heart_rate_text);
        imageView_heart = view.findViewById(R.id.imageView_heart);

        if (heartSensor != null) {
            // Register heart rate sensor listener
            mSensorManager.registerListener(this, heartSensor, SensorManager.SENSOR_DELAY_NORMAL);
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.BODY_SENSORS},STORAGE_PERMISSION_CODE);
        } else {
            textView_heart_rate_text.setText(R.string.step_sensor_is_not_available);
        }

        return view;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            Log.d("heartSensor", "onSensorChanged");
            int initialHeartRate = heartRate;
            heartRate = (int) sensorEvent.values[0];
            if (heartRate == 0) {
                return;
            } else if (initialHeartRate == 0) {
                textView_heart_rate_text.setText(getString(R.string.your_heart_rate_is));
                Log.d("heartSensor", "your heart rate.");
            }
            String heartRateStr = "" + heartRate;
            textView_heart_rate.setText(heartRateStr);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        if (sensor.getType() == Sensor.TYPE_HEART_RATE) {
            Log.d("heartSensor", "onAccuracyChanged");
            if (i == SensorManager.SENSOR_STATUS_NO_CONTACT) {
                textView_heart_rate_text.setText(R.string.place_your_finger_on_the_sensor);
                imageView_heart.clearAnimation();
                anim_zoomInOut.cancel();
                anim_zoomInOut.reset();
            } else if (i == SensorManager.SENSOR_STATUS_UNRELIABLE) {
                if(heartRate == 0) {
                    textView_heart_rate_text.setText(R.string.place_your_finger_properly);
                }
                imageView_heart.clearAnimation();
                anim_zoomInOut.cancel();
                anim_zoomInOut.reset();
            } else {
                imageView_heart.startAnimation(anim_zoomInOut);
            }
        }
    }

    @Override
    public void setMenuVisibility(final boolean visible) {
        super.setMenuVisibility(visible);
        if (mSensorManager == null) return;
        if (visible) {
            // If fragment visible, register sensor listener
            mSensorManager.registerListener(this, heartSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            // If fragment not visible, unregister sensor listener
            mSensorManager.unregisterListener(this);
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onResume() {
        super.onResume();
        // If fragment is resumed and is not hidden, register sensor listener
        if (isMenuVisible()) {
            mSensorManager.registerListener(this, heartSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        // If fragment is paused, unregister sensor listener
        mSensorManager.unregisterListener(this);
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /* This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity. */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
