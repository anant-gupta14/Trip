package com.dev.infinitoz.trip;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dev.infinitoz.TripContext;
import com.dev.infinitoz.model.User;
import com.dev.infinitoz.trip.util.Messages;
import com.dev.infinitoz.trip.util.Utility;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UserMapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private Location lastLocation;
    private Button logout, leaveTripButton;
    private String tripId;
    private DatabaseReference tripDatabaseReference, userTripDBRef;
    private String userId;
    private LatLng startPointLatLng, destLatLng, currentLocation, adminLatLng, meetPointLatLng;
    private Marker myMarker, startMarker, endMarker, adminMarker, meetMarker;
    private boolean isBasicTripInfoCaptured, isTripClosed;
    private List<Marker> userMarkers;
    private SupportMapFragment mapFragment;
    private View mapView;
    private AlertDialog.Builder sosDialog;
    private Set<String> prevSOSUsers;

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
                    checkTripAvailableForMe();
                }
            }

        }
    };
    private DatabaseReference userDBReference;
    private boolean isSOSDIalogEnabled;
    private MediaPlayer mp;

    private void checkTripAvailableForMe() {
        if (userTripDBRef != null) {
            userTripDBRef.child(userId).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0 && !isTripClosed) {
                        Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                        if (map.get(Constants.IS_REMOVED) == null || !(boolean) map.get(Constants.IS_REMOVED)) {
                            getTripInfo();
                            getOtherUsers();
                        } else {
                            Toast.makeText(UserMapsActivity.this, "You are not anymore with this trip :" + tripId, Toast.LENGTH_SHORT).show();
                            leaveTrip();
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

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
        if (TripContext.getValue(Constants.RELOAD_TRIP) != null) {
            tripId = TripContext.getValue(Constants.TRIP_ID).toString();
        } else {
            tripId = getIntent().getExtras().getString(Constants.TRIP_ID);
            TripContext.addValue(Constants.TRIP_ID, tripId);
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayShowCustomEnabled(true);
            View customView = getLayoutInflater().inflate(R.layout.title, null);
            TextView textView = customView.findViewById(R.id.toolbar_title);

            textView.setOnClickListener(view -> {
                TripContext.addValue(Constants.RELOAD_TRIP, true);
                TripContext.addValue(Constants.IS_USER_VIEW, true);
                Intent intent = new Intent(UserMapsActivity.this, AdminManagementActivity.class);
                startActivity(intent);
            });
            actionBar.setCustomView(customView);
        }
        //fetchTripDetails();
    }

    private void initialize() {

        //logout = findViewById(R.id.logout);
        leaveTripButton = findViewById(R.id.leaveTrip);

        mapView = mapFragment.getView();


    }

    private void setListeners() {
        /*logout.setOnClickListener((v) -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(UserMapsActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        });*/


        leaveTripButton.setOnClickListener((view) -> {
            leaveTrip();
            return;

        });
    }

    private void leaveTrip() {
        isTripClosed = true;
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        Utility.removeUserFromTrip(true, userId, tripId);
        removeExistingUsers();
        Utility.updateTripIdToUser(false, userId);
        Intent intent = new Intent(UserMapsActivity.this, MenuActivity.class);
        startActivity(intent);

    }

    private void fetchTripDetails() {
        if (isBasicTripInfoCaptured) {
            return;
        }
        tripDatabaseReference = FirebaseDatabase.getInstance().getReference().child(Constants.TRIP).child(tripId);
        FirebaseUser user = (FirebaseUser) TripContext.getValue(Constants.USER);
        userTripDBRef = tripDatabaseReference.child(Constants.USERS);
        userId = user.getUid();
        //Utility.removeUserFromTrip(false,userId,tripId);
        //Utility.updateUserToTrip(true, userId);
        currentLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
        GeoFire geoFire = new GeoFire(userTripDBRef);
        geoFire.setLocation(userId, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));
        isBasicTripInfoCaptured = true;
    }

    private void getTripInfo() {
        currentLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));


        GeoFire geoFire = new GeoFire(userTripDBRef);
        geoFire.setLocation(userId, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));

        if (myMarker != null) {
            myMarker.remove();
        }

        //myMarker = mMap.addMarker(new MarkerOptions().position(currentLocation).title.xml("User").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_bike)));
        if (startPointLatLng != null && destLatLng != null && startMarker == null && endMarker == null) {
            startMarker = mMap.addMarker(new MarkerOptions().position(startPointLatLng).title(Constants.START).icon(BitmapDescriptorFactory.fromResource(R.mipmap.start)));
            endMarker = mMap.addMarker(new MarkerOptions().position(destLatLng).title(Constants.END).icon(BitmapDescriptorFactory.fromResource(R.mipmap.end)));
        }
        if (meetPointLatLng != null && meetMarker == null) {
            meetMarker = mMap.addMarker(new MarkerOptions().position(meetPointLatLng).title(Constants.MEET_POINT.toUpperCase()).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_meetpoint)));
        }
        tripDatabaseReference = FirebaseDatabase.getInstance().getReference().child(Constants.TRIP).child(tripId);
        FirebaseUser user = (FirebaseUser) TripContext.getValue(Constants.USER);
        userId = user.getUid();
        tripValueEventListener = tripDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Log.d("listneres", "reached inside the datasnaphot");
                //dataSnapshot.exists();
                String adminId = null;
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
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
                        Utility.updateTripIdToUser(true, userId);
                    }
                    if (map.get(Constants.MEET_POINT) != null) {
                        Map<String, Object> startPointMap = (Map<String, Object>) map.get(Constants.MEET_POINT);
                        List<Object> list = (List<Object>) startPointMap.get("l");
                        meetPointLatLng = new LatLng(Double.parseDouble(list.get(0).toString()), Double.parseDouble(list.get(1).toString()));

                    } else {
                        if (meetMarker != null) {
                            meetMarker.remove();
                        }
                        meetPointLatLng = null;
                    }
                    if (map.get(Constants.ADMIN) != null) {
                        adminId = (String) map.get(Constants.ADMIN);
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
                    User admin = getAdminDetails(adminId);
                    if (admin != null) {
                        adminMarker = mMap.addMarker(new MarkerOptions().position(adminLatLng).title(admin.getName()).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_admin)));
                    }
                    //}
                    if (map.get(Constants.SOS) != null) {
                        Map<String, Object> sosUserMap = (Map<String, Object>) map.get(Constants.SOS);
                        Set<String> users = sosUserMap.keySet();
                        if (compareSOSUsers(users)) {
                            prevSOSUsers = users;
                            creatAlertDialog(getSOSMessage(users));
                        }

                    } else {
                        if (isSOSDIalogEnabled) {
                            prevSOSUsers = null;
                            if (mp != null) {
                                mp.stop();
                                sosDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialog) {
                                        dialog.dismiss();
                                    }
                                });
                            }
                            isSOSDIalogEnabled = false;
                        }
                    }

                    if (isBasicTripInfoCaptured) {
                        return;
                    }

                    // Utility.updateUserToTrip(true, userId);
                    isBasicTripInfoCaptured = true;
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private boolean compareSOSUsers(Set<String> users) {
        if (prevSOSUsers == null) {
            return true;
        }
        if (prevSOSUsers.size() < users.size()) {
            return true;
        } else {
            for (String userid : prevSOSUsers) {
                if (!users.contains(userid)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void creatAlertDialog(String msg) {
        isSOSDIalogEnabled = true;
        if (mp == null) {
            mp = MediaPlayer.create(UserMapsActivity.this, Settings.System.DEFAULT_RINGTONE_URI);
        }
        if (sosDialog == null) {
            sosDialog = new AlertDialog.Builder(UserMapsActivity.this);
            sosDialog.setCancelable(true);
            sosDialog.setTitle(Messages.SOS_TITLE);
        }
        sosDialog.setMessage(msg);
        mp.start();
        sosDialog.setPositiveButton(Constants.OK, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mp.stop();
            }
        });
        sosDialog.show();


    }

    private String getSOSMessage(Set<String> users) {
        StringBuilder builder = new StringBuilder(Messages.SOS_MSG);
        builder.append("\n");
        for (String userid : users) {
            if (userid != userId) {
                User user = getUserDetailsFromMap(userid);
                if (user != null) {
                    builder.append(user.getName() + ":::" + user.getPhone());
                    builder.append("\n");
                }
            }
        }
        return builder.toString();
    }

    private User getAdminDetails(String adminId) {
        TripContext.addValue(Constants.ADMIN, adminId);
        return getUserDetailsFromMap(adminId);
    }

    private void getOtherUsers() {
        tripDatabaseReference.child(Constants.USERS).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    removeExistingUsers();
                    userMarkers = new ArrayList<>();
                    Map<String, Object> users = (Map<String, Object>) dataSnapshot.getValue();
                    for (String usrID : users.keySet()) {
                        if (!userId.equals(usrID)) {
                            Map<String, Object> userData = (Map<String, Object>) users.get(usrID);
                            if (userData.get(Constants.IS_REMOVED) == null || !(boolean) userData.get(Constants.IS_REMOVED)) {
                                List<Object> list = (List<Object>) userData.get("l");
                                LatLng userLatLng = new LatLng(Double.parseDouble(list.get(0).toString()), Double.parseDouble(list.get(1).toString()));
                                User user = getUserDetailsFromMap(usrID);
                                if (user != null) {
                                    Marker userMarker = mMap.addMarker(new MarkerOptions().position(userLatLng).title(user.getName()));
                                    switch (user.getVehicleType()) {
                                        case Constants.CAR:
                                            userMarker.setIcon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_car));
                                            break;
                                        case Constants.BIKE:
                                            userMarker.setIcon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_bike));
                                            break;
                                        case Constants.WALK:
                                            userMarker.setIcon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_walk));
                                            break;
                                    }
                                    userMarkers.add(userMarker);
                                }
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

    private User getUserDetailsFromMap(String userId) {
        Map<String, User> userMap = (Map<String, User>) TripContext.getValue(Constants.USER_DATA_MAP);
        if (userMap == null) {
            userMap = new HashMap<>();
            TripContext.addValue(Constants.USER_DATA_MAP, userMap);
        }

        if (userMap.get(userId) != null) {
            return userMap.get(userId);
        } else {
            userDBReference = FirebaseDatabase.getInstance().getReference(Constants.USERS).child(userId);
            userDBReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    User user = null;
                    if (dataSnapshot.exists()) {
                        user = dataSnapshot.getValue(User.class);
                        user.setuId(userId);
                        ((Map<String, User>) TripContext.getValue(Constants.USER_DATA_MAP)).put(userId, user);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
        return userMap.get(userId);
    }

    private void removeExistingUsers() {
        if (userMarkers != null && !userMarkers.isEmpty()) {
            for (Marker marker : userMarkers) {
                marker.remove();
            }
        }
        userMarkers = null;
        if (adminMarker != null) {
            adminMarker.remove();
        }
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
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

        View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();

        int[] ruleList = layoutParams.getRules();
        for (int i = 0; i < ruleList.length; i++) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                layoutParams.removeRule(i);
            }
        }

        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
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


    @Override
    protected void onStop() {
        super.onStop();
       /* if (fusedLocationProviderClient != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
            fusedLocationProviderClient = null;
            locationRequest = null;
        }*/

    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }
}
