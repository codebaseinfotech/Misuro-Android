package com.CB.MisureFinestre.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.CB.MisureFinestre.R;
import com.CB.MisureFinestre.UserDataAdapter;
import com.CB.MisureFinestre.ViewPieceDataAdapter;
import com.CB.MisureFinestre.api.ApiInterface;
import com.CB.MisureFinestre.api.RetrofitClient;
import com.CB.MisureFinestre.model.CustomerData;
import com.CB.MisureFinestre.model.CustomerShowResponse;
import com.CB.MisureFinestre.model.UserDataModel;
import com.CB.MisureFinestre.utils.AppConstants;
import com.CB.MisureFinestre.utils.PreferenceManager;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class YouSeeDataActivity extends AppCompatActivity {

    ImageView imgBack;
    EditText edtClient, edtDate, edtDelivery, edtLocation, edtGlassWindow, edtColor, edtCremonese,
            edtPersian, edtFlat, edtSpacers, edtRollerShutter, edtDumpster, edtMosquitoNet, edtMarbleBase;
    private RecyclerView rvPieceData;
    int customerId;
    String token = "";
    ViewPieceDataAdapter viewPieceDataAdapter;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_you_see_data);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });

        viewById();
        apiCustomerShowData();

    }


    private void viewById() {
        PreferenceManager pref = new PreferenceManager(this);
        token = pref.getToken();
        customerId = getIntent().getIntExtra("CUSTOMER_ID", -1);

        imgBack = findViewById(R.id.imgBack);
        imgBack.setOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());
        edtClient = findViewById(R.id.edtClient);
        edtDate = findViewById(R.id.edtDate);
        edtDelivery = findViewById(R.id.edtDelivery);
        edtLocation = findViewById(R.id.edtLocation);
        edtGlassWindow = findViewById(R.id.edtGlassWindow);
        edtColor = findViewById(R.id.edtColor);
        edtCremonese = findViewById(R.id.edtCremonese);
        edtPersian = findViewById(R.id.edtPersian);
        edtFlat = findViewById(R.id.edtFlat);
        edtSpacers = findViewById(R.id.edtSpacers);
        edtRollerShutter = findViewById(R.id.edtRollerShutter);
        edtDumpster = findViewById(R.id.edtDumpster);
        edtMosquitoNet = findViewById(R.id.edtMosquitoNet);
        edtMarbleBase = findViewById(R.id.edtMarbleBase);
        rvPieceData = findViewById(R.id.rvPieceData);
    }


    private void apiCustomerShowData() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(AppConstants.PLEASE_WAIT_MSG);
        progressDialog.setCancelable(false);
        progressDialog.show();
        ApiInterface api = RetrofitClient.getClient().create(ApiInterface.class);
        Map<String, Integer> body = new HashMap<>();
        body.put("id", customerId);

        Call<CustomerShowResponse> call = api.getCustomerDetail("Bearer " + token, body);
        call.enqueue(new Callback<CustomerShowResponse>() {
            @Override
            public void onResponse(Call<CustomerShowResponse> call, Response<CustomerShowResponse> response) {
                progressDialog.dismiss();
                Log.e("aaa", "CustomerShowResponse: " + new Gson().toJson(response.body()));

                if (!response.isSuccessful()) {
                    return;

                }

                CustomerShowResponse res = response.body();
                if (res == null || !res.success) {
//                    Toast.makeText(YouSeeDataActivity.this, "No Data Found", Toast.LENGTH_SHORT).show();
                    return;
                }

                CustomerData d = res.data;

                edtClient.setText(d.customer);
                if (d.date == null || d.date.isEmpty()) {
                    edtDate.setText("");
                } else {
                    try {
                        edtDate.setText(d.date.substring(0, 10));
                    } catch (Exception e) {
                        edtDate.setText(d.date); // fallback
                    }
                }
                edtDelivery.setText(d.delivery);
                edtLocation.setText(d.location);
                edtGlassWindow.setText(d.glass_window);
                edtColor.setText(d.color);
                edtCremonese.setText(d.cremonese);
                edtPersian.setText(d.persian);
                edtFlat.setText(d.flat);
                edtSpacers.setText(safeInt(d.spacers));
//                  edtSpacers.setText(String.valueOf(d.spacers));
//                edtSpacers.setText(d.spacers);
                edtRollerShutter.setText(d.roller_shutter);
                edtDumpster.setText(d.dumpster);
                edtMosquitoNet.setText(d.mosquito_net);
                edtMarbleBase.setText(d.marble_base);

                // ---------- SET PIECE LIST ----------
                if (d.pieces != null && !d.pieces.isEmpty()) {
                    Log.e("aaa", "onResponse: "+ new Gson().toJson(response.body()) );

                    LinearLayoutManager layoutManager = new LinearLayoutManager(YouSeeDataActivity.this);
                    layoutManager.setReverseLayout(true);
                    layoutManager.setStackFromEnd(true);
                    rvPieceData.setLayoutManager(layoutManager);
                    viewPieceDataAdapter = new ViewPieceDataAdapter(YouSeeDataActivity.this, d.pieces);
                    rvPieceData.setAdapter(viewPieceDataAdapter);

                }
            }

            @Override
            public void onFailure(Call<CustomerShowResponse> call, Throwable t) {
                progressDialog.dismiss();
                Toast toast = Toast.makeText(YouSeeDataActivity.this, "Failed: " + t.getMessage(), Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                Log.e("aaa", "Error: " + t.getMessage());
            }
        });
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String safeInt(Integer value) {
        return value == null ? "" : String.valueOf(value);
    }


}
