package com.example.outpick.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.outpick.common.ActivitySectionActivity;
import com.example.outpick.R;
import com.example.outpick.common.adapters.UserActivityLogsAdapter;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.example.outpick.database.models.UserModel;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserActivityLogsActivity extends AppCompatActivity {

    private static final String TAG = "UserActivityLogs";

    private RecyclerView recyclerView;
    private UserActivityLogsAdapter adapter;
    private SupabaseService supabaseService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logins_last_login);

        // Initialize Supabase service
        supabaseService = SupabaseClient.getService();

        // --- Back Button ---
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(UserActivityLogsActivity.this, ActivitySectionActivity.class);
            startActivity(intent);
            finish(); // optional: remove this activity from the stack
        });

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerViewUserLogs);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Load users from Supabase
        loadUsersFromSupabase();
    }

    private void loadUsersFromSupabase() {
        Call<List<JsonObject>> call = supabaseService.getUsers();
        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Convert JsonObject to UserModel
                    List<UserModel> userModels = convertToUserModels(response.body());

                    // Set up adapter with converted data
                    adapter = new UserActivityLogsAdapter(userModels);
                    recyclerView.setAdapter(adapter);

                    if (userModels.isEmpty()) {
                        Toast.makeText(UserActivityLogsActivity.this, "No users found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(UserActivityLogsActivity.this, "Failed to load user activity logs", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "API Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                Toast.makeText(UserActivityLogsActivity.this, "Error loading user activity: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Network error: " + t.getMessage());
            }
        });
    }

    private List<UserModel> convertToUserModels(List<JsonObject> jsonUsers) {
        List<UserModel> userModels = new ArrayList<>();

        for (JsonObject jsonUser : jsonUsers) {
            try {
                UserModel user = new UserModel();

                // ✅ SAFELY Set user ID (String from Supabase UUID)
                user.setId(getSafeString(jsonUser, "id", ""));

                // ✅ SAFELY Set basic user information
                user.setUsername(getSafeString(jsonUser, "username", "N/A"));
                user.setRole(getSafeString(jsonUser, "role", "User"));
                user.setGender(getSafeString(jsonUser, "gender", "Not specified"));

                // ✅ SAFELY Set dates - adjust field names based on your Supabase table structure
                user.setSignupDate(formatDate(getSafeString(jsonUser, "created_at", "Unknown")));
                user.setLastLogin(formatDate(getSafeString(jsonUser, "last_login", "Never")));
                user.setLastLogout(formatDate(getSafeString(jsonUser, "last_logout", "N/A")));

                // ✅ SAFELY Set status and suspended flag
                boolean isActive = getSafeBoolean(jsonUser, "is_active", true);
                user.setSuspended(!isActive);
                user.setStatus(isActive ? "Active" : "Suspended");

                // ✅ SAFELY Set profile image if available
                user.setProfileImageUri(getSafeString(jsonUser, "profile_image_uri", null));

                userModels.add(user);

            } catch (Exception e) {
                Log.e(TAG, "Error parsing user: " + e.getMessage(), e);
                // Continue with next user instead of crashing the whole app
            }
        }
        return userModels;
    }

    // ✅ SAFE method to get string values from JsonObject
    private String getSafeString(JsonObject json, String key, String defaultValue) {
        if (json == null || !json.has(key)) {
            return defaultValue;
        }

        JsonElement element = json.get(key);
        if (element == null || element.isJsonNull()) {
            return defaultValue;
        }

        try {
            return element.getAsString();
        } catch (Exception e) {
            Log.w(TAG, "Error getting string for key '" + key + "': " + e.getMessage());
            return defaultValue;
        }
    }

    // ✅ SAFE method to get boolean values
    private boolean getSafeBoolean(JsonObject json, String key, boolean defaultValue) {
        if (json == null || !json.has(key)) {
            return defaultValue;
        }

        JsonElement element = json.get(key);
        if (element == null || element.isJsonNull()) {
            return defaultValue;
        }

        try {
            return element.getAsBoolean();
        } catch (Exception e) {
            Log.w(TAG, "Error getting boolean for key '" + key + "': " + e.getMessage());
            return defaultValue;
        }
    }

    // Helper method to format dates if needed
    private String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty() || dateString.equals("null") || dateString.equals("Unknown") || dateString.equals("Never")) {
            return dateString; // Return as-is for these special values
        }

        // Simple date formatting - you can enhance this based on your needs
        try {
            // If the date is in ISO format (e.g., "2024-01-15T10:30:00Z")
            if (dateString.contains("T")) {
                // Extract just the date part
                String datePart = dateString.split("T")[0];
                return datePart;
            }
            return dateString;
        } catch (Exception e) {
            return dateString; // Return original if parsing fails
        }
    }
}