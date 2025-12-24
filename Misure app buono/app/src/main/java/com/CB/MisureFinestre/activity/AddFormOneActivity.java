package com.CB.MisureFinestre.activity;

import com.CB.MisureFinestre.offline.AppDatabase;
import com.CB.MisureFinestre.offline.OfflineCustomerEntity;
import com.bugfender.sdk.Bugfender;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.view.MotionEvent;
import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.CB.MisureFinestre.R;
import com.CB.MisureFinestre.api.ApiInterface;
import com.CB.MisureFinestre.api.RetrofitClient;
import com.CB.MisureFinestre.model.CustomerData;
import com.CB.MisureFinestre.model.CustomerShowResponse;
import com.CB.MisureFinestre.model.PiecesModel;
import com.CB.MisureFinestre.utils.AppConstants;
import com.CB.MisureFinestre.utils.PreferenceManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;

public class AddFormOneActivity extends AppCompatActivity {

    ImageView imgBack,  btnSave;
//    Button btnSave;
    LinearLayout piecesContainer;
    private EditText edtClient, edtDate, edtDelivery, edtLocation, edtGlassWindow, edtColor, edtCremonese,
            edtPersian, edtFlat, edtSpacers, edtRollerShutter, edtDumpster, edtMosquitoNet, edtMarbleBase;


    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> editImageLauncher;

    private LinearLayout currentPhotoContainer;
    private ArrayList<Bitmap> currentImageList;
    private int currentImageIndex = -1;

    // map each piece view -> its image list
    private final Map<View, ArrayList<Bitmap>> pieceImagesMap = new HashMap<>();

    // created customer id returned by add_customer API
    private String createdCustomerId = "";
    private ProgressDialog progressDialog;

    // counters for piece upload completion
    private AtomicInteger piecesToUpload = new AtomicInteger(0);
    private AtomicInteger piecesUploaded = new AtomicInteger(0);
    private AtomicInteger piecesFailed = new AtomicInteger(0);

    int customerId, userId;
    String strDoublicat, token;
    String TAG = "aaa";
    private List<PiecesModel> allPiecesFromServer = new ArrayList<>();
    private EditText lastFocusedEditText;
    private InputMethodManager inputMethodManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bugfender.d("LIFECYCLE", "AddFormOneActivity - onCreate() started");

        setContentView(R.layout.activity_add_form_one);
        Bugfender.d("LIFECYCLE", "AddFormOneActivity - Layout inflated");

        // Window soft input mode is configured in AndroidManifest.xml
        // Removed redundant setSoftInputMode() call to prevent IME confusion
        Bugfender.d("INPUT_CONFIG", "Window soft input mode: adjustResize|stateHidden (from manifest)");

        PreferenceManager pref = new PreferenceManager(this);
        token = pref.getToken();
        Bugfender.d("AUTH", "Token retrieved: " + (token != null && !token.isEmpty() ? "Valid" : "Invalid"));

        // Initialize InputMethodManager for handwriting input fixes
        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        Bugfender.d("IME", "InputMethodManager initialized for handwriting support");

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(AppConstants.PLEASE_WAIT_MSG);
        progressDialog.setCancelable(false);

        viewById();


        customerId = getIntent().getIntExtra("CUSTOMER_ID", -1);
        userId = getIntent().getIntExtra("USER_ID", -1);
        strDoublicat = getIntent().getStringExtra("DOUBLICAT");

        Bugfender.d("DATA", String.format("Intent data - customerId: %d, userId: %d, doublicat: %s",
                customerId, userId, strDoublicat));

        if (customerId != -1) {
            // EDIT MODE
            Bugfender.d("MODE", "EDIT MODE - Loading customer " + customerId);
            loadCustomerDetails(customerId);
        } else if (Objects.equals(strDoublicat, "doublicat")) {
            Bugfender.d("MODE", "DUPLICATE MODE - Loading customer " + customerId);
            loadCustomerDetails(customerId);
        } else {
            Bugfender.d("MODE", "NEW CUSTOMER MODE");
        }

        allButtonClick();
        setupLaunchers();

        // Add 20 pieces
        Bugfender.d("VIEW_INFLATION", "Starting to inflate 20 piece sections");
        long startTime = System.currentTimeMillis();
        for (int i = 1; i <= 20; i++) {
            addPieceSection(i);
        }
        long endTime = System.currentTimeMillis();
        Bugfender.d("VIEW_INFLATION", String.format("20 pieces inflated in %d ms", (endTime - startTime)));

        Bugfender.d("LIFECYCLE", "AddFormOneActivity - onCreate() completed");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Bugfender.d("LIFECYCLE", "AddFormOneActivity - onStart()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Bugfender.d("LIFECYCLE", "AddFormOneActivity - onResume()");
        Bugfender.d("INPUT_STATE", "Activity resumed - input should be ready");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Bugfender.d("LIFECYCLE", "AddFormOneActivity - onPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Bugfender.d("LIFECYCLE", "AddFormOneActivity - onStop()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Bugfender.d("LIFECYCLE", "AddFormOneActivity - onDestroy()");
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // Track stylus/pen input events for debugging handwriting issues
        if (ev.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS ||
                ev.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER) {

            String action = "";
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    action = "DOWN";
                    Bugfender.d("STYLUS", "Stylus DOWN - Starting handwriting stroke");
                    break;
                case MotionEvent.ACTION_MOVE:
                    action = "MOVE";
                    break;
                case MotionEvent.ACTION_UP:
                    action = "UP";
                    Bugfender.d("STYLUS", "Stylus UP - Handwriting stroke complete");
                    break;
                case MotionEvent.ACTION_CANCEL:
                    action = "CANCEL";
                    Bugfender.w("STYLUS", "Stylus CANCEL - Stroke interrupted!");
                    break;
            }

            String focusInfo = (getCurrentFocus() != null) ?
                    getCurrentFocus().getClass().getSimpleName() + " (ID: " + getCurrentFocus().getId() + ")" :
                    "NO FOCUS";

            if (!action.equals("MOVE")) { // Don't spam logs with MOVE events
                Bugfender.d("STYLUS", "Stylus " + action + " on " + focusInfo);
            }
        }

        return super.dispatchTouchEvent(ev);
    }

