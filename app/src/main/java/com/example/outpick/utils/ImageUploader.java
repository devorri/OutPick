package com.example.outpick.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ImageUploader {
    private static final String TAG = "ImageUploader";
    private SupabaseService supabaseService;
    private Context context;

    public ImageUploader(Context context) {
        this.context = context;
        this.supabaseService = SupabaseClient.getStorageService();
    }

    public interface UploadCallback {
        void onSuccess(String publicUrl);
        void onError(String error);
    }

    // FIXED: Handle byte array uploads
    public void uploadImage(byte[] imageData, String bucketName, String fileName, UploadCallback callback) {
        try {
            Log.d(TAG, "Uploading byte array, size: " + imageData.length + " bytes");

            // Convert byte array to file with proper compression
            File tempFile = new File(context.getCacheDir(), fileName);
            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.write(imageData);
            fos.close();

            Log.d(TAG, "Temporary file created: " + tempFile.length() + " bytes");

            // Upload the file
            uploadFileToSupabase(tempFile, bucketName, fileName, callback);

        } catch (Exception e) {
            Log.e(TAG, "Error converting byte array to file: " + e.getMessage());
            callback.onError("Failed to process image: " + e.getMessage());
        }
    }

    // FIXED: Handle URI uploads
    public void uploadImage(Uri imageUri, String bucketName, String fileName, UploadCallback callback) {
        try {
            File file = getFileFromUri(imageUri, fileName);
            if (file == null || file.length() == 0) {
                callback.onError("Could not convert URI to file or file is empty");
                return;
            }

            Log.d(TAG, "File ready for upload: " + file.getAbsolutePath() + ", size: " + file.length() + " bytes");
            uploadFileToSupabase(file, bucketName, fileName, callback);

        } catch (Exception e) {
            Log.e(TAG, "Upload error: " + e.getMessage());
            callback.onError("Upload error: " + e.getMessage());
        }
    }

    // NEW: Common upload method
    private void uploadFileToSupabase(File file, String bucketName, String fileName, UploadCallback callback) {
        try {
            // Verify file is not empty
            if (file.length() == 0) {
                callback.onError("File is empty - 0 bytes");
                return;
            }

            // Determine correct MIME type
            MediaType mediaType = getMediaTypeForFile(fileName);
            RequestBody requestFile = RequestBody.create(mediaType, file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", fileName, requestFile);

            String filePath = fileName;

            Log.d(TAG, "Uploading to bucket: " + bucketName + ", file: " + filePath +
                    ", size: " + file.length() + " bytes, MIME: " + mediaType);

            Call<JsonObject> call = supabaseService.uploadFile(bucketName, filePath, body);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (response.isSuccessful()) {
                        String publicUrl = "https://xaekxlyllgjxneyhurfp.supabase.co/storage/v1/object/public/" +
                                bucketName + "/" + filePath;
                        Log.d(TAG, "Upload successful: " + publicUrl);
                        callback.onSuccess(publicUrl);
                    } else {
                        String error = "Upload failed: " + response.code();
                        if (response.errorBody() != null) {
                            try {
                                String errorBody = response.errorBody().string();
                                error += " - " + errorBody;
                                Log.e(TAG, "Error response: " + errorBody);
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

    // FIXED: Proper file conversion from URI
    private File getFileFromUri(Uri uri, String fileName) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Log.e(TAG, "Input stream is null for URI: " + uri);
                return null;
            }

            File file = new File(context.getCacheDir(), fileName);
            FileOutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[4096]; // Increased buffer size
            int bytesRead;
            int totalBytes = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }

            inputStream.close();
            outputStream.close();

            Log.d(TAG, "File created: " + file.getAbsolutePath() + ", size: " + totalBytes + " bytes");

            if (totalBytes == 0) {
                Log.e(TAG, "WARNING: File is 0 bytes! URI: " + uri);
                return null;
            }

            return file;
        } catch (Exception e) {
            Log.e(TAG, "Error converting URI to file: " + e.getMessage());
            return null;
        }
    }

    private MediaType getMediaTypeForFile(String fileName) {
        if (fileName.toLowerCase().endsWith(".png")) {
            return MediaType.parse("image/png");
        } else if (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")) {
            return MediaType.parse("image/jpeg");
        } else if (fileName.toLowerCase().endsWith(".gif")) {
            return MediaType.parse("image/gif");
        } else if (fileName.toLowerCase().endsWith(".webp")) {
            return MediaType.parse("image/webp");
        }
        return MediaType.parse("image/jpeg");
    }
}