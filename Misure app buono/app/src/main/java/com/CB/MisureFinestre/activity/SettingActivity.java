package com.CB.MisureFinestre.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.CB.MisureFinestre.R;
import com.CB.MisureFinestre.api.ApiInterface;
import com.CB.MisureFinestre.api.RetrofitClient;
import com.CB.MisureFinestre.utils.AppConstants;
import com.CB.MisureFinestre.utils.PreferenceManager;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingActivity extends AppCompatActivity {

    ImageView imgBack;
    Button btnContactUs, btnLogout;
    EditText edtCompanyCode, edtCompanyName, edtEmail, edtNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_setting);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });

        viewById();
        getUserData();
        allBtnClick();
    }

    private void viewById() {
        imgBack = findViewById(R.id.imgBack);
        edtCompanyCode = findViewById(R.id.edtCompanyCode);
        edtCompanyName = findViewById(R.id.edtCompanyName);
        edtEmail = findViewById(R.id.edtEmail);
        edtNumber = findViewById(R.id.edtNumber);
        btnContactUs = findViewById(R.id.btnContactUs);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void getUserData() {
        PreferenceManager pref = new PreferenceManager(this);
        edtCompanyCode.setText(pref.getCompanyCode());
        edtCompanyName.setText(pref.getCompanyName());
        edtEmail.setText(pref.getEmail());
        edtNumber.setText(pref.getPhone());
    }
    private void allBtnClick() {
        imgBack.setOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());
        btnLogout.setOnClickListener(view -> showLogoutDialog());
        btnContactUs.setOnClickListener(view -> {
            openWhatsApp(AppConstants.WHATSAPP_NUMBER);
        });
    }

    private void openWhatsApp(String number) {
        try {
            String url = "https://wa.me/" + number;  // WhatsApp API format
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            intent.setPackage("com.whatsapp"); // Force open only in WhatsApp
            startActivity(intent);
        } catch (Exception e) {
            // If WhatsApp is not installed
            intentToPlayStore();
        }
    }

    private void intentToPlayStore() {
        Intent i = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=com.whatsapp"));
        startActivity(i);
    }

//    private void openWhatsApp(String number) {
//        try {
//            // Remove any spaces
//            number = number.replace(" ", "");
//            // WhatsApp URI format
//            String url = "https://wa.me/" + number;
//            Intent intent = new Intent(Intent.ACTION_VIEW);
//            intent.setData(android.net.Uri.parse(url));
//            intent.setPackage("com.whatsapp");
//            // Check if WhatsApp installed
//            if (intent.resolveActivity(getPackageManager()) != null) {
//                startActivity(intent);
//            } else {
//                Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//        }
//    }

    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout");
        builder.setMessage("sei sicuro che vuoi uscire?");
        builder.setCancelable(true);

        builder.setPositiveButton("si", (dialog, which) -> {
            apiLogout();
        });
        builder.setNegativeButton("no", (dialog, which) -> dialog.dismiss());
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void apiLogout() {

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(AppConstants.PLEASE_WAIT_MSG);
        progressDialog.setCancelable(false);
        progressDialog.show();

        PreferenceManager pref = new PreferenceManager(this);
        String token = "Bearer " + pref.getToken();
        Log.e("aaa", "apiLogout: " + token);

        ApiInterface api = RetrofitClient.getClient().create(ApiInterface.class);
        Call<JsonObject> call = api.logout(token);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    boolean success = response.body().get("success").getAsBoolean();
                    if (success) {
                        String message = response.message();
                        Toast toast = Toast.makeText(SettingActivity.this, "Logout Success", Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                        // Clear saved login token
//                        RetrofitClient.clearToken(SettingActivity.this);
//                        pref.logout();
                        // Move to login screen
//                        Intent intent = new Intent(SettingActivity.this, LoginActivity.class);
//                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
//                                Intent.FLAG_ACTIVITY_NEW_TASK |
//                                Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                        startActivity(intent);
//                        finish();


                        PreferenceManager pref = new PreferenceManager(SettingActivity.this);
                        pref.logout();   // clears everything
                        pref.setFirstLogin(true); // optional if you want to show login again

                        startActivity(new Intent(SettingActivity.this, LoginActivity.class));
                        finish();



                        Log.e("aaa", "logout onResponse: "+ response.toString() );
                    } else {
//                        Toast.makeText(SettingActivity.this, "Logout failed!", Toast.LENGTH_SHORT).show();
                        Log.e("aaa", "logout onResponse: failed" );
                    }
                } else {
//                    Toast.makeText(SettingActivity.this, "Server error", Toast.LENGTH_SHORT).show();
                    Log.e("aaa", "logout onResponse: Server error"  );
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                progressDialog.dismiss();
                Log.e("aaa", "onFailure: " + t.getMessage());
            }
        });
    }



}