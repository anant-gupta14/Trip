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
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dev.infinitoz.trip.util.Utility;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
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

import java.util.HashMap;
import java.util.Map;

public class AdminMapActivity extends FragmentActivity implements OnMapReadyCallback {

    LocationRequest locationRequest;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LatLng startPointLatLng, destLatLng, currentLocation;
    private String startPoint, endPoint, tripID, userId;
    private PlaceAutocompleteFragment startAutocompleteFragment, destAutocompleteFragment;

    private Button logout, startTripButton;
    private boolean isTripStarted;

    private Location lastLocation;

    private Marker adminMarker, startMarker, endMarker;
    private LinearLayout linearLayout;
    private TextView tripIdTextView;
    private DatabaseReference tripDBReference;
    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            for (Location location : locationResult.getLocations()) {
                if (getApplicationContext() != null) {
                    //on location changes code
                    lastLocation = location;
                    currentLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                    if (tripDBReference != null) {
                        GeoFire geoFire = new GeoFire(tripDBReference);
                        geoFire.setLocation(userId, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));
                        adminMarker = mMap.addMarker(new MarkerOptions().position(currentLocation).title("Admin").icon(BitmapDescriptorFactory.fromResource(R.mipmap.admin)));
                    }
                }
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        initialize();

        setListeners();
    }

    private void initialize() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        logout = findViewById(R.id.logout);
        startTripButton = findViewById(R.id.createTrip);

        startAutocompleteFragment = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.startAutoComp);
        destAutocompleteFragment = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.destAutoComp);
        linearLayout = findViewById(R.id.cards);
        tripIdTextView = findViewById(R.id.tripId);

    }

    private void setListeners() {

        startAutocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                startPoint = place.getName().toString();
                startPointLatLng = place.getLatLng();
            }

            @Override
            public void onError(Status status) {

            }
        });

        destAutocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                destLatLng = place.getLatLng();
                endPoint = place.getName().toString();
            }

            @Override
            public void onError(Status status) {

            }
        });
        logout.setOnClickListener((v) -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(AdminMapActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        });

        startTripButton.setOnClickListener((view) -> {
            createTrip();
        });

    }

    private void createTrip() {
        if (isTripStarted) {
            endTrip();
            tripDBReference.removeValue();
            return;
        }
        isTripStarted = true;
        setStartAndDestLocation();
        updateUsersToTrip();

        startTripButton.setText("End Trip");
        linearLayout.setVisibility(View.GONE);
        tripIdTextView.setText(tripID);
        tripIdTextView.setVisibility(View.VISIBLE);

    }

    private void setStartAndDestLocation() {
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        tripID = Utility.generateTripId(userId);
        tripDBReference = FirebaseDatabase.getInstance().getReference("Trip").child(tripID);
        GeoFire geoFire = new GeoFire(tripDBReference);
        geoFire.setLocation(userId, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));

        GeoFire geoFireStart = new GeoFire(tripDBReference);
        geoFire.setLocation("start", new GeoLocation(startPointLatLng.latitude, startPointLatLng.longitude));

        GeoFire geoFireEnd = new GeoFire(tripDBReference);
        geoFire.setLocation("end", new GeoLocation(destLatLng.latitude, destLatLng.longitude));


        Map<String, Object> map = new HashMap<>();
        map.put("admin", userId);
        tripDBReference.updateChildren(map);


        //adminMarker = mMap.addMarker(new MarkerOptions().position(currentLocation).title("Admin").icon(BitmapDescriptorFactory.fromResource(R.mipmap.admin)));
        startMarker = mMap.addMarker(new MarkerOptions().position(startPointLatLng).title("Start").icon(BitmapDescriptorFactory.fromResource(R.mipmap.start)));
        endMarker = mMap.addMarker(new MarkerOptions().position(destLatLng).title("End").icon(BitmapDescriptorFactory.fromResource(R.mipmap.end)));


    }

    private void updateUsersToTrip() {
        DatabaseReference adminRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        Map<String, Object> map = new HashMap<>();
        map.put("onTrip", true);
    }

    private void endTrip() {
        isTripStarted = false;
        if (startMarker != null) {
            startMarker.remove();
        }
        if (endMarker != null) {
            endMarker.remove();
        }
        if (adminMarker != null) {
            adminMarker.remove();
        }
        linearLayout.setVisibility(View.VISIBLE);
        startTripButton.setText("Start Trip");
        tripIdTextView.setVisibility(View.GONE);
        removeTripId();
    }

    private void removeTripId() {
        DatabaseReference historyDB = FirebaseDatabase.getInstance().getReference("History").child(tripID);
        tripDBReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Iterable<DataSnapshot> children = dataSnapshot.getChildren();
                    historyDB.setValue(dataSnapshot.getValue(), new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            if (databaseError != null) {
                                System.out.println("copy faild");
                            } else {
                                System.out.println("copy done..............");
                            }
                        }
                    });
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        //tripDBReference.removeValue();
        tripDBReference = null;

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
                    ActivityCompat.requestPermissions(AdminMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                })
                        .create()
                        .show();

            } else {
                ActivityCompat.requestPermissions(AdminMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
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
