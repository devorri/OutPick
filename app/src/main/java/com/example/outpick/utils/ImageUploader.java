package com.example.outpick.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.google.gson.JsonObject;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ImageUploader {
    private static final String TAG = "ImageUploader";
    private SupabaseService supabaseService;
    private Context context;

    public ImageUploader(Context context) {
        this.context = context;
        this.supabaseService = SupabaseClient.getStorageService(); // Use storage service
    }

    public interface UploadCallback {
        void onSuccess(String publicUrl);
        void onError(String error);
    }

    public void uploadImage(Uri imageUri, String bucketName, String fileName, UploadCallback callback) {
        try {
            File file = getFileFromUri(imageUri, fileName);
            if (file == null) {
                callback.onError("Could not convert URI to file");
                return;
            }

            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", fileName, requestFile);

            String filePath = fileName; // You can add folders: "outfits/" + fileName

            Call<JsonObject> call = supabaseService.uploadFile(bucketName, filePath, body);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (response.isSuccessful()) {
                        // Construct public URL for the uploaded file
                        String publicUrl = "https://xaekxlyllgjxneyhurfp.supabase.co/storage/v1/object/public/" +
                                bucketName + "/" + filePath;
                        Log.d(TAG, "Upload successful: " + publicUrl);
                        callback.onSuccess(publicUrl);
                    } else {
                        String error = "Upload failed: " + response.code();
                        if (response.errorBody() != null) {
                            try {
                                error += " - " + response.errorBody().string();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        Log.e(TAG, error);
                        callback.onError(error);
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.e(TAG, "Upload network error: " + t.getMessage());
                    callback.onError("Network error: " + t.getMessage());
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Upload error: " + e.getMessage());
            callback.onError("Upload error: " + e.getMessage());
        }
    }

    private File getFileFromUri(Uri uri, String fileName) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                return null;
            }

            File file = new File(context.getCacheDir(), fileName);
            FileOutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();
            return file;
        } catch (Exception e) {
            Log.e(TAG, "Error converting URI to file: " + e.getMessage());
            return null;
        }
    }
}