package com.example.outpick.auth;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.outpick.R;
import com.example.outpick.common.BaseDrawerActivity;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ChangePasswordActivity
 * - Uses Supabase for password verification and update
 * - Verifies current password by reading from Supabase
 * - Updates password in Supabase via API
 */
public class ChangePasswordActivity extends AppCompatActivity {

    private static final String TAG = "ChangePasswordActivity";

    private ImageButton backButton;
    private TextInputEditText currentPasswordEditText, newPasswordEditText, confirmPasswordEditText;
    private Button changeButton;
    private SupabaseService supabaseService;
    private String currentUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_change_password);

        // Initialize Supabase service
        supabaseService = SupabaseClient.getService();

        // Get logged-in username from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUsername = prefs.getString(BaseDrawerActivity.PREF_IMMUTABLE_USERNAME, null);
        if (currentUsername == null) {
            // Try the legacy display-name key as fallback
            currentUsername = prefs.getString("username", null);
        }
        Log.d(TAG, "Loaded currentUsername from prefs: " + currentUsername);

        // UI views
        backButton = findViewById(R.id.backButton);
        currentPasswordEditText = findViewById(R.id.currentPasswordEditText);
        newPasswordEditText = findViewById(R.id.newPasswordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        changeButton = findViewById(R.id.changeButton);

        if (backButton != null) backButton.setOnClickListener(v -> finish());

        changeButton.setOnClickListener(v -> handleChangePassword());
    }

    private void handleChangePassword() {
        String currentPassword = currentPasswordEditText.getText() != null
                ? currentPasswordEditText.getText().toString().trim() : "";
        String newPassword = newPasswordEditText.getText() != null
                ? newPasswordEditText.getText().toString().trim() : "";
        String confirmPassword = confirmPasswordEditText.getText() != null
                ? confirmPasswordEditText.getText().toString().trim() : "";

        if (TextUtils.isEmpty(currentPassword) || TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUsername == null || currentUsername.isEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "currentUsername is null or empty in prefs");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPassword.equals(currentPassword)) {
            Toast.makeText(this, "New password cannot be the same as current", Toast.LENGTH_SHORT).show();
            return;
        }

        // First, get the user from Supabase to verify current password
        verifyAndUpdatePassword(currentPassword, newPassword);
    }

    private void verifyAndUpdatePassword(String currentPassword, String newPassword) {
        // Get user data from Supabase to verify current password
        Call<List<JsonObject>> getUserCall = supabaseService.getUserByUsername(currentUsername);
        getUserCall.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    JsonObject user = response.body().get(0); // Get first user from the list

                    // Verify current password
                    String storedPassword = user.has("password") ? user.get("password").getAsString() : "";
                    if (!storedPassword.equals(currentPassword)) {
                        Toast.makeText(ChangePasswordActivity.this, "Current password is incorrect", Toast.LENGTH_SHORT).show();
                        Log.i(TAG, "Password mismatch. entered:[" + currentPassword + "] stored:[" + storedPassword + "]");
                        return;
                    }

                    // Current password is correct, proceed with update
                    updatePasswordInSupabase(newPassword);
                } else {
                    Toast.makeText(ChangePasswordActivity.this, "Failed to verify user", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to get user from Supabase: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                Toast.makeText(ChangePasswordActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Network error getting user: " + t.getMessage());
            }
        });
    }

    private void updatePasswordInSupabase(String newPassword) {
        // Create update object
        JsonObject updates = new JsonObject();
        updates.addProperty("password", newPassword);

        // ✅ FIXED: Use the corrected method that returns List<JsonObject>
        Call<List<JsonObject>> updateCall = supabaseService.updateUser(currentUsername, updates);
        updateCall.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ChangePasswordActivity.this, "Password changed successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(ChangePasswordActivity.this, "Failed to update password", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Supabase updatePassword failed: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                Toast.makeText(ChangePasswordActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Network error updating password: " + t.getMessage());
            }
        });
    }
}