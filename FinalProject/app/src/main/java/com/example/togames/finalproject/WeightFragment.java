package com.example.togames.finalproject;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link WeightFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link WeightFragment#getInstance} singleton factory
 * method to get an instance of this fragment.
 */
public class WeightFragment extends Fragment implements SensorEventListener {

    private OnFragmentInteractionListener mListener;

    private static WeightFragment instance;

    private String weight = "";
    private String height = "";
    private boolean viewCreated = false;
    private TextView textView_height, textView_weight, textView_bmi, textView_result;

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private float accLast, accCurrent, shake;
    private boolean popupOpened;

    public WeightFragment() {
        // Required empty public constructor
    }

    // Singleton getInstance() method
    public static WeightFragment getInstance() {
        if (instance == null) instance = new WeightFragment();
        return instance;
    }

    /*
    public static WeightFragment getInstance(String weight, String height) {
        WeightFragment instance = new WeightFragment();
        Bundle args = new Bundle();
        args.putString("weight", weight);
        args.putString("height", height);
        Log.d("rottest", "getInstance " + "weight: " + weight + " height: " + height);
        instance.setArguments(args);
        return instance;
    }*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //weight = getArguments().getString("weight");
        //height = getArguments().getString("height");

        popupOpened = false;
        // Initialize accelerometer-sensor related variables
        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        // Register the sensor listener
        //mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_weight, container, false);

        textView_bmi = view.findViewById(R.id.textView_bmi);
        textView_height = view.findViewById(R.id.textView_height);
        textView_weight = view.findViewById(R.id.textView_weight);
        textView_result = view.findViewById(R.id.textView_result);
        viewCreated = true;
        Log.d("WeightFragmentProblem", "onCreateView = Weight: " + weight + ", Height: " + height);
        updateGUI();
        return view;
    }

    public synchronized void setData(String weight, String height) {
        // Set new values and update GUI
        this.weight = weight;
        this.height = height;
        if(viewCreated) updateGUI();
        Log.d("WeightFragmentProblem", "setData = Weight: " + weight + ", Height: " + height);
    }

    private synchronized void updateGUI() {
        if (weight.isEmpty() || height.isEmpty()) return;

        Log.d("WeightFragmentProblem", "updateGUI = Weight: " + weight + ", Height: " + height);

        textView_weight.setText(weight);
        textView_height.setText(height);

        // Calculate bmi value using weight and height variables
        int w = Integer.parseInt(weight);
        int h = Integer.parseInt(height);
        float bmi = (float) w / ((float) h * h / 10000);
        textView_bmi.setText(String.format(Locale.getDefault(), "%.2f", bmi));

        // Adjust the result text according to the bmi value
        if (bmi < 18.5) {
            textView_result.setText(R.string.you_are_thin);
        } else if (bmi < 25) {
            textView_result.setText(R.string.you_are_healthy);
        } else if (bmi < 30) {
            textView_result.setText(R.string.you_are_overweight);
        } else {
            textView_result.setText(R.string.you_are_obese);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d("WeightFragment", "onAttach");
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
        Log.d("WeightFragment", "onDetach");
        mListener = null;
    }

    /* This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity. */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(String weight, String height);
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        // Detect shake motion using accelerometer sensor values,
        // and show an alert dialog to change weight and height
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        accLast = accCurrent;
        accCurrent = (float) Math.sqrt((double) x * x + y * y + z * z);
        float delta = accCurrent - accLast;
        shake = shake * 0.9f + delta;

        // If shake detected and there is no current popup already opened
        if (isAdded() && shake > 12 && !popupOpened) {
            popupOpened = true;
            AlertDialog.Builder menu_setWeight = new AlertDialog.Builder(getContext());
            menu_setWeight.setTitle(R.string.set_new_values);
            View dialogView = getLayoutInflater().inflate(R.layout.popup_weight, null);
            menu_setWeight.setView(dialogView);
            final EditText editText_setWeight = dialogView.findViewById(R.id.editText_setWeight);
            final EditText editText_setHeight = dialogView.findViewById(R.id.editText_setHeight);
            editText_setWeight.setHint(weight);
            editText_setHeight.setHint(height);

            // "OK" button sets the new values for weight and height
            menu_setWeight.setPositiveButton("OK", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int whichButton) {
                    popupOpened = false;
                    weight = editText_setWeight.getText().toString();
                    height = editText_setHeight.getText().toString();
                    mListener.onFragmentInteraction(weight, height);
                    updateGUI();
                }
            });

            // "Cancel" button dismisses the alert
            menu_setWeight.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    popupOpened = false;
                }
            });
            menu_setWeight.setCancelable(false);

            AlertDialog dialog = menu_setWeight.create();
            dialog.show();
            dialog.getWindow().setBackgroundDrawableResource(
                    AppSettings.getInstance(getContext()).isDarkTheme ?
                            R.color.dark_colorMenuBackground :
                            R.color.colorMenuBackground);
            //menu_setWeight.show();
        }
    }

    /*
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        Log.d("WeightFragment", "onHiddenChanged");
        if (hidden) {
            // If fragment not visible, unregister sensor listener
            mSensorManager.unregisterListener(this);
        } else {
            // If fragment visible, register sensor listener
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }
    */

    @SuppressLint("RestrictedApi")
    @Override
    public void onResume() {
        super.onResume();
        Log.d("WeightFragment", "onResume");
        // If fragment is resumed and is not hidden, register sensor listener
        if (isMenuVisible()) {
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("WeightFragment", "onPause");
        // If fragment is paused, unregister sensor listener
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewCreated = false;
    }

    @Override
    public void setMenuVisibility(final boolean visible) {
        super.setMenuVisibility(visible);
        Log.d("WeightFragment", "setMenuVisibility: " + visible);
        if(mSensorManager == null) return;
        if (visible) {
            // If fragment visible, register sensor listener
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            // If fragment not visible, unregister sensor listener
            mSensorManager.unregisterListener(this);
        }
    }

}
