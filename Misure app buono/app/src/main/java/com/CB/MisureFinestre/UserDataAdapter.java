package com.CB.MisureFinestre;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.CB.MisureFinestre.activity.AddFormOneActivity;
import com.CB.MisureFinestre.activity.YouSeeDataActivity;
import com.CB.MisureFinestre.api.ApiInterface;
import com.CB.MisureFinestre.api.RetrofitClient;
import com.CB.MisureFinestre.model.AllCustomerResponse;
import com.CB.MisureFinestre.utils.AppConstants;
import com.CB.MisureFinestre.utils.PreferenceManager;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserDataAdapter extends RecyclerView.Adapter<UserDataAdapter.ViewHolder> {

    List<AllCustomerResponse.Customer> list;
    Context context;
    ProgressDialog progressDialog;

    public UserDataAdapter(List<AllCustomerResponse.Customer> list, Context context) {
        this.list = list;
        this.context = context;

        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage(AppConstants.PLEASE_WAIT_MSG);
        progressDialog.setCancelable(false);
    }

    @NonNull
    @Override
    public UserDataAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.adapter_view_data, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserDataAdapter.ViewHolder holder, int position) {
        AllCustomerResponse.Customer item = list.get(position);
        holder.tvId.setText(String.valueOf(item.id));
        holder.tvCustomerName.setText(item.customer);
        holder.tvDate.setText(formatSimpleDate(item.date));

        holder.btnYouSee.setOnClickListener(view -> {
            Intent intent = new Intent(context, YouSeeDataActivity.class);
            intent.putExtra("CUSTOMER_ID", item.id );
            context.startActivity(intent);
        });
        holder.btnDupli.setOnClickListener(view -> {
            Intent intent = new Intent(context,AddFormOneActivity.class);
            intent.putExtra("CUSTOMER_ID", item.id );
            intent.putExtra("DOUBLICAT","doublicat");
            context.startActivity(intent);
        });
        holder.btnDelete.setOnClickListener(view -> showDeleteDialog("delete",item.id, position));
        holder.btnEdit.setOnClickListener(view -> showDeleteDialog("edit", item.id, position));
        holder.btnPdf.setOnClickListener(view -> {
//            String pdfUrl = "https://example-files.online-convert.com/document/pdf/example.pdf";
            String pdfUrl = item.pdf_url;
            Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(pdfUrl));
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvId, tvCustomerName, tvDate;
        Button btnYouSee, btnDupli, btnDelete, btnEdit, btnPdf;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvId = itemView.findViewById(R.id.tvId);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvDate = itemView.findViewById(R.id.tvDate);
            btnYouSee = itemView.findViewById(R.id.btnYouSee);
            btnDupli = itemView.findViewById(R.id.btnDupli);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnPdf = itemView.findViewById(R.id.btnPdf);
        }
    }

    public static String formatSimpleDate(String date) {
        try {
            SimpleDateFormat api = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            return out.format(api.parse(date));
        } catch (Exception e) {
            return date;
        }
    }

    private void showDeleteDialog(String type, int customerId, int position) {

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_delete);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false);

        EditText edtPassword = dialog.findViewById(R.id.edtPassword);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        Button btnOkay = dialog.findViewById(R.id.btnOkay);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnOkay.setOnClickListener(v -> {
            String pass = edtPassword.getText().toString().trim();
            PreferenceManager pref = new PreferenceManager(context);
            String passwordCode = pref.getCompanyCode();

            if (pass.isEmpty()) {
                edtPassword.setError("inserisci la password");
                return;
            }
            if (!passwordCode.equals(pass)) {
                edtPassword.setError("Password errata");
                return;
            }


            dialog.dismiss();
            // ---- ACTION BASED ON TYPE ----
            if (type.equals("delete")) {
                apiDeleteItem(customerId, position);   // already created
            }
            else if (type.equals("edit")) {
                Intent intent = new Intent(context, AddFormOneActivity.class);
                intent.putExtra("CUSTOMER_ID", customerId);
                intent.putExtra("USER_ID", list.get(position).user_id);
                context.startActivity(intent);
            }
        });

        dialog.show();

        // Make popup full width
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
        }
    }

    private void apiDeleteItem(int id, int position) {
        ApiInterface api = RetrofitClient.getClient().create(ApiInterface.class);
        progressDialog.show();
        // Prepare body
        JsonObject body = new JsonObject();
        body.addProperty("id", id);
        PreferenceManager pref = new PreferenceManager(context);
        // Token
        String token = "Bearer " + pref.getToken();
        Call<JsonObject> call = api.deleteCustomer(token, body);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    boolean success = response.body().get("success").getAsBoolean();
                    if (success) {
                        Log.e("aaa", "Delete onResponse: " + response.message() );

//                        Toast toast = Toast.makeText(context, "Deleted Successfully", Toast.LENGTH_SHORT);
//                        toast.setGravity(Gravity.CENTER, 0, 0);
//                        toast.show();
                        // Remove item from list
                        list.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, list.size());

                    } else {
                        Toast toast = Toast.makeText(context, "Deleted failed", Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                } else {
                    Toast toast = Toast.makeText(context, "Server Error", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                progressDialog.dismiss();
                Log.e("aaa", "onFailure: "+ t.getMessage());
            }
        });

    }
}
