package com.CB.MisureFinestre.activity;

import com.bugfender.sdk.Bugfender;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.CB.MisureFinestre.utils.AppConstants;
import com.CB.MisureFinestre.utils.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;

import com.CB.MisureFinestre.R;
import com.CB.MisureFinestre.api.ApiInterface;
import com.CB.MisureFinestre.api.RetrofitClient;
import com.CB.MisureFinestre.model.LoginResponse;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    ImageView imgBack;
    private EditText edtEmail, edtPassword;
    private Button btnLogin, btnContactUs;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bugfender.d("LIFECYCLE", "LoginActivity - onCreate()");
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().getInsetsController().show(WindowInsets.Type.statusBars());
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

//        PreferenceManager pref = new PreferenceManager(this);
//        if (pref.isLoggedIn()) {
//            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
//            finish();
//            return;
//        }


        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(AppConstants.PLEASE_WAIT_MSG);
        progressDialog.setCancelable(false);

        viewById();
        allButtonClick();

    }

    private void viewById() {
        imgBack = findViewById(R.id.imgBack);
        imgBack.setVisibility(View.GONE);
        edtEmail = findViewById(R.id.edtCompanyCode);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnContactUs = findViewById(R.id.btnContactUs);
    }

    private void allButtonClick() {
        btnLogin.setOnClickListener(view -> {
//            edtEmail.setText("mitesh@gmail.com");
//            edtPassword.setText("123456");
            String strEmail = edtEmail.getText().toString().trim();
            String strPassword = edtPassword.getText().toString().trim();

            if (validateForm(strEmail, strPassword)) {
                apiCallLogin(strEmail, strPassword);
            }
        });
        btnContactUs.setOnClickListener(view -> {
            openWhatsApp(AppConstants.WHATSAPP_NUMBER);
        });
    }

    private boolean validateForm(String strEmail, String strPassword) {
        if (strEmail.isEmpty()) {
            edtEmail.setError("Please enter email");
            edtEmail.requestFocus();
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(strEmail).matches()) {
            edtEmail.setError("Invalid email format");
            edtEmail.requestFocus();
            return false;
        }
        if (strPassword.isEmpty()) {
            edtPassword.setError("Please enter password");
            edtPassword.requestFocus();
            return false;
        }
        if (strPassword.length() < 6) {
            edtPassword.setError("Password must be at least 6 characters");
            edtPassword.requestFocus();
            return false;
        }
        return true;
    }


    private void apiCallLogin(String strEmail, String strPassword) {
        Bugfender.d("API", "Login attempt - Email: " + strEmail);
        progressDialog.show();
        ApiInterface apiInterface = RetrofitClient.getClient().create(ApiInterface.class);
        Call<LoginResponse> call = apiInterface.loginUser(strEmail, strPassword);

        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                progressDialog.dismiss();
                Bugfender.d("API", "Login response code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse data = response.body();
                    if (data.isSuccess()) {
                        Bugfender.d("API", "Login SUCCESS");

                        String token = data.getToken();
                        LoginResponse.User user = data.getUser();

                        Log.e("aaa", " Login onResponse: " + new Gson().toJson(response.body()));

                        PreferenceManager pref = new PreferenceManager(LoginActivity.this);
                        pref.saveToken(token);
                        pref.saveUserId(user.getId());
                        pref.saveEmail(user.getEmail());
                        pref.saveName(user.getName());
                        pref.saveAddress(user.getAddress());
                        pref.savePhone(user.getPhone());
                        pref.saveCompanyCode(user.getCompany_code());
                        pref.saveCompanyName(user.getCompany_name());
//                        pref.saveLoginData(token, user);
                        pref.setFirstLogin(false);
                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                        startActivity(intent);

                    } else {
                        Toast toast = Toast.makeText(LoginActivity.this, "Invalid login", Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                progressDialog.dismiss();
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Unknown error";
                Bugfender.e("API", "Login FAILED: " + errorMsg);
                Log.e("aaa", " Login onFailure: " + errorMsg);
            }
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
//
//            // Check if WhatsApp installed
//            if (intent.resolveActivity(getPackageManager()) != null) {
//                startActivity(intent);
//            } else {
//                Toast toast = Toast.makeText(LoginActivity.this, "WhatsApp not installed", Toast.LENGTH_SHORT);
//                toast.setGravity(Gravity.CENTER, 0, 0);
//                toast.show();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            Toast toast = Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT);
//            toast.setGravity(Gravity.CENTER, 0, 0);
//            toast.show();
//        }
//    }

}