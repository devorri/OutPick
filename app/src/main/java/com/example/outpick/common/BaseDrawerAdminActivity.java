package com.example.outpick.common;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.outpick.R;
import com.example.outpick.auth.LoginActivity;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BaseDrawerAdminActivity extends AppCompatActivity {

    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected SupabaseService supabaseService;
    protected String currentUserId;
    protected String currentUsername;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Supabase
        supabaseService = SupabaseClient.getService();

        // Load user data from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUserId = prefs.getString("user_id", "");
        currentUsername = prefs.getString("username", "Admin");
    }

    /**
     * Call this in onCreate() of AdminDashboardActivity
     * after setContentView()
     */
    protected void setupDrawer(String currentUsername) {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        if (navigationView != null) {
            // Set admin username in drawer header
            View headerView = navigationView.getHeaderView(0);
            TextView tvUsername = headerView.findViewById(R.id.nav_header_username);
            if (tvUsername != null) {
                tvUsername.setText(currentUsername != null ? currentUsername : "Admin");
            }

            // Handle drawer item clicks
            navigationView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();

                if (id == R.id.nav_logout) {
                    // Logout pressed
                    handleLogout();
                    return true;
                }

                drawerLayout.closeDrawers();
                return true;
            });
        }
    }

    /** Handles logout logic with Supabase */
    private void handleLogout() {
        // Update last logout timestamp in Supabase
        updateLastLogoutInSupabase();

        // Clear local user session
        clearUserSession();

        // Go back to LoginActivity
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    /** Update last logout timestamp in Supabase */
    private void updateLastLogoutInSupabase() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            return; // No user to update
        }

        JsonObject updates = new JsonObject();
        updates.addProperty("last_logout", System.currentTimeMillis()); // Or use proper timestamp format

        Call<JsonObject> call = supabaseService.updateUserById(currentUserId, updates);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                // Logout proceeds regardless of success/failure
                if (!response.isSuccessful()) {
                    // Silent fail - user can still logout
                    System.out.println("Failed to update logout timestamp in Supabase");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                // Silent fail - user can still logout
                System.out.println("Network error updating logout timestamp");
            }
        });
    }

    /** Clear user session from SharedPreferences */
    private void clearUserSession() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("user_id");
        editor.remove("username");
        editor.remove("email");
        editor.apply();
    }

    /** Open the drawer programmatically on the right side */
    protected void openDrawer() {
        if (drawerLayout != null) {
            drawerLayout.openDrawer(GravityCompat.END); // Drawer opens from the right
        }
    }

    /** Get current user ID for use in child activities */
    protected String getCurrentUserId() {
        return currentUserId;
    }

    /** Get current username for use in child activities */
    protected String getCurrentUsername() {
        return currentUsername;
    }
}