    private void viewById() {
        imgBack = findViewById(R.id.imgBack);
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
        piecesContainer = findViewById(R.id.piecesContainer);
        btnSave = findViewById(R.id.btnSave);
        btnSave.setVisibility(View.VISIBLE);

        // Setup handwriting input fixes for all main form EditTexts
        setupHandwritingInputFix(edtClient);
        setupHandwritingInputFix(edtDate);
        setupHandwritingInputFix(edtDelivery);
        setupHandwritingInputFix(edtLocation);
        setupHandwritingInputFix(edtGlassWindow);
        setupHandwritingInputFix(edtColor);
        setupHandwritingInputFix(edtCremonese);
        setupHandwritingInputFix(edtPersian);
        setupHandwritingInputFix(edtFlat);
        setupHandwritingInputFix(edtSpacers);
        setupHandwritingInputFix(edtRollerShutter);
        setupHandwritingInputFix(edtDumpster);
        setupHandwritingInputFix(edtMosquitoNet);
        setupHandwritingInputFix(edtMarbleBase);
        Bugfender.d("IME", "Handwriting input fix applied to 14 main form EditTexts");
    }

    /**
     * Setup handwriting input fix for EditText
     * Restarts IME input connection when EditText gains focus
     * Prevents parent ScrollView from stealing stylus touch events
     * This fixes handwriting recognition breaking after 2-3 field changes
     */
    private void setupHandwritingInputFix(EditText editText) {
        if (editText == null) return;

        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                lastFocusedEditText = (EditText) v;
                Bugfender.d("IME", "EditText focused (ID: " + v.getId() + ") - Restarting IME for handwriting");

                // Force IME to restart its input connection
                // This fixes handwriting recognition breaking after multiple field changes
                v.post(() -> {
                    if (inputMethodManager != null) {
                        inputMethodManager.restartInput(v);
                        Bugfender.d("IME", "IME input restarted successfully");
                    }
                });
            } else {
                Bugfender.d("IME", "EditText lost focus (ID: " + v.getId() + ")");
            }
        });

        // Prevent parent ScrollView from intercepting stylus touch events
        editText.setOnTouchListener((v, event) -> {
            // Check if this is a stylus/pen event
            boolean isStylus = (event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS ||
                    event.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER);

            if (isStylus) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Request parent not to intercept stylus events
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        Bugfender.d("TOUCH", "EditText (ID: " + v.getId() + ") requested parent to not intercept stylus events");

                        // Ensure EditText gets focus
                        if (!v.hasFocus()) {
                            v.requestFocus();
                            Bugfender.d("FOCUS", "EditText (ID: " + v.getId() + ") focus requested on stylus DOWN");
                        }

                        // CRITICAL FIX: Hide keyboard/IME to prevent handwriting mode from starting
                        // This prevents MIUI's handwriting system from cancelling stylus events
                        if (inputMethodManager != null) {
                            inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
                            Bugfender.d("IME", "Hidden IME to prevent handwriting mode interference");
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        // Allow parent to intercept events again
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        Bugfender.d("TOUCH", "EditText (ID: " + v.getId() + ") allowed parent to intercept events again");
                        break;
                }
            }

            // Return false to allow EditText to handle the event normally
            return false;
        });
    }

    private void allButtonClick() {
        Bugfender.d("SETUP", "Setting up button click listeners");

        imgBack.setOnClickListener(view -> {
            Bugfender.d("NAVIGATION", "Back button clicked");
            getOnBackPressedDispatcher().onBackPressed();
        });

        // Configure date picker fields - prevent keyboard but allow focus
        edtDate.setInputType(android.text.InputType.TYPE_NULL);
        edtDate.setFocusableInTouchMode(true);
        edtDelivery.setInputType(android.text.InputType.TYPE_NULL);
        edtDelivery.setFocusableInTouchMode(true);
        Bugfender.d("INPUT_CONFIG", "Date picker fields configured with TYPE_NULL input");

        edtDate.setOnClickListener(v -> {
            Bugfender.d("INPUT", "Date field clicked - opening date picker");
            openDatePicker(edtDate);
        });
        edtDelivery.setOnClickListener(v -> {
            Bugfender.d("INPUT", "Delivery field clicked - opening date picker");
            openDatePickerDelivery(edtDelivery);
        });

        btnSave.setOnClickListener(view -> {
            Bugfender.d("ACTION", "Save button clicked");
            if (customerId == -1) {
                if (hasInternet()) {
                    apiAddCustomer();   // EXISTING FLOW
                } else {
                    saveOfflineCustomer();  // NEW
                }
                Bugfender.d("API", "Calling apiAddCustomer() - New customer");
//                apiAddCustomer();   // NEW CUSTOMER
                Log.e("aaa", "allButtonClick:  AddCustomer();");
            } else if (Objects.equals(strDoublicat, "doublicat")) {
                if (hasInternet()) {
                    apiAddCustomer();   // EXISTING FLOW
                } else {
                    saveOfflineCustomer();  // NEW
                }
                Bugfender.d("API", "Calling apiAddCustomer() - Duplicate customer");
//                apiAddCustomer();
                Log.e("aaa", "strDoublicat: " + strDoublicat);
            } else {
                Bugfender.d("API", "Calling apiUpdateCustomer() - Edit existing");
                apiUpdateCustomer(); // EDIT CUSTOMER
                Log.e("aaa", "allButtonClick:  UpdateCustomer();");
            }
        });

        Bugfender.d("SETUP", "Button click listeners setup completed");
    }


    public boolean hasInternet() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    private void openDatePickerDelivery(EditText edtDelivery) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                AddFormOneActivity.this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    int finalMonth = selectedMonth + 1;
                    String formattedMonth = (finalMonth < 10 ? "0" : "") + finalMonth;
                    String formattedDay = (selectedDay < 10 ? "0" : "") + selectedDay;
                    String date = selectedYear + "-" + formattedMonth + "-" + formattedDay;
                    edtDelivery.setText(date);
                },
                year, month, day
        );
        datePickerDialog.getDatePicker().setMinDate(Calendar.getInstance().getTimeInMillis());
        datePickerDialog.show();
    }

    private void openDatePicker(EditText editText) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                AddFormOneActivity.this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    int finalMonth = selectedMonth + 1;
                    String formattedMonth = (finalMonth < 10 ? "0" : "") + finalMonth;
                    String formattedDay = (selectedDay < 10 ? "0" : "") + selectedDay;
                    String date = selectedYear + "-" + formattedMonth + "-" + formattedDay;
                    editText.setText(date);
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    private void apiAddCustomer() {
        Bugfender.d("API", "apiAddCustomer() started");
        progressDialog.show();
        PreferenceManager pref = new PreferenceManager(this);
        String token = pref.getToken();
        Bugfender.d("API", "Token for API call: " + (token != null && !token.isEmpty() ? "Valid" : "Invalid"));

        ApiInterface api = RetrofitClient.getClient().create(ApiInterface.class);
        JsonObject body = new JsonObject();
        body.addProperty("customer", edtClient.getText().toString().trim());
        body.addProperty("date", edtDate.getText().toString().trim());           // e.g. 2025-11-12
        body.addProperty("delivery", edtDelivery.getText().toString().trim());
        body.addProperty("location", edtLocation.getText().toString().trim());
        body.addProperty("glass_window", edtGlassWindow.getText().toString().trim());
        body.addProperty("color", edtColor.getText().toString().trim());
        body.addProperty("cremonese", edtCremonese.getText().toString().trim());
        body.addProperty("persian", edtPersian.getText().toString().trim());
        body.addProperty("flat", edtFlat.getText().toString().trim());
        body.addProperty("spacers", edtSpacers.getText().toString().trim());
        body.addProperty("roller_shutter", edtRollerShutter.getText().toString().trim());
        body.addProperty("dumpster", edtDumpster.getText().toString().trim());
        body.addProperty("mosquito_net", edtMosquitoNet.getText().toString().trim());
        body.addProperty("marble_base", edtMarbleBase.getText().toString().trim());

        Bugfender.d("API", "Sending addCustomer request with data: " + body.toString());

        Call<JsonObject> call = api.addCustomer("Bearer " + token, body);
        call.enqueue(new retrofit2.Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, retrofit2.Response<JsonObject> response) {
                progressDialog.dismiss();
                Bugfender.d("API", "addCustomer response code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    Bugfender.d("API", "addCustomer SUCCESS: " + response.body().toString());

                    Toast toast = Toast.makeText(AddFormOneActivity.this, "progetto salvato correttamente", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();


                    try {
                        Log.e("aaa", "add customer onResponse: " + response.body().toString());
                        // read returned customer id from response.data.customer.id or response.data.id depending on backend
                        if (response.body().has("data")) {
                            JsonObject data = response.body().getAsJsonObject("data");
                            // Postman earlier returned data.customer.id
                            if (data.has("id")) {
                                createdCustomerId = data.get("id").getAsString();
                            }

                        }
                    } catch (Exception ex) {
                        Log.e("aaa", "add customer onResponse: " + ex.getMessage());
                    }
                    if (createdCustomerId == null || createdCustomerId.isEmpty()) {
                        // fallback - try reading data.id
                        createdCustomerId = "";
                    }

//                    getOnBackPressedDispatcher().onBackPressed();
                    // Now call pieces upload
                    apiAddAllPieces("Bearer " + token);

                } else {
                    Log.e("aaa", "add customer else onResponse: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                progressDialog.dismiss();
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Unknown error";
                Bugfender.e("API", "addCustomer FAILED: " + errorMsg);
                Log.e("aaa", "add customer onFailure" + errorMsg);
            }
        });
    }

    private void apiAddAllPieces(String bearerToken) {
        Log.e(TAG, "apiAddAllPieces: START");
        progressDialog.show();

        List<View> toUpload = new ArrayList<>();

        // STEP 1: FILTER ONLY PIECES THAT HAVE ANY DATA
        for (View pieceView : pieceImagesMap.keySet()) {

            EditText edtDescription = pieceView.findViewById(R.id.edtDescription);
            EditText edtLength = pieceView.findViewById(R.id.edtLength);
            EditText edtHeight = pieceView.findViewById(R.id.edtHeight);
            EditText edtAnte = pieceView.findViewById(R.id.edtAnte);
            EditText edtOpening = pieceView.findViewById(R.id.edtOpening);
            EditText edtGlass = pieceView.findViewById(R.id.edtGlass);
            EditText edtChassis = pieceView.findViewById(R.id.edtChassis);
            EditText edtNote = pieceView.findViewById(R.id.edtNote);

            ArrayList<Bitmap> images = pieceImagesMap.get(pieceView);

            boolean hasAnyText =
                    !getSafeText(edtDescription).isEmpty() ||
                            !getSafeText(edtLength).isEmpty() ||
                            !getSafeText(edtHeight).isEmpty() ||
                            !getSafeText(edtAnte).isEmpty() ||
                            !getSafeText(edtOpening).isEmpty() ||
                            !getSafeText(edtGlass).isEmpty() ||
                            !getSafeText(edtChassis).isEmpty() ||
                            !getSafeText(edtNote).isEmpty();

            boolean hasImage = images != null && !images.isEmpty();

            if (hasAnyText || hasImage) {
                toUpload.add(pieceView);
            }
        }

        if (toUpload.isEmpty()) {
            progressDialog.dismiss();
            Toast.makeText(this, "No pieces to upload", Toast.LENGTH_SHORT).show();
            getOnBackPressedDispatcher().onBackPressed();
            return;
        }

        piecesToUpload.set(toUpload.size());
        piecesUploaded.set(0);
        piecesFailed.set(0);

        ApiInterface api = RetrofitClient.getClient().create(ApiInterface.class);

        // STEP 2: CALL API ONLY FOR PIECES WITH DATA
        for (View pieceView : toUpload) {

            EditText edtDescription = pieceView.findViewById(R.id.edtDescription);
            EditText edtLength = pieceView.findViewById(R.id.edtLength);
            EditText edtHeight = pieceView.findViewById(R.id.edtHeight);
            EditText edtAnte = pieceView.findViewById(R.id.edtAnte);
            EditText edtOpening = pieceView.findViewById(R.id.edtOpening);
            EditText edtGlass = pieceView.findViewById(R.id.edtGlass);
            EditText edtChassis = pieceView.findViewById(R.id.edtChassis);
            EditText edtNote = pieceView.findViewById(R.id.edtNote);

            ArrayList<Bitmap> images = pieceImagesMap.get(pieceView);

            // FIXED IMAGE MULTIPART
            List<MultipartBody.Part> photosParts = new ArrayList<>();
            if (images != null) {
                for (int i = 0; i < images.size(); i++) {
                    File file = bitmapToFile(images.get(i));
                    RequestBody requestFile =
                            RequestBody.create(MediaType.parse("image/jpeg"), file);

                    MultipartBody.Part body =
                            MultipartBody.Part.createFormData("photos[" + i + "]", file.getName(), requestFile);

                    photosParts.add(body);
                }
            }

            Log.e(TAG, "Uploading Piece: " + getSafeText(edtDescription) +
                    " | Images Count = " + photosParts.size());

            // API CALL
            Call<JsonObject> call = api.addPiece(
                    bearerToken,
                    createPart(createdCustomerId),
                    createPart(getSafeText(edtDescription)),
                    createPart(getSafeText(edtLength)),
                    createPart(getSafeText(edtHeight)),
                    createPart(getSafeText(edtAnte)),
                    createPart(getSafeText(edtOpening)),
                    createPart(getSafeText(edtGlass)),
                    createPart(getSafeText(edtChassis)),
                    createPart(getSafeText(edtNote)),
                    photosParts
            );

            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {

                    Log.e(TAG, "Response Code: " + response.code());

                    if (response.isSuccessful()) {
                        piecesUploaded.incrementAndGet();
                    } else {
                        piecesFailed.incrementAndGet();
                    }

                    checkAllDone();
                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                    piecesFailed.incrementAndGet();
                    Log.e(TAG, "API Error: " + t.getMessage());
                    checkAllDone();
                }
            });
        }
    }

    private void checkAllDone() {
        if (piecesUploaded.get() + piecesFailed.get() == piecesToUpload.get()) {
            progressDialog.dismiss();
            getOnBackPressedDispatcher().onBackPressed();
        }
    }

    private void apiUpdateCustomer() {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage(AppConstants.PLEASE_WAIT_MSG);
        dialog.show();

        String token = "Bearer " + new PreferenceManager(this).getToken();

        ApiInterface api = RetrofitClient.getClient().create(ApiInterface.class);

        JsonObject body = new JsonObject();
        body.addProperty("id", customerId);
        body.addProperty("user_id", userId);
        body.addProperty("customer", edtClient.getText().toString());
        body.addProperty("date", edtDate.getText().toString());
        body.addProperty("delivery", edtDelivery.getText().toString());
        body.addProperty("location", edtLocation.getText().toString());
        body.addProperty("glass_window", edtGlassWindow.getText().toString());
        body.addProperty("color", edtColor.getText().toString());
        body.addProperty("cremonese", edtCremonese.getText().toString());
        body.addProperty("persian", edtPersian.getText().toString());
        body.addProperty("flat", edtFlat.getText().toString());
        body.addProperty("spacers", edtSpacers.getText().toString());
        body.addProperty("roller_shutter", edtRollerShutter.getText().toString());
        body.addProperty("dumpster", edtDumpster.getText().toString());
        body.addProperty("mosquito_net", edtMosquitoNet.getText().toString());
        body.addProperty("marble_base", edtMarbleBase.getText().toString());


        Call<JsonObject> call = api.updateCustomer(token, body);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                dialog.dismiss();
                if (response.isSuccessful()) {
                    String message = response.message();
//                    finish();
                    Log.e("aaa", "update Customer onResponse: " + message);
                    apiUpdatePiece();
                } else {
                    Log.e("aaa", "else update Customer onResponse: Successfully");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                dialog.dismiss();
                Log.e("aaa", "update Customer onFailure: fail");
            }
        });
    }

    private void apiUpdatePiece() {

        String token = "Bearer " + new PreferenceManager(this).getToken();
        ApiInterface api = RetrofitClient.getClient().create(ApiInterface.class);

        List<View> pieceViews = new ArrayList<>();
        // collect only non-empty pieces like add
        for (View pieceView : pieceImagesMap.keySet()) {
            EditText edtDescription = pieceView.findViewById(R.id.edtDescription);
            ArrayList<Bitmap> images = pieceImagesMap.get(pieceView);

            boolean hasDesc = edtDescription != null && !edtDescription.getText().toString().trim().isEmpty();
            boolean hasImg = images != null && !images.isEmpty();

            if (hasDesc || hasImg) {
                pieceViews.add(pieceView);
            }
        }

        if (pieceViews.isEmpty()) {
            finish();
            return;
        }

        for (int i = 0; i < pieceViews.size(); i++) {

            View pieceView = pieceViews.get(i);

            boolean isExistingPiece = i < allPiecesFromServer.size();
            PiecesModel model = null;

            if (isExistingPiece) {
                model = allPiecesFromServer.get(i);
            }

            // Read fields...
            EditText edtDescription = pieceView.findViewById(R.id.edtDescription);
            EditText edtLength = pieceView.findViewById(R.id.edtLength);
            EditText edtHeight = pieceView.findViewById(R.id.edtHeight);
            EditText edtAnte = pieceView.findViewById(R.id.edtAnte);
            EditText edtOpening = pieceView.findViewById(R.id.edtOpening);
            EditText edtGlass = pieceView.findViewById(R.id.edtGlass);
            EditText edtChassis = pieceView.findViewById(R.id.edtChassis);
            EditText edtNote = pieceView.findViewById(R.id.edtNote);

            ArrayList<Bitmap> images = pieceImagesMap.get(pieceView);
            List<MultipartBody.Part> photoParts = new ArrayList<>();

            if (images != null) {
                for (Bitmap bitmap : images) {
                    File file = bitmapToFile(bitmap);
                    RequestBody req = RequestBody.create(MediaType.parse("image/*"), file);
                    photoParts.add(MultipartBody.Part.createFormData("photos[]", file.getName(), req));
                }
            }

            // 🟢 EXISTING PIECE → UPDATE
            if (isExistingPiece) {
                api.updatePiece(
                        token,
                        createPart(String.valueOf(model.id)),
                        createPart(String.valueOf(customerId)),
                        createPart(getSafeText(edtDescription)),
                        createPart(getSafeText(edtLength)),
                        createPart(getSafeText(edtHeight)),
                        createPart(getSafeText(edtAnte)),
                        createPart(getSafeText(edtOpening)),
                        createPart(getSafeText(edtGlass)),
                        createPart(getSafeText(edtChassis)),
                        createPart(getSafeText(edtNote)),
                        photoParts
                ).enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        progressDialog.dismiss();
                        Log.e(TAG, "update piece onResponse" + response.body().toString());
                        if (response.isSuccessful()) {
                            try {
                                Log.e(TAG, "update piece onResponse" + response.body().toString());
                                getOnBackPressedDispatcher().onBackPressed();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        progressDialog.dismiss();
                        Log.e(TAG, "updatepiece onFailure: " + t.getMessage());
                    }
                });
            }

            // 🔵 NEW PIECE → ADD
            else {
                api.addPiece(
                        token,
                        createPart(String.valueOf(customerId)),
                        createPart(getSafeText(edtDescription)),
                        createPart(getSafeText(edtLength)),
                        createPart(getSafeText(edtHeight)),
                        createPart(getSafeText(edtAnte)),
                        createPart(getSafeText(edtOpening)),
                        createPart(getSafeText(edtGlass)),
                        createPart(getSafeText(edtChassis)),
                        createPart(getSafeText(edtNote)),
                        photoParts
                ).enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        progressDialog.dismiss();
                        Log.e(TAG, "update piece onResponse" + response.body().toString());
                        if (response.isSuccessful()) {
                            Log.e(TAG, "update piece onResponse" + response.body().toString());
                            getOnBackPressedDispatcher().onBackPressed();
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        progressDialog.dismiss();
                        Log.e(TAG, "updatepiece onFailure: " + t.getMessage());
                    }
                });
            }
        }

    }

    // utils
    private RequestBody createPart(String s) {
        if (s == null) s = "";
        return RequestBody.create(MediaType.parse("text/plain"), s);
    }

    private String getSafeText(EditText e) {
        return e == null ? "" : e.getText().toString().trim();
    }

    private void setupLaunchers() {

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            try {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    if (currentImageList != null) {
                        currentImageList.add(bitmap);
                        refreshPhotoSlots(currentPhotoContainer, currentImageList);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && photoUri != null) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);
                            bitmap = rotateImageIfRequired(bitmap, photoUri); // Apply correct rotation
                            if (currentImageList != null) {
                                currentImageList.add(bitmap);
                                refreshPhotoSlots(currentPhotoContainer, currentImageList);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
        );


        editImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            // Implementation depends on your EditImageActivity result contract
            // For this example we'll skip editing details.
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                String uriString = result.getData().getStringExtra("editedImageUri");
                if (uriString != null && currentImageList != null && currentImageIndex >= 0) {
                    Uri imageUri = Uri.parse(uriString);
                    try {
                        Bitmap editedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                        currentImageList.set(currentImageIndex, editedBitmap);
                        refreshPhotoSlots(currentPhotoContainer, currentImageList);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private Bitmap rotateImageIfRequired(Bitmap img, Uri uri) {
        try {
            ExifInterface exif = new ExifInterface(getContentResolver().openInputStream(uri));
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
                default:
                    return img; // no rotation needed
            }
            return Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        } catch (Exception e) {
            e.printStackTrace();
            return img;
        }
    }

    private void addPieceSection(int number) {
        Bugfender.d("VIEW_INFLATION", "Inflating piece section #" + number);

        View pieceView = getLayoutInflater().inflate(R.layout.item_piece, piecesContainer, false);

        TextView tvPieceTitle = pieceView.findViewById(R.id.tvPieceTitle);
        ImageView imgPieceToggle = pieceView.findViewById(R.id.imgPiece1);
        LinearLayout llPieceContent = pieceView.findViewById(R.id.llPiece1);
        LinearLayout photoContainer = pieceView.findViewById(R.id.photoContainer);

        EditText edtDescription = pieceView.findViewById(R.id.edtDescription);
        EditText edtLength = pieceView.findViewById(R.id.edtLength);
        EditText edtHeight = pieceView.findViewById(R.id.edtHeight);
        EditText edtAnte = pieceView.findViewById(R.id.edtAnte);
        EditText edtOpening = pieceView.findViewById(R.id.edtOpening);
        EditText edtGlass = pieceView.findViewById(R.id.edtGlass);
        EditText edtChassis = pieceView.findViewById(R.id.edtChassis);
        EditText edtNote = pieceView.findViewById(R.id.edtNote);


        // Each piece gets its own image list
        ArrayList<Bitmap> pieceImages = new ArrayList<>();
        pieceImagesMap.put(pieceView, pieceImages);

        // Set title dynamically
        tvPieceTitle.setText("Pezzo " + number);

        // Toggle visibility
        imgPieceToggle.setOnClickListener(v -> {
            boolean willBeVisible = llPieceContent.getVisibility() != View.VISIBLE;
            Bugfender.d("USER_INTERACTION", "Piece #" + number + " toggled - " + (willBeVisible ? "EXPANDED" : "COLLAPSED"));

            if (llPieceContent.getVisibility() == View.VISIBLE)
                llPieceContent.setVisibility(View.GONE);
            else
                llPieceContent.setVisibility(View.VISIBLE);
        });

        // Apply handwriting input fix to all EditTexts in this piece
        setupHandwritingInputFix(edtDescription);
        setupHandwritingInputFix(edtLength);
        setupHandwritingInputFix(edtHeight);
        setupHandwritingInputFix(edtAnte);
        setupHandwritingInputFix(edtOpening);
        setupHandwritingInputFix(edtGlass);
        setupHandwritingInputFix(edtChassis);
        setupHandwritingInputFix(edtNote);
        Bugfender.d("IME", "Handwriting input fix applied to 8 EditTexts in Piece #" + number);

        // Initialize photo section
        refreshPhotoSlots(photoContainer, pieceImages);
        // Add to parent layout
        piecesContainer.addView(pieceView);

        Bugfender.d("VIEW_INFLATION", "Piece section #" + number + " added to container");
    }

    private void loadCustomerDetails(int customerId) {
        progressDialog.show();
        Map<String, Integer> body = new HashMap<>();
        body.put("id", customerId);

        ApiInterface api = RetrofitClient.getClient().create(ApiInterface.class);
        String token = new PreferenceManager(this).getToken();

        Call<CustomerShowResponse> call = api.getCustomerDetail("Bearer " + token, body);
        call.enqueue(new Callback<CustomerShowResponse>() {
            @Override
            public void onResponse(Call<CustomerShowResponse> call, Response<CustomerShowResponse> response) {
                progressDialog.dismiss();
                if (!response.isSuccessful() || response.body() == null) {
                    return;
                }

                Log.e("aaa", "get data onResponse: " + new Gson().toJson(response.body()));
                CustomerData d = response.body().data;

                // SET DATA INTO FORM
                edtClient.setText(d.customer);
//                edtDate.setText(d.date.substring(0, 10));
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
                edtSpacers.setText(String.valueOf(d.spacers));
                edtRollerShutter.setText(d.roller_shutter);
                edtDumpster.setText(d.dumpster);
                edtMosquitoNet.setText(d.mosquito_net);
                edtMarbleBase.setText(d.marble_base);

                if (userId != -1) {
                    List<PiecesModel> p = response.body().data.pieces;
                    allPiecesFromServer = p;
                    if (p != null && !p.isEmpty()) {
                        showPieceData(p);
                    }
                } else {

                }
            }

            @Override
            public void onFailure(Call<CustomerShowResponse> call, Throwable t) {
                progressDialog.dismiss();
            }
        });
    }

    private void showPieceData(List<PiecesModel> pieces) {

        for (int i = 0; i < pieces.size(); i++) {
            // safety check (you created 20 pieces)
            if (i >= piecesContainer.getChildCount()) continue;

            View pieceView = piecesContainer.getChildAt(i);
            PiecesModel model = pieces.get(i);

            LinearLayout llPieceContent = pieceView.findViewById(R.id.llPiece1);
            EditText edtDescription = pieceView.findViewById(R.id.edtDescription);
            EditText edtLength = pieceView.findViewById(R.id.edtLength);
            EditText edtHeight = pieceView.findViewById(R.id.edtHeight);
            EditText edtAnte = pieceView.findViewById(R.id.edtAnte);
            EditText edtOpening = pieceView.findViewById(R.id.edtOpening);
            EditText edtGlass = pieceView.findViewById(R.id.edtGlass);
            EditText edtChassis = pieceView.findViewById(R.id.edtChassis);
            EditText edtNote = pieceView.findViewById(R.id.edtNote);
            LinearLayout photoContainer = pieceView.findViewById(R.id.photoContainer);

            // VISIBILITY LOGIC ⭐
            if (hasPieceData(model)) {
                llPieceContent.setVisibility(View.VISIBLE);
            } else {
                llPieceContent.setVisibility(View.GONE);
            }

            // SET TEXT
            edtDescription.setText(model.description);
            edtLength.setText(model.length);
            edtHeight.setText(model.height);
            edtAnte.setText(model.ante);
            edtOpening.setText(model.opening);
            edtGlass.setText(model.glass);
            edtChassis.setText(model.chassis);
            edtNote.setText(model.note);

            // SET IMAGES
            ArrayList<Bitmap> bitmaps = new ArrayList<>();
            pieceImagesMap.put(pieceView, bitmaps);

            if (model.photos != null) {
                for (String url : model.photos) {
//                    Log.e("aaa", "showPieceData Photo URL: " + url);

                    Glide.with(this)
                            .asBitmap()
                            .load(url)
                            .into(new CustomTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                    bitmaps.add(resource);
                                    refreshPhotoSlots(photoContainer, bitmaps);
                                }

                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) {
                                }
                            });
                }
            }

            // map this piece's image list to global map
            pieceImagesMap.put(pieceView, bitmaps);
            // show the images
            refreshPhotoSlots(photoContainer, bitmaps);
        }
    }

