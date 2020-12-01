package com.zedtechs.penawarcare_agent;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity {


    private EditText mobileNumber;

    private Button Login;

    //private Session session;//global variable

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //setTheme(R.style.SplashTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mobileNumber = findViewById(R.id.etMobileNumber);
        mobileNumber.setText("+");
        mobileNumber.setSelection(mobileNumber.getText().length());
        mobileNumber.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {

                String phoneFormatted;
                String text = mobileNumber.getText().toString();
                if (text.length() < 1) { // handling backspace in keyboard
                    phoneFormatted = "+";
                    mobileNumber.setText(phoneFormatted);
                }
                mobileNumber.setSelection(mobileNumber.getText().length());
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        Login = findViewById(R.id.btnLogin);
        Login.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {

                String mobile = mobileNumber.getText().toString().trim();

                if(!isValidMobile(mobile)){
                    mobileNumber.setError("Enter a valid mobile");
                    mobileNumber.requestFocus();

                } else {

                    Intent intent = new Intent(MainActivity.this, VerifyPhoneActivity.class);
                    //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.putExtra("MOBILE", mobile);

                    startActivity(intent);
                }


            }
        });



    }


    private boolean isValidMobile(String phone) {
        if(!Pattern.matches("[a-zA-Z]+", phone)) {
            return phone.length() > 6 && phone.length() <= 13;
        }
        return false;
    }




}
