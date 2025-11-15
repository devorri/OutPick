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
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String PREFS_NAME = "UserPrefs";

    // Constants based on LoginActivity and BaseDrawerActivity
    public static final String PREF_IMMUTABLE_USERNAME = "immutable_username"; // The unique ID for DB lookups
    private static final String PREF_DISPLAY_NAME_KEY = "username"; // The mutable name for UI display
    private static final String PREF_PROFILE_IMAGE_URI = "profile_image_uri";

    // NOTE: Replace R.drawable.account_circle with the actual ID of your default profile image
    private static final int DEFAULT_PROFILE_IMAGE_RES_ID = R.drawable.account_circle;

    private EditText editUsername;
    private Button btnSave;
    private ImageButton btnBack;
    private CardView profileCard;
    private ImageView profileImagePlaceholder;

    private SupabaseService supabaseService;

    // CRITICAL: Hold the immutable ID and the mutable display name separately
    private String immutableLoginId;
    private String currentDisplayName;
    private Uri selectedImageUri; // Holds the URI if a new picture is selected or if existing one is loaded

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Initialize Supabase service
        supabaseService = SupabaseClient.getService();

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // CRITICAL FIX: Load the immutable ID (for DB lookups) and the mutable display name (for UI)
        immutableLoginId = prefs.getString(PREF_IMMUTABLE_USERNAME, null);
        currentDisplayName = prefs.getString(PREF_DISPLAY_NAME_KEY, null);

        // Initialize Views
        editUsername = findViewById(R.id.editUsername);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);
        profileCard = findViewById(R.id.profileCard);
        profileImagePlaceholder = findViewById(R.id.profileImagePlaceholder);

        // Show current display name in EditText
        if (currentDisplayName != null && !currentDisplayName.isEmpty()) {
            editUsername.setText(currentDisplayName);
        } else {
            editUsername.setText("");
            Log.w("EditProfileActivity", "User session not found. Display name is null or empty.");
            Toast.makeText(this, "User session not found. Please re-login.", Toast.LENGTH_LONG).show();
        }

        // Check if the immutable ID is missing (a critical session error)
        if (immutableLoginId == null || immutableLoginId.isEmpty()) {
            Log.e("EditProfileActivity", "CRITICAL ERROR: Immutable Login ID not found in session!");
            // Disable save button to prevent catastrophic DB errors
            btnSave.setEnabled(false);
        }

        // Load existing profile image (if any)
        loadProfileImage(DEFAULT_PROFILE_IMAGE_RES_ID);

        // 🔹 Back Button → go back to previous screen
        btnBack.setOnClickListener(v -> onBackPressed());

        // 🔹 Profile Card Click → Open Gallery
        profileCard.setOnClickListener(v -> openGallery());

        // 🔹 Save Button → update username and/or profile picture
        btnSave.setOnClickListener(v -> saveProfileChanges());
    }

    /**
     * Loads the profile image from SharedPreferences on activity creation.
     * Includes the CRITICAL try-catch block to handle lost URI permissions.
     * @param defaultImageResId The resource ID for the default profile image.
     */
    private void loadProfileImage(int defaultImageResId) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedUriString = prefs.getString(PREF_PROFILE_IMAGE_URI, null);

        // 1. Check if a URI string is saved
        if (savedUriString != null && !savedUriString.isEmpty()) {
            try {
                // Attempt to parse the URI
                Uri savedUri = Uri.parse(savedUriString);

                // Attempt to display the image. This line can throw SecurityException
                // if Android has revoked the app's read permission.
                profileImagePlaceholder.setImageURI(savedUri);

                // If loading succeeds, keep this URI in memory as the current state
                selectedImageUri = savedUri;
                Log.d("EditProfileActivity", "Loaded saved profile image successfully: " + savedUriString);

                // Set the scale type to CENTER_CROP to ensure the image fills the circular ImageView
                profileImagePlaceholder.setScaleType(ImageView.ScaleType.CENTER_CROP);

            } catch (SecurityException e) {
                // 2. Catch failure due to lost permission or bad URI
                Log.e("EditProfileActivity", "CRITICAL: Permission failure or bad URI. Failed to load saved profile image. Clearing URI.", e);

                // Clear the problematic URI from SharedPreferences and DB
                prefs.edit().remove(PREF_PROFILE_IMAGE_URI).apply();
                if (immutableLoginId != null) {
                    updateProfileImageUriInSupabase(null);
                }

                // Fallback to default
                profileImagePlaceholder.setImageResource(defaultImageResId);
                profileImagePlaceholder.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                selectedImageUri = null;
                Toast.makeText(this, "Profile picture failed to load. Please select a new one.", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                // Catch other exceptions like parsing errors
                Log.e("EditProfileActivity", "General failure loading profile image.", e);
                // Clear the problematic URI from SharedPreferences and DB
                prefs.edit().remove(PREF_PROFILE_IMAGE_URI).apply();
                if (immutableLoginId != null) {
                    updateProfileImageUriInSupabase(null);
                }
                profileImagePlaceholder.setImageResource(defaultImageResId);
                profileImagePlaceholder.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                selectedImageUri = null;
            }
        } else {
            // Set default if no URI is saved.
            profileImagePlaceholder.setImageResource(defaultImageResId);
            profileImagePlaceholder.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            selectedImageUri = null;
        }
    }

    /**
     * Opens the gallery using an Intent to pick an image.
     */
    private void openGallery() {
        // Use Intent.ACTION_GET_CONTENT which is generally better for requesting content
        // for which you need a long-term permission.
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();

            // **CRITICAL FIX**: Persist read permission for the URI using takePersistableUriPermission.
            try {
                // Request READ permission
                final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;

                if (imageUri != null) {
                    // Request persistent access from the Content Resolver
                    getContentResolver().takePersistableUriPermission(imageUri, takeFlags);
                    Log.d("EditProfileActivity", "Persisted URI permission successfully for: " + imageUri.toString());
                }
            } catch (SecurityException e) {
                // Log exception if permission fails but continue trying to use the URI for now
                Log.e("EditProfileActivity", "Failed to persist URI permission. Image may not persist.", e);
            }

            // Set the selected image to the circular ImageView immediately
            profileImagePlaceholder.setImageURI(imageUri);
            profileImagePlaceholder.setScaleType(ImageView.ScaleType.FIT_XY); // CRITICAL: Ensure image fills the circle
            selectedImageUri = imageUri; // Store the new URI temporarily

            Toast.makeText(this, "New picture selected. Click 'Save' to confirm.", Toast.LENGTH_SHORT).show();
        } else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Image selection cancelled.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handles the logic for updating the display name and saving the profile picture URI.
     */
    private void saveProfileChanges() {
        String newDisplayName = editUsername.getText().toString().trim();

        // Compare new input against the original loaded display name
        boolean displayNameChangeRequested = currentDisplayName != null && !newDisplayName.equals(currentDisplayName);

        // NOTE: This check handles if a new URI was selected or if the saved URI was cleared.
        boolean imageChangeRequested = (selectedImageUri != null && !isImageUriSaved(PREF_PROFILE_IMAGE_URI)) || (selectedImageUri == null && isImageUriSaved(PREF_PROFILE_IMAGE_URI));

        if (newDisplayName.isEmpty()) {
            Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show();
            return;
        }

        if (immutableLoginId == null || immutableLoginId.isEmpty()) {
            Toast.makeText(this, "Session Error: Cannot update profile. Please re-login.", Toast.LENGTH_LONG).show();
            return;
        }

        // Check if no effective changes were made
        if (!displayNameChangeRequested && !imageChangeRequested) {
            Toast.makeText(this, "No changes made", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Update profile in Supabase
        updateProfileInSupabase(newDisplayName, displayNameChangeRequested, imageChangeRequested);
    }

    private void updateProfileInSupabase(String newDisplayName, boolean updateDisplayName, boolean updateImage) {
        JsonObject updates = new JsonObject();

        if (updateDisplayName) {
            updates.addProperty("username", newDisplayName);
        }

        if (updateImage) {
            String uriToSave = selectedImageUri != null ? selectedImageUri.toString() : null;
            if (uriToSave != null) {
                updates.addProperty("profile_image_uri", uriToSave);
            } else {
                updates.addProperty("profile_image_uri", ""); // Clear the image
            }
        }

        Call<JsonObject> call = supabaseService.updateUser(immutableLoginId, updates);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    // Update SharedPreferences on success
                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();

                    if (updateDisplayName) {
                        editor.putString(PREF_DISPLAY_NAME_KEY, newDisplayName);
                        currentDisplayName = newDisplayName;
                    }

                    if (updateImage) {
                        String uriToSave = selectedImageUri != null ? selectedImageUri.toString() : null;
                        if (uriToSave != null) {
                            editor.putString(PREF_PROFILE_IMAGE_URI, uriToSave);
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
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(EditProfileActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                setResult(RESULT_CANCELED);
                finish();
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

        Call<JsonObject> call = supabaseService.updateUser(immutableLoginId, updates);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    Log.e("EditProfileActivity", "Failed to update profile image URI in Supabase");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e("EditProfileActivity", "Network error updating profile image: " + t.getMessage());
            }
        });
    }

    /**
     * Utility to check if the current selected URI is the one already saved
     * @param prefKey The SharedPreferences key where the URI is stored.
     */
    private boolean isImageUriSaved(String prefKey) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedUriString = prefs.getString(prefKey, null);

        // Return true if saved URI exists and is equal to the URI currently in memory
        return savedUriString != null && selectedImageUri != null && savedUriString.equals(selectedImageUri.toString());
    }
}