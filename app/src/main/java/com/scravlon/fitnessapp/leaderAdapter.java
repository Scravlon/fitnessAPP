package com.scravlon.fitnessapp;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Leaderboard adapter to organize and display user data on the Leaderboard in the main activity
 */

public class leaderAdapter extends ArrayAdapter<User> {

    private final ArrayList<User> userlist;
    private final Context context;

    public leaderAdapter(@NonNull Context context, ArrayList<User> objects) {
        super(context, 0, objects);
        this.context = context;
        this.userlist = objects;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if(listItem ==null){
            listItem = LayoutInflater.from(context).inflate(R.layout.leader_list_layout,parent,false);
        }
        User u = userlist.get(position);
        TextView txt1 = listItem.findViewById(R.id.text_id);
        TextView txt2 = listItem.findViewById(R.id.text_name);
        TextView txt3 = listItem.findViewById(R.id.text_distance);
        txt1.setText(position+ ". ");
        txt2.setText(u.username);
        txt3.setText(String.valueOf(Math.floor(u.walkDistance*100)/100) + " feet");
        return listItem;
    }


}
