package com.scravlon.fitnessapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;

import java.util.HashMap;

public class DBStorage extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "userData.db";
    public static final String USERDATA_TABLE_NAME = "users";
    public static final String USER_ID = "id";
    public static final String USER_NAME = "name";
    public static final String USER_PASSWORD = "password";


    public DBStorage(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }


    public DBStorage(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE "
                + USERDATA_TABLE_NAME + " (" +
                USER_ID + " INTEGER PRIMARY KEY, " +
                USER_NAME + " TEXT NOT NULL, " +
                USER_PASSWORD + " TEXT NOT NULL)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {}

    /**
     * Insert a new User
     * @param name: username of the user
     * @param password: Password of user after encryption
     * @return: boolean of the status of adding
     */
    public boolean insertUser (String name, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(USER_ID, getCount());
        contentValues.put(USER_NAME, name);
        contentValues.put(USER_PASSWORD, password);
        db.insert("contacts", null, contentValues);
        return true;
    }

    /**
     * Check if the user
     * @param username
     * @param password
     * @return -2 if user not found, -1 if user Not found else user id
     */
    public int queryPassword(String username, String password){
        Cursor c = getData(username);
        if(c == null){
            return -2;
        }
        c.moveToFirst();
        String passwordRef = c.getString(c.getColumnIndex(USER_PASSWORD));
        if(password.equals(passwordRef)){
            return c.getInt((int)c.getColumnIndex(USER_ID));
        } else {return -1;}
    }

    public boolean checkUser(String username, String password){
        Cursor c = getData(username);
        if(c == null || c.getCount() ==0){
            return false;
        }
        c.moveToFirst();
        if(c.getString(c.getColumnIndex(USER_PASSWORD)).equals(password)){
            return true;
        }
        return false;
    }

    public boolean checkUserDB(String username, String password){
        Cursor c = getData(username);
        if(c == null || c.getCount() ==0){
            return false;
        }
        c.moveToFirst();
        if(c.getString(c.getColumnIndex(USER_NAME)).equals(username)){
            return true;
        }
        return false;
    }

    void addContact(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(USER_ID, getCount());
        values.put(USER_NAME, username);
        values.put(USER_PASSWORD, password);
        db.insert(USERDATA_TABLE_NAME, null, values);
        db.close(); // Closing database connection
    }

    public Cursor getData(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        if(getCount()==0){
            return null;
        }
        Cursor res =  db.rawQuery( "SELECT * FROM " + USERDATA_TABLE_NAME + " WHERE " + USER_NAME + "=\"" +username+"\"", null );
        return res;
    }

    public String[] strArray(String username){
        Cursor c = getData(username);
        if(c == null){
            return null;
        }
        c.moveToFirst();
        String[] retVal = new String[2];
        retVal[0] = c.getString(c.getColumnIndex(USER_NAME));
        retVal[1] = c.getString(c.getColumnIndex(USER_ID));
        return retVal;
    }

    /**
     * Get number of row in the Database
     * @return: Int of row in database
     */
    public int getCount(){
        SQLiteDatabase db = this.getReadableDatabase();
        int numRows = (int) DatabaseUtils.queryNumEntries(db, USERDATA_TABLE_NAME);
        return numRows;
    }

    public void delete(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(USERDATA_TABLE_NAME, null, null);    }

    public Cursor getAllRecords(){
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + USERDATA_TABLE_NAME;
        return db.rawQuery(query, null);
    }
}
