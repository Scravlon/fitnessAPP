package com.scravlon.fitnessapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * UserActivity handles every detail of the login user. It contains the distance walked and achievement.
 * User can update the office location by clicking UPDATE OFFICE in the menu which will update the current location to the office location.
 * Assume that one step of the step is about 2.4 feet of the distance.
 */
public class UserActivity extends AppCompatActivity {

    private static final long LOCATION_UPDATE_DELAY = 240000; // Location update interval, 4 min
    private static final long HOUR_UPDATE_DELAY = 3600000; // Office stand up reminder interval, 1 hour
    ArrayList<User> allUser;
    User runningUser;
    String CHANNEL_ID = "stand_id";
    int loginUser;
    int NOTIFICATION_ID;
    long step = 0;
    boolean inOffice = false;
    boolean timer_running = false;
    FusedLocationProviderClient fusedLocationClient;
    SensorManager sManager;
    SensorEventListener sensorEventListener;
    Location currentLocation;
    /*UI variables*/
    Handler hour_handler;
    Handler handler;
    Runnable hour_run;
    Runnable location_run;
    TextView tv_distance;
    TextView text_user;
    TextView text_left;
    TextView text_achievement;
    ProgressBar progress_walk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        createNotificationChannel();
        text_user = findViewById(R.id.text_username);
        tv_distance = findViewById(R.id.text_distance);
        text_left = findViewById(R.id.text_distance_left);
        text_achievement = findViewById(R.id.text_milestone);
        progress_walk = findViewById(R.id.progressBar);
        if (!getUserList()) {
            allUser = new ArrayList<>();
            runningUser = null;
        } else {
            runningUser = allUser.get(loginUser);
        }
        runningUser.checkDate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getCurrentLocation();
        sManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(android.hardware.SensorEvent sensorEvent) {
                step++;
                //Update the data every 2 steps
                if (step % 2 == 0) {
                    updateStep();
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        sManager.registerListener(sensorEventListener, sManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER),
                SensorManager.SENSOR_DELAY_NORMAL);
        Intent intent = getIntent();
        loginUser = intent.getIntExtra(MainActivity.EXTRA, -1);
        runningUser = allUser.get(loginUser);
        updateUI();
        text_user.setText(runningUser.username);
        init_handler_runnable();
        /*Run a location check every 4 minutes to check if the user is in his/her office*/
        if (runningUser.officeLocation != null) {
            handler.postDelayed(location_run, 1000);
        }
    }

    /**
     *  Initiate all handler and runnable. Call once onCreate.
     */
    private void init_handler_runnable() {
        hour_handler = new Handler();
        handler = new Handler();
        hour_run = new Runnable() {
            @Override
            public void run() {
                stand_reminder("Stand up and walk! It's been an hour");
                hour_handler.postDelayed(this, HOUR_UPDATE_DELAY);
            }
        };
        location_run = new Runnable() {
            @Override
            public void run() {
                getCurrentLocation();
                if (currentLocation != null && runningUser.officeLocation.distanceTo(currentLocation) <= 100) {
                    inOffice = true;
                    run_notification();
                } else {
                    inOffice = false;
                }
                handler.postDelayed(this, LOCATION_UPDATE_DELAY);
            }
        };
    }

    /**
     * Run the notification timer that reminds user to stand up every hour.
     */
    private void run_notification() {
        if (!inOffice && timer_running) {
            timer_running = false;
            hour_handler.removeCallbacks(hour_run);
        } else if (inOffice && !timer_running) {
            timer_running = true;
            hour_handler.postDelayed(hour_run, HOUR_UPDATE_DELAY);
        }
    }

    /**
     * Create a NotificationChannel if the Android API is 26+
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Create a notification to remind user to stand up and walk.
     */
    private void stand_reminder(String notiDes) {
        NOTIFICATION_ID = 1;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("fitnessApp")
                .setContentText(notiDes)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    /**
     * Update the distance walked from the user and the statistics on the screen
     */
    private void updateStep() {
        runningUser.getDistanceWalk(step);
        step = 0;
        //allUser.set(loginUser,runningUser);
        if (runningUser.goalReach()) {
            runningUser.updateGoal();
            stand_reminder("Congratulation on reaching: " + runningUser.walkGoal + " feet!");
        }
        updateUI();
        updateUserList();
    }

    /**
     * Update the user statistics UI on the achievement, current walked distance and distance till achievement
     */
    private void updateUI() {
        tv_distance.setText(String.valueOf(Math.floor(runningUser.walkDistance * 100) / 100) + " feet");
        float mileStone = runningUser.walkGoal;
        float goalLeft = runningUser.walkGoal - runningUser.walkDistance;
        text_left.setText("- " + String.valueOf(Math.floor(goalLeft * 100) / 100) + " feet");
        text_achievement.setText(String.valueOf(mileStone) + " feet");
        progress_walk.setProgress((int) (Math.ceil(((1000 - goalLeft) / 1000) * 100)));
    }


    /**
     * Retrieve data of last user login and user list from local storage
     * @return true if data is found, false if not
     */
    private boolean getUserList() {
        SharedPreferences sharedPreferences = getSharedPreferences("runningUser", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString("user_list", null);
        loginUser = sharedPreferences.getInt("currentUser", -1);
        Type type = new TypeToken<ArrayList<User>>() {
        }.getType();
        allUser = gson.fromJson(json, type);
        if (allUser == null) {
            return false;
        }
        return true;
    }

    /**
     * Update the allUser arraylist to the storage
     */
    private void updateUserList() {
        SharedPreferences sharedPreferences = getSharedPreferences("runningUser", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(allUser);
        editor.putString("user_list", json);
        editor.putInt("currentUser", loginUser);
        editor.apply();
    }

    /**
     * Set the current location to the Location currentLocation,
     * function will be called every 4 minutes (240000 millisecond)
     */
    private void getCurrentLocation() {
        // Get the last known location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        currentLocation = task.getResult();
                    }
                });
    }

    /**
     * Set user office location to current location
     */
    private void setCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        // Get the last known location
        fusedLocationClient.getLastLocation()
                .addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        runningUser.updateLocation(task.getResult());
                        inOffice = true;
                        //allUser.set(loginUser,runningUser);
                        updateUserList();
                    }
                });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_user, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_office_location) {
            setCurrentLocation();
            handler.removeCallbacks(location_run);
            handler.postDelayed(location_run, LOCATION_UPDATE_DELAY);
            Toast.makeText(this, "Office location updated to current location!", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sManager.unregisterListener(sensorEventListener);
        updateUserList();
        handler.removeCallbacks(location_run);
        hour_handler.removeCallbacks(hour_run);
    }
}
