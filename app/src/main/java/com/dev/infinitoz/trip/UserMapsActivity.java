package com.dev.infinitoz.trip;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Button;

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

public class UserMapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private Location lastLocation;
    private Button logout, leaveTripButton;
    private String tripId;
    private DatabaseReference tripDatabaseReference;
    private String userId;
    private LatLng startPointLatLng, destLatLng, currentLocation, adminLatLng;
    private Marker myMarker, startMarker, endMarker, adminMarker;
    private boolean isBasicTripInfoCaptured;

    private ValueEventListener tripValueEventListener;
    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            for (Location location : locationResult.getLocations()) {
                if (getApplicationContext() != null) {
                    //on location changes code
                    lastLocation = location;
                    fetchTripDetails();
                    getTripInfo();
                    getAdminUsersDetails();
                }
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        initialize();
        setListeners();
        tripId = getIntent().getExtras().getString("tripId");
        //fetchTripDetails();
    }

    private void initialize() {

        logout = findViewById(R.id.logout);
        leaveTripButton = findViewById(R.id.leaveTrip);
    }

    private void setListeners() {
        logout.setOnClickListener((v) -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(UserMapsActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        });

        leaveTripButton.setOnClickListener((view) -> {
        });
    }

    private void fetchTripDetails() {
        tripDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Trip").child(tripId);
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();


        currentLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        DatabaseReference userDBRef = tripDatabaseReference.child("Users");
        GeoFire geoFire = new GeoFire(userDBRef);
        geoFire.setLocation(userId, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));

        if (myMarker != null) {
            myMarker.remove();
        }
        if (adminMarker != null) {
            //      adminMarker.remove();
        }

        myMarker = mMap.addMarker(new MarkerOptions().position(currentLocation).title("User").icon(BitmapDescriptorFactory.fromResource(R.mipmap.user)));
        if (startPointLatLng != null && destLatLng != null && startMarker == null && endMarker == null) {
            startMarker = mMap.addMarker(new MarkerOptions().position(startPointLatLng).title("Start").icon(BitmapDescriptorFactory.fromResource(R.mipmap.start)));
            endMarker = mMap.addMarker(new MarkerOptions().position(destLatLng).title("End").icon(BitmapDescriptorFactory.fromResource(R.mipmap.end)));
        }

    }

    private void getTripInfo() {
        tripValueEventListener = tripDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d("listneres", "reached inside the datasnaphot");
                dataSnapshot.exists();
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    isBasicTripInfoCaptured = true;
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (startPointLatLng == null) {

                        if (map.get("start") != null) {
                            Map<String, Object> startPointMap = (Map<String, Object>) map.get("start");
                            List<Object> list = (List<Object>) startPointMap.get("l");
                            startPointLatLng = new LatLng(Double.parseDouble(list.get(0).toString()), Double.parseDouble(list.get(1).toString()));
                        }
                        if (map.get("end") != null) {
                            Map<String, Object> endPointMap = (Map<String, Object>) map.get("end");
                            List<Object> list = (List<Object>) endPointMap.get("l");
                            destLatLng = new LatLng(Double.parseDouble(list.get(0).toString()), Double.parseDouble(list.get(1).toString()));
                        }
                    }
                    if (map.get("admin") != null) {
                        String adminId = (String) map.get("admin");
                        if (map.get(adminId) != null) {
                            Map<String, Object> adminLoc = (Map<String, Object>) map.get(adminId);
                            List<Object> list = (List<Object>) adminLoc.get("l");
                            adminLatLng = new LatLng(Double.parseDouble(list.get(0).toString()), Double.parseDouble(list.get(1).toString()));
                            if (adminMarker == null && adminLatLng != null) {
                                adminMarker = mMap.addMarker(new MarkerOptions().position(adminLatLng).title("Admin").icon(BitmapDescriptorFactory.fromResource(R.mipmap.admin)));
                            }

                        }
                    }
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
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
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        mMap.setMyLocationEnabled(true);
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this).setTitle("Give GPS permission")
                        .setMessage("Enable GPS").setPositiveButton("OK", (d, i) -> {
                    ActivityCompat.requestPermissions(UserMapsActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                })
                        .create()
                        .show();

            } else {
                ActivityCompat.requestPermissions(UserMapsActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    private void getAdminUsersDetails() {
    }
}