//    private void refreshPhotoSlots(LinearLayout container, ArrayList<Bitmap> images) {
//        Bugfender.d("IMAGE", "refreshPhotoSlots() - Current images: " + images.size());
//
//        // Optimized: Only update if view count changed, otherwise just update bitmaps
//        int expectedViewCount = images.size() + 1; // images + add button
//        boolean needsRebuild = container.getChildCount() != expectedViewCount;
//
//        if (needsRebuild) {
//            Bugfender.d("IMAGE", "Rebuilding photo container - view count changed");
//            container.removeAllViews();
//        } else {
//            Bugfender.d("IMAGE", "Skipping rebuild - view count unchanged (optimization)");
//        }
//
//        // Show all selected images
//        for (int i = 0; i < images.size(); i++) {
//            ImageView imgView = new ImageView(this);
//            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(200, 200);
//            params.setMargins(12, 0, 12, 0);
//            imgView.setLayoutParams(params);
//            imgView.setScaleType(ImageView.ScaleType.CENTER_CROP);
//            imgView.setImageBitmap(images.get(i));
//
//            GradientDrawable border = new GradientDrawable();
//            border.setCornerRadius(20);
//            imgView.setBackground(border);
//            imgView.setClipToOutline(true);
//
//            int finalI = i;
//            imgView.setOnClickListener(v -> {
//                Bugfender.d("USER_INTERACTION", "Image #" + finalI + " clicked - opening editor");
//                progressDialog.show();
//
//                // Move heavy I/O operation to background thread
//                new Thread(() -> {
//                    long imageProcessStart = System.currentTimeMillis();
//                    Bugfender.d("IMAGE", "Starting image processing on background thread");
//
//                    try {
//                        File file = new File(getCacheDir(), "temp_image_" + System.currentTimeMillis() + ".png");
//                        FileOutputStream fos = new FileOutputStream(file);
//                        images.get(finalI).compress(Bitmap.CompressFormat.PNG, 100, fos);
//                        fos.flush();
//                        fos.close();
//
//                        long imageProcessEnd = System.currentTimeMillis();
//                        Bugfender.d("IMAGE", "Image compression completed in " + (imageProcessEnd - imageProcessStart) + "ms");
//
//                        // Switch back to UI thread for Activity launch
//                        runOnUiThread(() -> {
//                            progressDialog.dismiss();
//                            Bugfender.d("NAVIGATION", "Launching EditImageActivity");
//
//                            Intent intent = new Intent(AddFormOneActivity.this, EditImageActivity.class);
//                            intent.putExtra("imageUri", Uri.fromFile(file).toString());
//
//                            currentImageList = images;
//                            currentPhotoContainer = container;
//                            currentImageIndex = finalI;
//                            editImageLauncher.launch(intent);
//                        });
//
//                    } catch (Exception e) {
//                        Bugfender.e("IMAGE", "Image processing FAILED: " + e.getMessage());
//                        e.printStackTrace();
//                        runOnUiThread(() -> progressDialog.dismiss());
//                    }
//                }).start();
//            });
//            container.addView(imgView);
//        }
//
//        // Add "+" button
//        ImageView addButton = new ImageView(this);
//        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(200, 200);
//        params.setMargins(12, 0, 12, 0);
//        addButton.setLayoutParams(params);
//        addButton.setScaleType(ImageView.ScaleType.CENTER);
//        addButton.setImageResource(android.R.drawable.ic_input_add);
//        addButton.setBackgroundResource(R.drawable.edittext_border);
//
//        addButton.setOnClickListener(v -> {
//            currentPhotoContainer = container;

    /// /            currentImageList = images;
