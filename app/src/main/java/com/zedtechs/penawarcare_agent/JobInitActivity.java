package com.zedtechs.penawarcare_agent;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JobInitActivity extends AppCompatActivity {


    String agentid, jobid, mobileno, securetokenid, lastscrupdate = "";
    String jobcatcode,jobstatuscode;
    Boolean routeFound = false, jobDetailsAttained = false, arrivedAtDestination = false, waitFlag = false;
    ImageView ivMapPreview;
    TextView tvJobID, tvJobType, tvJobStatus, tvClinicName, tvDestAddress, tvTotalDistance;
    TextView tvBottomNotice;
    Button btnActionButton;
    String parsedDistance = "", duration = "";
    Double valueDistance = 0.0;
    Double minimumRangeToArrive = 20.0; // in meters
    ProgressBar pbNavLoading;

    private LatLng userLoc;
    private LatLng clinicLoc;
    private LatLng custLoc;

    RequestQueue requestQueue;
    SharedPreferences sharedpreferences;
    Handler mHandler;
    String google_maps_key;

    public static final int refreshDelaySeconds = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_init);

        //------------------------------------------------------
        // Retrieving shared variables
        //------------------------------------------------------
        sharedpreferences = getSharedPreferences(SplashActivity.MyPREFERENCES, Context.MODE_PRIVATE);
        agentid = sharedpreferences.getString("AGENTID","");
        mobileno = sharedpreferences.getString("MOBILE","");
        securetokenid = sharedpreferences.getString("SECURE_TOKEN_ID","");

        Bundle extras = getIntent().getExtras();
        jobid = extras.getString("jobID");

        //------------------------------------------------------
        // Linking resources to variables
        //------------------------------------------------------
        tvJobID = findViewById(R.id.tvJobInit_JobID);
        tvJobType = findViewById(R.id.tvJobInit_JobType);
        tvJobStatus = findViewById(R.id.tvJobInit_JobStatus);
        tvClinicName = findViewById(R.id.tvJobInit_ClinicName);
        tvDestAddress= findViewById(R.id.tvJobInit_DestAddress);
        tvTotalDistance = findViewById(R.id.tvJobInit_TotalDistance);
        ivMapPreview = findViewById(R.id.ivMapPreview);
        btnActionButton = findViewById(R.id.btnJobInit_Action);
        tvBottomNotice = findViewById(R.id.tvJobInit_BottomNotice);
        pbNavLoading = findViewById(R.id.pbJobInit_Loading);

        google_maps_key =  getResources().getString(R.string.google_maps_key);
        //--------------------------------------------------------------
        // Setup request queue
        //--------------------------------------------------------------
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // Instantiate the cache - 1MB cap
        Network network = new BasicNetwork(new HurlStack()); // Set up the network to use HttpURLConnection as the HTTP client.
        requestQueue = new RequestQueue(cache, network); // Instantiate the RequestQueue with the cache and network.
        requestQueue.start(); // Start the queue

        //--------------------------------------------------------------
        // Init resources properties
        //--------------------------------------------------------------
        btnActionButton.setVisibility(View.GONE);

        //------------------------------------------------------------------
        // Setup handler
        //------------------------------------------------------------------
        mHandler = new Handler();
        mHandler.postDelayed(m_Runnable,1000);

    }

    // @Override
    protected void onResume() {

        super.onResume();
    }

    // @Override
    protected void onPause() {
        super.onPause();
    }

    // @Override
    protected void onStop() {
        super.onStop();
    }

    // @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private final Runnable m_Runnable = new Runnable()
    {
        public void run()
        {
            if (waitFlag) {
                pbNavLoading.setVisibility(View.VISIBLE);
                tvBottomNotice.setVisibility(View.GONE);
                btnActionButton.setVisibility(View.GONE);
            } else {
                pbNavLoading.setVisibility(View.GONE);
                getJobDetails();
            }

            JobInitActivity.this.mHandler.postDelayed(m_Runnable, refreshDelaySeconds * 1000);
        }
    };//runnable


    private void getJobDetails(){

        waitFlag = true;
        //Toast.makeText(JobInitActivity.this, "Getting job details", Toast.LENGTH_SHORT).show();

        // API info
        String url ="https://www.penawarcare.com/public/api.php";

        // Data to be sent
        final Map<String, String> dataparams = new HashMap<>();
        dataparams.put("mode", "api_agent");
        dataparams.put("select", "get_job_info");
        dataparams.put("jobid", jobid);
        dataparams.put("agentid", agentid);
        dataparams.put("securetokenid", securetokenid);

        // On SUCCESS
        Response.Listener<String> responseOK = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                Log.i("GetJobDetails:",response);

                try {
                    JsonObject convertedObject = new Gson().fromJson(response, JsonObject.class);

                    String returnValue = convertedObject.get("value").toString().replace("\"","");
                    String message = convertedObject.get("msg").toString().replace("\"","");

                    if (returnValue.equals("1")) {

                        // Process based on Job Type
                        jobstatuscode = convertedObject.get("JOB_STATUS_CODE").toString().replace("\"", "");
                        jobcatcode = convertedObject.get("JOB_CATEGORY_CODE").toString().replace("\"", "");

                        String AGENT_ID = convertedObject.get("AGENT_ID").toString().replace("\"", "");
                        String OFFER_DATETIME = convertedObject.get("OFFER_DATETIME").toString().replace("\"", "");
                        String DELIVERY_ADDRESS = convertedObject.get("DELIVERY_ADDRESS").toString().replace("\"", "");
                        String DELIVERY_ADDRESS2 = convertedObject.get("DELIVERY_ADDRESS2").toString().replace("\"", "");
                        String DELIVERY_CITY = convertedObject.get("DELIVERY_CITY").toString().replace("\"", "");
                        String DELIVERY_POSTCODE = convertedObject.get("DELIVERY_POSTCODE").toString().replace("\"", "");
                        String DELIVERY_STATE = convertedObject.get("DELIVERY_STATE").toString().replace("\"", "");

                        String DEST_ADDRESS = DELIVERY_ADDRESS + " " + DELIVERY_ADDRESS2 + " " + DELIVERY_CITY + " " + DELIVERY_POSTCODE + " " + DELIVERY_STATE;

                        // Show data
                        tvJobID.setText(convertedObject.get("JOB_ID").toString().replace("\"", ""));
                        tvJobType.setText(convertedObject.get("JOB_CATEGORY_NAME").toString().replace("\"", ""));
                        tvJobStatus.setText(convertedObject.get("JOB_STATUS_DESC").toString().replace("\"", ""));
                        tvClinicName.setText(convertedObject.get("CLINIC_NAME").toString().replace("\"", ""));
                        tvDestAddress.setText(DEST_ADDRESS);

                        String agent_latitude = convertedObject.get("AGENT_LATITUDE").toString().replace("\"", "");
                        String agent_longitude = convertedObject.get("AGENT_LONGITUDE").toString().replace("\"", "");
                        String clinic_latitude = convertedObject.get("CLINIC_LATITUDE").toString().replace("\"", "");
                        String clinic_longitude = convertedObject.get("CLINIC_LONGITUDE").toString().replace("\"", "");
                        String cust_latitude = convertedObject.get("DELIVERY_LATITUDE").toString().replace("\"", "");
                        String cust_longitude = convertedObject.get("DELIVERY_LONGITUDE").toString().replace("\"", "");

                        userLoc = new LatLng(Double.parseDouble(agent_latitude), Double.parseDouble(agent_longitude));
                        clinicLoc = new LatLng(Double.parseDouble(clinic_latitude), Double.parseDouble(clinic_longitude));
                        custLoc = new LatLng(Double.parseDouble(cust_latitude), Double.parseDouble(cust_longitude));

                        //---------------------------------------------------------------
                        // Check  distance
                        // If distance is less than predefined range in meters, process which button to show
                        //---------------------------------------------------------------

                        if ((clinicLoc != null) && (custLoc != null)) {

                            if ((jobstatuscode.equals("WAITING")) || (jobstatuscode.equals("READY"))) {
                                getDistance(userLoc, clinicLoc);
                            } else {
                                getDistance(userLoc, custLoc);
                            }

                            arrivedAtDestination = valueDistance < minimumRangeToArrive;
                        }


                        tvBottomNotice.setVisibility(View.GONE);
                        btnActionButton.setVisibility(View.GONE);

                        if (!agentid.equals(AGENT_ID)) {
                            //-------------------------------------------------
                            // Agent no longer offered for this job
                            //-------------------------------------------------
                            tvBottomNotice.setText("Job offer has been revoked");
                            tvBottomNotice.setVisibility(View.VISIBLE);

                        } else {
                            //-------------------------------------------------
                            // Set delivery map preview
                            //-------------------------------------------------
                            String srcImageUrl = "https://maps.googleapis.com/maps/api/staticmap?center=" + cust_latitude + "," + cust_longitude + "&zoom=14&size=600x600&key=" + google_maps_key;

                            new DownloadImageFromInternet(ivMapPreview).execute(srcImageUrl);

                            ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                                    ConstraintLayout.LayoutParams.MATCH_PARENT
                            );

                            ivMapPreview.setLayoutParams(params);

                            waitFlag = false;

                            //-----------------------------------------------------------------------
                            // Stage determination
                            //-----------------------------------------------------------------------

                            switch (jobcatcode) {
                                case "RD01":
                                    //-------------------------------------------------------------
                                    // Stage functions for agent type : RIDER
                                    //-------------------------------------------------------------

                                    if (jobstatuscode.equals("WAITING")) {

                                        btnActionButton.setVisibility(View.VISIBLE);
                                        btnActionButton.setText("Accept Job");
                                        btnActionButton.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                confirmAcceptJob();
                                            }
                                        });

                                    } else if (jobstatuscode.equals("READY")) {

                                        if (arrivedAtDestination) {
                                            //-------------------------------------------------------------
                                            // Agent arrived at clinic for item pickup
                                            //-------------------------------------------------------------
                                            btnActionButton.setVisibility(View.VISIBLE);
                                            btnActionButton.setText("Ready for Pickup");

                                            btnActionButton.setOnClickListener(v -> {
                                                verifyAtClinic();
                                            });
                                        } else {
                                            //-------------------------------------------------------------
                                            // Agent not arrived at clinic yet
                                            //-------------------------------------------------------------
                                            btnActionButton.setVisibility(View.VISIBLE);
                                            btnActionButton.setText("Navigate to Clinic");

                                            btnActionButton.setOnClickListener(v -> {
                                                // Go to google maps
                                                useGoogleMapsApp(clinicLoc);
                                            });

                                        }

                                    } else if (jobstatuscode.equals("WAITITEM")) {

                                        //-------------------------------------------------------------
                                        // Arrived at item collection location
                                        // Waiting for item pickup confirmation from clinic web app
                                        //-------------------------------------------------------------
                                        tvBottomNotice.setText("Waiting for clinic confirmation");
                                        tvBottomNotice.setVisibility(View.VISIBLE);

                                        jobDetailsAttained = false;

                                    } else if (jobstatuscode.equals("PICKUP")) {

                                        if (!arrivedAtDestination) {
                                            //-------------------------------------------------------------
                                            // Item picked up and on its way
                                            //-------------------------------------------------------------
                                            btnActionButton.setVisibility(View.VISIBLE);
                                            btnActionButton.setText("Navigate to Customer");

                                            btnActionButton.setOnClickListener(v -> {
                                                // goto google maps
                                                useGoogleMapsApp(custLoc);
                                            });

                                        } else {
                                            //-------------------------------------------------------------
                                            // Agent arrived at customer location
                                            //-------------------------------------------------------------
                                            btnActionButton.setText("Verify Arrival");
                                            btnActionButton.setVisibility(View.VISIBLE);

                                            btnActionButton.setOnClickListener(v -> {
                                                verifyDelivery();
                                            });
                                        }

                                    } else if (jobstatuscode.equals("DELIVERED")) {
                                        //-------------------------------------------------------------
                                        // Package delivery has already been confirmed by agent
                                        // Waiting for receipt confirmation from patient app
                                        //-------------------------------------------------------------
                                        tvBottomNotice.setText("Waiting for customer's confirmation");
                                        tvBottomNotice.setVisibility(View.VISIBLE);

                                        jobDetailsAttained = false;

                                    } else if (jobstatuscode.equals("RECEIVED")) {

                                        //-------------------------------------------------------------
                                        // All process complete
                                        // Destroy this activity and go back to job details activity
                                        //-------------------------------------------------------------
                                        finish();
                                    } else {
                                        //-------------------------------------------------------------
                                        // Actions for other stages
                                        //-------------------------------------------------------------
                                        btnActionButton.setVisibility(View.GONE);
                                        tvBottomNotice.setText("Loading..");
                                        tvBottomNotice.setVisibility(View.VISIBLE);
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

                    } else {

                            // Invalid. Show error
                            Snackbar snackbar = Snackbar.make(findViewById(R.id.constraintLayoutJobInit), message, Snackbar.LENGTH_LONG);
                            snackbar.setAction("Dismiss", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {

                                }
                            });
                            snackbar.show();

                            finish();
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

    private void confirmAcceptJob(){

        waitFlag = true;
        //Toast.makeText(JobInitActivity.this, "Updating job details", Toast.LENGTH_SHORT).show();

        // API info
        String url ="https://www.penawarcare.com/public/api.php";

        // Data to be sent
        final Map<String, String> dataparams = new HashMap<>();
        dataparams.put("mode", "api_agent");
        dataparams.put("select", "update_job_status");
        dataparams.put("jobid", jobid);
        dataparams.put("jobstatuscode", "READY");
        dataparams.put("securetokenid", securetokenid);

        // On SUCCESS
        Response.Listener<String> responseOK = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                waitFlag = false;
                Log.i("UpdateJobStatus:",response);

                try {
                    JsonObject convertedObject = new Gson().fromJson(response, JsonObject.class);

                    String returnValue = convertedObject.get("value").toString().replace("\"","");
                    String message = convertedObject.get("msg").toString().replace("\"","");

                    if (returnValue.equals("1")) {


                    } else {

                        // Invalid. Show error
                        Snackbar snackbar = Snackbar.make(findViewById(R.id.constraintLayoutJobInit), message, Snackbar.LENGTH_LONG);
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
        btnActionButton.setText("Verifying arrival..");
        Toast.makeText(JobInitActivity.this, "Verifying arrival at item collection location..", Toast.LENGTH_SHORT).show();

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
                        Snackbar snackbar = Snackbar.make(findViewById(R.id.constraintLayoutJobInit), message, Snackbar.LENGTH_LONG);
                        snackbar.setAction("Dismiss", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                            }
                        });
                        snackbar.show();

                        resetNavigation();

                    } else {

                        // Invalid. Show error. An alert dialog would be better
                        Snackbar snackbar = Snackbar.make(findViewById(R.id.constraintLayoutJobInit), message, Snackbar.LENGTH_LONG);
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

    private void resetNavigation(){

        jobDetailsAttained = false;
        routeFound = false;

    }

    private void verifyDelivery(){

        waitFlag = true;
        pbNavLoading.setVisibility(View.VISIBLE);

        jobstatuscode = "DELIVERED";
        tvJobStatus.setText("Verifying item delivery..");

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
                        /*Snackbar snackbar = Snackbar.make(findViewById(R.id.constraintLayoutJobInit), message, Snackbar.LENGTH_LONG);
                        snackbar.setAction("Dismiss", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                            }
                        });
                        snackbar.show();
                        */
                        resetNavigation();

                    } else {

                        String sql = convertedObject.get("sql").toString().replace("\"","");
                        Log.i("Error SQL:",sql);

                        // Invalid. Show error. An alert dialog would be better
                        Snackbar snackbar = Snackbar.make(findViewById(R.id.constraintLayoutJobInit), message, Snackbar.LENGTH_LONG);
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



    private void getDistance(LatLng originLoc,LatLng destLoc){

        //Toast.makeText(JobInitActivity.this, "Updating job details", Toast.LENGTH_SHORT).show();

        Log.i("Origin loc:",originLoc.latitude+","+originLoc.longitude);
        Log.i("Dest loc:",destLoc.latitude+","+destLoc.longitude);
        // API info
        String url ="https://www.penawarcare.com/public/api.php";

        // Data to be sent
        final Map<String, String> dataparams = new HashMap<>();
        dataparams.put("mode", "api_maps");
        dataparams.put("select", "distance");
        dataparams.put("originLoc", originLoc.latitude+","+originLoc.longitude);
        dataparams.put("destLoc", destLoc.latitude+","+destLoc.longitude);
        dataparams.put("securetokenid", securetokenid);

        // On SUCCESS
        Response.Listener<String> responseOK = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.i("Distance_Response:",response);
                JsonObject convertedObject = new Gson().fromJson(response, JsonObject.class);
                try {
                    routeFound = true;
                    processDistance(convertedObject);
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

    private void processDistance(JsonObject obj) throws JSONException {


        String status = obj.get("status").toString().replace("\"", "");
        Log.i("Info:","Route Get Status:"+status);

        if (status.equals("OK")){

            JSONObject jObject;

            Log.i("Route JSON:",obj.toString());

            jObject = new JSONObject(obj.toString());

            Log.i("jObject:",jObject.toString());

            // rows => elements => distance => text/value
            JSONArray rowsArray = jObject.getJSONArray("rows");
            JSONObject elementObj = rowsArray.getJSONObject(0);

            Log.i("elementObj:",elementObj.toString());

            JSONArray elementArray = elementObj.getJSONArray("elements");
            JSONObject element = elementArray.getJSONObject(0);

            Log.i("element:",element.toString());

            status = element.get("status").toString();

            Log.i("status:",status);


            if (status.equals("OK")) {

                parsedDistance = element.getJSONObject("distance").get("text").toString();
                valueDistance = Double.parseDouble(element.getJSONObject("distance").get("value").toString());
                duration = element.getJSONObject("duration").get("text").toString();

                tvTotalDistance.setText(parsedDistance+" ("+duration+")");

            } else {

                tvTotalDistance.setText("----");

            }

            waitFlag = false;

        } else {

            //String error_message = obj.get("status").toString().replace("\"", "");
            // Invalid. Show error
            /*Snackbar snackbar = Snackbar.make(findViewById(R.id.constraintLayoutJobInit), "Retrieving navigation data..", Snackbar.LENGTH_LONG);
            snackbar.setAction("Dismiss", new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
            snackbar.show();*/

        }

    }

    private void beginNavigation(){
        Intent intent = new Intent(JobInitActivity.this, JobNavigation.class);
        intent.putExtra("jobID", jobid);
        startActivity(intent);
    }

    private void useGoogleMapsApp(LatLng destLoc){
        Uri gmmIntentUri = Uri.parse("google.navigation:q="+destLoc.latitude+","+destLoc.longitude);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }
}