package com.CB.MisureFinestre.offline;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.CB.MisureFinestre.api.ApiInterface;
import com.CB.MisureFinestre.api.RetrofitClient;
import com.CB.MisureFinestre.utils.PreferenceManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Response;

public class OfflineSyncWorker extends Worker {

    public OfflineSyncWorker(@NonNull Context context,
                             @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {

        AppDatabase db = AppDatabase.get(getApplicationContext());
        List<OfflineCustomerEntity> list = db.offlineDao().getPending();

        if (list == null || list.isEmpty()) {
            return Result.success();
        }

        ApiInterface api =
                RetrofitClient.getClient().create(ApiInterface.class);

        String token = "Bearer " +
                new PreferenceManager(getApplicationContext()).getToken();

        Gson gson = new Gson();

        for (OfflineCustomerEntity entity : list) {

            try {

                JsonObject root =
                        gson.fromJson(entity.customerJson, JsonObject.class);

                JsonObject customerJson = root.getAsJsonObject("customer");
                JsonArray piecesArray = root.getAsJsonArray("pieces");

                // =============================
                // 1️⃣ ADD CUSTOMER
                // =============================
                Response<JsonObject> customerRes =
                        api.addCustomer(token, customerJson).execute();

                if (!customerRes.isSuccessful()
                        || customerRes.body() == null) {
                    return Result.retry();
                }

                int customerId =
                        customerRes.body()
                                .getAsJsonObject("data")
                                .get("id")
                                .getAsInt();

                // =============================
                // 2️⃣ ADD PIECES + IMAGES
                // =============================
                for (JsonElement element : piecesArray) {

                    JsonObject p = element.getAsJsonObject();

                    RequestBody rbCustomerId =
                            createPart(customerId + "");

                    RequestBody description =
                            createPart(p.get("description").getAsString());
                    RequestBody length =
                            createPart(p.get("length").getAsString());
                    RequestBody height =
                            createPart(p.get("height").getAsString());
                    RequestBody ante =
                            createPart(p.get("ante").getAsString());
                    RequestBody opening =
                            createPart(p.get("opening").getAsString());
                    RequestBody glass =
                            createPart(p.get("glass").getAsString());
                    RequestBody chassis =
                            createPart(p.get("chassis").getAsString());
                    RequestBody note =
                            createPart(p.get("note").getAsString());

                    // 🔹 IMAGES
                    List<MultipartBody.Part> photos = new ArrayList<>();

                    JsonArray imgs = p.getAsJsonArray("images");
                    for (JsonElement img : imgs) {
                        File file = new File(img.getAsString());
                        if (!file.exists()) continue;

                        RequestBody imgBody =
                                RequestBody.create(
                                        MediaType.parse("image/*"), file);

                        MultipartBody.Part part =
                                MultipartBody.Part.createFormData(
                                        "photos[]",
                                        file.getName(),
                                        imgBody
                                );
                        photos.add(part);
                    }

                    Response<JsonObject> pieceRes =
                            api.addPiece(
                                    token,
                                    rbCustomerId,
                                    description,
                                    length,
                                    height,
                                    ante,
                                    opening,
                                    glass,
                                    chassis,
                                    note,
                                    photos
                            ).execute();

                    if (!pieceRes.isSuccessful()) {
                        return Result.retry();
                    }
                }

                // =============================
                // 3️⃣ MARK SYNCED
                // =============================
                db.offlineDao().markSynced(entity.id);

            } catch (Exception e) {
                e.printStackTrace();
                return Result.retry();
            }
        }

        return Result.success();
    }

    // =============================
    // HELPER
    // =============================
    private RequestBody createPart(String value) {
        return RequestBody.create(
                MediaType.parse("text/plain"),
                value == null ? "" : value
        );
    }
}


