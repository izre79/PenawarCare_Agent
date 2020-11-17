package com.zedtechs.penawarcare_agent;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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
import com.google.gson.JsonSyntaxException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private String mobileno, agentid, securetokenid, ip_address, android_ver;
    SharedPreferences sharedpreferences;
    RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //----------------------------------------------------
        // Variables declaration
        //----------------------------------------------------
        TextView tvMobileNo;
        Button btnLogin, btnReset;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //----------------------------------------------------
        // Getting saved info for quick login
        //----------------------------------------------------
        sharedpreferences = getSharedPreferences(SplashActivity.MyPREFERENCES, Context.MODE_PRIVATE);
        mobileno = sharedpreferences.getString("MOBILE","");
        agentid = sharedpreferences.getString("AGENTID","");
        securetokenid = sharedpreferences.getString("SECURE_TOKEN_ID","");

        //----------------------------------------------------
        // Getting IP Address
        //----------------------------------------------------
        ip_address = getIpAddress(this);

        //----------------------------------------------------
        // Getting android version
        //----------------------------------------------------
        String release = Build.VERSION.RELEASE;
        int sdkVersion = Build.VERSION.SDK_INT;
        android_ver = "Android SDK: " + sdkVersion + " (" + release +")";

        //----------------------------------------------------
        // Linking resources
        //----------------------------------------------------
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        tvMobileNo = findViewById(R.id.tvMobileNumber);
        btnLogin = findViewById(R.id.btnLogin);
        btnReset = findViewById(R.id.btnResetMobile);

        String displayed_text = mobileno;
        tvMobileNo.setText(displayed_text);

        //----------------------------------------------------
        // OnClicks setup
        //----------------------------------------------------

        btnLogin.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                processLinking();
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {

                new AlertDialog.Builder(LoginActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Resetting Mobile Number")
                        .setMessage("Are you sure to reset your mobile number?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {


                                SharedPreferences.Editor editor = sharedpreferences.edit();
                                editor.putString("MOBILE", null);
                                editor.apply();

                                // Valid. Continue login
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            }

                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });


        //----------------------------------------------------
        // Init quick login if saved info exists
        //----------------------------------------------------

        if (!securetokenid.isEmpty()) {
            processLinking();
        }
    }

    public static String getIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                .getSystemService(WIFI_SERVICE);

        String ipAddress = intToInetAddress(wifiManager.getDhcpInfo().ipAddress).toString();

        ipAddress = ipAddress.substring(1);

        return ipAddress;
    }

    public static InetAddress intToInetAddress(int hostAddress) {
        byte[] addressBytes = { (byte)(0xff & hostAddress),
                (byte)(0xff & (hostAddress >> 8)),
                (byte)(0xff & (hostAddress >> 16)),
                (byte)(0xff & (hostAddress >> 24)) };

        try {
            return InetAddress.getByAddress(addressBytes);
        } catch (UnknownHostException e) {
            throw new AssertionError();
        }
    }

    private void processLinking() {

        Log.i("Processing","Linking...");

        // Setup request queue
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // Instantiate the cache - 1MB cap
        Network network = new BasicNetwork(new HurlStack()); // Set up the network to use HttpURLConnection as the HTTP client.
        requestQueue = new RequestQueue(cache, network); // Instantiate the RequestQueue with the cache and network.
        requestQueue.start(); // Start the queue

        // API info
        String url ="https://www.penawarcare.com/public/api.php";

        // Filtering input
        String username = etUsername.getText().toString();
        String password = etPassword.getText().toString();

        // Data to be sent
        final Map<String, String> dataparams = new HashMap<>();
        dataparams.put("mode", "api_agent");
        dataparams.put("select", "validate_login");
        dataparams.put("agentid", agentid);
        dataparams.put("username", username);
        dataparams.put("password", password);
        dataparams.put("mobile_number", mobileno);
        dataparams.put("securetokenid", securetokenid);
        dataparams.put("firstlogin", "T");
        dataparams.put("ip_address", ip_address);
        dataparams.put("android_ver", android_ver);

        // To create catch function for SSL error due to unknown CA
        // TO popup dialog box asking user whether to install PenawarCare Cert
        // If yes, open browser to file and install
        // If no, do nothing

        // On SUCCESS
        Response.Listener<String> responseOK = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                Log.i("Response:",response);

                try{

                    JsonObject convertedObject = new Gson().fromJson(response, JsonObject.class);

                    String returnValue = convertedObject.get("value").toString();
                    String message = convertedObject.get("msg").toString();

                    if (returnValue.equals("1")){

                        //---------------------------------------------------------
                        // User exists in Database. Continue logging in
                        //---------------------------------------------------------
                        agentid = convertedObject.get("agentid").toString().replace("\"","");
                        securetokenid = convertedObject.get("securetokenid").toString().replace("\"","");

                        //---------------------------------------------------------
                        // Continue login
                        //---------------------------------------------------------
                        Intent intent = new Intent(LoginActivity.this, HomePageActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                        SharedPreferences.Editor editor = sharedpreferences.edit();
                        editor.putString("AGENTID", agentid);
                        editor.putString("SECURE_TOKEN_ID", securetokenid);
                        editor.apply();

                        startActivity(intent);

                    } else {
                        // Invalid. Show error
                        Snackbar snackbar = Snackbar.make(findViewById(R.id.constraintLayoutPatientLink), message.replace("\"",""), Snackbar.LENGTH_LONG);
                        snackbar.setAction("Dismiss", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                            }
                        });
                        snackbar.show();
                    }
                } catch (JsonSyntaxException ex) {
                    // Error catched
                    Toast.makeText(getApplicationContext(), "ID not found.", Toast.LENGTH_SHORT).show();

                }

            }

        };

        // On FAIL
        Response.ErrorListener responseError = new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {

                Log.i("Error",error.toString());
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
