package com.zedtechs.penawarcare_agent;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.maps.android.PolyUtil;
import com.zedtechs.penawarcare_agent.classes.DirectionsJSONParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JobNavigation extends AppCompatActivity
        implements
        OnCameraMoveStartedListener,
        OnMapReadyCallback
    {

    private GoogleMap mMap;

    private Double wayLatitude = 0.0, wayLongitude = 0.0;
    private Float userBearing;

    String agentid, jobid, mobileno, securetokenid, lastscrupdate = "";
    String jobcatcode,jobstatuscode;
    Boolean routeFound = false, jobDetailsAttained = false, arrivedAtDestination = false, waitFlag = false;
    Double minimumRangeToArrive = 20.0; // in meters
    Boolean followMeFlag = true;
    ProgressBar pbNavLoading;

    TextView tvYourLoc, tvOriginLoc, tvDestLoc, tvDistance, tvCurrentProcess, tvBottomNotice;
    Button btnAutoNav,btnManualNav,btnActionButton;
    CardView cvLocInfo, cvBottomButtonRow;

    List<List<HashMap<String, String>>> routes = null;
    List<Polyline> polylines = new ArrayList<Polyline>();

    FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest;
    SupportMapFragment supportMapFragment;

    private LatLng myLoc;
    private LatLng mOrigin;
    private LatLng mDestination;

    Double valueDistance = 0.0;
    String parsedDistance = "---";

    private Polyline mPolyline;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    Handler mHandler;
    RequestQueue requestQueue;
    SharedPreferences sharedpreferences;

    int focusClinicID;


    private int REQUEST_CLINIC_DETAILS = 1000;
    private static final int LOCATION_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_navigation);

        System.out.println("Entering onCreate");

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        focusClinicID = 0;

        //------------------------------------------------------
        // Retrieving shared variables
        //------------------------------------------------------
        sharedpreferences = getSharedPreferences(SplashActivity.MyPREFERENCES, Context.MODE_PRIVATE);
        agentid = sharedpreferences.getString("AGENTID","");
        mobileno = sharedpreferences.getString("MOBILE","");
        securetokenid = sharedpreferences.getString("SECURE_TOKEN_ID","");

        //------------------------------------------------------
        // Retrieving passed variables from parent activity
        //------------------------------------------------------
        Bundle extras = getIntent().getExtras();
        jobid = extras.getString("jobID");

        //------------------------------------------------------
        // Setting up resource variables
        //------------------------------------------------------
        cvLocInfo = findViewById(R.id.cvYourLocation);
        cvBottomButtonRow = findViewById(R.id.cvBottomButtonRow);

        tvYourLoc = findViewById(R.id.tvYourLoc);
        tvOriginLoc = findViewById(R.id.tvOriginLoc);
        tvDestLoc = findViewById(R.id.tvDestLoc);
        tvDistance = findViewById(R.id.tvDistance);
        tvCurrentProcess = findViewById(R.id.tvCurrentProcess);
        tvBottomNotice = findViewById(R.id.tvBottomNotice);

        pbNavLoading = findViewById(R.id.pbNavLoading);

        btnAutoNav = findViewById(R.id.btnAutoNav);
        btnManualNav = findViewById(R.id.btnManualNav);
        btnActionButton = findViewById(R.id.btnActionButton);

        //------------------------------------------------------
        // Setting up button onclick events
        //------------------------------------------------------
        btnAutoNav.setOnClickListener(v -> {
            initAutoNavigation();
        });
        btnManualNav.setOnClickListener(v -> {
            initManualNavigation();
        });
        //--------------------------------------------------------------
        // Setup request queue
        //--------------------------------------------------------------
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // Instantiate the cache - 1MB cap
        Network network = new BasicNetwork(new HurlStack()); // Set up the network to use HttpURLConnection as the HTTP client.
        requestQueue = new RequestQueue(cache, network); // Instantiate the RequestQueue with the cache and network.
        requestQueue.start(); // Start the queue

        //--------------------------------------------------------------
        // Setup GoogleMap : Current user location
        //--------------------------------------------------------------
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        supportMapFragment.getMapAsync(this);

        //------------------------------------------------------------------
        // Setup handler for continuous record status check
        //------------------------------------------------------------------
        mHandler = new Handler();
        //mHandler.postDelayed(m_Runnable,5000); // Begin after 5 seconds

        cvBottomButtonRow.setVisibility(View.GONE);

    }

    private final Runnable m_Runnable = new Runnable()
    {
        public void run()
        {
            //Toast.makeText(HomePageActivity.this,"Refreshing..",Toast.LENGTH_SHORT).show();
            JobNavigation.this.mHandler.postDelayed(m_Runnable, 5000);
        }
    };//runnable

        // @Override
        protected void onResume() {
            startLocationUpdates();
            super.onResume();
        }

        // @Override
        protected void onPause() {
            stopLocationUpdates();
            super.onPause();
        }

        // @Override
        protected void onStop() {
            stopLocationUpdates();
            super.onStop();
        }

    // @Override
    protected void onDestroy() {
        stopLocationUpdates();
        super.onDestroy();
    }



    private void stopLocationUpdates() {

        System.out.println("------------ Stopping location update ----------------");
        mHandler.removeCallbacks(m_Runnable);
        mFusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private void startLocationUpdates() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            } else {
                //Request Location Permission
                checkLocationPermission();
            }
        }
        else {
            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            navigateUpTo(new Intent(this, JobNavigation.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        System.out.println("Entering onMapReady");
        tvCurrentProcess.setText("Initiating Google Map");

        mMap = googleMap;
        mMap.setMapType(mMap.MAP_TYPE_NORMAL);
        mMap.setOnCameraMoveStartedListener(this);

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng arg0) {
                // TODO Auto-generated method stub
                Log.d("Touched :", arg0.latitude + "-" + arg0.longitude);
                initManualNavigation();

            }
        });

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5 * 1000); // Update user location every 5 seconds

        startLocationUpdates();

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                mMap.setMyLocationEnabled(true);
            } else {
                //Request Location Permission
                checkLocationPermission();
            }
        }
        else {
            mMap.setMyLocationEnabled(true);
        }


        /*
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[] { Manifest.permission.ACCESS_FINE_LOCATION },LOCATION_PERMISSION_CODE);
        }
        */

        // Zoom to user location
        followMeFlag = true;
    }

    //--------------------------------------------------------------------------------
    // Location callback
    //--------------------------------------------------------------------------------
    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {

            if (locationResult == null) {
                Toast.makeText(JobNavigation.this, "Unable to get your location", Toast.LENGTH_SHORT).show();
                tvYourLoc.setText(wayLatitude+", "+wayLongitude+" [C]");
                return;
            }

            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                //The last location in the list is the newest
                Location location = locationList.get(locationList.size() - 1);
                Log.i("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());

                mLastLocation = location;
                if (mCurrLocationMarker != null) {
                    mCurrLocationMarker.remove();
                }

                //Place current location marker
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title("Current Position");
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
                mCurrLocationMarker = mMap.addMarker(markerOptions);

                myLoc = new LatLng(location.getLatitude(),location.getLongitude());

                updateAgentLocation();

                wayLatitude = location.getLatitude();
                wayLongitude = location.getLongitude();
                userBearing = location.getBearing();

                tvYourLoc.setText(wayLatitude+", "+wayLongitude);


                if (waitFlag) {
                    pbNavLoading.setVisibility(View.VISIBLE);
                } else {
                    pbNavLoading.setVisibility(View.GONE);
                    //---------------------------------------------------------------
                    // Check  distance
                    // If distance is less than predefined range in meters, process which button to show
                    //---------------------------------------------------------------

                    if ((mOrigin != null) && (mDestination != null)) {

                        //valueDistance = distance(myLoc.latitude, myLoc.longitude, mDestination.latitude, mDestination.longitude) * 1000;

                        //tvDistance.setText(Math.round(valueDistance) + " m");
                        tvDistance.setText(parsedDistance);
                        //tvCurrentProcess.setText("Distance to Destination : "+Math.round(valueDistance) + " m");

                        if (valueDistance < minimumRangeToArrive) {

                            // Toast.makeText(getApplicationContext(), "You have arrived!", Toast.LENGTH_LONG).show();
                            // Show the confirm arrival button

                            arrivedAtDestination = true;

                            // Once verified, app will update destination location to customer's location
                        } else {
                            arrivedAtDestination = false;
                        }
                    }

                    if (followMeFlag) {
                        Log.i("MapsActivity", "Recentering to your position....");
                        recentreMarker();
                    } else {
                        Log.i("MapsActivity", "No need Recentering.");
                    }

                    //---------------------------------------------------------------
                    // Check whether job details needs to be refreshed
                    //---------------------------------------------------------------
                    if (!jobDetailsAttained) {
                        followMeFlag = true;
                        tvCurrentProcess.setText("Downloading travel data..");
                        getJobDetails();
                    } else {

                        //---------------------------------------------------------------
                        // Check if  user already arrived
                        //---------------------------------------------------------------
                        if (!arrivedAtDestination) {

                            //---------------------------------------------------------------
                            // Check whether user on current route
                            // If not, process new route
                            //---------------------------------------------------------------
                            if (!routeFound || !userIsOnRoute()) {
                                // Toast.makeText(getApplicationContext(), "Re-calculating route...", Toast.LENGTH_SHORT).show();
                                tvCurrentProcess.setText("Re-calculating route...");
                                getDirection();
                            }

                        } else {
                            tvCurrentProcess.setText("Arrived at destination");
                        }

                        switch (jobcatcode) {
                            case "RD01":
                                //-------------------------------------------------------------
                                // Stage functions for agent type : RIDER
                                //-------------------------------------------------------------

                                if (jobstatuscode.equals("READY")){

                                    if (arrivedAtDestination) {
                                        //-------------------------------------------------------------
                                        // Agent arrived at clinic for item pickup
                                        //-------------------------------------------------------------
                                        tvCurrentProcess.setText("Arrived at clinic");
                                        btnActionButton.setText("Ready for Pickup");
                                        tvBottomNotice.setVisibility(View.GONE);
                                        cvBottomButtonRow.setVisibility(View.VISIBLE);

                                        btnActionButton.setOnClickListener(v -> {
                                            verifyAtClinic();
                                        });
                                    } else {
                                        //-------------------------------------------------------------
                                        // Agent not arrived at clinic yet
                                        //-------------------------------------------------------------
                                        //tvCurrentProcess.setText("Navigating to clinic");
                                        cvBottomButtonRow.setVisibility(View.GONE);

                                    }

                                } else if (jobstatuscode.equals("WAITITEM")) {
                                    //-------------------------------------------------------------
                                    // Arrived at item collection location
                                    // Waiting for item pickup confirmation from clinic web app
                                    //-------------------------------------------------------------
                                    tvCurrentProcess.setText("Waiting for item");
                                    btnActionButton.setVisibility(View.GONE);
                                    tvBottomNotice.setText("Waiting for clinic confirmation");
                                    tvBottomNotice.setVisibility(View.VISIBLE);
                                    cvBottomButtonRow.setVisibility(View.VISIBLE);

                                    getJobDetails();

                                } else if (jobstatuscode.equals("PICKUP")) {

                                    if (!arrivedAtDestination) {
                                        //-------------------------------------------------------------
                                        // Item picked up and on its way
                                        //-------------------------------------------------------------
                                        //tvCurrentProcess.setText("Delivering item to customer");
                                        cvBottomButtonRow.setVisibility(View.GONE);

                                    } else {
                                        //-------------------------------------------------------------
                                        // Agent arrived at customer location
                                        //-------------------------------------------------------------
                                        tvCurrentProcess.setText("Arrived at customer's location");
                                        btnActionButton.setText("Verify Arrival");
                                        btnActionButton.setVisibility(View.VISIBLE);
                                        tvBottomNotice.setVisibility(View.GONE);
                                        cvBottomButtonRow.setVisibility(View.VISIBLE);

                                        btnActionButton.setOnClickListener(v -> {
                                            verifyDelivery();
                                        });
                                    }

                                } else if (jobstatuscode.equals("DELIVERED")) {
                                    //-------------------------------------------------------------
                                    // Package delivery has already been confirmed by agent
                                    // Waiting for receipt confirmation from patient app
                                    //-------------------------------------------------------------
                                    tvCurrentProcess.setText("Waiting for customer's confirmation");
                                    btnActionButton.setVisibility(View.GONE);
                                    tvBottomNotice.setVisibility(View.VISIBLE);
                                    cvBottomButtonRow.setVisibility(View.VISIBLE);

                                    getJobDetails();

                                } else if (jobstatuscode.equals("RECEIVED")) {

                                    //-------------------------------------------------------------
                                    // All process complete
                                    // Destroy this activity and go back to job details activity
                                    //-------------------------------------------------------------
                                    tvCurrentProcess.setText("Delivery successful!");
                                    finish();
                                } else {
                                    //-------------------------------------------------------------
                                    // Actions for other stages
                                    //-------------------------------------------------------------
                                    cvBottomButtonRow.setVisibility(View.GONE);
                                }
                                break;

                            case "AM01":
                                //-------------------------------------------------------------
                                // Stage functions for agent type : DRIVER
                                //-------------------------------------------------------------
                                break;
                            case "DC01":
                                //-------------------------------------------------------------
                                // Stage functions for agent type : DOCTOR
                                //-------------------------------------------------------------
                                break;
                            case "NS01":
                                //-------------------------------------------------------------
                                // Stage functions for agent type : NURSE
                                //-------------------------------------------------------------

                                break;
                            default:
                                btnActionButton.setText("Verify Arrival");
                        }
                    }
                }
            }

        }
    };

    private void initAutoNavigation(){

        Toast.makeText(JobNavigation.this,"Auto navigation",Toast.LENGTH_SHORT).show();
        followMeFlag = true;
        recentreMarker();
    }
    private void initManualNavigation(){

        Toast.makeText(JobNavigation.this,"Manual navigation",Toast.LENGTH_SHORT).show();
        followMeFlag = false;

    }

    private boolean userIsOnRoute(){
            boolean isOnPolyline = false;
            for (Polyline polyline : polylines) {
                if (PolyUtil.isLocationOnPath(myLoc, polyline.getPoints(), false, 100)) { // tolerance 100 metres
                    isOnPolyline = true;
                }
            }

            if (!isOnPolyline) {
                Log.i("Notice:","User not on route");
            } else{
                Log.i("Notice:","User on route");
            }

            return isOnPolyline;

    };

    @Override
    public void onCameraMoveStarted(int reason) {

        //followMeFlag = false;
        //Toast.makeText(this, "Camera moved.", Toast.LENGTH_LONG).show();

    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(JobNavigation.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION );
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public void recentreMarker(){

        CameraPosition cameraPosition = new CameraPosition.Builder().
                target(myLoc).
                tilt(60).
                zoom(18).
                bearing(userBearing).
                build();

        // Goto user current location
        //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(wayLatitude,wayLongitude), 10f));
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

    }

    @Override
    public void onBackPressed() {

        //Toast.makeText(getApplicationContext(), "Back pressed", Toast.LENGTH_SHORT).show();
        super.onBackPressed();
        stopLocationUpdates();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==REQUEST_CLINIC_DETAILS)
        {
            if (resultCode == RESULT_OK) {

                String registerStatus = data.getExtras().getString("registerStatus");

                if (registerStatus.equals("T")){
                    Intent intent=new Intent();
                    intent.putExtra("registerStatus",registerStatus);
                    setResult(RESULT_OK,intent);
                    finish();
                }

            }

        }
    }

    private double distance(double lat1, double lon1, double lat2, double lon2) {
            double theta = lon1 - lon2;
            double dist = Math.sin(deg2rad(lat1))
                    * Math.sin(deg2rad(lat2))
                    + Math.cos(deg2rad(lat1))
                    * Math.cos(deg2rad(lat2))
                    * Math.cos(deg2rad(theta));
            dist = Math.acos(dist);
            dist = rad2deg(dist);
            dist = dist * 60 * 1.1515;
            return (dist);
    }

    private double deg2rad(double deg) {
            return (deg * Math.PI / 180.0);
    }

    private double rad2deg(double rad) {
            return (rad * 180.0 / Math.PI);
    }

    private void getDirection() {

        //Toast.makeText(JobNavigation.this, "Processing best route..", Toast.LENGTH_SHORT).show();
        if (jobstatuscode.equals("READY")) {
            tvCurrentProcess.setText("Processing best route to clinic");
        } else if (jobstatuscode.equals("PICKUP")) {
            tvCurrentProcess.setText("Processing best route to customer");
        }

        pbNavLoading.setVisibility(View.VISIBLE);

        mOrigin = myLoc;

        String url, str_origin = "0.0,0.0", str_dest = "0.0,0.0";

        if ((mOrigin!=null)&&(mDestination!=null)) {

            // Origin of route
            str_origin = mOrigin.latitude+","+mOrigin.longitude;

            // Destination of route
            str_dest = mDestination.latitude+","+mDestination.longitude;

            tvOriginLoc.setText(str_origin);
            tvDestLoc.setText(str_dest);

        }


        // API info
        url ="https://www.penawarcare.com/public/api.php";

        // Key
        String key = getString(R.string.google_maps_key);

        // Data to be sent
        final Map<String, String> dataparams = new HashMap<>();
        dataparams.put("mode", "api_maps");
        dataparams.put("select", "directions");
        dataparams.put("origin", str_origin);
        dataparams.put("dest", str_dest);
        dataparams.put("key", key);

        // On SUCCESS
        Response.Listener<String> responseOK = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                //Log.i("Response:",response);
                JsonObject convertedObject = new Gson().fromJson(response, JsonObject.class);
                try {
                    routeFound = true;
                    processRoute(convertedObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };

        // On FAIL
        Response.ErrorListener responseError = new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                // TODO: Handle error
                waitFlag = false;
            }
        };

        // Compile request data
        StringRequest jsonRequest = new StringRequest (Request.Method.POST, url,responseOK,responseError){

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                return dataparams;
            }
        };

        // Add the request to the RequestQueue.
        requestQueue.add(jsonRequest);
    }

    private void processRoute(JsonObject obj) throws JSONException {

        String status = obj.get("status").toString().replace("\"", "");
        Log.i("Info:","Route Get Status:"+status);

        if (status.equals("OK")){

            waitFlag = false;

            JSONObject jObject;

            Log.i("Route JSON:",obj.toString());

            jObject = new JSONObject(obj.toString());
            DirectionsJSONParser parser = new DirectionsJSONParser();

            // Starts parsing data
            Log.i("Processing:","Parsing route..");

            routes = parser.parse(jObject);

            Log.i("Processing:","Parsing done");

            JSONArray array = jObject.getJSONArray("routes");
            JSONObject routesArray = array.getJSONObject(0);
            JSONArray legs = routesArray.getJSONArray("legs");
            JSONObject steps = legs.getJSONObject(0);
            JSONObject distance = steps.getJSONObject("distance");

            parsedDistance = distance.getString("text");
            valueDistance = Double.parseDouble(distance.getString("value"));

            drawRoute();

        } else {

            //String error_message = obj.get("status").toString().replace("\"", "");
            // Invalid. Show error
            /*Snackbar snackbar = Snackbar.make(findViewById(R.id.constraintLayoutJobNavigation), "Retrieving navigation data..", Snackbar.LENGTH_LONG);
            snackbar.setAction("Dismiss", new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
            snackbar.show();*/

        }

    }


    private void drawRoute(){

        Log.i("Processing:","Drawing route..");
        tvCurrentProcess.setText("Plotting route..");
        //Toast.makeText(JobNavigation.this, "Plotting route..", Toast.LENGTH_SHORT).show();

        ArrayList<LatLng> points = null;
        PolylineOptions lineOptions = null;

        if (routes!=null) {

            // Traversing through all the routes
            for (int i = 0; i < routes.size(); i++) {
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = routes.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);

                    //Toast.makeText(getApplicationContext(),"Lat :"+lat+"Long :"+lng, Toast.LENGTH_LONG).show();
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(10);
                lineOptions.color(Color.BLUE);
            }
        }

        // Drawing polyline in the Google Map for the i-th route
        if(lineOptions != null) {
            if(mPolyline != null){
                mPolyline.remove();
                polylines.clear();
            }
            mPolyline = mMap.addPolyline(lineOptions);
            polylines.add(mPolyline);
        } else {
            Toast.makeText(getApplicationContext(),"No route is found", Toast.LENGTH_LONG).show();
        }

        if (jobstatuscode.equals("READY")) {
            tvCurrentProcess.setText("Navigating to clinic");
        } else if (jobstatuscode.equals("PICKUP")) {
            tvCurrentProcess.setText("Delivering item to customer");
        }
    }

    private void resetNavigation(){

        cvBottomButtonRow.setVisibility(View.GONE);

        jobDetailsAttained = false;
        routeFound = false;
        routes.clear();
        polylines.clear();
    }

    private void getJobDetails(){
        //tvCurrentProcess.setText("Getting job details..");
        //Toast.makeText(JobNavigation.this, "Getting job details", Toast.LENGTH_SHORT).show();
        waitFlag = true;
        pbNavLoading.setVisibility(View.VISIBLE);

        // API info
        String url ="https://www.penawarcare.com/public/api.php";

        // Data to be sent
        final Map<String, String> dataparams = new HashMap<>();
        dataparams.put("mode", "api_agent");
        dataparams.put("select", "get_job_info");
        dataparams.put("jobid", jobid);
        dataparams.put("securetokenid", securetokenid);

        // On SUCCESS
        Response.Listener<String> responseOK = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                waitFlag = false;
                Log.i("GetJobDetails:",response);

                try {
                    JsonObject convertedObject = new Gson().fromJson(response, JsonObject.class);

                    String returnValue = convertedObject.get("value").toString().replace("\"","");
                    String message = convertedObject.get("msg").toString().replace("\"","");

                    if (returnValue.equals("1")) {

                        // Process based on Job Type
                        jobstatuscode = convertedObject.get("JOB_STATUS_CODE").toString().replace("\"","");
                        jobcatcode = convertedObject.get("JOB_CATEGORY_CODE").toString().replace("\"","");

                        String latitude = "0.0", longitude = "0.0";

                        switch (jobcatcode) {
                            case "RD01":
                                //----------------------------------------------------
                                // Check if rider already picked up the item
                                //----------------------------------------------------
                                if (jobstatuscode.equals("READY")) {
                                    // If not, set to clinic location. also show destination quick details
                                    latitude = convertedObject.get("CLINIC_LATITUDE").toString().replace("\"", "");
                                    longitude = convertedObject.get("CLINIC_LONGITUDE").toString().replace("\"", "");
                                } else if (jobstatuscode.equals("PICKUP")) {
                                    // If yes, set to customer location
                                    latitude = convertedObject.get("DELIVERY_LATITUDE").toString().replace("\"", "");
                                    longitude = convertedObject.get("DELIVERY_LONGITUDE").toString().replace("\"", "");
                                }
                                break;
                            default:
                                latitude = convertedObject.get("DELIVERY_LATITUDE").toString().replace("\"","");
                                longitude = convertedObject.get("DELIVERY_LONGITUDE").toString().replace("\"","");
                        }

                        mDestination = new LatLng(Double.parseDouble(latitude),Double.parseDouble(longitude));

                        jobDetailsAttained = true;

                    } else {

                        // Invalid. Show error
                        Snackbar snackbar = Snackbar.make(findViewById(R.id.constraintLayoutJobNavigation), message, Snackbar.LENGTH_LONG);
                        snackbar.setAction("Dismiss", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                            }
                        });
                        snackbar.show();
                    }

                } catch (JsonSyntaxException ex) {
                    // Error catched
                }
            }

        };

        // On FAIL
        Response.ErrorListener responseError = new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                // TODO: Handle error
            }
        };

        // Compile request data
        StringRequest jsonRequest = new StringRequest (Request.Method.POST, url,responseOK,responseError){

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                return dataparams;
            }
        };

        // Add the request to the RequestQueue.
        requestQueue.add(jsonRequest);
    }

    private void verifyAtClinic(){

        waitFlag = true;
        pbNavLoading.setVisibility(View.VISIBLE);

        jobstatuscode = "WAITITEM";
        tvCurrentProcess.setText("Verifying arrival..");
        Toast.makeText(JobNavigation.this, "Verifying arrival at item collection location..", Toast.LENGTH_SHORT).show();

        // API info
        String url ="https://www.penawarcare.com/public/api.php";

        // Data to be sent
        final Map<String, String> dataparams = new HashMap<>();
        dataparams.put("mode", "api_agent");
        dataparams.put("select", "update_job_status");
        dataparams.put("jobid", jobid);
        dataparams.put("jobstatuscode", jobstatuscode);
        dataparams.put("securetokenid", securetokenid);

        // On SUCCESS
        Response.Listener<String> responseOK = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                waitFlag = false;

                Log.i("Response:",response);

                try {
                    JsonObject convertedObject = new Gson().fromJson(response, JsonObject.class);

                    String returnValue = convertedObject.get("value").toString().replace("\"","");
                    String message = convertedObject.get("msg").toString().replace("\"","");

                    if (returnValue.equals("1")) {

                        // Show message
                        Snackbar snackbar = Snackbar.make(findViewById(R.id.constraintLayoutJobNavigation), message, Snackbar.LENGTH_LONG);
                        snackbar.setAction("Dismiss", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                            }
                        });
                        snackbar.show();

                        resetNavigation();

                    } else {

                        // Invalid. Show error. An alert dialog would be better
                        Snackbar snackbar = Snackbar.make(findViewById(R.id.constraintLayoutJobNavigation), message, Snackbar.LENGTH_LONG);
                        snackbar.setAction("Dismiss", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                            }
                        });
                        snackbar.show();
                    }

                } catch (JsonSyntaxException ex) {
                    // Error catched
                }
            }

        };

        // On FAIL
        Response.ErrorListener responseError = new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                // TODO: Handle error
            }
        };

        // Compile request data
        StringRequest jsonRequest = new StringRequest (Request.Method.POST, url,responseOK,responseError){

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                return dataparams;
            }
        };

        // Add the request to the RequestQueue.
        requestQueue.add(jsonRequest);
    }

    private void verifyDelivery(){

        waitFlag = true;
        pbNavLoading.setVisibility(View.VISIBLE);

        jobstatuscode = "DELIVERED";
        tvCurrentProcess.setText("Verifying item delivery..");

        // API info
        String url ="https://www.penawarcare.com/public/api.php";

        // Data to be sent
        final Map<String, String> dataparams = new HashMap<>();
        dataparams.put("mode", "api_agent");
        dataparams.put("select", "update_job_status");
        dataparams.put("jobid", jobid);
        dataparams.put("jobstatuscode", jobstatuscode);
        dataparams.put("securetokenid", securetokenid);

        // On SUCCESS
        Response.Listener<String> responseOK = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                waitFlag = false;

                Log.i("Response:",response);

                try {
                    JsonObject convertedObject = new Gson().fromJson(response, JsonObject.class);

                    String returnValue = convertedObject.get("value").toString().replace("\"","");
                    String message = convertedObject.get("msg").toString().replace("\"","");

                    if (returnValue.equals("1")) {

                        // Show message
                        Snackbar snackbar = Snackbar.make(findViewById(R.id.constraintLayoutJobNavigation), message, Snackbar.LENGTH_LONG);
                        snackbar.setAction("Dismiss", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                            }
                        });
                        snackbar.show();

                        resetNavigation();

                    } else {

                        String sql = convertedObject.get("sql").toString().replace("\"","");
                        Log.i("Error SQL:",sql);

                        // Invalid. Show error. An alert dialog would be better
                        Snackbar snackbar = Snackbar.make(findViewById(R.id.constraintLayoutJobNavigation), message, Snackbar.LENGTH_LONG);
                        snackbar.setAction("Dismiss", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                            }
                        });
                        snackbar.show();
                    }

                } catch (JsonSyntaxException ex) {
                    // Error catched
                }
            }

        };

        // On FAIL
        Response.ErrorListener responseError = new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                // TODO: Handle error
            }
        };

        // Compile request data
        StringRequest jsonRequest = new StringRequest (Request.Method.POST, url,responseOK,responseError){

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                return dataparams;
            }
        };

        // Add the request to the RequestQueue.
        requestQueue.add(jsonRequest);

    }

    private void updateAgentLocation() {

        // API info
        String url ="https://www.penawarcare.com/public/api.php";

        // Data to be sent
        final Map<String, String> dataparams = new HashMap<>();
        dataparams.put("mode", "api_agent");
        dataparams.put("select", "update_agent_location");
        dataparams.put("agentid", agentid);
        dataparams.put("latitude", Double.toString(myLoc.latitude));
        dataparams.put("longitude", Double.toString(myLoc.longitude));
        dataparams.put("securetokenid", securetokenid);

        // On SUCCESS
        Response.Listener<String> responseOK = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                Log.i("AgentLocUpdate:",response);

                try {
                    JsonObject convertedObject = new Gson().fromJson(response, JsonObject.class);
                    String returnValue = convertedObject.get("value").toString().replace("\"","");
                    String message = convertedObject.get("msg").toString().replace("\"","");

                    if (returnValue.equals("1")) {

                        // Do nothing

                    } else {

                        // Also do nothing
                    }

                } catch (JsonSyntaxException ex) {
                    // Error catched
                }
            }

        };

        // On FAIL
        Response.ErrorListener responseError = new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                // TODO: Handle error
            }
        };

        // Compile request data
        StringRequest jsonRequest = new StringRequest (Request.Method.POST, url,responseOK,responseError){

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                return dataparams;
            }
        };

        // Add the request to the RequestQueue.
        requestQueue.add(jsonRequest);
    }
}

