package com.dev.infinitoz.trip;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserMapsActivity extends AppCompatActivity implements OnMapReadyCallback {

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
    private List<Marker> userMarkers;
    private SupportMapFragment mapFragment;
    private View mapView;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private NavigationView navigationView;

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
                    getOtherUsers();
                }
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        initialize();
        setListeners();
        tripId = getIntent().getExtras().getString(Constants.TRIP_ID);
        //fetchTripDetails();
    }

    private void initialize() {

        //logout = findViewById(R.id.logout);
        leaveTripButton = findViewById(R.id.leaveTrip);
        drawerLayout = findViewById(R.id.nav);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close);
        mapView = mapFragment.getView();

        navigationView = findViewById(R.id.navView);
        navigationView.bringToFront();
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                Intent intent = null;
                switch (id) {
                    case R.id.userProfile:
                        intent = new Intent(UserMapsActivity.this, UserProfileActivity.class);
                        startActivity(intent);
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
                        intent = new Intent(UserMapsActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                        break;
                }
                return true;
            }
        });

    }

    private void setListeners() {
        /*logout.setOnClickListener((v) -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(UserMapsActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        });*/

        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        leaveTripButton.setOnClickListener((view) -> {

            tripDatabaseReference.child("Users").child(userId).removeValue();
            Intent intent = new Intent(UserMapsActivity.this, MenuActivity.class);
            startActivity(intent);
            finish();
            return;

        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchTripDetails() {
        tripDatabaseReference = FirebaseDatabase.getInstance().getReference().child(Constants.TRIP).child(tripId);
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();


        currentLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
        DatabaseReference userDBRef = tripDatabaseReference.child(Constants.USERS);
        GeoFire geoFire = new GeoFire(userDBRef);
        geoFire.setLocation(userId, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));

        if (myMarker != null) {
            myMarker.remove();
        }

        //myMarker = mMap.addMarker(new MarkerOptions().position(currentLocation).title("User").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_bike)));
        if (startPointLatLng != null && destLatLng != null && startMarker == null && endMarker == null) {
            startMarker = mMap.addMarker(new MarkerOptions().position(startPointLatLng).title(Constants.START).icon(BitmapDescriptorFactory.fromResource(R.mipmap.start)));
            endMarker = mMap.addMarker(new MarkerOptions().position(destLatLng).title(Constants.END).icon(BitmapDescriptorFactory.fromResource(R.mipmap.end)));
        }

    }

    private void getTripInfo() {
        tripValueEventListener = tripDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Log.d("listneres", "reached inside the datasnaphot");
                //dataSnapshot.exists();
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    isBasicTripInfoCaptured = true;
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (startPointLatLng == null) {

                        if (map.get(Constants.START) != null) {
                            Map<String, Object> startPointMap = (Map<String, Object>) map.get(Constants.START);
                            List<Object> list = (List<Object>) startPointMap.get("l");
                            startPointLatLng = new LatLng(Double.parseDouble(list.get(0).toString()), Double.parseDouble(list.get(1).toString()));
                        }
                        if (map.get(Constants.END) != null) {
                            Map<String, Object> endPointMap = (Map<String, Object>) map.get(Constants.END);
                            List<Object> list = (List<Object>) endPointMap.get("l");
                            destLatLng = new LatLng(Double.parseDouble(list.get(0).toString()), Double.parseDouble(list.get(1).toString()));
                        }
                    }
                    if (map.get(Constants.ADMIN) != null) {
                        String adminId = (String) map.get(Constants.ADMIN);
                        if (map.get(adminId) != null) {
                            Map<String, Object> adminLoc = (Map<String, Object>) map.get(adminId);
                            List<Object> list = (List<Object>) adminLoc.get("l");
                            adminLatLng = new LatLng(Double.parseDouble(list.get(0).toString()), Double.parseDouble(list.get(1).toString()));
                            if (adminMarker != null) {
                                adminMarker.remove();
                            }

                        }
                    }
                    //  if (adminMarker == null && adminLatLng != null) {
                    adminMarker = mMap.addMarker(new MarkerOptions().position(adminLatLng).title(Constants.ADMIN).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_admin)));
                    //}

                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void getOtherUsers() {
        tripDatabaseReference.child(Constants.USERS).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    removeExistingUsers();
                    userMarkers = new ArrayList<>();
                    Map<String, Object> users = (Map<String, Object>) dataSnapshot.getValue();
                    for (String str : users.keySet()) {
                        if (!userId.equals(str)) {
                            int i = 0;
                            Map<String, Object> userData = (Map<String, Object>) users.get(str);
                            List<Object> list = (List<Object>) userData.get("l");
                            LatLng userLatLng = new LatLng(Double.parseDouble(list.get(0).toString()), Double.parseDouble(list.get(1).toString()));
                            Marker userMarker = null;
                            if (i % 2 == 0) {
                                userMarker = mMap.addMarker(new MarkerOptions().position(userLatLng).title(Integer.toString(i))
                                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_bike)));
                            } else {
                                userMarker = mMap.addMarker(new MarkerOptions().position(userLatLng).title(Integer.toString(i))
                                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.user)));
                            }

                            i++;
                            userMarkers.add(userMarker);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void removeExistingUsers() {
        if (userMarkers != null && !userMarkers.isEmpty()) {
            for (Marker marker : userMarkers) {
                marker.remove();
            }
        }
        userMarkers = null;
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
        locationRequest.setInterval(3000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();

        int[] ruleList = layoutParams.getRules();
        for (int i = 0; i < ruleList.length; i++) {
            layoutParams.removeRule(i);
        }

        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);

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

    @Override
    protected void onStop() {
        super.onStop();
        if (fusedLocationProviderClient != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
            fusedLocationProviderClient = null;
            locationRequest = null;
        }

    }
}