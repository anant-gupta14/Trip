package com.dev.infinitoz.trip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dev.infinitoz.ChangePasswordActivity;
import com.dev.infinitoz.TripContext;
import com.dev.infinitoz.model.User;
import com.dev.infinitoz.trip.util.Messages;
import com.dev.infinitoz.trip.util.Utility;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.math.BigInteger;

public class MenuActivity extends AppCompatActivity {



    private Button startButton, joinTripButton;
    private EditText tripID;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private NavigationView navigationView;
    private User currentUser;
    private static final int TIME_INTERVAL = 2000;
    private LinearLayout linearLayout;
    private Snackbar snackbar;
    public BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String status = Utility.getConnectivityStatusString(context);
            Utility.setSnackbarMessage(snackbar, linearLayout, status, false);
        }
    };
    private boolean internetConnected = true;


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void validateTripID() {
    }

    private long backPressed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        startButton = findViewById(R.id.startTrip);
        joinTripButton = findViewById(R.id.joinTrip);
        tripID = findViewById(R.id.tripId);
        currentUser = (User) TripContext.getValue(Constants.CURRENT_USER);
        linearLayout = findViewById(R.id.mainLineraLayout);

        startButton.setOnClickListener((v) -> {
            Intent intent = new Intent(MenuActivity.this, AdminMapActivity.class);
            startActivity(intent);
            finish();
                }
        );

        joinTripButton.setOnClickListener((v) -> {
                    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child(Constants.TRIP).child(tripID.getText().toString());
                    databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                                FirebaseDatabase.getInstance().getReference(Constants.USERS).child(currentUser.getuId()).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.exists()) {
                                            User dbUser = dataSnapshot.getValue(User.class);
                                            String coins = dbUser.getCoins();
                                            if (Integer.valueOf(coins) >= Constants.USER_DEDUCT_COINS) {
                                                DatabaseReference userDBref = FirebaseDatabase.getInstance().getReference(Constants.USERS).child(currentUser.getuId()).child(Constants.COINS);
                                                userDBref.runTransaction(new Transaction.Handler() {
                                                    @Override
                                                    public Transaction.Result doTransaction(MutableData mutableData) {
                                                        String userCoins = (String) mutableData.getValue();
                                                        if (userCoins == null) {
                                                            return Transaction.success(mutableData);
                                                        }
                                                        BigInteger credits = new BigInteger(String.valueOf(Constants.USER_DEDUCT_COINS));
                                                        BigInteger userCoinsInt = new BigInteger(userCoins);
                                                        userCoinsInt = userCoinsInt.subtract(credits);
                                                        currentUser.setCoins(userCoinsInt.toString());
                                                        mutableData.setValue(userCoinsInt.toString());
                                                        return Transaction.success(mutableData);
                                                    }

                                                    @Override
                                                    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                                                        if (b) {
                                                            Intent intent = new Intent(MenuActivity.this, UserMapsActivity.class);
                                                            intent.putExtra(Constants.TRIP_ID, tripID.getText().toString());
                                                            startActivity(intent);
                                                            finish();
                                                        }
                                                    }
                                                });

                                            } else {
                                                Toast.makeText(MenuActivity.this, Messages.INSUFFICIENT_COINS_USER, Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });



                            } else {
                                Toast.makeText(MenuActivity.this, Messages.TRIP_ID_NOT_EXIST, Toast.LENGTH_SHORT).show();
                            }

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }

        );

        drawerLayout = findViewById(R.id.nav);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        navigationView = findViewById(R.id.navView);
        View headerView = navigationView.getHeaderView(0);
        TextView userEmailView = headerView.findViewById(R.id.userEmail);
        FirebaseUser user = (FirebaseUser) TripContext.getValue(Constants.USER);
        userEmailView.setText(user.getEmail());
        navigationView.bringToFront();
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                Intent intent = null;
                switch (id) {
                    case R.id.userProfile:
                        intent = new Intent(MenuActivity.this, UserProfileActivity.class);
                        intent.putExtra(Constants.BACK_MENU, Constants.MENU_ACTIVITY);
                        startActivity(intent);
                        MenuActivity.super.onBackPressed();
                        finish();
                        break;
                    case R.id.earnCoins:
                        intent = new Intent(MenuActivity.this, EarnCoinsActivity.class);
                        intent.putExtra(Constants.USER_ID, ((FirebaseUser) TripContext.getValue(Constants.USER)).getUid());
                        startActivity(intent);
                        break;
                    case R.id.history:
                        intent = new Intent(MenuActivity.this, HistoryActivity.class);
                        intent.putExtra(Constants.USER_ID, ((FirebaseUser) TripContext.getValue(Constants.USER)).getUid());
                        startActivity(intent);
                        break;
                    case R.id.contactUs:
                        break;
                    case R.id.privacyPolicy:
                        intent = new Intent(MenuActivity.this, AdminManagementActivity.class);
                        startActivity(intent);
                        break;
                    case R.id.changePass:
                        intent = new Intent(MenuActivity.this, ChangePasswordActivity.class);
                        startActivity(intent);
                        finish();
                        break;
                    case R.id.logoutNav:
                        FirebaseAuth.getInstance().signOut();
                        intent = new Intent(MenuActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                        break;
                }
                return true;
            }
        });

    }

    @Override
    public void onBackPressed() {
        if (backPressed + TIME_INTERVAL > System.currentTimeMillis()) {
            super.onBackPressed();
            return;
        } else {
            Toast.makeText(getBaseContext(), Messages.TAP_TWICE_EXIT, Toast.LENGTH_SHORT).show();
        }
        backPressed = System.currentTimeMillis();

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
