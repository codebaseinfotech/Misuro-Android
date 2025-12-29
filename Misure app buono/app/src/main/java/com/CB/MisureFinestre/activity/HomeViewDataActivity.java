package com.CB.MisureFinestre.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import com.CB.MisureFinestre.offline.AppDatabase;
import com.CB.MisureFinestre.offline.OfflineCustomerEntity;
import com.CB.MisureFinestre.offline.customerGetData.CustomerCacheEntity;
import com.CB.MisureFinestre.utils.NetworkUtil;
import com.CB.MisureFinestre.utils.PreferenceManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeViewDataActivity extends AppCompatActivity {

    private ImageView imgBack;
    private RecyclerView rvViewData;
    private ProgressBar progressBar;
    private UserDataAdapter userDataAdapter;
    private AppDatabase db;
    private static final String TAG = "HomeViewData";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_view_data);

        db = AppDatabase.get(this);
        initView();
    }

    private void initView() {
        imgBack = findViewById(R.id.imgBack);
        rvViewData = findViewById(R.id.rvViewData);
        progressBar = findViewById(R.id.progressBar);
        rvViewData.setLayoutManager(new LinearLayoutManager(this));
        imgBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    @Override
    protected void onResume() {
        super.onResume();
        callCustomerApi();
    }

    // API CALL
    private void callCustomerApi() {

        if (!NetworkUtil.isConnected(this)) {
            loadFromLocalDb();
            progressBar.setVisibility(View.GONE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        String token = "Bearer " + new PreferenceManager(this).getToken();

        ApiInterface api = RetrofitClient.getClient().create(ApiInterface.class);
        api.getCustomers(token).enqueue(new Callback<AllCustomerResponse>() {
            @Override
            public void onResponse(Call<AllCustomerResponse> call, Response<AllCustomerResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (!response.isSuccessful() || response.body() == null) {
                    loadFromLocalDb();
                    return;
                }

                // 🔐 Token expired
                if ("Token is Expired".equalsIgnoreCase(response.body().status)) {
                    Intent i = new Intent(HomeViewDataActivity.this, LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    finish();
                    return;
                }

                // ✅ Save API data to cache table
                List<CustomerCacheEntity> cacheList = new ArrayList<>();

                for (AllCustomerResponse.Customer c : response.body().data) {
                    CustomerCacheEntity e = new CustomerCacheEntity();
                    e.id = c.id;
                    e.user_id = c.user_id;
                    e.pdf_url = c.pdf_url;
                    e.customer = c.customer;
                    e.location = c.location;
                    e.date = c.date;
                    e.isOffline = false;
                    cacheList.add(e);
                }

                db.customerCacheDao().clear();
                db.customerCacheDao().insertAll(cacheList);
                loadFromLocalDb();
            }

            @Override
            public void onFailure(Call<AllCustomerResponse> call, Throwable t) {
                Log.e(TAG, "API error: " + t.getMessage());
                progressBar.setVisibility(View.GONE);
                loadFromLocalDb();
            }
        });
    }

    private void loadFromLocalDb() {

        List<CustomerCacheEntity> finalList = new ArrayList<>();

        Gson gson = new Gson();

        // ================== 1️⃣ OFFLINE DATA (TOP) ==================
        List<OfflineCustomerEntity> offlineList = db.offlineDao().getPending();
        Log.e("OFFLINE_COUNT", "Count = " + offlineList.size());

        // newest offline first
        Collections.sort(offlineList, (a, b) -> b.id - a.id);

        for (OfflineCustomerEntity o : offlineList) {
            try {
                JsonObject root = gson.fromJson(o.customerJson, JsonObject.class);
                if (root == null || !root.has("customer")) continue;

                JsonObject cust = root.getAsJsonObject("customer");

                CustomerCacheEntity offlineItem = new CustomerCacheEntity();
                offlineItem.id = -o.id;          // 🔴 negative unique id
                offlineItem.user_id = 0;
                offlineItem.customer = cust.has("customer") ? cust.get("customer").getAsString() : "";
                offlineItem.location = cust.has("location") ? cust.get("location").getAsString() : "";
                offlineItem.date = cust.has("date") ? cust.get("date").getAsString() : "";
                offlineItem.pdf_url = cust.has("pdf_url") ? cust.get("pdf_url").getAsString() : "";
                offlineItem.isOffline = true;

                // 🔥 ADD AT TOP
                finalList.add(offlineItem);

            } catch (Exception e) {
                Log.e(TAG, "Offline parse error", e);
            }
        }

        // ================== 2️⃣ ONLINE API CACHE DATA ==================
        List<CustomerCacheEntity> apiList = db.customerCacheDao().getAll(); // already DESC
        for (CustomerCacheEntity c : apiList) {
            c.isOffline = false;
            finalList.add(c);
        }

        // ================== 3️⃣ SET ADAPTER ==================
        if (userDataAdapter == null) {
            userDataAdapter = new UserDataAdapter(finalList, this);
            rvViewData.setAdapter(userDataAdapter);
        } else {
            userDataAdapter.updateList(finalList);
        }
    }


//    private void loadFromLocalDb() {
//
//        Map<Integer, CustomerCacheEntity> map = new LinkedHashMap<>();
//
//        // 1️⃣ API CACHE DATA
//        List<CustomerCacheEntity> apiList = db.customerCacheDao().getAll();
//        for (CustomerCacheEntity c : apiList) {
//            map.put(c.id, c);
//        }
//
//        // 2️⃣ OFFLINE DATA
//        List<OfflineCustomerEntity> offlineList = db.offlineDao().getPending();
//        Log.e("OFFLINE_COUNT", "Count = " + offlineList.size());
//
//        Gson gson = new Gson();
//
//        Collections.reverse(offlineList);
//        for (OfflineCustomerEntity o : offlineList) {
//
//            try {
//                JsonObject root = gson.fromJson(o.customerJson, JsonObject.class);
//                if (root == null || !root.has("customer")) continue;
//                JsonObject cust = root.getAsJsonObject("customer");
//
//                // 🔐 SAFE READ
//                String name = cust.has("customer") ? cust.get("customer").getAsString() : "";
//                String location = cust.has("location") ? cust.get("location").getAsString() : "";
//                String date = cust.has("date") ? cust.get("date").getAsString() : "";
//
//                CustomerCacheEntity fake = new CustomerCacheEntity();
//                fake.id = -o.id;          // 🔴 NEGATIVE ID (UNIQUE)
//                fake.user_id = 0;
//                fake.customer = name;
//                fake.location = location;
//                fake.date = date;
//                fake.isOffline = true;
//                map.put(fake.id, fake);
//
//            } catch (Exception e) {
//                Log.e(TAG, "Offline parse error: " + e.getMessage());
//            }
//        }
//
//        List<CustomerCacheEntity> finalList = new ArrayList<>(map.values());
//
//        if (userDataAdapter == null) {
//            userDataAdapter = new UserDataAdapter(finalList, this);
//            rvViewData.setAdapter(userDataAdapter);
//        } else {
//            userDataAdapter.updateList(finalList);
//        }
//    }


//    private void loadFromLocalDb() {
//
//        Map<Integer, CustomerCacheEntity> map = new LinkedHashMap<>();
//
//        // 1️⃣ API CACHE
//        for (CustomerCacheEntity c :
//                db.customerCacheDao().getAll()) {
//            map.put(c.id, c);
//        }
//
//        // 2️⃣ OFFLINE DATA
//        List<OfflineCustomerEntity> offlineList =
//                db.offlineDao().getPending();
//
//        Gson gson = new Gson();
//
//        for (OfflineCustomerEntity o : offlineList) {
//            try {
//                JsonObject root =
//                        gson.fromJson(o.customerJson, JsonObject.class);
//
//                JsonObject cust =
//                        root.getAsJsonObject("customer");
//
//                CustomerCacheEntity fake =
//                        new CustomerCacheEntity();
//
//                fake.id = -o.id;        // 🔴 NEGATIVE
//                fake.user_id = 0;
//                fake.customer = cust.get("customer").getAsString();
//                fake.location = cust.get("location").getAsString();
//                fake.date = cust.get("date").getAsString();
//                fake.isOffline = true;
//
//                map.put(fake.id, fake); // 🔴 NO DUPLICATE
//
//            } catch (Exception ignored) {
//            }
//        }
//
//        List<CustomerCacheEntity> finalList =
//                new ArrayList<>(map.values());
//
//        if (userDataAdapter == null) {
//            userDataAdapter =
//                    new UserDataAdapter(finalList, this);
//            rvViewData.setAdapter(userDataAdapter);
//        } else {
//            userDataAdapter.updateList(finalList);
//        }
//    }
}

//    // LOAD DATA (API CACHE + OFFLINE)
//    private void loadFromLocalDb() {
//        List<CustomerCacheEntity> finalList = new ArrayList<>();
//
//        // 1️⃣ Cached API customers
//        finalList.addAll(db.customerCacheDao().getAll());
//
//        // 2️⃣ Offline customers (not synced yet)
//        List<OfflineCustomerEntity> offlineList = db.offlineDao().getPending();
//
//        Gson gson = new Gson();
//        for (OfflineCustomerEntity o : offlineList) {
//            try {
//                JsonObject root = gson.fromJson(o.customerJson, JsonObject.class);
//                JsonObject cust = root.getAsJsonObject("customer");
//
//                CustomerCacheEntity fake = new CustomerCacheEntity();
//                fake.id = -o.id; // 🔴 temp negative id
//                fake.user_id = 0; // 🔴 offline placeholder
//                fake.customer = cust.get("customer").getAsString();
//                fake.location = cust.get("location").getAsString();
//                fake.date = cust.get("date").getAsString();
//                fake.isOffline = true;
//
//                // 🔝 show offline on top
//                finalList.add(0, fake);
//
//            } catch (Exception e) {
//                Log.e(TAG, "Offline parse error", e);
//            }
//        }
//
//        if (userDataAdapter == null) {
//            userDataAdapter = new UserDataAdapter(finalList, this);
//            rvViewData.setAdapter(userDataAdapter);
//        } else {
//            userDataAdapter.updateList(finalList); // 🔹 adapter method
//        }
//    }


//}


//package com.CB.MisureFinestre.activity;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.View;
//import android.widget.ImageView;
//import android.widget.ProgressBar;
//
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.CB.MisureFinestre.R;
//import com.CB.MisureFinestre.UserDataAdapter;
//import com.CB.MisureFinestre.api.ApiInterface;
//import com.CB.MisureFinestre.api.RetrofitClient;
//import com.CB.MisureFinestre.model.AllCustomerResponse;
//
//import com.CB.MisureFinestre.offline.AppDatabase;
//import com.CB.MisureFinestre.offline.OfflineCustomerEntity;
//import com.CB.MisureFinestre.offline.customerGetData.CustomerCacheEntity;
//import com.CB.MisureFinestre.utils.NetworkUtil;
//import com.CB.MisureFinestre.utils.PreferenceManager;
//import com.google.gson.Gson;
//import com.google.gson.JsonObject;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import retrofit2.Call;
//import retrofit2.Callback;
//import retrofit2.Response;
//
//public class HomeViewDataActivity extends AppCompatActivity {
//
//
//    ImageView imgBack;
//    RecyclerView rvViewData;
//    ProgressBar progressBar;
//    List<AllCustomerResponse.Customer> userDataModels = new ArrayList<>();
//    UserDataAdapter userDataAdapter;
//    String TAG = "aaa";
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_home_view_data);
//
//        viewById();
//        callCustomerApi();
//    }
//
//    private void viewById() {
//        imgBack = findViewById(R.id.imgBack);
//        imgBack.setOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());
//        rvViewData = findViewById(R.id.rvViewData);
//        progressBar = findViewById(R.id.progressBar);
//    }
//
////    private void callCustomerApi() {
////        progressBar.setVisibility(ProgressBar.VISIBLE);
////        PreferenceManager pref = new PreferenceManager(this);
////        String token = pref.getToken();
////        ApiInterface api = RetrofitClient.getClient().create(ApiInterface.class);
////        api.getCustomers("Bearer " + token).enqueue(new Callback<AllCustomerResponse>() {
////            @Override
////            public void onResponse(Call<AllCustomerResponse> call, Response<AllCustomerResponse> response) {
////                progressBar.setVisibility(ProgressBar.GONE);
////                Log.e(TAG, "message: " + response.message());
////                Log.e(TAG, "code: " + response.code());
////                Log.e(TAG, "onResponse: " + response.toString());
////
////                if (response.isSuccessful() && response.body() != null) {
////                    String status = response.body().status != null ? response.body().status : "";
////                    //  Token expired check
////                    if (status.equalsIgnoreCase("Token is Expired")) {
////                        // Open Login screen
////                        Intent i = new Intent(HomeViewDataActivity.this, LoginActivity.class);
////                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
////                        startActivity(i);
////                        finish();
////                        return;
////                    }
////
////                    // Normal success flow
////                    userDataModels.clear();
////                    userDataModels.addAll(response.body().data);
////                    rvViewData.setVisibility(RecyclerView.VISIBLE);
////                    rvViewData.setLayoutManager(new LinearLayoutManager(HomeViewDataActivity.this));
////                    userDataAdapter = new UserDataAdapter(userDataModels, HomeViewDataActivity.this);
////                    rvViewData.setAdapter(userDataAdapter);
////                    userDataAdapter.notifyDataSetChanged();
////                }
////            }
////
////            @Override
////            public void onFailure(Call<AllCustomerResponse> call, Throwable t) {
////                progressBar.setVisibility(ProgressBar.GONE);
////                Log.e("aaa", "Failed: " + t.getMessage());
////            }
////        });
////    }
//
//    @Override
//    protected void onStart() {
//        callCustomerApi();
//        super.onStart();
//    }
//
//    private void callCustomerApi() {
//
//        if (!NetworkUtil.isConnected(this)) {
//            loadFromLocalDb();
//            return;
//        }
//
//        progressBar.setVisibility(View.VISIBLE);
//
//        String token = "Bearer " + new PreferenceManager(this).getToken();
//        ApiInterface api = RetrofitClient.getClient().create(ApiInterface.class);
//
//        api.getCustomers(token).enqueue(new Callback<AllCustomerResponse>() {
//            @Override
//            public void onResponse(Call<AllCustomerResponse> call,
//                                   Response<AllCustomerResponse> response) {
//
//                progressBar.setVisibility(View.GONE);
//
//                if (response.isSuccessful() && response.body() != null) {
//
//                    List<CustomerCacheEntity> cacheList = new ArrayList<>();
//
//                    for (AllCustomerResponse.Customer c : response.body().data) {
//
//                        CustomerCacheEntity e = new CustomerCacheEntity();
//                        e.id = c.id;
//                        e.customer = c.customer;
//                        e.location = c.location;
//                        e.date = c.date;
//                        e.isOffline = false;
//
//                        cacheList.add(e);
//                    }
//
//                    AppDatabase db = AppDatabase.get(HomeViewDataActivity.this);
//                    db.customerCacheDao().clear();
//                    db.customerCacheDao().insertAll(cacheList);
//
//                    loadFromLocalDb();
//                }
//            }
//
//            @Override
//            public void onFailure(Call<AllCustomerResponse> call, Throwable t) {
//                progressBar.setVisibility(View.GONE);
//                loadFromLocalDb();
//            }
//        });
//    }
//
//
//    private void loadFromLocalDb() {
//
//        AppDatabase db = AppDatabase.get(this);
//
//        List<CustomerCacheEntity> finalList = new ArrayList<>();
//
//        // 1️⃣ API cached customers
//        finalList.addAll(db.customerCacheDao().getAll());
//
//        // 2️⃣ Offline added customers
//        List<OfflineCustomerEntity> offline = db.offlineDao().getPending();
//
//        for (OfflineCustomerEntity o : offline) {
//
//            JsonObject root = new Gson().fromJson(o.customerJson, JsonObject.class);
//            JsonObject cust = root.getAsJsonObject("customer");
//
//            CustomerCacheEntity fake = new CustomerCacheEntity();
//            fake.id = -o.id; // 🔴 TEMP ID
//            fake.customer = cust.get("customer").getAsString();
//            fake.location = cust.get("location").getAsString();
//            fake.date = cust.get("date").getAsString();
//            fake.isOffline = true;
//
//            finalList.add(0, fake); // top ma show
//        }
//
//        userDataAdapter = new UserDataAdapter(finalList, this);
//        rvViewData.setLayoutManager(new LinearLayoutManager(this));
//        rvViewData.setAdapter(userDataAdapter);
//    }
//
//
//}
//
