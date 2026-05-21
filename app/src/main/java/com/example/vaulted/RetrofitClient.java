package com.example.vaulted;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String STEAM_BASE = "https://api.steampowered.com/";
    private static Retrofit steamInstance;

    public static Retrofit getSteamClient() {
        if (steamInstance == null) {
            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            log.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(log).build();

            steamInstance = new Retrofit.Builder()
                    .baseUrl(STEAM_BASE)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return steamInstance;
    }
}