package com.dev.infinitoz.trip;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.dev.infinitoz.TripContext;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MenuActivity extends AppCompatActivity {


    private FusedLocationProviderClient fusedLocationProviderClient;

    private Button startButton, joinTripButton;
    private EditText tripID;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        startButton = findViewById(R.id.startTrip);
        joinTripButton = findViewById(R.id.joinTrip);
        tripID = findViewById(R.id.tripId);

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
                                Intent intent = new Intent(MenuActivity.this, UserMapsActivity.class);
                                intent.putExtra(Constants.TRIP_ID, tripID.getText().toString());
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(MenuActivity.this, "Trip id doesnot exist", Toast.LENGTH_SHORT).show();
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
                    case R.id.contactUs:
                        break;
                    case R.id.privacyPolicy:
                        break;
                    case R.id.changePass:
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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void validateTripID() {
    }
}
