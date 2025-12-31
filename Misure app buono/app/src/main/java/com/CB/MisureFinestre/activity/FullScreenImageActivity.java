package com.CB.MisureFinestre.activity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ProgressBar;
import com.CB.MisureFinestre.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.github.chrisbanes.photoview.PhotoView;

public class FullScreenImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);

        PhotoView img = findViewById(R.id.imgFull);
        ImageView btnClose = findViewById(R.id.btnClose);
        ProgressBar progressBar = findViewById(R.id.progressBar);

        String url = getIntent().getStringExtra("img");
        Glide.with(this)
                .asBitmap()
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .apply(new RequestOptions().encodeQuality(100))
                .skipMemoryCache(false)
                .dontTransform()
                .dontAnimate()
                .format(DecodeFormat.PREFER_ARGB_8888)
                .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                .listener(new RequestListener<Bitmap>() {
                    @Override
                    public boolean onLoadFailed(GlideException e, Object model, Target<android.graphics.Bitmap> target, boolean isFirstResource) {
                        progressBar.setVisibility(android.view.View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(android.graphics.Bitmap resource, Object model, Target<android.graphics.Bitmap> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                        progressBar.setVisibility(android.view.View.GONE);
                        return false;
                    }
                })
                .into(img);
        // Close button click
        btnClose.setOnClickListener(v -> finish());
    }
}