package com.CB.MisureFinestre.activity;

import com.bugfender.sdk.Bugfender;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.CB.MisureFinestre.R;
import com.CB.MisureFinestre.api.ApiInterface;
import com.CB.MisureFinestre.api.RetrofitClient;
import com.CB.MisureFinestre.model.LoginResponse;
import com.CB.MisureFinestre.model.ProfileResponse;
import com.CB.MisureFinestre.utils.PreferenceManager;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    ImageView imgBack, imgSetting;
    private Button btnAdd, btnView;
    String TAG = "aaa";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bugfender.d("LIFECYCLE", "HomeActivity - onCreate()");
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
        apiHome();
        viewById();
        allButtonClick();
    }

    private void apiHome() {


        PreferenceManager pref = new PreferenceManager(this);
        String bearerToken = "Bearer " + pref.getToken();  // make sure token is set
        Log.e(TAG, "apiHome: "+bearerToken );
        Log.e(TAG, "getToken: "+pref.getToken() );
        Log.e(TAG, "getPhone: "+pref.getPhone() );
        Log.e(TAG, "getEmail: "+pref.getEmail() );
        Log.e(TAG, "getName: "+pref.getName() );
        Log.e(TAG, "getCompanyName: "+pref.getCompanyName() );
        Log.e(TAG, "getCompanyCode: "+pref.getCompanyCode() );
        Log.e(TAG, "getAddress: "+pref.getAddress() );
        Log.e(TAG, "getUserId: "+pref.getUserId() );



        ApiInterface api = RetrofitClient.getClient().create(ApiInterface.class);
        Call<ProfileResponse> call = api.getUserProfile(bearerToken);
        call.enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(Call<ProfileResponse> call, Response<ProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ProfileResponse profile = response.body();
                    ProfileResponse.User user = profile.user;
                    if (profile.success) {

                        PreferenceManager pref = new PreferenceManager(HomeActivity.this);
                        pref.saveUserId(user.id);
                        pref.saveEmail(user.email);
                        pref.saveName(user.name);
                        pref.saveAddress(user.address);
                        pref.savePhone(user.phone);
                        pref.saveCompanyCode(user.company_code);
                        pref.saveCompanyName(user.company_name);
                        Log.e(TAG, "onResponse: "+ user.address );
                        Log.e(TAG, "User Name: " + profile.user.name);

//                        pref.saveProfileData(profile.user);
//                        pref.saveProfileData(profile.user);
                    }
                } else {
                    Log.e(TAG, "API Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ProfileResponse> call, Throwable t) {
                Log.e(TAG, "Failure: " + t.getMessage());
            }
        });


    }

    private void viewById() {
        imgBack = findViewById(R.id.imgBack);
        imgBack.setVisibility(View.GONE);
        imgSetting = findViewById(R.id.imgSetting);
        imgSetting.setVisibility(View.VISIBLE);
        btnAdd = findViewById(R.id.btnAdd);
        btnView = findViewById(R.id.btnView);
    }

    private void allButtonClick() {
        imgSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HomeActivity.this, SettingActivity.class);
                startActivity(intent);
            }
        });
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HomeActivity.this, AddFormOneActivity.class);
                startActivity(intent);
            }
        });
        btnView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HomeActivity.this, HomeViewDataActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        apiHome();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }

}