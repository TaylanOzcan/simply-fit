package com.example.togames.finalproject;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

public class RecordActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageButton buttonRecord, buttonStopRecord, buttonPlay, buttonStop, buttonAudio,
            imageButton_record_back;
    private String savePath = "";

    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;

    final int REQUEST_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme((AppSettings.getInstance(this).isDarkTheme) ?
                R.style.NoTitleThemeDark : R.style.NoTitleTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        buttonRecord = findViewById(R.id.buttonRecord);
        buttonStopRecord = findViewById(R.id.buttonStopRecord);
        buttonPlay = findViewById(R.id.buttonPlay);
        buttonStop = findViewById(R.id.buttonStop);
        buttonAudio = findViewById(R.id.buttonAudio);
        imageButton_record_back = findViewById(R.id.imageButton_record_back);

        imageButton_record_back.setOnClickListener(this);

        if (checkPermissionFromDevice()) {
            buttonRecord.setOnClickListener(this);
            buttonStopRecord.setOnClickListener(this);
            buttonPlay.setOnClickListener(this);
            buttonStop.setOnClickListener(this);
            buttonAudio.setOnClickListener(this);
        } else {
            requestPermission();
        }
    }

    private void setupMediaRecorder() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        mediaRecorder.setOutputFile(savePath);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                }
            }
            break;
        }
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO}, REQUEST_PERMISSION_CODE);
    }

    private boolean checkPermissionFromDevice() {
        int write_external_storage_result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int record_audio_result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO);
        return write_external_storage_result == PackageManager.PERMISSION_GRANTED &&
                record_audio_result == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.buttonRecord:
                buttonPlay.setEnabled(false);
                buttonStop.setEnabled(false);
                buttonRecord.setEnabled(false);
                buttonStopRecord.setEnabled(true);
                savePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" +
                        UUID.randomUUID().toString() + "_audio_record_3gp";
                setupMediaRecorder();
                try {
                    mediaRecorder.prepare();
                    mediaRecorder.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Toast.makeText(RecordActivity.this, "Recording started",
                        Toast.LENGTH_SHORT).show();
                break;
            case R.id.buttonStopRecord:
                buttonStopRecord.setEnabled(false);
                buttonPlay.setEnabled(true);
                buttonRecord.setEnabled(true);
                buttonStop.setEnabled(false);
                mediaRecorder.stop();
                Toast.makeText(RecordActivity.this, "Recording ended",
                        Toast.LENGTH_SHORT).show();
                break;
            case R.id.buttonPlay:
                buttonRecord.setEnabled(false);
                buttonStopRecord.setEnabled(false);
                buttonStop.setEnabled(true);
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        buttonRecord.setEnabled(true);
                        buttonStop.setEnabled(false);
                        Toast.makeText(RecordActivity.this,
                                "Playback ended",Toast.LENGTH_SHORT).show();
                    }
                });
                try {
                    mediaPlayer.setDataSource(savePath);
                    mediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mediaPlayer.start();
                Toast.makeText(RecordActivity.this, "Playing",
                        Toast.LENGTH_SHORT).show();
                break;
            case R.id.buttonStop:
                buttonRecord.setEnabled(true);
                buttonStopRecord.setEnabled(false);
                buttonStop.setEnabled(false);
                buttonPlay.setEnabled(true);
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    setupMediaRecorder();
                }
                Toast.makeText(RecordActivity.this, "Stopped",
                        Toast.LENGTH_SHORT).show();
                break;
            case R.id.buttonAudio:
                String voiceNotePath = Environment.getExternalStorageDirectory().getPath() +
                        "/Voice Recorder/Voice01.m4a";
                MediaPlayer mp_intro = MediaPlayer.create(RecordActivity.this,
                        Uri.parse(voiceNotePath));
                mp_intro.start();
                break;
            case R.id.imageButton_record_back:
                finish();
                break;
            default:
                break;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}