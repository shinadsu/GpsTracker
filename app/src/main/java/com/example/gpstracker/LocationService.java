package com.example.gpstracker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.Manifest;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class LocationService extends Service {
    private static final String CHANNEL_ID = "LocationServiceChannel";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private DatabaseHelper databaseHelper;
    private boolean isFinishing = false;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        databaseHelper = new DatabaseHelper();

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);  // 10 секунд
        locationRequest.setFastestInterval(5000);  // 5 секунд
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    String deviceId = getSharedPreferences("MyPrefs", MODE_PRIVATE).getString("DeviceId", null);
                    databaseHelper.insertOrUpdateLocation(deviceId, latitude, longitude, success -> {
                        if (success) {
                            Log.d("LocationService", "Данные успешно отправлены: " + latitude + ", " + longitude);
                        } else {
                            Log.e("LocationService", "Ошибка при отправке данных");
                        }
                    });
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Notification notification = createNotification();
        startForeground(1, notification);

        databaseHelper.connect(success -> {
            if (success) {
                startLocationUpdates();
            } else {
                Log.e("LocationService", "Ошибка подключения к базе данных");
            }
        });

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isFinishing) {
            databaseHelper.close(); // Закрываем соединение только при завершении приложения
        }
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        isFinishing = true;
        super.onTaskRemoved(rootIntent);
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Сбор данных местоположения")
                .setContentText("Приложение собирает данные о вашем местоположении.")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        } else {
            Log.e("LocationService", "Нет разрешения на доступ к местоположению");
            // Обработка ситуации, когда разрешение не предоставлено
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
