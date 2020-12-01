package com.zedtechs.penawarcare_agent;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

import com.zedtechs.penawarcare_agent.util.NetworkFunctions;

public class SplashActivity extends AppCompatActivity {

    private TextView tvMessage;
    private ImageView logo;
    private static final int splashTimeOut=2000;
    private Handler h;
    private Runnable r;

    String  agentid, mobile, securetokenid, ip_address, android_ver;

    SharedPreferences sharedpreferences;
    RequestQueue requestQueue;

    public static final String MyPREFERENCES = "MyPrefs" ;
    public static final String Mobile = "MOBILE";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        //----------------------------------------------------------------
        // Setup resources
        //----------------------------------------------------------------
        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        mobile = sharedpreferences.getString("MOBILE","");
        agentid = sharedpreferences.getString("AGENTID","");
        securetokenid = sharedpreferences.getString("SECURE_TOKEN_ID","");

        tvMessage = findViewById(R.id.tvSplash_Message);
        ip_address = NetworkFunctions.getIPAddress(true);
        android_ver = Build.VERSION.RELEASE;

        //----------------------------------------------------------------
        // Setup request queue
        //----------------------------------------------------------------
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // Instantiate the cache - 1MB cap
        Network network = new BasicNetwork(new HurlStack()); // Set up the network to use HttpURLConnection as the HTTP client.
        requestQueue = new RequestQueue(cache, network); // Instantiate the RequestQueue with the cache and network.
        requestQueue.start(); // Start the queue

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkUpdates();
            }
        },splashTimeOut);

        Log.w("Splash", "Fetching FCM registration now...");
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w("Splash", "Fetching FCM registration token failed", task.getException());
                            Toast.makeText(SplashActivity.this, "Fetching FCM registration token failed", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Get new FCM registration token
                        String token = task.getResult();

                        // Log and toast
                        Log.d("Splash", token);
                        Toast.makeText(SplashActivity.this, "Fetching FCM registration token success", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
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
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    // Shows the system bars by removing all the flags
// except for the ones that make the content appear under the system bars.
    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    private void checkUpdates(){

        try {
            PackageInfo pInfo = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0);
            String versionName = pInfo.versionName;
            int versionCode = pInfo.versionCode;

            System.out.println("This app version :"+versionName+"("+versionCode+")");

            // API info
            String url ="https://www.penawarcare.com/public/api.php";

            // Data to be sent
            final Map<String, String> dataparams = new HashMap<>();
            dataparams.put("mode", "api_login");
            dataparams.put("select", "check_version");

            // On SUCCESS
            Response.Listener<String> responseOK = new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {

                    JsonObject convertedObject = new Gson().fromJson(response, JsonObject.class);

                    String returnValue = convertedObject.get("value").toString();
                    String message = convertedObject.get("msg").toString();
                    Integer latestVersionCode = Integer.parseInt(convertedObject.get("AGENT_ANDROID_VERSION").toString().replace("\"", ""));
                    System.out.println("Latest  app version :"+latestVersionCode.toString());

                    if (latestVersionCode<=versionCode) {
                        // If user is using the latest version then continue
                        checkSession();
                    } else {
                        new AlertDialog.Builder(SplashActivity.this)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setTitle("New updated version required")
                                .setMessage("In order to continue using this app, an update is required. Do you want to update now?")
                                .setPositiveButton("Update Now", new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object
                                        try {
                                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                                        } catch (android.content.ActivityNotFoundException anfe) {
                                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                                        }
                                        finish();
                                        System.exit(0);
                                    }

                                })
                                .setNegativeButton("Exit", new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                        System.exit(0);
                                    }

                                })
                                .show();
                    }
                }
            };

            // On FAIL
            Response.ErrorListener responseError = new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {

                    System.out.println("Error checking updates :"+error.toString());
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


        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void checkSession() {

        tvMessage.setText("Connecting to server..");

        // Check if MOBILE_NO & USERID has been registered in session

        if (!mobile.isEmpty()) {
            if (!agentid.isEmpty()) {
                // Both Mobile number and userid has been registered
                // Check authenticity in database
                //Toast.makeText(SplashActivity.this,"Checking Login Data", Toast.LENGTH_LONG).show();
                checkLoginData();

            } else {
                // Mobile number already registered but patregno has not been registered
                // Send to LinkPatient
                Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        } else {
            // Send to Mobile Number registration
            Intent intent = new Intent(SplashActivity.this,MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }

    }

    private void checkLoginData() {

        tvMessage.setText("Checking login data..");

        // API info
        String url ="https://www.penawarcare.com/public/api.php";

        // Data to be sent
        final Map<String, String> dataparams = new HashMap<>();
        dataparams.put("mode", "api_agent");
        dataparams.put("select", "validate_login");
        dataparams.put("agentid", agentid);
        dataparams.put("securetokenid", securetokenid);
        dataparams.put("ip_address", ip_address);
        dataparams.put("android_ver", android_ver);

        // On SUCCESS
        Response.Listener<String> responseOK = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                Log.i("Response:",response);
                JsonObject convertedObject = new Gson().fromJson(response, JsonObject.class);

                String returnValue = convertedObject.get("value").toString();
                String message = convertedObject.get("msg").toString();

                if (returnValue.equals("1")){

                    SharedPreferences.Editor editor = sharedpreferences.edit();
                    editor.putString("LAST_SCR_UPDATE", "");
                    editor.apply();

                    // If link exists, send to HomePage
                    Intent intent = new Intent(SplashActivity.this, HomePageActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    //overridePendingTransition(R.anim.fade_in, R.anim.fade_out);


                } else {
                    // Mobile number already registered but patregno has not been registered
                    // Send to LinkPatient
                    Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
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
