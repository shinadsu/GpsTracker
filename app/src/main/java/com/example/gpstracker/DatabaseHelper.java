package com.example.gpstracker;

import android.os.AsyncTask;
import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseHelper {
    private static final String URL = "jdbc:jtds:sqlserver://192.168.1.51:1433/LocationTracker;user=sa;password=mitsuru12345;";
    private Connection connection;


    public void connect(final ConnectionCallback callback) {
        new ConnectTask(callback).execute();
    }

    private class ConnectTask extends AsyncTask<Void, Void, Boolean> {
        private ConnectionCallback callback;

        public ConnectTask(ConnectionCallback callback) {
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                // Загрузка драйвера
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                connection = DriverManager.getConnection(URL);
                Log.d("DatabaseHelper", "Соединение установлено");
                return true; // Успех
            } catch (ClassNotFoundException e) {
                Log.e("DatabaseHelper", "Не удалось загрузить драйвер: " + e.getMessage());
                return false; // Неудача
            } catch (SQLException e) {
                Log.e("DatabaseHelper", "Ошибка подключения к базе данных: " + e.getMessage());
                return false; // Неудача
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (callback != null) {
                callback.onConnectionResult(result);
            }
        }
    }

    public void insertOrUpdateLocation(String uniqueId, double latitude, double longitude, InsertUpdateCallback callback) {
        new InsertUpdateTask(uniqueId, latitude, longitude, callback).execute();
    }

    private class InsertUpdateTask extends AsyncTask<Void, Void, Boolean> {
        private String uniqueId;
        private double latitude;
        private double longitude;
        private InsertUpdateCallback callback;

        public InsertUpdateTask(String uniqueId, double latitude, double longitude, InsertUpdateCallback callback) {
            this.uniqueId = uniqueId;
            this.latitude = latitude;
            this.longitude = longitude;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            if (connection == null) {
                Log.e("DatabaseHelper", "Соединение не установлено. Пожалуйста, подключитесь к базе данных.");
                return false;
            }

            String sql = "IF EXISTS (SELECT * FROM DeviceLocations WHERE DeviceId = ?) " +
                    "UPDATE DeviceLocations SET Latitude = ?, Longitude = ? WHERE DeviceId = ? " +
                    "ELSE " +
                    "INSERT INTO DeviceLocations (DeviceId, Latitude, Longitude) VALUES (?, ?, ?)";

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, uniqueId);
                statement.setDouble(2, latitude);
                statement.setDouble(3, longitude);
                statement.setString(4, uniqueId);
                statement.setString(5, uniqueId);
                statement.setDouble(6, latitude);
                statement.setDouble(7, longitude);

                statement.executeUpdate();
                return true; // Успех
            } catch (SQLException e) {
                Log.e("DatabaseHelper", "Ошибка выполнения запроса: " + e.getMessage());
                return false; // Неудача
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (callback != null) {
                callback.onInsertUpdateResult(result);
            }
        }
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
                Log.d("DatabaseHelper", "Соединение закрыто");
            } catch (SQLException e) {
                Log.e("DatabaseHelper", "Ошибка закрытия соединения: " + e.getMessage());
            } finally {
                connection = null; // Обнуляем ссылку на соединение
            }
        }
    }

    public interface ConnectionCallback {
        void onConnectionResult(boolean success);
    }

    public interface InsertUpdateCallback {
        void onInsertUpdateResult(boolean success);
    }
}
