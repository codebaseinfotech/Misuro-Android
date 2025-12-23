package com.CB.MisureFinestre;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.CB.MisureFinestre.activity.FullScreenImageActivity;
import com.CB.MisureFinestre.model.PiecesModel;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;

import java.util.List;

public class ViewPieceDataAdapter extends RecyclerView.Adapter<ViewPieceDataAdapter.ViewHolder> {

    private Context context;
    private List<PiecesModel> piecesList;

    public ViewPieceDataAdapter(Context context, List<PiecesModel> piecesList) {
        this.context = context;
        this.piecesList = piecesList;
    }

    @NonNull
    @Override
    public ViewPieceDataAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.adapter_view_piece_data, parent, false);
        return new ViewPieceDataAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewPieceDataAdapter.ViewHolder holder, int position) {
        PiecesModel piece = piecesList.get(position);
        holder.tvDescription.setText(piece.description);
        holder.tvLength.setText(piece.length);
        holder.tvHeight.setText(piece.height);
        holder.tvAnte.setText(piece.ante);
        holder.tvOpening.setText(piece.opening);
        holder.tvGlass.setText(piece.glass);
        holder.tvChassis.setText(piece.chassis);

        if (piece.note != null && !piece.note.isEmpty()) {
            holder.btnNote.setBackgroundResource(R.drawable.bg_button_green);
        } else {
            holder.btnNote.setBackgroundResource(R.drawable.bg_button_gre);
        }

        if (piece.photos != null && !piece.photos.isEmpty()) {
            holder.btnPhoto.setBackgroundResource(R.drawable.bg_button_green);
        } else {
            holder.btnPhoto.setBackgroundResource(R.drawable.bg_button_gre);
        }

        holder.imgHidePhoto.setOnClickListener(view -> {
            if (holder.llHidePhoto.getVisibility() == View.VISIBLE)
                holder.llHidePhoto.setVisibility(View.GONE);
            else
                holder.llHidePhoto.setVisibility(View.VISIBLE);
        });
        holder.btnNote.setOnClickListener(view -> {
            if (piece.note != null && !piece.note.isEmpty()) {
                showNoteDialog(piece.note);
            } else {
//                showNoteDialog("No notes available");
            }
        });

        holder.btnPhoto.setOnClickListener(view -> {
            if (piece.photos != null && !piece.photos.isEmpty()) {
                showPhotosDialog(piece.photos);
            } else {
            }
        });
    }


    @Override
    public int getItemCount() {
        return piecesList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDescription, tvLength, tvHeight, tvAnte, tvOpening, tvGlass, tvChassis;
        Button btnNote, btnPhoto;
        ImageView imgHidePhoto;
        LinearLayout llHidePhoto;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvLength = itemView.findViewById(R.id.tvLength);
            tvHeight = itemView.findViewById(R.id.tvHeight);
            tvAnte = itemView.findViewById(R.id.tvAnte);
            tvOpening = itemView.findViewById(R.id.tvOpening);
            tvGlass = itemView.findViewById(R.id.tvGlass);
            tvChassis = itemView.findViewById(R.id.tvChassis);
            btnNote = itemView.findViewById(R.id.btnNote);
            btnPhoto = itemView.findViewById(R.id.btnPhoto);
            imgHidePhoto = itemView.findViewById(R.id.imgHidePhoto);
            llHidePhoto = itemView.findViewById(R.id.llHidePhoto);
        }
    }

    private void showNoteDialog(String noteText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Note");
        builder.setMessage(noteText);
        builder.setCancelable(true);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showPhotosDialog(List<String> photos) {

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_photos, null);
        LinearLayout layoutImages = view.findViewById(R.id.layoutImages);
        Button btnOk = view.findViewById(R.id.btnOk);

        // Load all images
        for (String link : photos) {
            ImageView img = new ImageView(context);
//            img.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 400));
            img.setLayoutParams(new LinearLayout.LayoutParams(400, 400));
            img.setPadding(20, 20, 20, 20);
            img.setAdjustViewBounds(true);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(context)
                    .load(link)
                    .thumbnail(0.1f)
//                    .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .into(img);

            img.setOnClickListener(v -> {
                Intent i = new Intent(context, FullScreenImageActivity.class);
                i.putExtra("img", link);
                context.startActivity(i);
            });

            layoutImages.addView(img);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.show();
        btnOk.setOnClickListener(v -> dialog.dismiss());
    }

}
