package com.example.outpick.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.outpick.MainActivity;
import com.example.outpick.R;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.example.outpick.utils.ImageUploader;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String PREFS_NAME = "UserPrefs";

    private ImageView profileImage;
    private EditText usernameEdit;
    private Button continueButton, skipButton;

    private Uri selectedImageUri;
    private String userId;
    private String currentUsername;
    private SupabaseService supabaseService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_profile);

        supabaseService = SupabaseClient.getService();

        profileImage = findViewById(R.id.profileImage);
        usernameEdit = findViewById(R.id.usernameEdit);
        continueButton = findViewById(R.id.continueButton);
        skipButton = findViewById(R.id.skipButton);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        userId = prefs.getString("user_id", null);
        currentUsername = prefs.getString("username", "");

        if (userId == null) {
            Toast.makeText(this, "Session error. Please log in again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!currentUsername.isEmpty()) {
            usernameEdit.setText(currentUsername);
        }

        profileImage.setOnClickListener(v -> openGallery());
        continueButton.setOnClickListener(v -> saveProfileAndContinue());
        skipButton.setOnClickListener(v -> skipSetupAndContinue());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        getContentResolver().takePersistableUriPermission(
                                selectedImageUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                    }
                } catch (SecurityException e) {
                    e.printStackTrace();
                }

                profileImage.setImageURI(selectedImageUri);
                profileImage.setScaleType(ImageView.ScaleType.FIT_XY);
            }
        }
    }

    private void saveProfileAndContinue() {
        String newDisplayName = usernameEdit.getText().toString().trim();

        if (newDisplayName.isEmpty()) {
            Toast.makeText(this, "Please enter a display name.", Toast.LENGTH_SHORT).show();
            return;
        }

        continueButton.setEnabled(false);
        continueButton.setText("Saving...");

        if (selectedImageUri != null) {
            uploadProfileImageAndSave(newDisplayName);
        } else {
            saveProfileToDatabase(newDisplayName, null);
        }
    }

    private void uploadProfileImageAndSave(String newDisplayName) {
        continueButton.setText("Uploading Image...");

        ImageUploader uploader = new ImageUploader(this);
        String fileName = "profile_" + userId + "_" + System.currentTimeMillis() + ".jpg";

        uploader.uploadImage(selectedImageUri, "profiles", fileName, new ImageUploader.UploadCallback() {
            @Override
            public void onSuccess(String cloudImageUrl) {
                saveProfileToDatabase(newDisplayName, cloudImageUrl);
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    continueButton.setEnabled(true);
                    continueButton.setText("Continue");
                    Toast.makeText(LoginProfileActivity.this,
                            "Failed to upload image", Toast.LENGTH_SHORT).show();
                    saveProfileToDatabase(newDisplayName, null);
                });
            }
        });
    }

    private void saveProfileToDatabase(String newDisplayName, String imageUrl) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("username", newDisplayName);
        if (imageUrl != null) {
            editor.putString("profile_image_uri", imageUrl);
        }

        editor.putBoolean("profile_setup_done_" + userId, true);
        editor.apply();

        updateProfileInSupabase(newDisplayName, imageUrl);
    }

    private void updateProfileInSupabase(String newDisplayName, String imageUrl) {
        JsonObject updates = new JsonObject();
        updates.addProperty("username", newDisplayName);
        if (imageUrl != null) {
            updates.addProperty("profile_image_uri", imageUrl);
        }

        // ✅ FIXED: Use the corrected method that returns List<JsonObject>
        Call<List<JsonObject>> call = supabaseService.updateUserById(userId, updates);
        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                runOnUiThread(() -> {
                    continueButton.setEnabled(true);
                    continueButton.setText("Continue");

                    if (response.isSuccessful()) {
                        Toast.makeText(LoginProfileActivity.this, "Profile set successfully!", Toast.LENGTH_SHORT).show();
                        goToMainScreen();
                    } else {
                        goToMainScreen();
                    }
                });
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                runOnUiThread(() -> {
                    continueButton.setEnabled(true);
                    continueButton.setText("Continue");
                    goToMainScreen();
                });
            }
        });
    }

    private void skipSetupAndContinue() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putBoolean("profile_setup_done_" + userId, true);
        editor.apply();

        Toast.makeText(this, "Skipped profile setup.", Toast.LENGTH_SHORT).show();
        goToMainScreen();
    }

    private void goToMainScreen() {
        Intent intent = new Intent(LoginProfileActivity.this, MainActivity.class);
        intent.putExtra("user_id", userId);
        intent.putExtra("username", usernameEdit.getText().toString().trim());
        startActivity(intent);
        finish();
    }
}