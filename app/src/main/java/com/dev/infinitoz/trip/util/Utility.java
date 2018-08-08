package com.dev.infinitoz.trip.util;

import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.dev.infinitoz.TripContext;
import com.dev.infinitoz.model.User;
import com.dev.infinitoz.trip.Constants;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

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
    public static int TYPE_WIFI = 1;
    public static int TYPE_MOBILE = 2;
    public static int TYPE_NOT_CONNECTED = 0;
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

    static boolean internetConnected = true;

    /*public static String getTimeStampByLocale(String timeStamp) {

        dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT_TIMEZONE);
        dateFormat.setTimeZone(TimeZone.getTimeZone(Constants.GMT));



    }*/

    public static void updateCoinsToUser(User user, String coins, boolean isAdd) {
        DatabaseReference userDBref = FirebaseDatabase.getInstance().getReference(Constants.USERS).child(user.getuId()).child(Constants.COINS);
        userDBref.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                String userCoins = (String) mutableData.getValue();
                if (userCoins == null) {
                    return Transaction.success(mutableData);
                }
//                String userCoins = user.getCoins();
                BigInteger credits = new BigInteger(coins);
                BigInteger userCoinsInt = new BigInteger(userCoins);
                if (isAdd) {
                    userCoinsInt = userCoinsInt.add(credits);
                } else {
                    userCoinsInt = userCoinsInt.subtract(credits);
                }
                user.setCoins(userCoinsInt.toString());
//                TripContext.addValue(Constants.CURRENT_USER,user);
                mutableData.setValue(userCoinsInt.toString());
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

            }
        });
        /*Map<String, Object> map = new HashMap<>();
        map.put(Constants.COINS, coins);
        userDBref.updateChildren(map);*/
    }

    public static boolean checkAvailableCoins(User currentUser, String role, Context ctx) {
        final boolean[] isAllowed = {false};
        if (currentUser != null) {
            FirebaseDatabase.getInstance().getReference(Constants.USERS).child(currentUser.getuId()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        User dbUser = dataSnapshot.getValue(User.class);
                        String coins = dbUser.getCoins();
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
                            isAllowed[0] = true;
                            DatabaseReference userDBref = FirebaseDatabase.getInstance().getReference(Constants.USERS).child(currentUser.getuId()).child(Constants.COINS);
                            int finalDeductCoins = deductCoins;
                            userDBref.runTransaction(new Transaction.Handler() {
                                @Override
                                public Transaction.Result doTransaction(MutableData mutableData) {
                                    String userCoins = (String) mutableData.getValue();
                                    if (userCoins == null) {
                                        return Transaction.success(mutableData);
                                    }
                                    BigInteger credits = new BigInteger(String.valueOf(finalDeductCoins));
                                    BigInteger userCoinsInt = new BigInteger(userCoins);
                                    userCoinsInt = userCoinsInt.subtract(credits);
                                    currentUser.setCoins(userCoinsInt.toString());
                                    mutableData.setValue(userCoinsInt.toString());
                                    return Transaction.success(mutableData);
                                }

                                @Override
                                public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                                }
                            });

                        } else {
                            Toast.makeText(ctx, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });


        }
        return isAllowed[0];
    }

    public static int getConnectivityStatus(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (null != activeNetwork) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI)
                return TYPE_WIFI;

            if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE)
                return TYPE_MOBILE;
        }
        return TYPE_NOT_CONNECTED;
    }

    public static String getConnectivityStatusString(Context context) {
        int conn = getConnectivityStatus(context);
        String status = null;
        if (conn == TYPE_WIFI) {
            status = "Wifi enabled";
        } else if (conn == TYPE_MOBILE) {
            status = "Mobile data enabled";
        } else if (conn == TYPE_NOT_CONNECTED) {
            status = "Not connected to Internet";
        }
        return status;
    }

    public static void setSnackbarMessage(Snackbar snackbar, View view, String status, boolean bar) {
        String internetStatus = "";
        if (status.equalsIgnoreCase("Wifi enabled") || status.equalsIgnoreCase("Mobile data enabled")) {
            internetStatus = "Internet Connected";
        } else {
            internetStatus = "Lost Internet Connection";
        }
        Snackbar finalSnackbar = snackbar;
        snackbar = Snackbar
                .make(view, internetStatus, Snackbar.LENGTH_LONG);
                /*.setAction("X", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        finalSnackbar.dismiss();
                    }
                });*/
        // Changing message text color
        snackbar.setActionTextColor(Color.WHITE);
        // Changing action button text color
        View sbView = snackbar.getView();
        TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);
        if (internetStatus.equalsIgnoreCase("Lost Internet Connection")) {
            if (internetConnected) {
                snackbar.show();
                internetConnected = false;
            }
        } else {
            if (!internetConnected) {
                internetConnected = true;
                snackbar.show();
            }
        }
    }


    public static IntentFilter getInternetFilterBoradcast() {
        IntentFilter internetFilter = new IntentFilter();
        internetFilter.addAction("android.net.wifi.STATE_CHANGE");
        internetFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        return internetFilter;

    }

}
