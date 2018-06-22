package com.dev.infinitoz.trip.util;

import java.util.Calendar;
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


}
