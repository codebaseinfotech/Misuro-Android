package com.CB.MisureFinestre.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.CB.MisureFinestre.utils.DrawingView;
import com.CB.MisureFinestre.R;
import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class EditImageActivity extends AppCompatActivity {

    ImageView imageView;
    Button btnColor, btnUndo, btnClose, btnSave;
    private SeekBar brushSize;
    private DrawingView drawingView;
    private int defaultColor ; // default black color
    private static final int REQUEST_STORAGE_PERMISSION = 1001;
    private Bitmap baseBitmap;
    private ProgressDialog progressDialog;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_image);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });

        viewById();
        allBtnClick();


        String uriString = getIntent().getStringExtra("imageUri");
        if (uriString != null) {
            Uri imageUri = Uri.parse(uriString);
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                imageView.setImageBitmap(bitmap);

                // Resize drawing view after ImageView layout is done
                imageView.post(() -> {
                    int w = imageView.getWidth();
                    int h = imageView.getHeight();

                    ViewGroup.LayoutParams params = drawingView.getLayoutParams();
                    params.width = w;
                    params.height = h;
                    drawingView.setLayoutParams(params);

                    drawingView.initCanvas(w, h);
                });

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void viewById() {
        imageView = findViewById(R.id.imageView);
        drawingView = findViewById(R.id.drawingView);
        btnColor = findViewById(R.id.btnColor);
        brushSize = findViewById(R.id.brushSize);
        btnUndo = findViewById(R.id.btnUndo);
        btnClose = findViewById(R.id.btnClose);
        btnSave = findViewById(R.id.btnSave);
    }

    private void allBtnClick() {
        btnColor.setOnClickListener(v -> openColorPicker());
        brushSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (i < 1) i = 1;
                drawingView.setBrushSize(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        btnUndo.setOnClickListener(v -> drawingView.undo());
        btnClose.setOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());
        btnSave.setOnClickListener(v -> {
//            progressDialog = new ProgressDialog(this);
//            progressDialog.setMessage("salvataggio in corso...");
//            progressDialog.setCancelable(false);
//            progressDialog.show();
//            returnEditedImage();

            showProgress();

            // Run heavy work on background thread
            new Thread(() -> {
                Bitmap finalBitmap = combineImageAndDrawing();
                Uri resultUri = saveToCache(finalBitmap);

                runOnUiThread(() -> {
                    hideProgress();

                    if (resultUri != null) {
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("editedImageUri", resultUri.toString());
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    }
                });
            }).start();
        });
    }

    private void openColorPicker() {
        new ColorPickerDialog.Builder(this)
                .setTitle("Pick Color")
                .setPositiveButton("Select", new ColorEnvelopeListener() {
                    @Override
                    public void onColorSelected(ColorEnvelope envelope, boolean fromUser) {
                        defaultColor = envelope.getColor();
                        btnColor.setBackgroundColor(defaultColor);
                        drawingView.setColor(defaultColor);
                    }
                })
                .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                .attachAlphaSlideBar(true)
                .attachBrightnessSlideBar(true)
                .show();
    }

    private Uri saveToCache(Bitmap bitmap) {
        try {
            File cacheDir = new File(getCacheDir(), "edited");
            if (!cacheDir.exists()) cacheDir.mkdirs();

            File file = new File(cacheDir, "edited_" + System.currentTimeMillis() + ".png");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();

            return Uri.fromFile(file);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private void returnEditedImage() {

        Bitmap finalBitmap = combineImageAndDrawing();

        try {

            // Save the image to cache folder
            File cacheDir = new File(getCacheDir(), "edited");
            if (!cacheDir.exists()) cacheDir.mkdirs();

            File file = new File(cacheDir, "edited_" + System.currentTimeMillis() + ".png");
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.PNG, 10, out);
            out.flush();
            out.close();

            // Send back only file Uri
            Intent resultIntent = new Intent();
            resultIntent.putExtra("editedImageUri", Uri.fromFile(file).toString());
            setResult(RESULT_OK, resultIntent);
            progressDialog.dismiss();
            finish();

        } catch (Exception e) {
            e.printStackTrace();
            progressDialog.dismiss();
//            Toast.makeText(this, "Failed to return image", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap combineImageAndDrawing() {
        Bitmap background = Bitmap.createBitmap(imageView.getWidth(), imageView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(background);
        // Draw image first, then drawing overlay
        imageView.draw(canvas);
        drawingView.draw(canvas);
        return background;
    }


    private void showProgress() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Salvataggio in corso...");
            progressDialog.setCancelable(false);
        }
        progressDialog.show();
    }

    private void hideProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }






    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
//                Toast.makeText(this, "Storage permission required to save image", Toast.LENGTH_SHORT).show();
            }
        }
    }

}