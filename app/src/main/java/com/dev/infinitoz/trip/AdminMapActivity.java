package com.dev.infinitoz.trip;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dev.infinitoz.remote.Common;
import com.dev.infinitoz.remote.IGoogleApi;
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
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminMapActivity extends AppCompatActivity implements OnMapReadyCallback {

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
    private List<Marker> userMarkers;
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
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
                        adminMarker = mMap.addMarker(new MarkerOptions().position(currentLocation).title("Admin").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_admin)));
                        // animateCar();
                        getOtherUsers();
                    }
                }
            }

        }
    };
    private List<LatLng> polylineList;
    private float v;
    private double lat, lng;
    private Handler handler;
    private PolylineOptions polylineOptions, blackPolylineOptions;
    private Polyline blackPolyline, greyPolyline;
    private int index, next;
    private IGoogleApi mService;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private NavigationView navigationView;

    private void animateCar() {
        mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                .target(mMap.getCameraPosition().target)
                .zoom(17)
                .bearing(30)
                .tilt(45)
                .build()));
        String requestUrl = null;
        try {
            requestUrl = "http://maps.googleapis.com/maps/api/direction/json?" +
                    "mode=driving&" +
                    "transit_routing_preference=less_driving&" +
                    "origin=" + currentLocation.latitude + "," + currentLocation.longitude + "&" +
                    "destination=" + endPoint + "&" +
                    "key=" + getResources().getString(R.string.google_directions_key);
            Log.d("URL:::: ", requestUrl);
            getGoogleApiData(requestUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getGoogleApiData(String requestUrl) {
        mService.getDataFromGoogleApi(requestUrl).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {

                try {
                    JSONObject jsonObject = new JSONObject(response.body().toString());
                    JSONArray jsonArray = jsonObject.getJSONArray("routes");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject route = jsonArray.getJSONObject(i);
                        JSONObject poly = route.getJSONObject("overview_polyline");
                        String polyline = poly.getString("points");
                        polylineList = decodePoly(polyline);

                    }

                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    for (LatLng latLng : polylineList) {
                        builder.include(latLng);
                        LatLngBounds bounds = builder.build();
                        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 2);
                        mMap.animateCamera(cameraUpdate);

                        polylineOptions = new PolylineOptions();
                        polylineOptions.color(Color.GRAY);
                        polylineOptions.width(5);
                        polylineOptions.startCap(new SquareCap());
                        polylineOptions.endCap(new SquareCap());
                        polylineOptions.jointType(JointType.ROUND);
                        polylineOptions.addAll(polylineList);

                        greyPolyline = mMap.addPolyline(polylineOptions);

                        blackPolylineOptions = new PolylineOptions();
                        blackPolylineOptions.color(Color.BLACK);
                        blackPolylineOptions.width(5);
                        blackPolylineOptions.startCap(new SquareCap());
                        blackPolylineOptions.endCap(new SquareCap());
                        blackPolylineOptions.jointType(JointType.ROUND);
                        blackPolylineOptions.addAll(polylineList);
                        blackPolyline = mMap.addPolyline(blackPolylineOptions);

                        mMap.addMarker(new MarkerOptions().position(polylineList.get(polylineList.size() - 1)));

                        //Animator
                        ValueAnimator polyAnimator = ValueAnimator.ofInt(100);
                        polyAnimator.setDuration(2000);
                        polyAnimator.setInterpolator(new LinearInterpolator());
                        polyAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                List<LatLng> points = greyPolyline.getPoints();
                                int percentValue = (int) valueAnimator.getAnimatedValue();
                                int size = points.size();
                                int newPoints = (int) (size * (percentValue / 100.0f));
                                List<LatLng> p = points.subList(0, newPoints);
                                blackPolyline.setPoints(p);
                            }
                        });
                        polyAnimator.start();
                        adminMarker = mMap.addMarker(new MarkerOptions().position(currentLocation).flat(true).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_car_top)));

                        // carmoving
                        handler = new Handler();
                        index = -1;
                        next = 1;
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (index < polylineList.size() - 1) {
                                    index++;
                                    next = index + 1;
                                }
                                startPointLatLng = polylineList.get(index);
                                destLatLng = polylineList.get(next);
                                ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
                                valueAnimator.setDuration(3000);
                                valueAnimator.setInterpolator(new LinearInterpolator());
                                valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                    @Override
                                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                        v = valueAnimator.getAnimatedFraction();
                                        lng = v * destLatLng.longitude + (1 - v)
                                                * startPointLatLng.longitude;
                                        lat = v * destLatLng.latitude + (1 - v)
                                                * startPointLatLng.latitude;
                                        LatLng newPos = new LatLng(lat, lng);
                                        adminMarker.setPosition(newPos);
                                        adminMarker.setAnchor(0.5f, 0.5f);
                                        adminMarker.setRotation(getBearing(startPointLatLng, newPos));
                                        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                                                .target(newPos)
                                                .zoom(15.5f)
                                                .build()));
                                    }
                                });
                                valueAnimator.start();
                                handler.postDelayed(this, 3000);

                            }
                        }, 3000);


                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(AdminMapActivity.this, "" + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private float getBearing(LatLng startPoint, LatLng newPos) {
        double lat = Math.abs(startPoint.latitude - newPos.latitude);
        double lng = Math.abs(startPoint.longitude - newPos.longitude);
        if (startPoint.latitude < newPos.latitude && startPoint.longitude < newPos.longitude) {
            return (float) (Math.toDegrees(Math.atan(lng / lat)));
        } else if (startPoint.latitude >= newPos.latitude && startPoint.longitude < newPos.longitude) {
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 90);
        } else if (startPoint.latitude >= newPos.latitude && startPoint.longitude >= newPos.longitude) {
            return (float) (Math.toDegrees(Math.atan(lng / lat)) + 180);
        } else if (startPoint.latitude < newPos.latitude && startPoint.longitude >= newPos.longitude) {
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 270);
        }
        return -1;
    }

    private List<LatLng> decodePoly(String encoded) {

        List poly = new ArrayList();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    private void getOtherUsers() {
        tripDBReference.child("Users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    removeExistingUsers();
                    userMarkers = new ArrayList<>();
                    Map<String, Object> users = (Map<String, Object>) dataSnapshot.getValue();
                    for (String str : users.keySet()) {
                        int i = 0;
                        Map<String, Object> userData = (Map<String, Object>) users.get(str);
                        List<Object> list = (List<Object>) userData.get("l");
                        LatLng userLatLng = new LatLng(Double.parseDouble(list.get(0).toString()), Double.parseDouble(list.get(1).toString()));
                        Marker userMarker = mMap.addMarker(new MarkerOptions().position(userLatLng).title(Integer.toString(i))
                                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.user)));
                        userMarkers.add(userMarker);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        initialize();

        setListeners();
    }

    private void initialize() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        //logout = findViewById(R.id.logout);
        startTripButton = findViewById(R.id.createTrip);
        drawerLayout = findViewById(R.id.nav);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close);

        startAutocompleteFragment = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.startAutoComp);
        destAutocompleteFragment = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.destAutoComp);
        linearLayout = findViewById(R.id.cards);
        tripIdTextView = findViewById(R.id.tripId);
        polylineList = new ArrayList<>();
        mService = Common.getGoogleApi();
        navigationView = findViewById(R.id.navView);
        navigationView.bringToFront();
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();

                switch (id) {

                    case R.id.settings:
                        break;
                    case R.id.logoutNav:
                        FirebaseAuth.getInstance().signOut();
                        Intent intent = new Intent(AdminMapActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                        break;
                    case R.id.vehicleType:
                        break;
                }
                return false;
            }
        });

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
        /*logout.setOnClickListener((v) -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(AdminMapActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        });*/

        startTripButton.setOnClickListener((view) -> {
            createTrip();
        });

        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
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


        //adminMarker = mMap.addMarker(new MarkerOptions().position(currentLocation).title("Admin").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_admin)));
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

        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setTrafficEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);



        locationRequest = new LocationRequest();
        locationRequest.setInterval(3000);
        locationRequest.setFastestInterval(3000);
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


   /* @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        switch(id){

            case R.id.userName:
                break;
            case R.id.logout:
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(AdminMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                break;
            case R.id.vehicleType:
                break;
        }
        return false;
    }*/
}
