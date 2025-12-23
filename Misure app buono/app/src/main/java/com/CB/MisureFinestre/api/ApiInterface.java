package com.CB.MisureFinestre.api;

import com.CB.MisureFinestre.model.AllCustomerResponse;
import com.CB.MisureFinestre.model.CustomerShowResponse;
import com.CB.MisureFinestre.model.LoginResponse;
import com.CB.MisureFinestre.model.ProfileResponse;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiInterface {

    //    Login
    @FormUrlEncoded
    @POST("login")
    Call<LoginResponse> loginUser(
            @Field("email") String email,
            @Field("password") String password
    );

    //    Home Profile
    @GET("user/profile")
    Call<ProfileResponse> getUserProfile(@Header("Authorization") String token);


    //    Add Customer
    @POST("add_customer")
    Call<JsonObject> addCustomer(
            @Header("Authorization") String token,
            @Body JsonObject body
    );

    //    Add Piece
    @Multipart
    @POST("add_piece")
    Call<JsonObject> addPiece(
            @Header("Authorization") String token,
            @Part("customer_id") RequestBody customerId,
            @Part("description") RequestBody description,
            @Part("length") RequestBody length,
            @Part("height") RequestBody height,
            @Part("ante") RequestBody ante,
            @Part("opening") RequestBody opening,
            @Part("glass") RequestBody glass,
            @Part("chassis") RequestBody chassis,
            @Part("note") RequestBody note,
            @Part List<MultipartBody.Part> photos
    );


    //Update Customer
    @POST("customers/update")
    Call<JsonObject> updateCustomer(
            @Header("Authorization") String token,
            @Body JsonObject body
    );


    // UPDATE PIECE
    @Multipart
    @POST("pieces/update")
    Call<JsonObject> updatePiece(
            @Header("Authorization") String token,
            @Part("id") RequestBody id,
            @Part("customer_id") RequestBody customerId,
            @Part("description") RequestBody description,
            @Part("length") RequestBody length,
            @Part("height") RequestBody height,
            @Part("ante") RequestBody ante,
            @Part("opening") RequestBody opening,
            @Part("glass") RequestBody glass,
            @Part("chassis") RequestBody chassis,
            @Part("note") RequestBody note,
            @Part List<MultipartBody.Part> photos
    );


    //    View Customer
    @POST("customers")
    Call<AllCustomerResponse> getCustomers(
            @Header("Authorization") String token
    );


    //    Customers Delete
    @POST("customers/delete")
    Call<JsonObject> deleteCustomer(
            @Header("Authorization") String token,
            @Body JsonObject body
    );


    //  Show Customer Detail
    @POST("customers/show")
    Call<CustomerShowResponse> getCustomerDetail(
            @Header("Authorization") String token,
            @Body Map<String, Integer> body
    );


    //    Logout
    @POST("logout")
    Call<JsonObject> logout(
            @Header("Authorization") String token
    );


}
