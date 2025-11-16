package com.example.outpick.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.outpick.R;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserEditActivity extends AppCompatActivity {

    private EditText editUsername, editCurrentPassword, editNewPassword;
    private Spinner spinnerGender, spinnerRole;
    private ImageButton btnTogglePassword, btnEditPassword, btnBack;
    private MaterialButton btnSave, btnCancel;
    private LinearLayout linearNewPassword;

    private boolean isPasswordVisible = false;
    private String userId;
    private SupabaseService supabaseService;
    private JsonObject currentUser;

    private static final String TAG = "UserEditActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_edit);

        // Initialize Supabase service
        supabaseService = SupabaseClient.getService();

        // --- Bind views ---
        editUsername = findViewById(R.id.editUsername);
        editCurrentPassword = findViewById(R.id.editCurrentPassword);
        editNewPassword = findViewById(R.id.editNewPassword);
        spinnerGender = findViewById(R.id.spinnerGender);
        spinnerRole = findViewById(R.id.spinnerRole);

        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        btnEditPassword = findViewById(R.id.btnEditPassword);
        btnBack = findViewById(R.id.btnBack);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        linearNewPassword = findViewById(R.id.linearNewPassword);

        // --- Setup Gender Spinner ---
        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(this,
                R.array.gender_array, android.R.layout.simple_spinner_item);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(genderAdapter);

        // --- Setup Role Spinner ---
        ArrayAdapter<CharSequence> roleAdapter = ArrayAdapter.createFromResource(this,
                R.array.role_array, android.R.layout.simple_spinner_item);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(roleAdapter);

        // --- Get userId from intent ---
        Intent intent = getIntent();
        userId = intent.getStringExtra("userId");

        if (userId != null && !userId.isEmpty()) {
            loadUserData(userId, genderAdapter, roleAdapter);
        } else {
            Toast.makeText(this, "Error: No user ID provided", Toast.LENGTH_SHORT).show();
            finish();
        }

        // --- Toggle current password visibility ---
        btnTogglePassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                editCurrentPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                btnTogglePassword.setImageResource(R.drawable.ic_eye_closed);
            } else {
                editCurrentPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                btnTogglePassword.setImageResource(R.drawable.ic_eye_open);
            }
            editCurrentPassword.setSelection(editCurrentPassword.getText().length());
            isPasswordVisible = !isPasswordVisible;
        });

        // --- Toggle New Password section when pen is clicked ---
        btnEditPassword.setOnClickListener(v -> {
            if (linearNewPassword.getVisibility() == View.GONE) {
                linearNewPassword.setVisibility(View.VISIBLE);
            } else {
                linearNewPassword.setVisibility(View.GONE);
                editNewPassword.setText(""); // clear input if hidden again
            }
        });

        // --- Back & Cancel ---
        btnBack.setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> finish());

        // --- Save changes ---
        btnSave.setOnClickListener(v -> saveUserChanges());
    }

    private void loadUserData(String userId, ArrayAdapter<CharSequence> genderAdapter, ArrayAdapter<CharSequence> roleAdapter) {
        Log.d(TAG, "Loading user data for ID: " + userId);

        // First, try the RPC method that should return password
        JsonObject params = new JsonObject();
        params.addProperty("user_id", userId);

        Call<List<JsonObject>> rpcCall = supabaseService.getUserByIdWithPasswordRpc(params);
        rpcCall.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    currentUser = response.body().get(0);
                    Log.d(TAG, "User data received via RPC: " + currentUser.toString());
                    debugUserData(currentUser);
                    populateUserData(genderAdapter, roleAdapter);
                } else {
                    Log.e(TAG, "RPC method failed. Code: " + response.code() + ", Message: " + response.message());
                    // Fallback to regular getUsers method
                    loadUserWithFallback(userId, genderAdapter, roleAdapter);
                }
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                Log.e(TAG, "RPC method error: " + t.getMessage());
                // Fallback to regular getUsers method
                loadUserWithFallback(userId, genderAdapter, roleAdapter);
            }
        });
    }

    private void loadUserWithFallback(String userId, ArrayAdapter<CharSequence> genderAdapter, ArrayAdapter<CharSequence> roleAdapter) {
        // Fallback: Get all users and find the one we need
        Call<List<JsonObject>> call = supabaseService.getUsers();

        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Find the user with matching ID
                    for (JsonObject user : response.body()) {
                        if (user.has("id") && userId.equals(user.get("id").getAsString())) {
                            currentUser = user;
                            Log.d(TAG, "User found via getUsers(): " + currentUser.toString());
                            debugUserData(currentUser);
                            populateUserData(genderAdapter, roleAdapter);
                            return;
                        }
                    }
                    Toast.makeText(UserEditActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "Fallback also failed. Code: " + response.code());
                    Toast.makeText(UserEditActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                Log.e(TAG, "Fallback error: " + t.getMessage());
                Toast.makeText(UserEditActivity.this, "Error loading user: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void debugUserData(JsonObject user) {
        Log.d(TAG, "=== DEBUG USER DATA ===");
        Log.d(TAG, "Total fields: " + user.keySet().size());
        for (String key : user.keySet()) {
            if (user.get(key).isJsonNull()) {
                Log.d(TAG, key + ": NULL");
            } else {
                try {
                    Log.d(TAG, key + ": " + user.get(key).getAsString());
                } catch (Exception e) {
                    Log.d(TAG, key + ": [Complex type - not a string]");
                }
            }
        }
        Log.d(TAG, "=== END DEBUG ===");
    }

    private void populateUserData(ArrayAdapter<CharSequence> genderAdapter, ArrayAdapter<CharSequence> roleAdapter) {
        // Populate UI with user data
        if (currentUser.has("username") && !currentUser.get("username").isJsonNull()) {
            editUsername.setText(currentUser.get("username").getAsString());
        } else {
            editUsername.setText("");
        }

        // Check if password field exists and is not null
        if (currentUser.has("password") && !currentUser.get("password").isJsonNull()) {
            String password = currentUser.get("password").getAsString();
            editCurrentPassword.setText(password);
            Log.d(TAG, "Password loaded successfully");
        } else {
            editCurrentPassword.setText("");
            Log.d(TAG, "Password field not available in response");
            // This is normal - passwords are often hidden for security
        }

        // Set gender
        if (currentUser.has("gender") && !currentUser.get("gender").isJsonNull()) {
            String gender = currentUser.get("gender").getAsString();
            int position = genderAdapter.getPosition(gender);
            if (position >= 0) {
                spinnerGender.setSelection(position);
            } else {
                spinnerGender.setSelection(0); // Default to first option
            }
        }

        // Set role
        if (currentUser.has("role") && !currentUser.get("role").isJsonNull()) {
            String role = currentUser.get("role").getAsString();
            int position = roleAdapter.getPosition(role);
            if (position >= 0) {
                spinnerRole.setSelection(position);
            } else {
                spinnerRole.setSelection(roleAdapter.getPosition("User")); // Default to User
            }
        }
    }

    private void saveUserChanges() {
        String newUsername = editUsername.getText().toString().trim();
        String newPassword = editNewPassword.getText().toString().trim();
        String selectedGender = spinnerGender.getSelectedItem().toString();
        String selectedRole = spinnerRole.getSelectedItem().toString();

        if (newUsername.isEmpty()) {
            Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create update object
        JsonObject updates = new JsonObject();
        updates.addProperty("username", newUsername);
        updates.addProperty("gender", selectedGender);
        updates.addProperty("role", selectedRole);

        // Only update password if a new one was provided
        if (!newPassword.isEmpty()) {
            if (newPassword.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }
            updates.addProperty("password", newPassword);
            Log.d(TAG, "Password will be updated");
        }

        Log.d(TAG, "Saving user updates: " + updates.toString());

        // Update user in Supabase
        Call<JsonObject> call = supabaseService.updateUserById(userId, updates);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "User updated successfully");
                    Toast.makeText(UserEditActivity.this, "User updated successfully!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    String errorMessage = "Failed to update user";
                    try {
                        if (response.errorBody() != null) {
                            errorMessage = "Error: " + response.code() + " - " + response.errorBody().string();
                        }
                    } catch (Exception e) {
                        errorMessage = "Error: " + response.code();
                    }
                    Toast.makeText(UserEditActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Error updating user: " + t.getMessage());
                Toast.makeText(UserEditActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}