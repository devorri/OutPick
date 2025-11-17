package com.example.outpick.admin;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.example.outpick.common.ActivitySectionActivity;
import com.example.outpick.common.BaseDrawerAdminActivity;
import com.example.outpick.common.ContentBoardActivity;
import com.example.outpick.R;
import com.example.outpick.auth.LoginActivity;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminDashboardActivity extends BaseDrawerAdminActivity {

    private CardView cardUsers, cardContent, cardActivity, cardAddOutfit;
    private String currentUsername; // store logged-in admin username
    private SupabaseService supabaseService;
    private static final String TAG = "AdminDashboardActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Initialize Supabase service
        supabaseService = SupabaseClient.getService();

        // Get the logged-in username from SharedPreferences
        SharedPreferences sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUsername = sharedPref.getString("username", "Admin");

        // Setup the drawer with current username
        setupDrawer(currentUsername);

        // Profile icon click opens the drawer
        ImageButton btnProfile = findViewById(R.id.btnProfile);
        btnProfile.setOnClickListener(v -> openDrawer());

        // Initialize CardViews
        cardUsers = findViewById(R.id.cardUsers);
        cardContent = findViewById(R.id.cardContent);
        cardActivity = findViewById(R.id.cardActivity);
        cardAddOutfit = findViewById(R.id.cardAddOutfit);

        // --- Card Click Listeners ---

        // Users Card → AdminUserListActivity
        cardUsers.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AdminUserListActivity.class);
            startActivity(intent);
        });

        // Content Card → ContentBoardActivity
        cardContent.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, ContentBoardActivity.class);
            startActivity(intent);
        });

        // Activity Logs Card → ActivitySectionActivity
        cardActivity.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, ActivitySectionActivity.class);
            startActivity(intent);
        });

        // Add Outfit Card → AdminAddOutfitActivity
        cardAddOutfit.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AdminAddOutfitActivity.class);
            startActivity(intent);
        });

        // --- Navigation Drawer Logout Handling ---
        NavigationView navigationView = findViewById(R.id.nav_view_admin);
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_logout) {
                showLogoutConfirmation();
                return true;
            }

            return false;
        });
    }

    // --- Logout Confirmation Dialog ---
    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Update last logout in Supabase before leaving
                    updateLastLogoutInSupabase();
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void updateLastLogoutInSupabase() {
        // Create update object for last_logout
        JsonObject updates = new JsonObject();
        updates.addProperty("last_logout", new java.util.Date().toString());

        // ✅ FIXED: Use the corrected method that returns List<JsonObject>
        String url = "users?username=eq." + currentUsername;
        Call<List<JsonObject>> call = supabaseService.updateUserById(url, updates);
        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Successfully updated logout timestamp");
                    // Successfully updated last logout, proceed with logout
                    performLogout();
                } else {
                    // If update fails, still proceed with logout but show message
                    Log.e(TAG, "Failed to update logout timestamp: " + response.code());
                    Toast.makeText(AdminDashboardActivity.this, "Error updating logout time", Toast.LENGTH_SHORT).show();
                    performLogout();
                }
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                // If network error, still proceed with logout
                Log.e(TAG, "Network error updating logout: " + t.getMessage());
                Toast.makeText(AdminDashboardActivity.this, "Network error during logout", Toast.LENGTH_SHORT).show();
                performLogout();
            }
        });
    }

    private void performLogout() {
        // Clear SharedPreferences
        SharedPreferences sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.apply();

        // Go to login activity and clear back stack
        Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}