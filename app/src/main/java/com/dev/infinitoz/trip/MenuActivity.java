package com.dev.infinitoz.trip;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.location.FusedLocationProviderClient;

public class MenuActivity extends AppCompatActivity {


    private FusedLocationProviderClient fusedLocationProviderClient;

    private Button startButton, joinTripButton;
    private EditText tripID;

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
                    Intent intent = new Intent(MenuActivity.this, UserMapsActivity.class);
                    validateTripID();
                    intent.putExtra("tripId", tripID.getText().toString());
                    startActivity(intent);
                    finish();
                }
        );
    }

    private void validateTripID() {
    }
}
