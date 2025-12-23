package com.CB.MisureFinestre.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.CB.MisureFinestre.model.LoginResponse;
import com.CB.MisureFinestre.model.ProfileResponse;

public class PreferenceManager {
    private static final String PREF_NAME = "IMEASURE_APP";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_COMPANY_NAME = "company_name";
    private static final String KEY_COMPANY_CODE = "company_code";
    private static final String KEY_FIRST_LOGIN = "first_login";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public PreferenceManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }


    public void saveToken(String token) {
        editor.putString(KEY_TOKEN, token);
        editor.apply();
    }

    public String getToken() {
        return sharedPreferences.getString(KEY_TOKEN, "");
    }

    // USER ID
    public void saveUserId(int id) {
        editor.putInt(KEY_USER_ID, id);
        editor.apply();
    }

    public int getUserId() {
        return sharedPreferences.getInt(KEY_USER_ID, 0);
    }

    // NAME
    public void saveName(String name) {
        editor.putString(KEY_NAME, name);
        editor.apply();
    }

    public String getName() {
        return sharedPreferences.getString(KEY_NAME, "");
    }

    // EMAIL
    public void saveEmail(String email) {
        editor.putString(KEY_EMAIL, email);
        editor.apply();
    }

    public String getEmail() {
        return sharedPreferences.getString(KEY_EMAIL, "");
    }

    // PHONE
    public void savePhone(String phone) {
        editor.putString(KEY_PHONE, phone);
        editor.apply();
    }

    public String getPhone() {
        return sharedPreferences.getString(KEY_PHONE, "");
    }

    // ADDRESS
    public void saveAddress(String address) {
        editor.putString(KEY_ADDRESS, address);
        editor.apply();
    }

    public String getAddress() {
        return sharedPreferences.getString(KEY_ADDRESS, "");
    }

    // COMPANY NAME
    public void saveCompanyName(String name) {
        editor.putString(KEY_COMPANY_NAME, name);
        editor.apply();
    }

    public String getCompanyName() {
        return sharedPreferences.getString(KEY_COMPANY_NAME, "");
    }

    // COMPANY CODE
    public void saveCompanyCode(String code) {
        editor.putString(KEY_COMPANY_CODE, code);
        editor.apply();
    }

    public String getCompanyCode() {
        return sharedPreferences.getString(KEY_COMPANY_CODE, "");
    }


    public void setFirstLogin(boolean isFirst) {
        editor.putBoolean(KEY_FIRST_LOGIN, isFirst);
        editor.apply();
    }

    public boolean isFirstLogin() {
        return sharedPreferences.getBoolean(KEY_FIRST_LOGIN, true);
        // Default true = first time
    }

    // ---------------- CHECK LOGIN ----------------
    public boolean isLoggedIn() {
        return !sharedPreferences.getString(KEY_TOKEN, "").isEmpty();
    }

    // ---------------- CLEAR DATA ----------------
    public void logout() {
        editor.clear();
        editor.apply();
    }









}