package com.example.outpick.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
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
        Call<List<JsonObject>> call = supabaseService.getUserById(userId);
        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    currentUser = response.body().get(0);

                    // Populate UI with user data
                    if (currentUser.has("username")) {
                        editUsername.setText(currentUser.get("username").getAsString());
                    }
                    if (currentUser.has("password")) {
                        editCurrentPassword.setText(currentUser.get("password").getAsString());
                    }
                    if (currentUser.has("gender")) {
                        String gender = currentUser.get("gender").getAsString();
                        spinnerGender.setSelection(genderAdapter.getPosition(gender));
                    }
                    if (currentUser.has("role")) {
                        String role = currentUser.get("role").getAsString();
                        spinnerRole.setSelection(roleAdapter.getPosition(role));
                    }
                } else {
                    Toast.makeText(UserEditActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                Toast.makeText(UserEditActivity.this, "Error loading user: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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
            updates.addProperty("password", newPassword);
        }

        // Update user in Supabase
        Call<JsonObject> call = supabaseService.updateUserById(userId, updates);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(UserEditActivity.this, "User updated successfully!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);   // notify previous activity to refresh RecyclerView
                    finish();
                } else {
                    Toast.makeText(UserEditActivity.this, "Failed to update user: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(UserEditActivity.this, "Error updating user: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}