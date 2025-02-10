package com.example.callvoicerecorder;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Show a Toast message or provide basic instructions
        Toast.makeText(this, "App is ready to record calls and voice messages.", Toast.LENGTH_LONG).show();

        // Start services or check permissions here
        startService(new Intent(this, CallReceiver.class));

        // Optionally start VoiceRecorder to monitor mic
        VoiceRecorder voiceRecorder = new VoiceRecorder();
        voiceRecorder.startRecording(getApplicationContext());
    }
}
