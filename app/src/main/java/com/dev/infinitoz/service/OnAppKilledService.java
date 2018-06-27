package com.dev.infinitoz.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.dev.infinitoz.TripContext;
import com.dev.infinitoz.trip.Constants;
import com.dev.infinitoz.trip.util.Utility;
import com.google.firebase.auth.FirebaseUser;

public class OnAppKilledService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        FirebaseUser user = (FirebaseUser) TripContext.getValue(Constants.USER);
        Utility.updateUserToTrip(false, user.getUid());
        Log.d("TESTING_SERVICE", "service is stopped");
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("TESTING_SERVICE", "service is started");
    }
}
