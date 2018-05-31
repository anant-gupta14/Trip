package com.dev.infinitoz.trip;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Map;

public class UserTripActivity extends FragmentActivity implements OnMapReadyCallback {

    Location lastLocation;
    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            for (Location location : locationResult.getLocations()) {
                if (getApplicationContext() != null) {
                    //on location changes code
                    lastLocation = location;
                }
            }

        }
    };
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private Button logout, leaveTripButton;
    private String tripId;
    private DatabaseReference tripDatabaseReference;
    private String userId;
    private LatLng startPointLatLng, destLatLng, currentLocation;
    private Marker myMarker, startMarker, endMarker;
    private ValueEventListener tripValueEventListener;
    private SupportMapFragment mapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_trip);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
        initialize();
        setListeners();
        tripId = getIntent().getExtras().getString("tripId");
        fetchTripDetails();
    }

    private void fetchTripDetails() {
        tripDatabaseReference = FirebaseDatabase.getInstance().getReference("Trip").child(tripId);
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();


        currentLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        DatabaseReference userDBRef = tripDatabaseReference.child("Users");
        GeoFire geoFire = new GeoFire(userDBRef);
        geoFire.setLocation(userId, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));
        getTripInfo();


        myMarker = mMap.addMarker(new MarkerOptions().position(currentLocation).title("User").icon(BitmapDescriptorFactory.fromResource(R.mipmap.user)));
        startMarker = mMap.addMarker(new MarkerOptions().position(startPointLatLng).title("Start").icon(BitmapDescriptorFactory.fromResource(R.mipmap.start)));
        // endMarker = mMap.addMarker(new MarkerOptions().position(destLatLng).title("End").icon(BitmapDescriptorFactory.fromResource(R.mipmap.end)));


    }

    private void getTripInfo() {
        tripValueEventListener = tripDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("start") != null) {
                        Map<String, Object> startPointMap = (Map<String, Object>) map.get("start");
                        List<Object> list = (List<Object>) startPointMap.get("l");
                        startPointLatLng = new LatLng(Double.parseDouble(list.get(0).toString()), Double.parseDouble(list.get(1).toString()));
                    }
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void setListeners() {
        logout.setOnClickListener((v) -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(UserTripActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        });

        leaveTripButton.setOnClickListener((view) -> {
        });
    }

    private void initialize() {

        logout = findViewById(R.id.logout);
        leaveTripButton = findViewById(R.id.leaveTrip);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.mMap = googleMap;

        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            /*if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){

            }
            else{*/
            checkLocationPermission();
            //}
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        mMap.setMyLocationEnabled(true);

    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this).setTitle("Give GPS permission")
                        .setMessage("Enable GPS").setPositiveButton("OK", (d, i) -> {
                    ActivityCompat.requestPermissions(UserTripActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                })
                        .create()
                        .show();

            } else {
                ActivityCompat.requestPermissions(UserTripActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    } else {
                        Toast.makeText(getApplicationContext(), "Please provide the permission", Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }
}
