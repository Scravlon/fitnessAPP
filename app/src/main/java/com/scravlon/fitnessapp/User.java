package com.scravlon.fitnessapp;

import android.location.Location;
import android.support.annotation.NonNull;
import java.util.Calendar;
import static java.util.Calendar.DAY_OF_MONTH;

/**
 * User Structure, assume we only care about the walking distance on the day
 */

class User implements Comparable<User>{
    String username;                                                //Username of the User
    int dayOfMonth;                                                 //Today day to keep track
    float walkDistance;                                             //Assume we only care about the step each day
    Location officeLocation;                                        //User define location
    float step_Length = 2.4f;                                       //Step length, assume2.4 feet
    float walkGoal;                                                 //The walking goal of the

    /**
     * Constructor to construct a new user. New user is created when register a new user
     * @param username Username of the user
     */
    User(String username){
        this.username = username;
        dayOfMonth = Calendar.getInstance().get(DAY_OF_MONTH);
        officeLocation = null;
        walkGoal = 1000f;
    }

    /**
     * Update step if the date has changed
     *
     * @return: The day since user register
     */
    public void checkDate(){
        int day_month = Calendar.getInstance().get(DAY_OF_MONTH);
        if(day_month != dayOfMonth){
            walkDistance = 0;
            dayOfMonth = day_month;
        }
    }
    /**
     * Calculate walking distance with a average of 2.4 feet step length and set it to walkDistance
     * @param steps
     * @return
     */
    public float getDistanceWalk(long steps){
        float retVal = (steps* step_Length);
        walkDistance += retVal;
        return retVal;
    }

    /**
     * Test if the user has reach the goal
     * @return user walking distance is greater or equal to the goal(multiple of 1000)
     */
    public boolean goalReach(){
        return walkDistance>=walkGoal;
    }

    /**
     * Update the walking goal with 1000 feet
     * Only call function if walking distance is greater or equal to the current goal(multiple of 1000)
     * @return updated walking goal
     */
    public float updateGoal(){
        float retVal = walkGoal +1000;
        walkGoal = retVal;
        return retVal;
    }

    /**
     * Update the user office location
     * @param result User office location
     */
    public void updateLocation(Location result) {
        officeLocation = result;
    }

    /*Getter function*/
    public Float getWalk(){
        return walkDistance;
    }

    /**
     * Comparator to sort leader board
     */
    @Override
    public int compareTo(@NonNull User user) {
        checkDate();
        user.checkDate();
        return this.getWalk().compareTo(user.getWalk());
    }
}
