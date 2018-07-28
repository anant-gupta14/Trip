package com.dev.infinitoz.trip.util;

import android.content.Context;
import android.widget.Toast;

import com.dev.infinitoz.TripContext;
import com.dev.infinitoz.model.User;
import com.dev.infinitoz.trip.Constants;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

public class Utility {
    private static final Random random = new Random();
    private static StringBuilder base = new StringBuilder("abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNOPQRSTUVWXYZ234567890");
    private static String CHARS;
    private static DateFormat dateFormat;
    private static String timeZone;

    static {
        Calendar cal = Calendar.getInstance();
        long milliDiff = cal.get(Calendar.ZONE_OFFSET);
        String[] ids = TimeZone.getAvailableIDs();
        for (String id : ids) {
            TimeZone tz = TimeZone.getTimeZone(id);
            if (tz.getRawOffset() == milliDiff) {
                timeZone = id;
                break;
            }
        }
    }
    public static String generateTripId(String userId) {
        base.append(userId);
        CHARS = base.toString();
        StringBuilder token = new StringBuilder(5);
        for (int i = 0; i < 5; i++) {
            token.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return token.toString();
    }

    public static String generateOTP() {

        Random random = new Random();
        return String.format("%04d", random.nextInt(10000));
    }

    public static String getCurrentTime() {
        dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT_TIMEZONE);
        dateFormat.setTimeZone(TimeZone.getTimeZone(Constants.GMT));
        return dateFormat.format(new Date());
    }

    /*public static DatabaseReference getDatabaseReference(){

    }*/

    /*public static void updateUserToTrip(boolean value, String userId) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(Constants.USERS).child(userId);
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.ON_TRIP, value);
        dbRef.updateChildren(map);
    }*/

    public static void removeUserFromTrip(boolean value, String userId, String tripId) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(Constants.TRIP).child(tripId).child(Constants.USERS).child(userId);
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.IS_REMOVED, value);
        dbRef.updateChildren(map);
        DatabaseReference userHistory = FirebaseDatabase.getInstance().getReference(Constants.USERS).child(userId).child(Constants.HISTORY);
        Map<String, Object> userHist = new HashMap<>();
        userHist.put(tripId, true);
        userHistory.updateChildren(userHist);
    }

    public static void updateTripIdToUser(boolean add, String userId) {
        String tripID = (String) TripContext.getValue(Constants.TRIP_ID);
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(Constants.USERS).child(userId);
        Map<String, Object> map = new HashMap<>();
        if (add) {
            map.put(Constants.TRIP_ID, tripID);
            dbRef.updateChildren(map);
        } else {
            dbRef.child(Constants.TRIP_ID).removeValue();
            DatabaseReference historyRef = dbRef.child(Constants.HISTORY);
            map.put(tripID, true);
            historyRef.updateChildren(map);
        }
    }

    public static String tripShareMessage(String tripId, String otp) {
        StringBuilder sb = new StringBuilder();
        sb.append(Constants.Trip_TEXT);
        sb.append(tripId);
        sb.append(" OTP::" + otp);
        return sb.toString();
    }

    public static void updateCoinsToUser(String userId, String coins) {
        DatabaseReference userDBref = FirebaseDatabase.getInstance().getReference(Constants.USERS).child(userId);
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.COINS, coins);
        userDBref.updateChildren(map);
    }

    /*public static String getTimeStampByLocale(String timeStamp) {

        dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT_TIMEZONE);
        dateFormat.setTimeZone(TimeZone.getTimeZone(Constants.GMT));



    }*/

    public static boolean checkAvailableCoins(User currentUser, String role, Context ctx) {
        boolean isAllowed = false;
        if (currentUser != null) {
            String coins = currentUser.getCoins();
            BigInteger credits = new BigInteger(coins);
            int deductCoins = 0;
            String errorMessage = null;
            switch (role) {
                case Constants.ADMIN:
                    deductCoins = Constants.ADMIN_DEDUCT_COINS;
                    errorMessage = Messages.INSUFFICIENT_COINS_ADMIN;
                    break;
                case Constants.USER:
                    deductCoins = Constants.USER_DEDUCT_COINS;
                    errorMessage = Messages.INSUFFICIENT_COINS_USER;
                    break;
            }
            if (credits.intValue() >= deductCoins) {
                credits = credits.subtract(BigInteger.valueOf(deductCoins));
                Utility.updateCoinsToUser(currentUser.getuId(), credits.toString());
                currentUser.setCoins(credits.toString());
                isAllowed = true;
            } else {
                Toast.makeText(ctx, errorMessage, Toast.LENGTH_LONG).show();
            }
        }
        return isAllowed;
    }



}
