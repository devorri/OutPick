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
import com.google.gson.JsonObject;

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
    private String userId; // ✅ CHANGED: Store user ID instead of username
    private String currentUsername;
    private SupabaseService supabaseService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_profile);

        // Initialize Supabase service
        supabaseService = SupabaseClient.getService();

        profileImage = findViewById(R.id.profileImage);
        usernameEdit = findViewById(R.id.usernameEdit);
        continueButton = findViewById(R.id.continueButton);
        skipButton = findViewById(R.id.skipButton);

        // Load user data from SharedPreferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        userId = prefs.getString("user_id", null); // ✅ CHANGED: Get user ID
        currentUsername = prefs.getString("username", "");

        if (userId == null) {
            Toast.makeText(this, "Session error. Please log in again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Pre-fill the username field
        if (!currentUsername.isEmpty()) {
            usernameEdit.setText(currentUsername);
        }

        // Set up listeners
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

                // Set image to fit XY inside circular ImageView
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

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        String imageUriToSave = selectedImageUri != null ? selectedImageUri.toString() : null;

        // Save locally to SharedPreferences
        editor.putString("username", newDisplayName);
        if (imageUriToSave != null) {
            editor.putString("profile_image_uri", imageUriToSave);
        }

        // ✅ FIXED: Use USER ID instead of username for profile setup flag
        editor.putBoolean("profile_setup_done_" + userId, true);
        editor.apply();

        // Save to Supabase
        updateProfileInSupabase(newDisplayName, imageUriToSave);
    }

    private void updateProfileInSupabase(String newDisplayName, String imageUri) {
        JsonObject updates = new JsonObject();
        updates.addProperty("username", newDisplayName);
        if (imageUri != null) {
            updates.addProperty("profile_image_uri", imageUri);
        }

        // ✅ FIXED: Use the current username to find the user, then update with new username
        Call<JsonObject> call = supabaseService.updateUser(currentUsername, updates);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(LoginProfileActivity.this, "Profile set successfully!", Toast.LENGTH_SHORT).show();
                    goToMainScreen();
                } else {
                    Toast.makeText(LoginProfileActivity.this, "Failed to save profile to server", Toast.LENGTH_SHORT).show();
                    // Still proceed to main screen since SharedPreferences are saved
                    goToMainScreen();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(LoginProfileActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                // Still proceed to main screen since SharedPreferences are saved
                goToMainScreen();
            }
        });
    }

    private void skipSetupAndContinue() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // ✅ FIXED: Use USER ID instead of username for profile setup flag
        editor.putBoolean("profile_setup_done_" + userId, true);
        editor.apply();

        Toast.makeText(this, "Skipped profile setup.", Toast.LENGTH_SHORT).show();
        goToMainScreen();
    }

    private void goToMainScreen() {
        Intent intent = new Intent(LoginProfileActivity.this, MainActivity.class);
        intent.putExtra("user_id", userId);
        intent.putExtra("username", currentUsername);
        startActivity(intent);
        finish();
    }
}