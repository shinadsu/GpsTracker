package com.example.gpstracker;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String PREFS_NAME = "MyPrefs";
    private String deviceId;

    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Получаем сохраненный идентификатор из SharedPreferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        deviceId = prefs.getString("DeviceId", null);

        // Если идентификатор не был сохранен, генерируем новый
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString();
            prefs.edit().putString("DeviceId", deviceId).apply();
        }

        databaseHelper = new DatabaseHelper(); // Инициализация DatabaseHelper

        // Подключаемся к базе данных
        databaseHelper.connect(success -> {
            if (success) {
                startLocationService(); // Запускаем сервис для отслеживания местоположения
            } else {
                Toast.makeText(MainActivity.this, "Ошибка подключения к базе данных", Toast.LENGTH_LONG).show();
            }
        });

        // Проверяем разрешение на доступ к местоположению
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Запрашиваем разрешение, если его нет
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            startLocationService(); // Запускаем сервис, если разрешение уже предоставлено
        }
    }

    private void startLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        startService(serviceIntent); // Запускаем фоновый сервис для отслеживания местоположения
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationService(); // Начинаем сервис отслеживания местоположения
            } else {
                Toast.makeText(this, "Разрешение на доступ к местоположению не предоставлено", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Здесь можно остановить фоновый сервис, если необходимо
        // stopService(new Intent(this, LocationService.class));
        databaseHelper.close(); // Закрываем соединение с базой данных
    }
}
