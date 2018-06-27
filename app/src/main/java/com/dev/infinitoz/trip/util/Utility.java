package com.dev.infinitoz.trip.util;

import com.dev.infinitoz.TripContext;
import com.dev.infinitoz.trip.Constants;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

public class Utility {
    private static final Random random = new Random();
    private static StringBuilder base = new StringBuilder("abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNOPQRSTUVWXYZ234567890");
    private static String CHARS;

    public static String generateTripId(String userId) {
        base.append(userId);
        CHARS = base.toString();
        StringBuilder token = new StringBuilder(5);
        for (int i = 0; i < 5; i++) {
            token.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return token.toString();
    }

    public static String getCurrentTime() {
        Calendar calendar = Calendar.getInstance();
        TimeZone timeZone = calendar.getTimeZone();
        return timeZone.toString();
    }

    /*public static DatabaseReference getDatabaseReference(){

    }*/

    public static void updateUserToTrip(boolean value, String userId) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(Constants.USERS).child(userId);
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.ON_TRIP, value);
        dbRef.updateChildren(map);
    }

    public static void updateTripIdToUser(boolean add, String userId) {
        String tripID = (String) TripContext.getValue(Constants.TRIP_ID);
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(Constants.USERS).child(userId);
        if (add) {
            Map<String, Object> map = new HashMap<>();
            map.put(Constants.TRIP_ID, tripID);
            dbRef.updateChildren(map);
        } else {
            dbRef.child(Constants.TRIP_ID).removeValue();
        }
    }

}
