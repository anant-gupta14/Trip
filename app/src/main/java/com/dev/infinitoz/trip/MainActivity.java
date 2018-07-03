package com.dev.infinitoz.trip;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dev.infinitoz.TripContext;
import com.dev.infinitoz.service.OnAppKilledService;
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

import java.util.Map;

interface OngetData {
    void onSuccess(DataSnapshot dataSnapshot);
}

public class MainActivity extends AppCompatActivity {

    private EditText mEmail, mPassword;
    private Button mLogin, mRegistration;


    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener fireBaseAuthListener;

    boolean isUserAvailable;
    private String userId;
    private RelativeLayout progressBar;
    private TextView forgotPassTextView;
    //private  static Bus bus;

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

        // bus = new Bus(ThreadEnforcer.MAIN);
        mAuth = FirebaseAuth.getInstance();
        fireBaseAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    TripContext.addValue(Constants.USER, user);
                    userId = user.getUid();
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
                mAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()) {
                            Toast.makeText(MainActivity.this, "sign up error", Toast.LENGTH_SHORT).show();
                        } else {
                            String user_id = mAuth.getCurrentUser().getUid();
                            DatabaseReference currentUserDb = FirebaseDatabase.getInstance().getReference().child("Users").child(user_id);
                            currentUserDb.setValue(true);
                        }
                    }
                });
            }
        });
        mLogin.setOnClickListener((view) -> {
            final String email = mEmail.getText().toString();
            final String pass = mPassword.getText().toString();
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


    private void checkUserAvailable() {
        progressBar.setVisibility(View.VISIBLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        DatabaseReference adminRef = FirebaseDatabase.getInstance().getReference(Constants.USERS).child(userId);
        adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> userDataMap = (Map<String, Object>) dataSnapshot.getValue();
                    if (userDataMap.get(Constants.ON_TRIP) != null && !(boolean) userDataMap.get(Constants.ON_TRIP)) {
                        checkIsAnyTripActive(userDataMap);

                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                        progressBar.setVisibility(View.GONE);
                        //bus.post(intent);
                        return;
                    }
                    Toast.makeText(MainActivity.this, "User already in on going trip", Toast.LENGTH_SHORT).show();
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    progressBar.setVisibility(View.GONE);
                    return;
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

    private void readData(DatabaseReference reference, OngetData ongetData) {
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ongetData.onSuccess(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}