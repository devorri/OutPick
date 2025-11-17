package com.example.outpick.database.supabase;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class SupabaseClient {
    private static final String BASE_URL = "https://xaekxlyllgjxneyhurfp.supabase.co/rest/v1/";
    private static final String STORAGE_BASE_URL = "https://xaekxlyllgjxneyhurfp.supabase.co/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhhZWt4bHlsbGdqeG5leWh1cmZwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjMxNzcxMDksImV4cCI6MjA3ODc1MzEwOX0.x2lg5DBnYJ_vWOjfNi3c7ODyrBpOgIPia3oZ5htWsNM";

    private static Retrofit retrofit = null;
    private static Retrofit storageRetrofit = null;

    public static SupabaseService getService() {
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        HttpUrl url = original.url();

                        if (url.queryParameter("username") != null && !url.queryParameter("username").startsWith("eq.")) {
                            HttpUrl newUrl = url.newBuilder()
                                    .removeAllQueryParameters("username")
                                    .addQueryParameter("username", "eq." + url.queryParameter("username"))
                                    .build();

                            Request newRequest = original.newBuilder()
                                    .url(newUrl)
                                    .header("Authorization", "Bearer " + API_KEY)
                                    .header("apikey", API_KEY)
                                    .header("Content-Type", "application/json")
                                    .build();

                            return chain.proceed(newRequest);
                        }

                        Request newRequest = original.newBuilder()
                                .header("Authorization", "Bearer " + API_KEY)
                                .header("apikey", API_KEY)
                                .header("Content-Type", "application/json")
                                .build();

                        return chain.proceed(newRequest);
                    })
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(SupabaseService.class);
    }

    public static SupabaseService getStorageService() {
        if (storageRetrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        Request newRequest = original.newBuilder()
                                .header("Authorization", "Bearer " + API_KEY)
                                .header("apikey", API_KEY)
                                .header("Content-Type", "multipart/form-data")
                                .build();
                        return chain.proceed(newRequest);
                    })
                    .build();

            storageRetrofit = new Retrofit.Builder()
                    .baseUrl(STORAGE_BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return storageRetrofit.create(SupabaseService.class);
    }
}