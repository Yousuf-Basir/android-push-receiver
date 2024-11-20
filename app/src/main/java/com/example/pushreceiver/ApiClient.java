package com.example.pushreceiver;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.*;
import android.util.Log;
import java.util.concurrent.TimeUnit;

public class ApiClient {
    private static final String TAG = "ApiClient";
    private static final String BASE_URL = "https://api.doctorachen.com/api/";
    private static ApiClient instance;
    private final ApiService apiService;

    private ApiClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message ->
                Log.d(TAG, "OkHttp: " + message));
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    public static synchronized ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient();
        }
        return instance;
    }

    public void registerOrUpdatePushToken(String phoneNumber, String firebaseToken, ApiCallback callback) {
        Log.d(TAG, "Checking if user exists for phone: " + phoneNumber);

        // First check if user exists
        apiService.getUser(phoneNumber).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // User exists, update token
                    updateExistingUser(phoneNumber, firebaseToken, callback);
                } else if (response.code() == 404) {
                    // User doesn't exist, create new
                    createNewUser(phoneNumber, firebaseToken, callback);
                } else {
                    String errorMsg = "Error checking user existence: " + response.code();
                    Log.e(TAG, errorMsg);
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                String errorMsg = "Network error while checking user: " + t.getMessage();
                Log.e(TAG, errorMsg, t);
                callback.onError(errorMsg);
            }
        });
    }

    private void createNewUser(String phoneNumber, String firebaseToken, ApiCallback callback) {
        Log.d(TAG, "Creating new user for phone: " + phoneNumber);
        TokenUpdate tokenUpdate = new TokenUpdate(phoneNumber, firebaseToken);

        apiService.createUser(tokenUpdate).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "User creation successful");
                    callback.onSuccess();
                } else {
                    String errorMsg = "Failed to create user: " + response.code();
                    Log.e(TAG, errorMsg);
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                String errorMsg = "Network error while creating user: " + t.getMessage();
                Log.e(TAG, errorMsg, t);
                callback.onError(errorMsg);
            }
        });
    }

    private void updateExistingUser(String phoneNumber, String firebaseToken, ApiCallback callback) {
        Log.d(TAG, "Updating existing user for phone: " + phoneNumber);
        TokenUpdate tokenUpdate = new TokenUpdate(phoneNumber, firebaseToken);

        apiService.updateToken(phoneNumber, tokenUpdate).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Update successful");
                    callback.onSuccess();
                } else {
                    String errorMsg = "Failed to update token: " + response.code();
                    Log.e(TAG, errorMsg);
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                String errorMsg = "Network error while updating: " + t.getMessage();
                Log.e(TAG, errorMsg, t);
                callback.onError(errorMsg);
            }
        });
    }

    interface ApiService {
        @GET("user-push-token/{phone}")
        Call<Void> getUser(@Path("phone") String phone);

        @POST("user-push-token")
        Call<Void> createUser(@Body TokenUpdate tokenUpdate);

        @PUT("user-push-token/{phone}")
        Call<Void> updateToken(@Path("phone") String phone, @Body TokenUpdate tokenUpdate);
    }

    static class TokenUpdate {
        String phone_number;
        String firebase_token;

        TokenUpdate(String phoneNumber, String firebaseToken) {
            this.phone_number = phoneNumber;
            this.firebase_token = firebaseToken;
        }
    }

    interface ApiCallback {
        void onSuccess();
        void onError(String error);
    }
}