package com.example.outpick.auth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.outpick.admin.AdminDashboardActivity;
import com.example.outpick.MainActivity;
import com.example.outpick.R;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "UserPrefs";
    public static final String PREF_IMMUTABLE_USERNAME = "immutable_username";
    private static final String TAG = "LoginActivity";

    private EditText usernameEditText, passwordEditText;
    private TextView textGoToSignup, tvUsernameError, tvPasswordError;
    private ImageView togglePassword;
    private Button buttonLogin;
    private boolean isPasswordVisible = false;

    private SupabaseService supabaseService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Supabase service
        supabaseService = SupabaseClient.getService();

        // Initialize views
        usernameEditText = findViewById(R.id.UsernameEditText);
        passwordEditText = findViewById(R.id.PasswordEditText);
        togglePassword = findViewById(R.id.togglePassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textGoToSignup = findViewById(R.id.textGoToSignup);
        tvUsernameError = findViewById(R.id.tvUsernameError);
        tvPasswordError = findViewById(R.id.tvPasswordError);

        // Test connection (optional - remove in production)
        testSupabaseConnection();

        // Toggle password visibility
        togglePassword.setOnClickListener(v -> {
            if (passwordEditText.getInputType() ==
                    (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                togglePassword.setImageResource(R.drawable.ic_eye_off);
            } else {
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PASSWORD);
                togglePassword.setImageResource(R.drawable.ic_eye);
            }
            passwordEditText.setSelection(passwordEditText.length());
        });

        // Login button click
        buttonLogin.setOnClickListener(v -> handleLogin());

        // Go to Signup
        textGoToSignup.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
            finish();
        });
    }

    // Login validation + navigation
    private void handleLogin() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        tvUsernameError.setVisibility(View.GONE);
        tvPasswordError.setVisibility(View.GONE);

        if (username.isEmpty() || password.isEmpty()) {
            showBothErrors("Invalid username or password");
            return;
        }

        // Admin shortcut
        if (username.equals("admin0215") && password.equals("admin0215")) {
            // For admin, we need to get the actual user data from Supabase
            validateUserWithSupabase(username, password);
            return;
        }

        // Validate user via Supabase
        validateUserWithSupabase(username, password);
    }

    // Validate user with Supabase
    private void validateUserWithSupabase(String username, String password) {
        if (!isNetworkAvailable()) {
            Toast.makeText(LoginActivity.this, "No internet connection", Toast.LENGTH_LONG).show();
            return;
        }

        Call<List<JsonObject>> call = supabaseService.getUserByUsername(username);
        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                if (response.isSuccessful()) {
                    List<JsonObject> users = response.body();
                    if (users != null && !users.isEmpty()) {
                        JsonObject user = users.get(0);

                        // Check password
                        String storedPassword = user.has("password") ? user.get("password").getAsString() : "";
                        if (storedPassword.equals(password)) {
                            String role = user.has("role") ? user.get("role").getAsString() : "user";
                            String displayUsername = user.has("username") ? user.get("username").getAsString() : username;

                            // ✅ GET THE ACTUAL USER ID FROM SUPABASE
                            String userId = user.has("id") ? user.get("id").getAsString() : "";

                            updateLastLoginInSupabase(username);
                            handleSuccessfulLogin(userId, displayUsername, role);
                        } else {
                            showBothErrors("Invalid username or password");
                        }
                    } else {
                        showBothErrors("Invalid username or password");
                    }
                } else {
                    // Handle HTTP error responses
                    String errorMessage = "Login failed: ";
                    try {
                        if (response.errorBody() != null) {
                            errorMessage += response.errorBody().string();
                        } else {
                            errorMessage += "HTTP " + response.code();
                        }
                    } catch (Exception e) {
                        errorMessage += "HTTP " + response.code();
                    }
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "API Error: " + errorMessage);
                }
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                String errorMessage = "Network error: " + t.getMessage();
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Network error during login: " + t.getMessage());

                // Log detailed error info
                if (t instanceof java.net.UnknownHostException) {
                    Log.e(TAG, "Cannot resolve Supabase host - check internet connection");
                } else if (t instanceof java.net.SocketTimeoutException) {
                    Log.e(TAG, "Connection timeout - server might be down");
                } else if (t instanceof javax.net.ssl.SSLHandshakeException) {
                    Log.e(TAG, "SSL handshake failed");
                }
            }
        });
    }

    // Update last login in Supabase - FIXED VERSION
    private void updateLastLoginInSupabase(String username) {
        JsonObject updates = new JsonObject();
        updates.addProperty("last_login", new java.util.Date().toString());
        updates.addProperty("status", "Active");

        // ✅ FIXED: Use the corrected method that returns List<JsonObject>
        Call<List<JsonObject>> call = supabaseService.updateUser(username, updates);
        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                if (!response.isSuccessful()) {
                    Log.w(TAG, "Failed to update last login in Supabase");
                }
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                Log.w(TAG, "Network error updating last login: " + t.getMessage());
            }
        });
    }

    // Handle successful login
    private void handleSuccessfulLogin(String userId, String username, String role) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // ✅ SAVE ALL NECESSARY USER DATA
        editor.putString("user_id", userId); // This is what MainActivity needs!
        editor.putString("username", username);
        editor.putString("role", role);
        editor.putBoolean("is_logged_in", true);
        editor.putString(PREF_IMMUTABLE_USERNAME, username);

        editor.apply();

        Log.d(TAG, "✅ Saved user data - UserID: " + userId + ", Username: " + username);

        Intent intent;

        if ("admin".equalsIgnoreCase(role)) {
            intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
        } else {
            // ✅ FIXED: Use USER ID instead of username for profile setup check
            boolean isSetupDone = prefs.getBoolean("profile_setup_done_" + userId, false);

            if (!isSetupDone) {
                Log.d(TAG, "First-time login → redirect to profile setup");
                intent = new Intent(LoginActivity.this, LoginProfileActivity.class);
            } else {
                intent = new Intent(LoginActivity.this, MainActivity.class);
            }
        }

        intent.putExtra("user_id", userId);
        intent.putExtra("username", username);
        startActivity(intent);
        finish();
    }

    // Show username/password errors
    private void showBothErrors(String message) {
        tvUsernameError.setText(message);
        tvPasswordError.setText(message);
        tvUsernameError.setVisibility(View.VISIBLE);
        tvPasswordError.setVisibility(View.VISIBLE);
    }

    // Check network availability
    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        } catch (Exception e) {
            Log.e(TAG, "Error checking network availability: " + e.getMessage());
            return false;
        }
    }

    // Test Supabase connection
    private void testSupabaseConnection() {
        Call<List<JsonObject>> testCall = supabaseService.getUsers();
        testCall.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ Supabase connection successful! Users count: " +
                            (response.body() != null ? response.body().size() : 0));
                } else {
                    Log.e(TAG, "❌ Supabase connection failed: HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                Log.e(TAG, "❌ Supabase connection failed: " + t.getMessage());
            }
        });
    }
}