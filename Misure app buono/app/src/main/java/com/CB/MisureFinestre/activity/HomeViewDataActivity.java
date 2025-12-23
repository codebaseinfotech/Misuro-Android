package com.CB.MisureFinestre.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.CB.MisureFinestre.R;
import com.CB.MisureFinestre.UserDataAdapter;
import com.CB.MisureFinestre.api.ApiInterface;
import com.CB.MisureFinestre.api.RetrofitClient;
import com.CB.MisureFinestre.model.AllCustomerResponse;

import com.CB.MisureFinestre.utils.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeViewDataActivity extends AppCompatActivity {


    ImageView imgBack;
    RecyclerView rvViewData;
    ProgressBar progressBar;
    List<AllCustomerResponse.Customer> userDataModels = new ArrayList<>();
    UserDataAdapter userDataAdapter;
    String TAG = "aaa";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home_view_data);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });


        viewById();
        imgBack.setOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());

        rvViewData.setLayoutManager(new LinearLayoutManager(this));
        userDataAdapter = new UserDataAdapter(userDataModels, this);
        rvViewData.setAdapter(userDataAdapter);

        progressBar.setVisibility(ProgressBar.VISIBLE);

        callCustomerApi();

    }

    private void viewById() {
        imgBack = findViewById(R.id.imgBack);
        rvViewData = findViewById(R.id.rvViewData);
        progressBar = findViewById(R.id.progressBar);
    }

    private void callCustomerApi() {
        PreferenceManager pref = new PreferenceManager(this);
        String token = pref.getToken();
        ApiInterface api = RetrofitClient.getClient().create(ApiInterface.class);
        api.getCustomers("Bearer " + token).enqueue(new Callback<AllCustomerResponse>() {
            @Override
            public void onResponse(Call<AllCustomerResponse> call, Response<AllCustomerResponse> response) {
                progressBar.setVisibility(ProgressBar.GONE);
                Log.e(TAG, "message: " + response.message());
                Log.e(TAG, "code: " + response.code());
                Log.e(TAG, "onResponse: " + response.toString());


//                String status = new Gson().toJson(response.body().status);
//                Log.e(TAG, "get dataerestrrte: " + status);
//
//                if (response.code() == 200){
//                    if (status == "Token is Expired"){
//                        Intent intent = new Intent(HomeViewDataActivity.this, LoginActivity.class);
//                        startActivity(intent);
//                    }else {
//                        if (response.isSuccessful() && response.body() != null) {
//                            Log.e("aaa", "getViewData: " + new Gson().toJson(response.body()));
//                            userDataModels.clear();
//                            userDataModels.addAll(response.body().data);
//                            rvViewData.setVisibility(RecyclerView.VISIBLE);
//                            userDataAdapter.notifyDataSetChanged();
//                        } else {
//                            Log.e("aaa", "Error: " + response.code());
//                        }
//                    }
//                }

                if (response.isSuccessful() && response.body() != null) {
                    String status = response.body().status != null ? response.body().status : "";
                    //  Token expired check
                    if (status.equalsIgnoreCase("Token is Expired")) {
                        // Open Login screen
                        Intent i = new Intent(HomeViewDataActivity.this, LoginActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                        finish();
                        return;
                    }

                    // Normal success flow
                    userDataModels.clear();
                    userDataModels.addAll(response.body().data);
                    rvViewData.setVisibility(RecyclerView.VISIBLE);
                    userDataAdapter.notifyDataSetChanged();
                }

            }

            @Override
            public void onFailure(Call<AllCustomerResponse> call, Throwable t) {
                progressBar.setVisibility(ProgressBar.GONE);
                Log.e("aaa", "Failed: " + t.getMessage());
            }
        });
    }

    @Override
    protected void onStart() {
        callCustomerApi();
        super.onStart();
    }
}