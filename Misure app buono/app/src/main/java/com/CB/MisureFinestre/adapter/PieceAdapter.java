package com.CB.MisureFinestre.adapter;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.CB.MisureFinestre.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PieceAdapter extends RecyclerView.Adapter<PieceAdapter.PieceViewHolder> {

    private Context context;
    private int pieceCount;
    private Map<Integer, PieceData> pieceDataMap;
    private PieceInteractionListener listener;

    public interface PieceInteractionListener {
        void onAddPhotoClicked(int position, ArrayList<Bitmap> images);
        void onPhotoClicked(int position, int imageIndex, ArrayList<Bitmap> images);
    }

    public PieceAdapter(Context context, int pieceCount, PieceInteractionListener listener) {
        this.context = context;
        this.pieceCount = pieceCount;
        this.listener = listener;
        this.pieceDataMap = new HashMap<>();

        // Initialize data for each piece
        for (int i = 0; i < pieceCount; i++) {
            pieceDataMap.put(i, new PieceData());
        }
    }

    @NonNull
    @Override
    public PieceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_piece, parent, false);
        return new PieceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PieceViewHolder holder, int position) {
        holder.bind(position + 1, pieceDataMap.get(position));
    }

    @Override
    public int getItemCount() {
        return pieceCount;
    }

    public PieceData getPieceData(int position) {
        return pieceDataMap.get(position);
    }

    public Map<Integer, PieceData> getAllPieceData() {
        return pieceDataMap;
    }

    // Data class to hold piece information
    public static class PieceData {
        public String description = "";
        public String length = "";
        public String height = "";
        public String ante = "";
        public String opening = "";
        public String glass = "";
        public String chassis = "";
        public String note = "";
        public ArrayList<Bitmap> images = new ArrayList<>();

        public boolean hasData() {
            return !description.isEmpty() || !length.isEmpty() || !height.isEmpty() ||
                   !ante.isEmpty() || !opening.isEmpty() || !glass.isEmpty() ||
                   !chassis.isEmpty() || !note.isEmpty() || !images.isEmpty();
        }
    }

    class PieceViewHolder extends RecyclerView.ViewHolder {
        TextView tvPieceTitle;
        ImageView imgPieceToggle;
        LinearLayout llPieceContent;
        LinearLayout photoContainer;
        EditText edtDescription, edtLength, edtHeight, edtAnte, edtOpening, edtGlass, edtChassis, edtNote;

        public PieceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPieceTitle = itemView.findViewById(R.id.tvPieceTitle);
            imgPieceToggle = itemView.findViewById(R.id.imgPiece1);
            llPieceContent = itemView.findViewById(R.id.llPiece1);
            photoContainer = itemView.findViewById(R.id.photoContainer);

            edtDescription = itemView.findViewById(R.id.edtDescription);
            edtLength = itemView.findViewById(R.id.edtLength);
            edtHeight = itemView.findViewById(R.id.edtHeight);
            edtAnte = itemView.findViewById(R.id.edtAnte);
            edtOpening = itemView.findViewById(R.id.edtOpening);
            edtGlass = itemView.findViewById(R.id.edtGlass);
            edtChassis = itemView.findViewById(R.id.edtChassis);
            edtNote = itemView.findViewById(R.id.edtNote);
        }

        public void bind(int number, PieceData data) {
            tvPieceTitle.setText("Pezzo " + number);

            // Toggle visibility
            imgPieceToggle.setOnClickListener(v -> {
                if (llPieceContent.getVisibility() == View.VISIBLE) {
                    llPieceContent.setVisibility(View.GONE);
                } else {
                    llPieceContent.setVisibility(View.VISIBLE);
                }
            });

            // Set data if exists
            if (data != null) {
                edtDescription.setText(data.description);
                edtLength.setText(data.length);
                edtHeight.setText(data.height);
                edtAnte.setText(data.ante);
                edtOpening.setText(data.opening);
                edtGlass.setText(data.glass);
                edtChassis.setText(data.chassis);
                edtNote.setText(data.note);

                refreshPhotoSlots(data.images);
            }
        }

        private void refreshPhotoSlots(ArrayList<Bitmap> images) {
            photoContainer.removeAllViews();

            // Show all selected images
            for (int i = 0; i < images.size(); i++) {
                ImageView imgView = new ImageView(context);
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
                    if (listener != null) {
                        listener.onPhotoClicked(getAdapterPosition(), finalI, images);
                    }
                });
                photoContainer.addView(imgView);
            }

            // Add "+" button
            ImageView addButton = new ImageView(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(200, 200);
            params.setMargins(12, 0, 12, 0);
            addButton.setLayoutParams(params);
            addButton.setScaleType(ImageView.ScaleType.CENTER);
            addButton.setImageResource(android.R.drawable.ic_input_add);
            addButton.setBackgroundResource(R.drawable.edittext_border);

            addButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAddPhotoClicked(getAdapterPosition(), images);
                }
            });

            photoContainer.addView(addButton);
        }
    }
}
