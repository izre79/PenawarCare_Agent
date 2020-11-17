package com.zedtechs.penawarcare_agent;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

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
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.zedtechs.penawarcare_agent.classes.ClinicOpenHours;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClinicDetails extends AppCompatActivity {

    SharedPreferences sharedpreferences;
    String clinicCode,clinicCodeMEDN;
    TextView tvClinicAddress, tvClinicName, tvClinicLicenseNo, tvClinicPhoneNo,
            tvDoctorOnDutyName, tvDoctorOnDutyAPC,tvDoctorOnDutyAPCLabel;
    TableLayout tbOpenHours;
    Button btnRegisterConsultation;

    Boolean doctorOnDuty = false;
    RequestQueue requestQueue;

    List<ClinicOpenHours> openDayList = new ArrayList<>();

    private int REQUEST_REGISTER_CONSULT = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clinic_details);

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        sharedpreferences = getSharedPreferences(SplashActivity.MyPREFERENCES, Context.MODE_PRIVATE);

        Bundle extras = getIntent().getExtras();
        clinicCode = extras.getString("clinicCode");

        tvClinicAddress = findViewById(R.id.tvClinicDetails_Address);
        tvClinicName = findViewById(R.id.tvClinicDetails_ClinicName);
        tvClinicLicenseNo  = findViewById(R.id.tvClinicDetails_LicenseNo);
        tvClinicPhoneNo = findViewById(R.id.tvClinicDetails_PhoneNo);
        tvDoctorOnDutyName = findViewById(R.id.tvDoctorOnDutyName);
        tvDoctorOnDutyAPC  = findViewById(R.id.tvDoctorOnDutyAPC);
        tvDoctorOnDutyAPCLabel = findViewById(R.id.tvDoctorOnDutyAPCLabel);
        btnRegisterConsultation = findViewById(R.id.btnRegisterConsultation);

        btnRegisterConsultation.setVisibility(View.GONE);
        //tvClinicAddress.setText(clinicCode);

        // Setup request queue

        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // Instantiate the cache - 1MB cap
        Network network = new BasicNetwork(new HurlStack()); // Set up the network to use HttpURLConnection as the HTTP client.
        requestQueue = new RequestQueue(cache, network); // Instantiate the RequestQueue with the cache and network.
        requestQueue.start(); // Start the queue

        // Clinic details
        getClinicDetails();

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==REQUEST_REGISTER_CONSULT)
        {
            if (resultCode == RESULT_OK) {
                //Toast.makeText(ClinicDetails.this,"Done registration", Toast.LENGTH_LONG).show();
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
            navigateUpTo(new Intent(this, HomePageActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void getClinicDetails() {

        // API info
        String url ="https://www.penawarcare.com/public/api.php";

        // Data to be sent
        final Map<String, String> dataparams = new HashMap<>();
        dataparams.put("mode", "api_clinic");
        dataparams.put("select", "get_clinic_data");
        dataparams.put("clinicCode", clinicCode);

        // On SUCCESS
        Response.Listener<String> responseOK = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.i("Response:",response);
                JsonObject convertedObject = new Gson().fromJson(response, JsonObject.class);
                showClinicDetails(convertedObject);
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

    private void showClinicDetails(JsonObject obj) {

        String returnValue = obj.get("value").toString().replace("\"", "");
        String message = obj.get("msg").toString().replace("\"", "");
        Log.i("Info:","Value:"+returnValue+" Msg:"+message);

        if (returnValue.equals("1")){

            ImageView imgClinicPic = findViewById(R.id.imgClinicPic);
            LinearLayout lytClinicPic = findViewById(R.id.lytClinicPic);

            // Show clinic details
            tvClinicName.setText(obj.get("NAME").toString().replace("\"",""));
            String clinicLicenseNo = obj.get("MOH_LICENSE_NO").toString().replace("\"","");

            if (!clinicLicenseNo.isEmpty()&&!clinicLicenseNo.equals("null")) {
                tvClinicLicenseNo.setText(clinicLicenseNo);
            } else {
                tvClinicLicenseNo.setText("----");
            }
            tvClinicAddress.setText(obj.get("ADDRESS").toString().replace("\"",""));
            tvClinicPhoneNo.setText(obj.get("PHONE").toString().replace("\"",""));

            clinicCodeMEDN = obj.get("MEDINET_CLINIC_CODE").toString().replace("\"","");

                    //-------------------------------------------------------------------
            // Need to use MediNET API to get Doctor-On-Duty details
            //-------------------------------------------------------------------
            getDoctorOnDutyDetails();

            //-------------------------------------------------------------------
            // Clinic opening hours
            //-------------------------------------------------------------------
            //getClinicOpenHours();

            // Show clinic image
            String clinicImageUrl = obj.get("IMAGE_FILENAME").toString().replace("\"","");

            Log.i("clinicImageUrl","'"+clinicImageUrl+"'");
            if ((!clinicImageUrl.isEmpty())&&(!clinicImageUrl.equals("null"))) {
                String urlHeader = "https://www.penawarcare.com/pics/clinic_images/";
                new DownloadImageFromInternet(imgClinicPic).execute(urlHeader + clinicImageUrl);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                );

                imgClinicPic.setLayoutParams(params);
                imgClinicPic.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                imgClinicPic.setImageResource(R.drawable.icon_clinic);
                imgClinicPic.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            }


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

    }

    private void getDoctorOnDutyDetails() {

        //btnRegisterConsultation.setVisibility(View.GONE);

        // API info
        String url ="https://www.medinet.com.my/penawarcare/api.php";

        // Data to be sent
        final Map<String, String> dataparams = new HashMap<>();
        dataparams.put("mode", "api_clinic");
        dataparams.put("select", "get_clinic_data");
        dataparams.put("clinicCode", clinicCodeMEDN);

        // On SUCCESS
        Response.Listener<String> responseOK = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                JsonObject obj = new Gson().fromJson(response, JsonObject.class);

                String DOCTOR_NAME = obj.get("DOCTOR_NAME_1").toString().replace("\"","");
                String DOCTOR_APC = obj.get("DOCTOR_APC_1").toString().replace("\"","");

                if (DOCTOR_NAME.equals("")){
                    tvDoctorOnDutyName.setText("No doctor on-duty");
                    doctorOnDuty = false;
                } else {
                    tvDoctorOnDutyName.setText("DR. "+DOCTOR_NAME);
                    doctorOnDuty = true;
                    btnRegisterConsultation.setVisibility(View.VISIBLE);
                }

                if (DOCTOR_APC.equals("")||DOCTOR_APC.equals("null")) {
                    tvDoctorOnDutyAPC.setText("n/a");
                } else{
                    tvDoctorOnDutyAPC.setText(DOCTOR_APC+"/2020");
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

    private void getClinicOpenHours() {

        // API info
        String url ="https://www.medinet.com.my/penawarcare/api.php";

        // Data to be sent
        final Map<String, String> dataparams = new HashMap<>();
        dataparams.put("mode", "api_clinic");
        dataparams.put("select", "get_clinic_openhours");
        dataparams.put("clinicCode", clinicCode);

        // On SUCCESS
        Response.Listener<String> responseOK = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                JsonObject convertedObject = new Gson().fromJson(response, JsonObject.class);
                showClinicOpenHours(convertedObject);
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


    private void showClinicOpenHours(JsonObject obj) {

        String returnValue = obj.get("value").toString().replace("\"", "");
        String message = obj.get("msg").toString().replace("\"", "");
        String data = obj.get("data").toString();
        Log.i("Info:", "Value:" + returnValue + " Msg:" + message);

        if (returnValue.equals("1")) {

            // Put data into array
            Type ClinicOpenHoursType = new TypeToken<ArrayList<ClinicOpenHours>>(){}.getType();
            openDayList = new Gson().fromJson(data, ClinicOpenHoursType);

            if (!openDayList.isEmpty()) {
                System.out.println("Open hours data not empty");

                // Add a marker for each clinic
                for (int i = 0; i < openDayList.size(); i++) {
                    String currDay = openDayList.get(i).DAY;
                    String currString = openDayList.get(i).OPEN_HOURS;

                    if ((currDay != null) && (currString != null)) {
                        System.out.println("DAY:" + currDay + "| OPEN:" + currString);

                        // Insert row into table
                        TableRow tr_head = new TableRow(this);
                        tr_head.setId(i);
                        //tr_head.setBackgroundColor(Color.GRAY);        // part1
                        tr_head.setLayoutParams(new ConstraintLayout.LayoutParams(
                                ConstraintLayout.LayoutParams.MATCH_PARENT,
                                ConstraintLayout.LayoutParams.WRAP_CONTENT));

                        TextView tableLabel = new TextView(this);
                        tableLabel.setId(i+20);
                        tableLabel.setText(currDay+" : ");
                        tableLabel.setTextColor(Color.BLUE);          // part2
                        tableLabel.setPadding(5, 5, 10, 5);
                        tr_head.addView(tableLabel);// add the column to the table row here

                        TextView tableData = new TextView(this);    // part3
                        tableData.setId(i+30);// define id that must be unique
                        tableData.setText(currString); // set the text for the header
                        tableData.setTextColor(Color.BLACK); // set the color
                        tableData.setPadding(5, 5, 5, 5); // set the padding (if required)
                        tr_head.addView(tableData); // add the column to the table row here

                        tbOpenHours.addView(tr_head, new TableLayout.LayoutParams(
                                ConstraintLayout.LayoutParams.WRAP_CONTENT,                    //part4
                                ConstraintLayout.LayoutParams.WRAP_CONTENT));
                    }
                }


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

        }
    }
}
