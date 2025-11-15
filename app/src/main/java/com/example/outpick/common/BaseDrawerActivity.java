package com.example.outpick.common;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.outpick.outfits.FavoritesActivity;
import com.example.outpick.outfits.OutfitHistoryActivity;
import com.example.outpick.R;
import com.example.outpick.database.models.UserModel;
import com.example.outpick.closet.YourClothesActivity;
import com.example.outpick.auth.EditProfileActivity;
import com.example.outpick.auth.LoginActivity;
import com.example.outpick.database.repositories.UserRepository;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public abstract class BaseDrawerActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    public static final int REQUEST_CODE_EDIT_PROFILE = 1001;
    public static final String PREF_PROFILE_IMAGE_URI = "profile_image_uri";
    public static final String PREF_IMMUTABLE_USERNAME = "immutable_username";
    public static final String PREF_USER_ID = "user_id";
    private static final String PREF_DISPLAY_NAME_KEY = "username";
    private static final String TAG = "BaseDrawerActivity";

    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected String immutableLoginId;
    protected String userId;
    protected SupabaseService supabaseService;
    protected UserRepository userRepository;

    private TextView usernameText;
    private ImageView profileImage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Supabase
        supabaseService = SupabaseClient.getService();
        userRepository = new UserRepository(supabaseService);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        immutableLoginId = prefs.getString(PREF_IMMUTABLE_USERNAME, null);
        userId = prefs.getString(PREF_USER_ID, null);
        String initialDisplayName = prefs.getString(PREF_DISPLAY_NAME_KEY, null);

        if (immutableLoginId == null) {
            Log.e(TAG, "Immutable Login ID is null on onCreate.");
            immutableLoginId = "";
        }

        if (immutableLoginId.isEmpty() && initialDisplayName != null) {
            String intentId = getIntent().getStringExtra("username");
            if (intentId != null) {
                immutableLoginId = intentId;
                Log.i(TAG, "Restored immutable ID from Intent: " + immutableLoginId);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDrawerHeader();
    }

    protected void setupDrawer(int drawerLayoutId, int navViewId) {
        drawerLayout = findViewById(drawerLayoutId);
        navigationView = findViewById(navViewId);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, R.string.app_name, R.string.app_name);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);

            View header = navigationView.getHeaderView(0);
            if (header != null) {
                usernameText = header.findViewById(R.id.nav_header_username);
                profileImage = header.findViewById(R.id.nav_header_profile_image);

                if (usernameText == null || profileImage == null) {
                    Log.e(TAG, "Header views not found. Check nav_header.xml IDs.");
                    return;
                }

                View.OnClickListener editProfileClickListener = v -> {
                    if (immutableLoginId == null || immutableLoginId.isEmpty()) {
                        Toast.makeText(this, "Session invalid. Please log in first.", Toast.LENGTH_SHORT).show();
                        handleLogout();
                        return;
                    }

                    Intent intent = new Intent(this, EditProfileActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_EDIT_PROFILE);
                };

                usernameText.setOnClickListener(editProfileClickListener);
                profileImage.setOnClickListener(editProfileClickListener);
            }

            updateDrawerHeader();
        }
    }

    protected void updateDrawerHeader() {
        if (usernameText == null || profileImage == null) {
            Log.w(TAG, "Drawer header views are null. Cannot update header.");
            return;
        }

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        immutableLoginId = prefs.getString(PREF_IMMUTABLE_USERNAME, "");
        userId = prefs.getString(PREF_USER_ID, "");

        if (immutableLoginId == null || immutableLoginId.isEmpty() || userId == null || userId.isEmpty()) {
            usernameText.setText(R.string.username_placeholder);
            profileImage.setImageResource(R.drawable.account_circle);
            return;
        }

        // Load user data from Supabase
        loadUserFromSupabase(userId);
    }

    private void loadUserFromSupabase(String userId) {
        Call<List<JsonObject>> call = supabaseService.getUserById(userId);
        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    JsonObject userJson = response.body().get(0);
                    updateHeaderWithUserData(userJson);
                } else {
                    // Fallback: try to get user by username
                    loadUserByUsername(immutableLoginId);
                }
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                Log.e(TAG, "Failed to load user from Supabase: " + t.getMessage());
                // Fallback: try to get user by username
                loadUserByUsername(immutableLoginId);
            }
        });
    }

    private void loadUserByUsername(String username) {
        Call<List<JsonObject>> call = supabaseService.getUserByUsername(username);
        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    JsonObject userJson = response.body().get(0);
                    updateHeaderWithUserData(userJson);

                    // Update stored user ID for future use
                    if (userJson.has("id")) {
                        String newUserId = userJson.get("id").getAsString();
                        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                        prefs.edit().putString(PREF_USER_ID, newUserId).apply();
                        userId = newUserId;
                    }
                } else {
                    setDefaultHeader();
                }
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                Log.e(TAG, "Failed to load user by username from Supabase: " + t.getMessage());
                setDefaultHeader();
            }
        });
    }

    private void updateHeaderWithUserData(JsonObject userJson) {
        runOnUiThread(() -> {
            try {
                String displayName = "";
                String profileUri = "";

                if (userJson.has("username")) {
                    displayName = userJson.get("username").getAsString();
                }
                if (userJson.has("profile_image_uri")) {
                    profileUri = userJson.get("profile_image_uri").getAsString();
                }

                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);

                if (displayName != null && !displayName.isEmpty()) {
                    usernameText.setText(displayName);
                    prefs.edit().putString(PREF_DISPLAY_NAME_KEY, displayName).apply();
                } else {
                    usernameText.setText(R.string.username_placeholder);
                }

                if (profileUri != null && !profileUri.isEmpty()) {
                    try {
                        Uri savedUri = Uri.parse(profileUri);
                        getContentResolver().takePersistableUriPermission(savedUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        profileImage.setScaleType(ImageView.ScaleType.FIT_XY);
                        profileImage.setImageURI(savedUri);
                    } catch (Exception e) {
                        Log.e(TAG, "Profile URI invalid or access revoked. Falling back to default.", e);
                        profileImage.setImageResource(R.drawable.account_circle);
                        // Update Supabase with null profile image
                        updateProfileImageInSupabase(null);
                    }
                } else {
                    profileImage.setImageResource(R.drawable.account_circle);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating header with user data", e);
                setDefaultHeader();
            }
        });
    }

    private void updateProfileImageInSupabase(String imageUri) {
        if (userId == null || userId.isEmpty()) return;

        JsonObject updates = new JsonObject();
        updates.addProperty("profile_image_uri", imageUri);

        Call<JsonObject> call = supabaseService.updateUserById(userId, updates);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Failed to update profile image in Supabase");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Network error updating profile image in Supabase", t);
            }
        });
    }

    private void setDefaultHeader() {
        runOnUiThread(() -> {
            usernameText.setText(R.string.username_placeholder);
            profileImage.setImageResource(R.drawable.account_circle);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_EDIT_PROFILE && resultCode == RESULT_OK) {
            Log.d(TAG, "Returned from EditProfileActivity. Header refresh handled by onResume.");
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        drawerLayout.closeDrawers();
        Intent intent = null;
        int id = item.getItemId();

        String userToPassId = getImmutableLoginId();

        if (id == R.id.nav_all_clothes) {
            intent = new Intent(this, YourClothesActivity.class);
        } else if (id == R.id.nav_history) {
            intent = new Intent(this, OutfitHistoryActivity.class);
        } else if (id == R.id.nav_favorites) {
            intent = new Intent(this, FavoritesActivity.class);
        } else if (id == R.id.nav_setting) {
            intent = new Intent(this, SettingActivity.class);
        } else if (id == R.id.nav_logout) {
            handleLogout();
            return true;
        }

        if (intent != null) {
            intent.putExtra("username", userToPassId);
            startActivity(intent);
            return true;
        }

        return false;
    }

    private void handleLogout() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String currentUserId = prefs.getString(PREF_USER_ID, null);

        if (currentUserId != null && !currentUserId.isEmpty()) {
            // Update user status in Supabase
            updateUserStatusInSupabase(currentUserId, "Offline");
        }

        // Clear session info
        prefs.edit()
                .remove(PREF_IMMUTABLE_USERNAME)
                .remove(PREF_USER_ID)
                .remove("username")
                .apply();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void updateUserStatusInSupabase(String userId, String status) {
        JsonObject updates = new JsonObject();
        updates.addProperty("status", status);
        updates.addProperty("last_logout", new java.util.Date().toString());

        Call<JsonObject> call = supabaseService.updateUserById(userId, updates);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Failed to update user status in Supabase");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Network error updating user status in Supabase", t);
            }
        });
    }

    public String getImmutableLoginId() {
        return immutableLoginId != null ? immutableLoginId : "";
    }
}