//            View owner = null;
//            for (Map.Entry<View, ArrayList<Bitmap>> e : pieceImagesMap.entrySet()) {
//                View key = e.getKey();
//                LinearLayout pc = key.findViewById(R.id.photoContainer);
//                if (pc == container) {
//                    owner = key;
//                    break;
//                }
//            }
//            if (owner != null) {
//                currentImageList = pieceImagesMap.get(owner);
//            } else {
//                currentImageList = new ArrayList<>();
//            }
//
//            showImageSourceDialog();
//        });
//
//        container.addView(addButton);
//    }
    private boolean hasPieceData(PiecesModel model) {

        if (model == null) return false;

        if (!TextUtils.isEmpty(model.description)) return true;
        if (!TextUtils.isEmpty(model.length)) return true;
        if (!TextUtils.isEmpty(model.height)) return true;
        if (!TextUtils.isEmpty(model.ante)) return true;
        if (!TextUtils.isEmpty(model.opening)) return true;
        if (!TextUtils.isEmpty(model.glass)) return true;
        if (!TextUtils.isEmpty(model.chassis)) return true;
        if (!TextUtils.isEmpty(model.note)) return true;

        return model.photos != null && !model.photos.isEmpty();
    }

    private void refreshPhotoSlots(LinearLayout container, ArrayList<Bitmap> images) {
        container.removeAllViews();

        // Show all selected images
        for (int i = 0; i < images.size(); i++) {
            ImageView imgView = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(200, 200);
            params.setMargins(12, 0, 12, 0);
            imgView.setLayoutParams(params);
            imgView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imgView.setImageBitmap(images.get(i));

            GradientDrawable border = new GradientDrawable();
            border.setCornerRadius(20);
            imgView.setBackground(border);
            imgView.setClipToOutline(true);

            int finalI = i;
            imgView.setOnClickListener(v -> {
                progressDialog.show();
                new Handler().postDelayed(() -> {
                    try {
                        File file = new File(getCacheDir(), "temp_image_" + System.currentTimeMillis() + ".png");
                        FileOutputStream fos = new FileOutputStream(file);
                        images.get(finalI).compress(Bitmap.CompressFormat.PNG, 100, fos);
                        fos.flush();
                        fos.close();

                        Intent intent = new Intent(this, EditImageActivity.class);
                        intent.putExtra("imageUri", Uri.fromFile(file).toString());

                        currentImageList = images;
                        currentPhotoContainer = container;
                        currentImageIndex = finalI;
                        editImageLauncher.launch(intent);

                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        progressDialog.dismiss();    // hide loading before opening Activity
                    }
                }, 150);

            });
            container.addView(imgView);
        }

        // Add "+" button
        ImageView addButton = new ImageView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(200, 200);
        params.setMargins(12, 0, 12, 0);
        addButton.setLayoutParams(params);
        addButton.setScaleType(ImageView.ScaleType.CENTER);
        addButton.setImageResource(android.R.drawable.ic_input_add);
        addButton.setBackgroundResource(R.drawable.edittext_border);

        addButton.setOnClickListener(v -> {
            currentPhotoContainer = container;
//            currentImageList = images;
            View owner = null;
            for (Map.Entry<View, ArrayList<Bitmap>> e : pieceImagesMap.entrySet()) {
                View key = e.getKey();
                LinearLayout pc = key.findViewById(R.id.photoContainer);
                if (pc == container) {
                    owner = key;
                    break;
                }
            }
            if (owner != null) {
                currentImageList = pieceImagesMap.get(owner);
            } else {
                currentImageList = new ArrayList<>();
            }

            showImageSourceDialog();
        });

        container.addView(addButton);
    }

    private void showImageSourceDialog() {
        String[] options = {"Camera", "Galleria"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleziona la sorgente dell'immagine")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openCamera();
                    } else {
                        openGallery();
                    }
                })
                .show();
    }

    private void openGallery() {
        String permission;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (checkAndRequestPermission(permission)) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            Intent chooser = Intent.createChooser(intent, "Select Image");
            galleryLauncher.launch(chooser);
        }
    }

    private Uri photoUri;

    private void openCamera() {
//        if (checkAndRequestPermission(Manifest.permission.CAMERA)) {
//            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//            cameraLauncher.launch(intent);
//        }

        if (!checkAndRequestPermission(Manifest.permission.CAMERA)) return;

        // Create file for full-size photo
        File photoFile = null;
        try {
            String fileName = "camera_image_" + System.currentTimeMillis();
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            photoFile = File.createTempFile(fileName, ".jpg", storageDir);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        if (photoFile != null) {
            photoUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".provider", // make sure provider is declared in Manifest
                    photoFile);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            cameraLauncher.launch(intent);
        }
    }

    private boolean checkAndRequestPermission(String permission) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, 200);
            return false;
        }
        return true;
    }

    private File bitmapToFile(Bitmap bitmap) {
        File file = new File(getCacheDir(), "IMG_" + System.currentTimeMillis() + ".jpg");
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

//    private File bitmapToFile(Bitmap bitmap) {
//        // Create a file inside cache
//        File file = new File(getCacheDir(), System.currentTimeMillis() + "_image.png");
//        try {
//            FileOutputStream fos = new FileOutputStream(file);
//            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
//            fos.flush();
//            fos.close();
//        } catch (Exception e) {
//            Log.e("bitmapToFile", "Error: " + e.getMessage());
//        }
//        return file;
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 200) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                Toast.makeText(this, "Permission granted. Please try again.", Toast.LENGTH_SHORT).show();
            } else {
//                Toast.makeText(this, "Permission denied. Cannot open camera/gallery.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void saveOfflineCustomer() {

        Gson gson = new Gson();

        // ---------------- CUSTOMER JSON ----------------
        JsonObject customer = new JsonObject();
        customer.addProperty("customer", edtClient.getText().toString());
        customer.addProperty("date", edtDate.getText().toString());
        customer.addProperty("delivery", edtDelivery.getText().toString());
        customer.addProperty("location", edtLocation.getText().toString());
        customer.addProperty("glass_window", edtGlassWindow.getText().toString());
        customer.addProperty("color", edtColor.getText().toString());
        customer.addProperty("cremonese", edtCremonese.getText().toString());
        customer.addProperty("persian", edtPersian.getText().toString());
        customer.addProperty("flat", edtFlat.getText().toString());
        customer.addProperty("spacers", edtSpacers.getText().toString());
        customer.addProperty("roller_shutter", edtRollerShutter.getText().toString());
        customer.addProperty("dumpster", edtDumpster.getText().toString());
        customer.addProperty("mosquito_net", edtMosquitoNet.getText().toString());
        customer.addProperty("marble_base", edtMarbleBase.getText().toString());

        // ---------------- PIECES JSON ----------------
        List<JsonObject> piecesArray = new ArrayList<>();
        List<String> imagePaths = new ArrayList<>();

        for (View pieceView : pieceImagesMap.keySet()) {

            EditText edtDescription = pieceView.findViewById(R.id.edtDescription);
            EditText edtLength = pieceView.findViewById(R.id.edtLength);
            EditText edtHeight = pieceView.findViewById(R.id.edtHeight);
            EditText edtAnte = pieceView.findViewById(R.id.edtAnte);
            EditText edtOpening = pieceView.findViewById(R.id.edtOpening);
            EditText edtGlass = pieceView.findViewById(R.id.edtGlass);
            EditText edtChassis = pieceView.findViewById(R.id.edtChassis);
            EditText edtNote = pieceView.findViewById(R.id.edtNote);

            ArrayList<Bitmap> images = pieceImagesMap.get(pieceView);

            boolean hasText =
                    !getSafeText(edtDescription).isEmpty() ||
                            !getSafeText(edtLength).isEmpty() ||
                            !getSafeText(edtHeight).isEmpty();

            if (!hasText && (images == null || images.isEmpty())) continue;

            JsonObject piece = new JsonObject();
            piece.addProperty("description", getSafeText(edtDescription));
            piece.addProperty("length", getSafeText(edtLength));
            piece.addProperty("height", getSafeText(edtHeight));
            piece.addProperty("ante", getSafeText(edtAnte));
            piece.addProperty("opening", getSafeText(edtOpening));
            piece.addProperty("glass", getSafeText(edtGlass));
            piece.addProperty("chassis", getSafeText(edtChassis));
            piece.addProperty("note", getSafeText(edtNote));

            List<String> pieceImgs = new ArrayList<>();
            if (images != null) {
                for (Bitmap b : images) {
                    File f = bitmapToFile(b);
                    pieceImgs.add(f.getAbsolutePath());
                    imagePaths.add(f.getAbsolutePath());
                }
            }

            piece.add("images", gson.toJsonTree(pieceImgs));
            piecesArray.add(piece);
        }

        JsonObject finalData = new JsonObject();
        finalData.add("customer", customer);
        finalData.add("pieces", gson.toJsonTree(piecesArray));

        OfflineCustomerEntity entity = new OfflineCustomerEntity();
        entity.customerJson = gson.toJson(finalData);
        entity.imagesJson = gson.toJson(imagePaths);
        entity.isSynced = false;

        AppDatabase.get(this).offlineDao().insert(entity);

        Toast.makeText(this,
                "Internet nathi. Data offline save thay gayo.",
                Toast.LENGTH_LONG).show();

        finish();
    }


}
