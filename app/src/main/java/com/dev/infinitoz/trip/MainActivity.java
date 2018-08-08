package com.dev.infinitoz.trip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Patterns;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dev.infinitoz.TripContext;
import com.dev.infinitoz.model.User;
import com.dev.infinitoz.service.OnAppKilledService;
import com.dev.infinitoz.trip.util.Messages;
import com.dev.infinitoz.trip.util.Utility;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText mEmail, mPassword;
    private Button mLogin, mRegistration;


    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener fireBaseAuthListener;

    boolean isUserAvailable;
    private String userId, userEmail;
    private RelativeLayout progressBar;
    private TextView forgotPassTextView;
    //private  static Bus bus;


    private Snackbar snackbar;
    private LinearLayout linearLayout;
    private boolean internetConnected = true;
    public BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String status = Utility.getConnectivityStatusString(context);
            Utility.setSnackbarMessage(snackbar, linearLayout, status, false);
        }
    };


    private void checkUserAvailable() {
        progressBar.setVisibility(View.VISIBLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference(Constants.USERS).child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> userDataMap = (Map<String, Object>) dataSnapshot.getValue();
                    User currentUser = dataSnapshot.getValue(User.class);
                    currentUser.setuId(userId);
                    currentUser.setEmailId(userEmail);
                    TripContext.addValue(Constants.CURRENT_USER, currentUser);
                    if (userDataMap.get(Constants.NAME) == null) {
                        Intent intent = new Intent(MainActivity.this, UserProfileActivity.class);
                        startActivity(intent);
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                        progressBar.setVisibility(View.GONE);
                        return;
                    }
                    // if (userDataMap.get(Constants.ON_TRIP) != null && !(boolean) userDataMap.get(Constants.ON_TRIP)) {
                        checkIsAnyTripActive(userDataMap);

                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                        progressBar.setVisibility(View.GONE);
                        //bus.post(intent);
                        return;
                    // }
                   /* Toast.makeText(MainActivity.this, "User already in on going trip", Toast.LENGTH_SHORT).show();
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    progressBar.setVisibility(View.GONE);
                    return;*/
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void checkIsAnyTripActive(Map<String, Object> userDataMap) {
        String tripID = (String) userDataMap.get(Constants.TRIP_ID);
        TripContext.addValue(Constants.TRIP_ID, tripID);
        if (tripID != null) {
            FirebaseDatabase.getInstance().getReference(Constants.TRIP).child(tripID).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                        Map<String, Object> tripDataMap = (Map<String, Object>) dataSnapshot.getValue();
                        String adminID = (String) tripDataMap.get(Constants.ADMIN);
                        Intent intent = null;
                        TripContext.addValue(Constants.RELOAD_TRIP, true);
                        if (adminID != null) {
                            if (adminID.equals(userId)) {
                                intent = new Intent(MainActivity.this, AdminMapActivity.class);
                            } else {
                                intent = new Intent(MainActivity.this, UserMapsActivity.class);
                            }
                        }
                        startActivity(intent);
                        return;
                        //finish();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        }
        Intent intent = new Intent(MainActivity.this, MenuActivity.class);
        startActivity(intent);
        finish();
    }

   /* @Subscribe
    private void callNextActivity(Intent intent) {
        startActivity(intent);
        finish();
    }*/

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(fireBaseAuthListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(fireBaseAuthListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mEmail = findViewById(R.id.email);
        mPassword = findViewById(R.id.password);

        mLogin = findViewById(R.id.login);
        mRegistration = findViewById(R.id.registration);
        progressBar = findViewById(R.id.loadingPanel);
        forgotPassTextView = findViewById(R.id.forgotPass);
        startService(new Intent(MainActivity.this, OnAppKilledService.class));
        /*Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);*/
        linearLayout = findViewById(R.id.mainLineraLayout);

        mAuth = FirebaseAuth.getInstance();
        fireBaseAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    TripContext.addValue(Constants.USER, user);
                    userId = user.getUid();
                    userEmail = user.getEmail();
                    //email verfication
                    /*if(!user.isEmailVerified()){
                        Intent intent = new Intent(MainActivity.this,VerifyEmailActivity.class);
                        startActivity(intent);
                        return;
                    }*/
                    checkUserAvailable();
                    return;
                }

            }
        };

        mRegistration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String email = mEmail.getText().toString();
                final String pass = mPassword.getText().toString();
                if (!validateEmail(email)) {
                    snackbar = Snackbar
                            .make(linearLayout, Messages.EMPTY_OR_INVALID_EMAIL, Snackbar.LENGTH_LONG);
                    return;
                }
                mAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()) {
                            Toast.makeText(MainActivity.this, "sign up error", Toast.LENGTH_SHORT).show();
                        } else {
                            String user_id = mAuth.getCurrentUser().getUid();
                            DatabaseReference currentUserDb = FirebaseDatabase.getInstance().getReference().child("Users").child(user_id);
                            currentUserDb.setValue(true);
                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put(Constants.ON_TRIP, false);
                            currentUserDb.updateChildren(userMap);
                        }
                    }
                });
            }
        });
        mLogin.setOnClickListener((view) -> {
            final String email = mEmail.getText().toString();
            final String pass = mPassword.getText().toString();
            if (!validateEmail(email)) {
                snackbar = Snackbar
                        .make(linearLayout, Messages.EMPTY_OR_INVALID_EMAIL, Snackbar.LENGTH_LONG);
                return;
            }
            mAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (!task.isSuccessful()) {
                        Toast.makeText(MainActivity.this, "sign in error", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    /*FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        TripContext.addValue(Constants.USER, user);
                        userId = user.getUid();
                        checkUserAvailable();
                        return;
                    }
*/

                }
            });


        });

        forgotPassTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, ForgotPasswordActivity.class));
                finish();
            }
        });
    }

    private boolean validateEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver, Utility.getInternetFilterBoradcast());
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }


}