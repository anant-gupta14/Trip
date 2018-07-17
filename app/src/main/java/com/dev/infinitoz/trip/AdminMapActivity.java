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
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dev.infinitoz.TripContext;
import com.dev.infinitoz.model.User;
import com.dev.infinitoz.remote.IGoogleApi;
import com.dev.infinitoz.trip.util.Utility;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
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
import com.google.android.gms.maps.Projection;
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
import com.google.firebase.auth.FirebaseUser;
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
    private String startPoint, endPoint, tripID, userId, otp;
    private PlaceAutocompleteFragment startAutocompleteFragment, destAutocompleteFragment;
    private SupportMapFragment mapFragment;
    private View mapView;

    private Button logout, startTripButton, shareTripButton;
    private boolean isTripStarted, isReloadTrip;

    private Location lastLocation;

    private Marker adminMarker, startMarker, endMarker;
    private LinearLayout linearLayout;
    private TextView tripIdTextView;
    private DatabaseReference tripDBReference, userDBReference;
    private List<Marker> userMarkers;
    private List<LatLng> directionLatLngs = new ArrayList<>();

    private ImageView appImageView;
    private AdView adView;
    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            for (Location location : locationResult.getLocations()) {
                if (getApplicationContext() != null) {
                    //on location changes code
                    lastLocation = location;
                    currentLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                    directionLatLngs.add(currentLocation);
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));

                    if (tripDBReference != null) {
                        if (isTripStarted) {
                            GeoFire geoFire = new GeoFire(tripDBReference);
                            geoFire.setLocation(userId, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));
                        }
                       /* if (adminMarker != null) {
                            adminMarker.remove();
                        }*/

                        // adminMarker = mMap.addMarker(new MarkerOptions().position(currentLocation).title.xml(Constants.ADMIN).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_admin)));
                        // animateCar();
                        getOtherUsers();
                        //setAnimation(mMap,directionLatLngs);
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
        tripDBReference.child(Constants.USERS).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                removeExistingUsersMarkers();
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    userMarkers = new ArrayList<>();
                    Map<String, Object> users = (Map<String, Object>) dataSnapshot.getValue();
                    for (String userId : users.keySet()) {
                        Map<String, Object> userData = (Map<String, Object>) users.get(userId);
                        if (userData.get(Constants.IS_REMOVED) == null || !(boolean) userData.get(Constants.IS_REMOVED)) {
                            List<Object> list = (List<Object>) userData.get("l");
                            LatLng userLatLng = new LatLng(Double.parseDouble(list.get(0).toString()), Double.parseDouble(list.get(1).toString()));
                            User user = getUserDetailsFromMap(userId);
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


    private void removeExistingUsersMarkers() {
        if (userMarkers != null && !userMarkers.isEmpty()) {
            for (Marker marker : userMarkers) {
                marker.remove();
            }
        }
        userMarkers = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        MobileAds.initialize(this, "ca-app-pub-3940256099942544~3347511713");
        adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().addTestDevice("8D8CED2F4594A1FD38529F7D241C99BF").build();
        adView.loadAd(adRequest);
        initialize();
        /*Toolbar toolbar = findViewById(R.id.toolbarAdmin);
        TextView tv = findViewById(R.id.toolbar_title);
        setSupportActionBar(toolbar);
        tv.setOnClickListener(view -> {
            Log.d("action bar clicked","true");
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
*/

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayShowCustomEnabled(true);
            View customView = getLayoutInflater().inflate(R.layout.title, null);
            TextView textView = customView.findViewById(R.id.toolbar_title);

            textView.setOnClickListener(view -> {
                if (isTripStarted) {
                    TripContext.addValue(Constants.RELOAD_TRIP, true);
                }
                Intent intent = new Intent(AdminMapActivity.this, AdminManagementActivity.class);
                startActivity(intent);
            });
            actionBar.setCustomView(customView);
        }
        setListeners();
        if (TripContext.getValue(Constants.RELOAD_TRIP) != null) {
            tripID = (String) TripContext.getValue(Constants.TRIP_ID);
            userId = ((FirebaseUser) TripContext.getValue(Constants.USER)).getUid();
            isReloadTrip = true;
            reloadTrip();
        }

    }

    private void reloadTrip() {
        tripDBReference = FirebaseDatabase.getInstance().getReference(Constants.TRIP).child(tripID);
        tripDBReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
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

                    if (map.get(userId) != null) {
                        Map<String, Object> endPointMap = (Map<String, Object>) map.get(userId);
                        List<Object> list = (List<Object>) endPointMap.get("l");
                        lastLocation = new Location("");
                        lastLocation.setLatitude(Double.parseDouble(list.get(0).toString()));
                        lastLocation.setLongitude(Double.parseDouble(list.get(1).toString()));
                    }
                    if (map.get(Constants.OTP) != null) {
                        otp = (String) map.get(Constants.OTP);
                    }
                    createTrip();
                    TripContext.removeValue(Constants.RELOAD_TRIP);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }


    private void initialize() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        //logout = findViewById(R.id.logout);
        startTripButton = findViewById(R.id.createTrip);
        mapView = mapFragment.getView();
        shareTripButton = findViewById(R.id.shareTrip);
        /*int id = getResources().getIdentifier("action_bar_title","id","android");
        findViewById(id).setOnClickListener(v->{
            Log.d("action bar clicked","true");
        });*/

        startAutocompleteFragment = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.startAutoComp);
        destAutocompleteFragment = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.destAutoComp);
        linearLayout = findViewById(R.id.cards);
        tripIdTextView = findViewById(R.id.tripId);
        polylineList = new ArrayList<>();
        //mService = Common.getGoogleApi();


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

        shareTripButton.setOnClickListener(v -> {
            Intent textIntent = new Intent();
            textIntent.setAction(Intent.ACTION_SEND);
            textIntent.putExtra(Intent.EXTRA_TEXT, Utility.tripShareMessage(tripID, otp));
            textIntent.setType("text/plain");
            //whatsAppIntent.setPackage("com.whatsapp");
            startActivity(Intent.createChooser(textIntent, getResources().getText(R.string.chooser_text)));
        });
    }

    private void createTrip() {
        if (isTripStarted) {
            endTrip();
            return;
        }
        isTripStarted = true;
        if (TripContext.getValue(Constants.RELOAD_TRIP) == null) {
            tripID = FirebaseDatabase.getInstance().getReference(Constants.TRIP).push().getKey();//Utility.generateTripId(userId);
            otp = Utility.generateOTP();
        }
        setStartAndDestLocation();
        //Utility.updateUserToTrip(true, userId);
        Utility.updateTripIdToUser(true, userId);

        startTripButton.setText(Constants.END_TRIP);
        linearLayout.setVisibility(View.GONE);
        tripIdTextView.setBackgroundColor(R.color.colorAccent);
        tripIdTextView.setText(tripID);
        tripIdTextView.setVisibility(View.VISIBLE);
        shareTripButton.setVisibility(View.VISIBLE);

    }

    private void setStartAndDestLocation() {
        userId = ((FirebaseUser) TripContext.getValue(Constants.USER)).getUid();

        TripContext.addValue(Constants.TRIP_ID, tripID);
        TripContext.addValue(Constants.OTP, otp);
        tripDBReference = FirebaseDatabase.getInstance().getReference(Constants.TRIP).child(tripID);
        GeoFire geoFire = new GeoFire(tripDBReference);
        geoFire.setLocation(userId, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));

        GeoFire geoFireStart = new GeoFire(tripDBReference);
        geoFire.setLocation(Constants.START, new GeoLocation(startPointLatLng.latitude, startPointLatLng.longitude));

        GeoFire geoFireEnd = new GeoFire(tripDBReference);
        geoFire.setLocation(Constants.END, new GeoLocation(destLatLng.latitude, destLatLng.longitude));

        if (!isReloadTrip) {
            Map<String, Object> map = new HashMap<>();
            map.put(Constants.ADMIN, userId);
            map.put(Constants.CREATED_TIME, Utility.getCurrentTime());
            map.put(Constants.OTP, otp);
            map.put(Constants.START_POINT, startPoint);
            map.put(Constants.END_POINT, endPoint);
            tripDBReference.updateChildren(map);
        }


        //adminMarker = mMap.addMarker(new MarkerOptions().position(currentLocation).title.xml("Admin").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_admin)));
        startMarker = mMap.addMarker(new MarkerOptions().position(startPointLatLng).title(Constants.START).icon(BitmapDescriptorFactory.fromResource(R.mipmap.start)));
        endMarker = mMap.addMarker(new MarkerOptions().position(destLatLng).title(Constants.END).icon(BitmapDescriptorFactory.fromResource(R.mipmap.end)));


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
        startTripButton.setText(Constants.START_TRIP);
        tripIdTextView.setVisibility(View.GONE);
        shareTripButton.setVisibility(View.GONE);
        removeExistingUsersMarkers();
        removeUsersFromTrip();
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.END_TIME, Utility.getCurrentTime());
        tripDBReference.updateChildren(map);
        removeTripId();
        //Utility.updateUserToTrip(false, userId);
        Utility.updateTripIdToUser(false, userId);
        TripContext.removeValue(tripID);
    }

    private void removeTripId() {
        DatabaseReference historyDB = FirebaseDatabase.getInstance().getReference(Constants.HISTORY).child(tripID);
        removeUsersFromTrip();
        tripDBReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    historyDB.setValue(dataSnapshot.getValue(), new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            if (databaseError != null) {
                                Log.i("copy trip to history", "copy faild");
                            } else {
                                Log.i("copy trip to history", "copy done..............");
                                tripDBReference.removeValue();
                                tripDBReference = null;
                            }
                        }
                    });
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }

    private void removeUsersFromTrip() {
        tripDBReference.child(Constants.USERS).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Map<String, Object> userTripMap = (Map<String, Object>) dataSnapshot.getValue();
                    for (String userId : userTripMap.keySet()) {
                        Utility.removeUserFromTrip(true, userId, tripID);
                    }

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.mMap = googleMap;

        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setTrafficEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();

        int[] ruleList = layoutParams.getRules();
        for (int i = 0; i < ruleList.length; i++) {
            layoutParams.removeRule(i);
        }

        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);



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

    public static void setAnimation(GoogleMap myMap, final List<LatLng> directionPoint) {


        Marker marker = myMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_car_top))
                .position(directionPoint.get(0))
                .flat(true));

        myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(directionPoint.get(0), 10));

        animateMarker(myMap, marker, directionPoint, false);
    }

    private static void animateMarker(GoogleMap myMap, final Marker marker, final List<LatLng> directionPoint,
                                      final boolean hideMarker) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = myMap.getProjection();
        final long duration = 30000;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            int i = 0;

            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                if (i < directionPoint.size())
                    marker.setPosition(directionPoint.get(i));
                i++;


                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                } else {
                    if (hideMarker) {
                        marker.setVisible(false);
                    } else {
                        marker.setVisible(true);
                    }
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (!isTripStarted) {
            Intent intent = new Intent(AdminMapActivity.this, MenuActivity.class);
            startActivity(intent);
            return;
        }

    }
}
