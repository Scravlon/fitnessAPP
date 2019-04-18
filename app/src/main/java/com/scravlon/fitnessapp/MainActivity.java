package com.scravlon.fitnessapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Main activity contains user login/register and leaderboard.
 */
public class MainActivity extends AppCompatActivity {

    public static final String EXTRA = "com.scravlon.fitnessapp.LOGINUSER";
    ArrayList<User> allUser; //All users stored
    DBStorage dbStorage; //Database of users
    User runningUser; //The login user
    int loginUser; //Assume that last login user is using the device
    static final int MY_PERMISSIONS_REQUEST_COARSE_LOCATION = 99;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        requestLocationService();
        dbStorage = new DBStorage(this);
        if (!getUserList()) {
            allUser = new ArrayList<>();
            runningUser = null;
        }
        if(allUser ==null){
            allUser = new ArrayList<>();
        }
        updateUserList();
        Button b_login = findViewById(R.id.but_login);
        Button b_register = findViewById(R.id.but_register);
        ListView listView = findViewById(R.id.list_leader);
        //SORT
        if(allUser.size() != 0){
            Collections.sort(allUser,Collections.<User>reverseOrder());
            leaderAdapter adapter = new leaderAdapter(this,allUser);
            listView.setAdapter(adapter);
        } else{
            TextView tv = findViewById(R.id.text_leader);
            tv.setText("Leaderboard is empty");
        }
        getUserList();
        /*Onclick, login check if user is register and the password matching */
        b_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText et_user = findViewById(R.id.input_user);
                EditText et_password = findViewById(R.id.input_password);
                String user = et_user.getText().toString();
                String pass = et_password.getText().toString();
                if (user.equals("") && pass.equals("")) {
                    Snackbar.make(view, "Username and Password cannot be empty", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                } else if (pass.equals("")) {
                    et_password.setError("Password cannot be empty");
                } else if (user.equals("")) {
                    et_user.setError("Username cannot be empty");
                } else {
                    login(user, pass);
                }
            }
        });
        /*Onclick, register register user if username is not in the database*/
        b_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText et_user = findViewById(R.id.input_user);
                EditText et_password = findViewById(R.id.input_password);
                String user = et_user.getText().toString();
                String pass = et_password.getText().toString();
                if (user.equals("") && pass.equals("")) {
                    Snackbar.make(view, "Username and Password cannot be empty", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                } else if (pass.equals("")) {
                    et_password.setError("Password cannot be empty");
                } else if (user.equals("")) {
                    et_user.setError("Username cannot be empty");
                } else {
                    registerUser(user, pass);
                }
            }
        });

    }

    /**
     * Request the location service from the user, required Android 6.0 and higher
     *
     * Reference: https://developer.android.com/training/permissions/requesting
     */
    private void requestLocationService() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_COARSE_LOCATION);
            }
        } else {
        }

    }

    /**
     * Handle the Location request
     * @param requestCode: Code of request
     * @param permissions: permission
     * @param grantResults: result
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_COARSE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                }
                return;
            }
        }
    }


    /**
     * Retrieve data of last user login and user list from local storage
     * @return true if data is found, false if not
     */
    public boolean getUserList(){
        SharedPreferences sharedPreferences = getSharedPreferences("runningUser", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString("user_list", null);
        loginUser = sharedPreferences.getInt("currentUser", -1);
        if(json == null){return false;}
        Type type = new TypeToken<ArrayList<User>>() {}.getType();
        allUser = gson.fromJson(json,type);
        if (allUser == null){
            return false;
        }
        return true;
    }

    /**
     * Update the allUser arraylist to the storage
     */
    public void updateUserList(){
            SharedPreferences sharedPreferences = getSharedPreferences("runningUser", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            Gson gson = new Gson();
            String json = gson.toJson(allUser);
            editor.putString("user_list", json);
            editor.putInt("currentUser",loginUser);
            editor.apply();
    }

    /**
     * Register new user and check if duplicate username in the database
     * @param username: username of the user
     * @param password: password of the user
     */
    public void registerUser(String username, String password){
        if(dbStorage.checkUserDB(username,hashSHA(password))){
            Toast toast = Toast.makeText(getApplicationContext(), "Username was taken!", Toast.LENGTH_SHORT);
            toast.show();
        } else{
            dbStorage.addContact(username,hashSHA(password));
            User newuser = new User(username);
            allUser.add(newuser);
            String[] ref = dbStorage.strArray(username);
            loginUser = Integer.parseInt(ref[1]);
            updateUserList();
            changeActivity();
            Toast toast = Toast.makeText(getApplicationContext(), "Welcome!", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    /**
     * Login current user and update to User account if the credential matches in the database.
     * @param username: username of user
     * @param password: password of user
     */
    public void login(String username, String password){
        if(dbStorage.checkUser(username,hashSHA(password))){
            String[] ref = dbStorage.strArray(username);
            loginUser = Integer.parseInt(ref[1]);
            updateUserList();
            changeActivity();
        } else{
            Toast toast = Toast.makeText(getApplicationContext(), "User credential is wrong!", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    /**
     * Update the activity of the app to the User statistics and start recording the user walk distance
     */
    public void changeActivity(){
        Intent intent = new Intent(this,UserActivity.class);
        intent.putExtra(EXTRA,loginUser);
        startActivity(intent);
        finish();
    }

    /**
     * Hash password with SHA-256
     * @param input: User entered password
     * @return: the value hash
     */
    public static String hashSHA(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger bi = new BigInteger(1, messageDigest);
            String hashtext = bi.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        }
        // For specifying wrong message digest algorithms
        catch (NoSuchAlgorithmException e) {
            return "";
        }
    }
}
