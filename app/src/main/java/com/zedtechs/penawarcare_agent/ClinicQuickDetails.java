package com.zedtechs.penawarcare_agent;

import android.content.Context;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

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
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.zedtechs.penawarcare_agent.classes.Clinic;

import java.util.HashMap;
import java.util.Map;

public class ClinicQuickDetails extends Fragment {

    Clinic currClinic = new Clinic();

    TextView tvClinicName, tvClinicAddress, linkViewClinicDetails;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Bundle bundle = getArguments();
        if (bundle != null) {
            currClinic.CLINIC_CODE = bundle.getString("CLINIC_CODE");
            currClinic.NAME = bundle.getString("NAME");
            currClinic.ADDRESS = bundle.getString("ADDRESS");
            System.out.println("--- Clinic Code Found 1: "+currClinic.CLINIC_CODE);
        } else {
            System.out.println("--- Clinic Code NOT FOUND ---");
        }

        return inflater.inflate(R.layout.activity_clinic_quick_details, container, false);
    }



    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        showClinicDetails(currClinic.CLINIC_CODE);

    }

    public boolean onBackPressed() {

        JobNavigation parentActivity = (JobNavigation) getActivity();

        getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();

        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

    }

    private void showClinicDetails(String CLINIC_CODE) {

        //tvClinicCode = (TextView) Objects.requireNonNull(getView()).findViewById(R.id._tvClinicCode);
        //tvClinicCode.setText(CLINIC_CODE);

        Context context = requireActivity().getApplicationContext();

        // Setup request queue
        RequestQueue requestQueue;
        Cache cache = new DiskBasedCache(context.getCacheDir(), 1024 * 1024); // Instantiate the cache - 1MB cap
        Network network = new BasicNetwork(new HurlStack()); // Set up the network to use HttpURLConnection as the HTTP client.
        requestQueue = new RequestQueue(cache, network); // Instantiate the RequestQueue with the cache and network.
        requestQueue.start(); // Start the queue

        // API info
        String url ="https://www.penawarcare.com/public/api.php";

        // Data to be sent
        final Map<String, String> dataparams = new HashMap<>();
        dataparams.put("mode", "api_clinic");
        dataparams.put("select", "get_clinic_data");
        dataparams.put("clinicCode", CLINIC_CODE);

        // On SUCCESS
        Response.Listener<String> responseOK = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                JsonObject convertedObject = new Gson().fromJson(response, JsonObject.class);

                String returnValue = convertedObject.get("value").toString();
                String message = convertedObject.get("msg").toString();

                if (returnValue.equals("1")){

                    // Show clinic details

                    currClinic.NAME = convertedObject.get("NAME").toString().replace("\"","");
                    currClinic.ADDRESS = convertedObject.get("ADDRESS").toString().replace("\"","");

                    //do your stuff for your fragment here
                    tvClinicName = requireView().findViewById(R.id.tvClinicName);
                    tvClinicAddress = requireView().findViewById(R.id.tvClinicAddress);
                    linkViewClinicDetails = requireView().findViewById(R.id.linkViewClinicDetails);

                    tvClinicName.setText(currClinic.NAME);
                    tvClinicAddress.setText(currClinic.ADDRESS);

                    linkViewClinicDetails.setMovementMethod(LinkMovementMethod.getInstance());

                } else {

                    tvClinicName.setText("No data");

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

