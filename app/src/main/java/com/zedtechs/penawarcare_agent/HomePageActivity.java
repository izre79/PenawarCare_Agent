package com.zedtechs.penawarcare_agent;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.zedtechs.penawarcare_agent.classes.Job;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomePageActivity extends AppCompatActivity {

    RequestQueue requestQueue;

    String agentid, mobileno, securetokenid, lastscrupdate = "";
    SharedPreferences sharedpreferences;

    Switch swAvailability;
    CardView cvUserInfo;
    TextView tvUserFullName, tvUserEmail, tvCurrProcess;
    RecyclerView recyclerView;
    ScrollView scrollViewJobListing;
    ImageView ivNotAvailable;

    public List<Job> jobList = new ArrayList<>();
    public Map<String, Job> ITEM_MAP =  new HashMap<String, Job>();

    String google_maps_key;
    FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest;
    private LatLng userLoc;
    String agentAvailability = "T";

    ProgressBar pbLoading;

    Handler mHandler;

    static public String consultRegID = "", consultQueueNo = "";

    static final String CHANNEL_ID = "penawarcare";

    public static final int refreshDelaySeconds = 5;

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        //------------------------------------------------------
        // Retrieving shared variables
        //------------------------------------------------------
        sharedpreferences = getSharedPreferences(SplashActivity.MyPREFERENCES, Context.MODE_PRIVATE);
        agentid = sharedpreferences.getString("AGENTID","");
        mobileno = sharedpreferences.getString("MOBILE","");
        securetokenid = sharedpreferences.getString("SECURE_TOKEN_ID","");

        //------------------------------------------------------
        // Linking resources to variables
        //------------------------------------------------------
        cvUserInfo = findViewById(R.id.cvPatientInfo);
        tvUserFullName = findViewById(R.id.tvUserFullName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvCurrProcess = findViewById(R.id.tvHomePage_CurrProcess);
        pbLoading = findViewById(R.id.pbHomePage_Loading);
        recyclerView = findViewById(R.id.job_list);
        swAvailability = findViewById(R.id.swAvailability);
        scrollViewJobListing = findViewById(R.id.scrollViewJobListing);
        ivNotAvailable  = findViewById(R.id.ivNotAvailable);

        //------------------------------------------------------
        // Initial object status
        //------------------------------------------------------
        String pattern = "MMM yyyy";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern,Locale.US);
        String date = simpleDateFormat.format(new Date());

        //tvMonthlyTransTitle.setText("Total Sale for "+date);

        //------------------------------------------------------
        // On Click event listeners
        //------------------------------------------------------

        //--------------------------------------------------------------
        // Setup GoogleMap : Current user location
        //--------------------------------------------------------------
        google_maps_key =  getResources().getString(R.string.google_maps_key);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5 * 1000); // Update user location every 5 seconds
        locationRequest.setFastestInterval(5*1000);

        //------------------------------------------------------------------
        // Setup request queue
        //------------------------------------------------------------------
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // Instantiate the cache - 1MB cap
        Network network = new BasicNetwork(new HurlStack()); // Set up the network to use HttpURLConnection as the HTTP client.
        requestQueue = new RequestQueue(cache, network); // Instantiate the RequestQueue with the cache and network.
        requestQueue.start(); // Start the queue

        //------------------------------------------------------------------
        // Get data from database
        //------------------------------------------------------------------
        getUserDetails();

        //------------------------------------------------------------------
        // Setup onClick listeners
        //------------------------------------------------------------------
        swAvailability.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (swAvailability.isChecked()) agentAvailability = "T"; else agentAvailability = "F";

                // Update agent availability
                updateAgentAvailability(agentAvailability);
                Log.i("Agent availability :", agentAvailability);
            }
        });
        //------------------------------------------------------------------
        // Setup handler
        //------------------------------------------------------------------
        tvCurrProcess.setText("Initiating job search..");

        mHandler = new Handler();
        mHandler.postDelayed(m_Runnable,5000);

        pbLoading.setVisibility(View.INVISIBLE);

        getTransListing();
        hideSystemUI();
        startLocationUpdates();
        resetBackground();

    }


    private final Runnable m_Runnable = new Runnable()
    {
        public void run()
        {
            //Toast.makeText(HomePageActivity.this,"Refreshing..",Toast.LENGTH_SHORT).show();

            Log.i("Runnable:","Refreshing....");
            getTransListing();
            //findJob();
            HomePageActivity.this.mHandler.postDelayed(m_Runnable, refreshDelaySeconds * 1000);
        }
    };//runnable

    @Override
    public void onRestart(){
        mHandler.postDelayed(m_Runnable,refreshDelaySeconds * 1000);
        super.onRestart();
    }

    @Override
    protected void onPause() {
        mHandler.removeCallbacks(m_Runnable);
        super.onPause();
    }

    @Override
    protected void onStop() {
        mHandler.removeCallbacks(m_Runnable);
        super.onStop();
    }

    @Override
    protected void onResume() {
        mHandler.postDelayed(m_Runnable,refreshDelaySeconds * 1000);
        super.onResume();
    }

    private void stopLocationUpdates() {

        System.out.println("------------ Stopping location update ----------------");
        mFusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private void startLocationUpdates() {
        Toast.makeText(HomePageActivity.this, "Getting your location..", Toast.LENGTH_SHORT).show();
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                System.out.println("------------ Location Permission already granted ----------------");
                mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            } else {
                //Request Location Permission
                System.out.println("------------ Request Location Permission ----------------");
                checkLocationPermission();
            }
        } else {
            System.out.println("------------ Request Location Else ----------------");
            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        }
    }

    private void checkLocationPermission() {

        System.out.println("------------ Check again ----------------");
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
                                ActivityCompat.requestPermissions(HomePageActivity.this,
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


    //--------------------------------------------------------------------------------
    // Location callback
    //--------------------------------------------------------------------------------
    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {

            Log.i("Inside", "--------------Location result callback------------------- ");
            //Toast.makeText(JobInitActivity.this, "Updating location..", Toast.LENGTH_SHORT).show();

            if (locationResult == null) {
                Toast.makeText(HomePageActivity.this, "Unable to get your location", Toast.LENGTH_SHORT).show();
                return;
            }

            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                //The last location in the list is the newest
                Location location = locationList.get(locationList.size() - 1);
                Log.i("MapsActivity", "-------------------- Location: " + location.getLatitude() + " " + location.getLongitude());

                userLoc = new LatLng(location.getLatitude(),location.getLongitude());

                updateAgentLocation();

            }

        }
    };

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

    private void showJobDetails(String jobid) {

        mHandler.removeCallbacks(m_Runnable);

        Intent intent = new Intent(HomePageActivity.this, JobInitActivity.class);
        intent.putExtra("jobID", jobid);
        startActivity(intent);
    }

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                //View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        //| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        //| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        hideSystemUI();
    }


    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.notify_channel_name);
            String description = getString(R.string.notify_channel_desc);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        pbLoading.setVisibility(View.INVISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {

            case R.id.menu_signout:

                //Sign Out
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putString("AGENTID", null);
                editor.putString("SECURE_TOKEN_ID", null);
                editor.apply();

                Intent intent = new Intent(HomePageActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateAgentLocation() {

        // API info
        String url ="https://www.penawarcare.com/public/api.php";

        // Data to be sent
        final Map<String, String> dataparams = new HashMap<>();
        dataparams.put("mode", "api_agent");
        dataparams.put("select", "update_agent_location");
        dataparams.put("agentid", agentid);
        dataparams.put("latitude", Double.toString(userLoc.latitude));
        dataparams.put("longitude", Double.toString(userLoc.longitude));
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

    private void findJob(){

        tvCurrProcess.setText("Searching for jobs..");

        // API info
        String url ="https://www.penawarcare.com/public/api.php";

        // Data to be sent
        final Map<String, String> dataparams = new HashMap<>();
        dataparams.put("mode", "api_agent");
        dataparams.put("select", "find_job");
        dataparams.put("agentid", agentid);
        dataparams.put("securetokenid", securetokenid);

        // On SUCCESS
        Response.Listener<String> responseOK = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                Log.i("Response-2:",response);

                try {


                    JsonObject convertedObject = new Gson().fromJson(response, JsonObject.class);

                    String returnValue = convertedObject.get("value").toString().replace("\"","");
                    String message = convertedObject.get("msg").toString().replace("\"","");

                    if (returnValue.equals("1")) {

                        String jobstatuscode = convertedObject.get("JOB_STATUS_CODE").toString().replace("\"","");
                        String jobid = convertedObject.get("JOB_ID").toString().replace("\"","");

                        if (jobstatuscode.equals("WAITING")) {
                            sendNotification("Job Offer", "You have been offered a job!");
                            showJobDetails(jobid);
                        }


                    } else {

                        // tvCurrProcess.setText("No job offer yet");

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

    private void getUserDetails() {

            // API info
            String url ="https://www.penawarcare.com/public/api.php";

            // Data to be sent
            final Map<String, String> dataparams = new HashMap<>();
            dataparams.put("mode", "api_agent");
            dataparams.put("select", "get_agent_info");
            dataparams.put("agentid", agentid);
            dataparams.put("securetokenid", securetokenid);

            // On SUCCESS
            Response.Listener<String> responseOK = new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {

                    Log.i("Response-2:",response);

                    try {
                        JsonObject convertedObject = new Gson().fromJson(response, JsonObject.class);

                        String returnValue = convertedObject.get("value").toString().replace("\"","");
                        String message = convertedObject.get("msg").toString().replace("\"","");

                        if (returnValue.equals("1")) {

                            // Show details
                            tvUserFullName.setText(convertedObject.get("NAME").toString().replace("\"",""));
                            tvUserEmail.setText(convertedObject.get("EMAIL").toString().replace("\"",""));

                        } else {

                            // Invalid. Show error
                            Snackbar snackbar = Snackbar.make(findViewById(R.id.constraintLayoutHomePage), message, Snackbar.LENGTH_LONG);
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

    private void updateAgentAvailability(String isavailable) {

        // API info
        String url ="https://www.penawarcare.com/public/api.php";

        // Data to be sent
        final Map<String, String> dataparams = new HashMap<>();
        dataparams.put("mode", "api_agent");
        dataparams.put("select", "update_availability");
        dataparams.put("agentid", agentid);
        dataparams.put("securetokenid", securetokenid);
        dataparams.put("isavailable", isavailable);

        // On SUCCESS
        Response.Listener<String> responseOK = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                Log.i("Response-2:",response);

                try {
                    JsonObject convertedObject = new Gson().fromJson(response, JsonObject.class);

                    String returnValue = convertedObject.get("value").toString().replace("\"","");
                    String message = convertedObject.get("msg").toString().replace("\"","");

                    if (returnValue.equals("1")) {

                        // If isavailable is T, show unavailable notice and darkens the listing area
                        resetBackground();
                        // If F, reactivate the listing

                    } else {

                        // Invalid. Show error
                        Snackbar snackbar = Snackbar.make(findViewById(R.id.constraintLayoutHomePage), message, Snackbar.LENGTH_LONG);
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

    public void resetBackground() {

        if (agentAvailability.equals("F")) {
            scrollViewJobListing.setVisibility(View.GONE);
            ivNotAvailable.setVisibility(View.VISIBLE);
            swAvailability.setText(swAvailability.getTextOff().toString());

            tvCurrProcess.setText("Currently off-duty");
        } else {
            scrollViewJobListing.setVisibility(View.VISIBLE);
            ivNotAvailable.setVisibility(View.GONE);
            swAvailability.setText(swAvailability.getTextOn().toString());

            tvCurrProcess.setText("Ready for job assignment");
        }

    }
    public void sendNotification(String title, String desc){

        NotificationManager mNotificationManager;

        // Create an Intent for the activity you want to start
        Intent resultIntent = new Intent(this, HomePageActivity.class);
        // Create the TaskStackBuilder and add the intent, which inflates the back stack
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        // Get the PendingIntent containing the entire back stack
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.pharmacy_icon)
                .setContentIntent(resultPendingIntent)
                .setContentTitle(title)
                .setContentText(desc)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(desc))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // === Removed some obsoletes
        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            String channelId = "Your_channel_id";
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_HIGH);
            mNotificationManager.createNotificationChannel(channel);
            mBuilder.setChannelId(channelId);
        }
*/
        // notificationID allows you to update the notification later on.
        mNotificationManager.notify(0, mBuilder.build());
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Confirmation")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        HomePageActivity.super.finishAffinity();
                        //finish();
                    }

                })
                .setNegativeButton("No", null)
                .show();
    }

    private void getTransListing() {


        System.out.println("Retrieving job listing...");

        // Setup request queue
        RequestQueue requestQueue;
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // Instantiate the cache - 1MB cap
        Network network = new BasicNetwork(new HurlStack()); // Set up the network to use HttpURLConnection as the HTTP client.
        requestQueue = new RequestQueue(cache, network); // Instantiate the RequestQueue with the cache and network.
        requestQueue.start(); // Start the queue

        // API info
        String url ="https://www.penawarcare.com/public/api.php";

        // Data to be sent
        final Map<String, String> dataparams = new HashMap<>();

        dataparams.put("mode", "api_agent");
        dataparams.put("select", "get_job_listing");
        dataparams.put("agentid", agentid);
        dataparams.put("isdone", "F");
        dataparams.put("securetokenid", securetokenid);

        // On SUCCESS
        Response.Listener<String> responseOK = new Response.Listener<String>() {

            @Override
            public void onResponse(String response){

                Log.i("Response:", response);

                JsonObject convertedObject = new Gson().fromJson(response, JsonObject.class);

                String returnValue = convertedObject.get("value").toString();
                String message = convertedObject.get("msg").toString();
                String data = convertedObject.get("listdata").toString();
                String sql = convertedObject.get("sql").toString();

                Log.i("Info:", "Value:" + returnValue + " Data:" + data);

                System.out.println("SQL:" + sql);

                if (returnValue.equals("1")) {

                    // Put data into array
                    Gson gson = new GsonBuilder().serializeNulls().create();
                    Type jobListType = new TypeToken<ArrayList<Job>>(){}.getType();
                    jobList = gson.fromJson(data, jobListType);

                    if (!jobList.isEmpty()) {
                        System.out.println("List is not empty. Size = " + jobList.size());
                        System.out.println("JOB ID [0] = " + jobList.get(0).JOB_ID);

                        Job currJob;

                        for (int i = 0; i < jobList.size(); i++) {
                            currJob = jobList.get(i);
                            System.out.println("JOB ID[" + i + "]-->" + currJob.getJobID() + "|Agent_ID=" + currJob.AGENT_ID);
                            ITEM_MAP.put(currJob.JOB_ID, currJob);
                        }

                    } else {
                        System.out.println("List is empty?");
                        Snackbar snackbar = Snackbar.make(findViewById(R.id.constraintLayoutHomePage), message, Snackbar.LENGTH_LONG);
                        snackbar.setAction("Dismiss", v -> {
                        });
                        snackbar.show();
                    }

                    assert recyclerView != null;
                    setupRecyclerView((RecyclerView) recyclerView);

                    // Sort by distance
                    //billList.sort(Comparator.comparing(Clinic::getDistance));

                } else {

                    System.out.println("Job not found");
                    recyclerView.setAdapter(null);
                    // Invalid. Show error
                /*Snackbar snackbar = Snackbar.make(findViewById(R.id.constraintLayoutHomePage), message, Snackbar.LENGTH_LONG);
                snackbar.setAction("Dismiss", v -> {});
                snackbar.show();
                */
                }

                pbLoading.setVisibility(View.INVISIBLE);

            }
        };

        // On FAIL
        Response.ErrorListener responseError = error -> {
            // TODO: Handle error
            pbLoading.setVisibility(View.INVISIBLE);

        };

        // Compile request data
        StringRequest jsonRequest = new StringRequest (Request.Method.POST, url,responseOK,responseError){

            @Override
            protected Map<String, String> getParams() {
                return dataparams;
            }
        };

        // Add the request to the RequestQueue.
        requestQueue.add(jsonRequest);
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter(this, jobList));
    }

    public static class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final HomePageActivity mParentActivity;
        private final List<Job> mValues;

        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String jobid = view.getTag().toString();

                Context context = view.getContext();
                Intent intent = new Intent(context, JobInitActivity.class);
                intent.putExtra("jobID", jobid);
                //intent.putExtra("NAME", NAME);
                context.startActivity(intent);

                //String NAME = Objects.requireNonNull(mParentActivity.ITEM_MAP.get(REGISTRATION_ID)).CUSTOMER_NAME;

                //System.out.println("Clicked --> "+NAME);

            }
        };

        SimpleItemRecyclerViewAdapter(HomePageActivity parent,
                                      List<Job> items) {
            mValues = items;
            mParentActivity = parent;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.job_list_content, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {

            holder.mView_CustName.setText(mValues.get(position).JOB_CATEGORY_NAME);
            holder.mView_RegDate.setText(mValues.get(position).OFFER_DATETIME);
            holder.mView_ClinicName.setText(mValues.get(position).CLINIC_NAME);
            holder.mView_StageCode.setText(mValues.get(position).JOB_STATUS_DESC);

            holder.itemView.setTag(mValues.get(position).JOB_ID);
            holder.itemView.setOnClickListener(mOnClickListener);
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView mView_CustName,mView_RegDate,mView_ClinicName, mView_StageCode;

            ViewHolder(View view) {
                super(view);
                mView_CustName = view.findViewById(R.id.tvJobList_JobCategoryName);
                mView_RegDate = view.findViewById(R.id.tvJobList_RegDate);
                mView_ClinicName = view.findViewById(R.id.tvJobList_ClinicName);
                mView_StageCode = view.findViewById(R.id.tvJobList_TransStatus);
            }
        }
    }
}
