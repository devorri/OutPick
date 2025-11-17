package com.example.outpick.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.outpick.R;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.example.outpick.utils.ImageUploader;
import com.bumptech.glide.Glide;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String PREFS_NAME = "UserPrefs";

    // Constants based on LoginActivity and BaseDrawerActivity
    public static final String PREF_IMMUTABLE_USERNAME = "immutable_username";
    private static final String PREF_DISPLAY_NAME_KEY = "username";
    private static final String PREF_PROFILE_IMAGE_URI = "profile_image_uri";

    private static final int DEFAULT_PROFILE_IMAGE_RES_ID = R.drawable.account_circle;

    private EditText editUsername;
    private Button btnSave;
    private ImageButton btnBack;
    private CardView profileCard;
    private ImageView profileImagePlaceholder;

    private SupabaseService supabaseService;

    private String immutableLoginId;
    private String currentDisplayName;
    private Uri selectedImageUri;
    private boolean isUploading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Initialize Supabase service
        supabaseService = SupabaseClient.getService();

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        immutableLoginId = prefs.getString(PREF_IMMUTABLE_USERNAME, null);
        currentDisplayName = prefs.getString(PREF_DISPLAY_NAME_KEY, null);

        // Initialize Views
        editUsername = findViewById(R.id.editUsername);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);
        profileCard = findViewById(R.id.profileCard);
        profileImagePlaceholder = findViewById(R.id.profileImagePlaceholder);

        if (currentDisplayName != null && !currentDisplayName.isEmpty()) {
            editUsername.setText(currentDisplayName);
        } else {
            editUsername.setText("");
            Log.w("EditProfileActivity", "User session not found. Display name is null or empty.");
            Toast.makeText(this, "User session not found. Please re-login.", Toast.LENGTH_LONG).show();
        }

        if (immutableLoginId == null || immutableLoginId.isEmpty()) {
            Log.e("EditProfileActivity", "CRITICAL ERROR: Immutable Login ID not found in session!");
            btnSave.setEnabled(false);
        }

        // Load existing profile image (if any)
        loadProfileImage(DEFAULT_PROFILE_IMAGE_RES_ID);

        btnBack.setOnClickListener(v -> onBackPressed());
        profileCard.setOnClickListener(v -> openGallery());
        btnSave.setOnClickListener(v -> saveProfileChanges());
    }

    private void loadProfileImage(int defaultImageResId) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedUriString = prefs.getString(PREF_PROFILE_IMAGE_URI, null);

        if (savedUriString != null && !savedUriString.isEmpty()) {
            try {
                // Check if it's a cloud URL or local URI
                if (isCloudUrl(savedUriString)) {
                    // It's a cloud URL - load with Glide
                    Glide.with(this)
                            .load(savedUriString)
                            .placeholder(defaultImageResId)
                            .error(defaultImageResId)
                            .into(profileImagePlaceholder);
                } else {
                    // It's a local URI
                    Uri savedUri = Uri.parse(savedUriString);
                    profileImagePlaceholder.setImageURI(savedUri);
                }

                profileImagePlaceholder.setScaleType(ImageView.ScaleType.CENTER_CROP);
                selectedImageUri = Uri.parse(savedUriString); // Keep reference for comparison

            } catch (SecurityException e) {
                Log.e("EditProfileActivity", "Permission failure loading profile image", e);
                prefs.edit().remove(PREF_PROFILE_IMAGE_URI).apply();
                updateProfileImageUriInSupabase(null);
                setDefaultProfileImage(defaultImageResId);
            } catch (Exception e) {
                Log.e("EditProfileActivity", "General failure loading profile image", e);
                prefs.edit().remove(PREF_PROFILE_IMAGE_URI).apply();
                updateProfileImageUriInSupabase(null);
                setDefaultProfileImage(defaultImageResId);
            }
        } else {
            setDefaultProfileImage(defaultImageResId);
        }
    }

    private void setDefaultProfileImage(int defaultImageResId) {
        profileImagePlaceholder.setImageResource(defaultImageResId);
        profileImagePlaceholder.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        selectedImageUri = null;
    }

    private boolean isCloudUrl(String imageUri) {
        return imageUri != null && (
                imageUri.startsWith("https://") ||
                        imageUri.startsWith("http://") ||
                        imageUri.contains("supabase.co/storage") ||
                        imageUri.contains("xaekxlyllgjxneyhurfp.supabase.co")
        );
    }

    private void openGallery() {
        if (isUploading) {
            Toast.makeText(this, "Please wait...", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();

            try {
                getContentResolver().takePersistableUriPermission(imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException e) {
                Log.e("EditProfileActivity", "Failed to persist URI permission", e);
            }

            // Show preview with Glide
            Glide.with(this)
                    .load(imageUri)
                    .placeholder(DEFAULT_PROFILE_IMAGE_RES_ID)
                    .into(profileImagePlaceholder);
            profileImagePlaceholder.setScaleType(ImageView.ScaleType.CENTER_CROP);
            selectedImageUri = imageUri;

            Toast.makeText(this, "New picture selected. Click 'Save' to confirm.", Toast.LENGTH_SHORT).show();
        } else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Image selection cancelled.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveProfileChanges() {
        if (isUploading) {
            Toast.makeText(this, "Please wait...", Toast.LENGTH_SHORT).show();
            return;
        }

        String newDisplayName = editUsername.getText().toString().trim();

        boolean displayNameChangeRequested = currentDisplayName != null && !newDisplayName.equals(currentDisplayName);
        boolean imageChangeRequested = (selectedImageUri != null && !isImageUriSaved(PREF_PROFILE_IMAGE_URI)) ||
                (selectedImageUri == null && isImageUriSaved(PREF_PROFILE_IMAGE_URI));

        if (newDisplayName.isEmpty()) {
            Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show();
            return;
        }

        if (immutableLoginId == null || immutableLoginId.isEmpty()) {
            Toast.makeText(this, "Session Error: Cannot update profile. Please re-login.", Toast.LENGTH_LONG).show();
            return;
        }

        if (!displayNameChangeRequested && !imageChangeRequested) {
            Toast.makeText(this, "No changes made", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // If new image selected, upload it first
        if (imageChangeRequested && selectedImageUri != null) {
            uploadProfileImageAndSave(newDisplayName, displayNameChangeRequested);
        } else {
            // No image change or removing image
            updateProfileInSupabase(newDisplayName, displayNameChangeRequested, imageChangeRequested, null);
        }
    }

    private void uploadProfileImageAndSave(String newDisplayName, boolean updateDisplayName) {
        isUploading = true;
        btnSave.setEnabled(false);
        btnSave.setText("Uploading...");

        ImageUploader uploader = new ImageUploader(this);
        String fileName = "profile_" + immutableLoginId + "_" + System.currentTimeMillis() + ".jpg";

        uploader.uploadImage(selectedImageUri, "profiles", fileName, new ImageUploader.UploadCallback() {
            @Override
            public void onSuccess(String cloudImageUrl) {
                updateProfileInSupabase(newDisplayName, updateDisplayName, true, cloudImageUrl);
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    isUploading = false;
                    btnSave.setEnabled(true);
                    btnSave.setText("Save");
                    Toast.makeText(EditProfileActivity.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                    // Fallback: save with local URI
                    updateProfileInSupabase(newDisplayName, updateDisplayName, true, selectedImageUri.toString());
                });
            }
        });
    }

    private void updateProfileInSupabase(String newDisplayName, boolean updateDisplayName, boolean updateImage, String imageUrl) {
        runOnUiThread(() -> {
            btnSave.setText("Saving...");
        });

        JsonObject updates = new JsonObject();

        if (updateDisplayName) {
            updates.addProperty("username", newDisplayName);
        }

        if (updateImage) {
            String finalImageUrl = imageUrl != null ? imageUrl : (selectedImageUri != null ? selectedImageUri.toString() : "");
            updates.addProperty("profile_image_uri", finalImageUrl);
        }

        // ✅ FIXED: Use the corrected method that returns List<JsonObject>
        Call<List<JsonObject>> call = supabaseService.updateUser(immutableLoginId, updates);
        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                runOnUiThread(() -> {
                    isUploading = false;
                    btnSave.setEnabled(true);
                    btnSave.setText("Save");

                    if (response.isSuccessful()) {
                        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();

                        if (updateDisplayName) {
                            editor.putString(PREF_DISPLAY_NAME_KEY, newDisplayName);
                            currentDisplayName = newDisplayName;
                        }

                        if (updateImage) {
                            String finalImageUrl = imageUrl != null ? imageUrl : (selectedImageUri != null ? selectedImageUri.toString() : null);
                            if (finalImageUrl != null && !finalImageUrl.isEmpty()) {
                                editor.putString(PREF_PROFILE_IMAGE_URI, finalImageUrl);
                            } else {
                                editor.remove(PREF_PROFILE_IMAGE_URI);
                            }
                        }

                        editor.apply();

                        Toast.makeText(EditProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(EditProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                });
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                runOnUiThread(() -> {
                    isUploading = false;
                    btnSave.setEnabled(true);
                    btnSave.setText("Save");
                    Toast.makeText(EditProfileActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    setResult(RESULT_CANCELED);
                    finish();
                });
            }
        });
    }

    private void updateProfileImageUriInSupabase(String imageUri) {
        JsonObject updates = new JsonObject();
        if (imageUri != null) {
            updates.addProperty("profile_image_uri", imageUri);
        } else {
            updates.addProperty("profile_image_uri", "");
        }

        // ✅ FIXED: Use the corrected method that returns List<JsonObject>
        Call<List<JsonObject>> call = supabaseService.updateUser(immutableLoginId, updates);
        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                if (!response.isSuccessful()) {
                    Log.e("EditProfileActivity", "Failed to update profile image URI in Supabase");
                }
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                Log.e("EditProfileActivity", "Network error updating profile image: " + t.getMessage());
            }
        });
    }

    private boolean isImageUriSaved(String prefKey) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedUriString = prefs.getString(prefKey, null);
        return savedUriString != null && selectedImageUri != null && savedUriString.equals(selectedImageUri.toString());
    }
}