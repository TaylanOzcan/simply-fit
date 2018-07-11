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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link StepsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link StepsFragment#getInstance} singleton factory
 * method to get an instance of this fragment.
 */
public class StepsFragment extends Fragment implements SensorEventListener, View.OnClickListener {

    private OnFragmentInteractionListener mListener;

    private static StepsFragment instance;

    private String temp;
    private TextView textView_stepCount, textView_temperature, textView_stepPercent,
            textView_temperatureText, textView_paceDecrease;
    private ImageView imageView_stepsPercent;
    private ProgressBar progressBar_steps;
    private SensorManager mSensorManager;
    private Sensor tempSensor, stepSensor;
    private int stepCount, stepProgress, stepGoal = 0;
    private boolean viewCreated = false;

    public StepsFragment() {
        // Required empty public constructor
    }

    // Singleton getInstance() method
    public static StepsFragment getInstance() {
        if (instance == null) instance = new StepsFragment();
        return instance;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize temperature and step counter sensors
        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        tempSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        stepSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_steps, container, false);
        textView_temperature = view.findViewById(R.id.textView_temperature);
        textView_temperatureText = view.findViewById(R.id.textView_temperatureText);
        textView_paceDecrease = view.findViewById(R.id.textView_paceDecrease);
        textView_stepPercent = view.findViewById(R.id.textView_stepsPercent);
        textView_stepCount = view.findViewById(R.id.textView_stepCount);
        progressBar_steps = view.findViewById(R.id.progressBar_steps);
        imageView_stepsPercent = view.findViewById((R.id.imageView_stepsPercent));
        stepProgress = 0;

        if (stepSensor != null) {
            // Register step counter sensor listener
            mSensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
        } else {
            textView_stepCount.setText(R.string.step_sensor_is_not_available);
        }
        if (tempSensor != null) {
            // Register the sensor listener
            mSensorManager.registerListener(this, tempSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            textView_temperatureText.setText("");//R.string.temperature_sensor_is_not_available);
        }
        viewCreated = true;
        return view;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            stepCount = (int) sensorEvent.values[0];
            if(stepGoal == 0) return;
            int progress = 100 * stepCount / stepGoal;
            textView_stepCount.setText(Integer.toString(stepCount));
            if (progress != stepProgress) {
                stepProgress = progress;
                String progress_str = "" + progress; // + "%";
                textView_stepPercent.setText(progress_str);
                progressBar_steps.setProgress(progress);
            }
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            // Get temperature value from temperature sensor
            // and show it to the user
            float ambient_temperature = sensorEvent.values[0];
            temp = String.format(Locale.getDefault(), "%.1f", ambient_temperature);
            textView_temperature.setText(temp);

            // If fragment is not added to the activity yet, return
            if (!isAdded()) return;

            // Show the appropriate pace reduction info,
            // according to the temperature value
            String paceText = getString(R.string.your_pace_will_reduce_by);
            if (ambient_temperature >= 12) {
                if (ambient_temperature < 15) {
                    paceText += " 1%";
                } else if (ambient_temperature < 18) {
                    paceText += " 3%";
                } else if (ambient_temperature < 21) {
                    paceText += " 5%";
                } else if (ambient_temperature < 24) {
                    paceText += " 7%";
                } else if (ambient_temperature < 27) {
                    paceText += " 12%";
                } else if (ambient_temperature < 30) {
                    paceText += " 20%";
                } else {
                    paceText = getString(R.string.you_should_avoid_physical_activities);
                }
            } else {
                paceText = "";
            }
            textView_paceDecrease.setText(paceText);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    /*
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            // If fragment not visible, unregister sensor listener
            mSensorManager.unregisterListener(this);
        } else {
            // If fragment visible, register sensor listener
            mSensorManager.registerListener(this, tempSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }
    */

    @Override
    public void setMenuVisibility(final boolean visible) {
        super.setMenuVisibility(visible);
        if (mSensorManager == null) return;
        if (visible) {
            // If fragment visible, register sensor listener
            mSensorManager.registerListener(this, tempSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            // If fragment not visible, unregister sensor listener
            mSensorManager.unregisterListener(this, tempSensor);
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onResume() {
        super.onResume();
        // If fragment is resumed and is not hidden, register sensor listener
        if (isMenuVisible()) {
            mSensorManager.registerListener(this, tempSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // If fragment is paused, unregister sensor listener
        mSensorManager.unregisterListener(this, tempSensor);
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

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.imageView_stepsPercent:
                AlertDialog.Builder menu_setGoal = new AlertDialog.Builder(getContext());
                menu_setGoal.setTitle(R.string.set_new_values);
                View dialogView = getLayoutInflater().inflate(R.layout.popup_steps, null);
                menu_setGoal.setView(dialogView);
                final EditText editText_setGoal = dialogView.findViewById(R.id.editText_setGoal);
                editText_setGoal.setHint("" + stepGoal);

                // "OK" button sets the new values for weight and height
                menu_setGoal.setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        stepGoal = Integer.parseInt(editText_setGoal.getText().toString());
                        mListener.onFragmentInteraction(stepGoal);
                        updateGUI();
                    }
                });

                // "Cancel" button dismisses the alert
                menu_setGoal.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });
                menu_setGoal.setCancelable(false);

                AlertDialog dialog = menu_setGoal.create();
                dialog.show();
                dialog.getWindow().setBackgroundDrawableResource(
                        AppSettings.getInstance(getContext()).isDarkTheme ?
                                R.color.dark_colorMenuBackground :
                                R.color.colorMenuBackground);
                break;
            default:
                break;
        }
    }

    private void updateGUI() {
        if (stepGoal == 0) return;

        int progress = 100 * stepCount / stepGoal;
        textView_stepCount.setText(Integer.toString(stepCount));
        if (progress != stepProgress) {
            stepProgress = progress;
            String progress_str = "" + progress; // + "%";
            textView_stepPercent.setText(progress_str);
            progressBar_steps.setProgress(progress);
        }
    }

    public void setData(int stepGoal) {
        this.stepGoal = stepGoal;
        if (viewCreated) {
            imageView_stepsPercent.setOnClickListener(this);
            updateGUI();
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewCreated = false;
    }

    /* This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity. */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(int stepGoal);
    }
}
