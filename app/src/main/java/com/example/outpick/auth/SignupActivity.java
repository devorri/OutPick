package com.example.outpick.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.*;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.outpick.R;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupActivity extends AppCompatActivity {

    private EditText editTextUsername, editTextPassword, editTextConfirmPassword;
    private ImageView togglePassword, toggleConfirmPassword;
    private ImageButton buttonMale, buttonFemale;
    private Button buttonSignUp;
    private TextView textLoginLink;

    private String selectedGender = null;
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    private SupabaseService supabaseService;

    private static final String TAG = "SignupActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Supabase service
        supabaseService = SupabaseClient.getService();

        initializeViews();
        setupPasswordToggles();
        setupGenderSelection();
        setupSignUpButton();
        setupLoginLink();
    }

    private void initializeViews() {
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        togglePassword = findViewById(R.id.togglePassword);
        toggleConfirmPassword = findViewById(R.id.toggleConfirmPassword);
        buttonMale = findViewById(R.id.buttonMale);
        buttonFemale = findViewById(R.id.buttonFemale);
        buttonSignUp = findViewById(R.id.buttonSignUp);
        textLoginLink = findViewById(R.id.textLoginLink);
    }

    private void setupPasswordToggles() {
        // Toggle password visibility
        togglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            editTextPassword.setInputType(isPasswordVisible ?
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            togglePassword.setImageResource(isPasswordVisible ? R.drawable.ic_eye_off : R.drawable.ic_eye);
            editTextPassword.setSelection(editTextPassword.length());
        });

        toggleConfirmPassword.setOnClickListener(v -> {
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
            editTextConfirmPassword.setInputType(isConfirmPasswordVisible ?
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            toggleConfirmPassword.setImageResource(isConfirmPasswordVisible ? R.drawable.ic_eye_off : R.drawable.ic_eye);
            editTextConfirmPassword.setSelection(editTextConfirmPassword.length());
        });
    }

    private void setupGenderSelection() {
        // Gender selection
        buttonMale.setOnClickListener(v -> {
            selectedGender = "Male";
            buttonMale.setBackgroundResource(R.drawable.bg_gender_selected_male);
            buttonFemale.setBackgroundResource(R.drawable.bg_gender_unselected);
        });

        buttonFemale.setOnClickListener(v -> {
            selectedGender = "Female";
            buttonFemale.setBackgroundResource(R.drawable.bg_gender_selected_female);
            buttonMale.setBackgroundResource(R.drawable.bg_gender_unselected);
        });
    }

    private void setupSignUpButton() {
        // Sign-up logic
        buttonSignUp.setOnClickListener(v -> {
            String username = editTextUsername.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();
            String confirmPassword = editTextConfirmPassword.getText().toString().trim();

            if (!validateInputs(username, password, confirmPassword)) {
                return;
            }

            // Check if username exists in Supabase
            checkUsernameAndSignup(username, password);
        });
    }

    private void setupLoginLink() {
        // Go to Login screen
        textLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });
    }

    private boolean validateInputs(String username, String password, String confirmPassword) {
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (username.length() < 3) {
            Toast.makeText(this, "Username must be at least 3 characters", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (selectedGender == null) {
            Toast.makeText(this, "Please select a gender", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void checkUsernameAndSignup(String username, String password) {
        Log.d(TAG, "Checking username: " + username);

        // Show loading state
        buttonSignUp.setEnabled(false);
        buttonSignUp.setText("Checking...");

        // Check if username already exists in Supabase
        Call<List<JsonObject>> call = supabaseService.getUserByUsername(username);
        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (!response.body().isEmpty()) {
                        // Username already exists
                        Log.d(TAG, "Username already taken");
                        buttonSignUp.setEnabled(true);
                        buttonSignUp.setText("Sign Up");
                        Toast.makeText(SignupActivity.this, "Username already taken", Toast.LENGTH_SHORT).show();
                    } else {
                        // Username is available, proceed with signup
                        Log.d(TAG, "Username available, creating user");
                        createUserInSupabase(username, password);
                    }
                } else {
                    // If there's an error, still try to create the user
                    Log.w(TAG, "Username check failed, but trying to create user anyway. Response code: " + response.code());
                    createUserInSupabase(username, password);
                }
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                Log.e(TAG, "Network error during username check: " + t.getMessage());
                buttonSignUp.setEnabled(true);
                buttonSignUp.setText("Sign Up");
                Toast.makeText(SignupActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createUserInSupabase(String username, String password) {
        Log.d(TAG, "Creating user: " + username);
        buttonSignUp.setText("Creating Account...");

        try {
            // Generate dates in the correct format for your schema
            String currentDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

            // Create user object that matches your EXACT schema
            JsonObject user = new JsonObject();
            user.addProperty("username", username);
            user.addProperty("password", password);
            user.addProperty("gender", selectedGender);
            user.addProperty("role", "User");
            user.addProperty("status", "Active");
            user.addProperty("display_name", username); // Using username as display name
            user.addProperty("signup_date", currentDateTime); // Use signup_date instead of created_at
            user.addProperty("last_login", currentDateTime);
            user.addProperty("last_logout", "Never");
            user.addProperty("suspended", false);
            // Note: Do NOT include created_at (it's auto-generated)
            // Note: Do NOT include is_active (column doesn't exist)
            // Note: profile_image_uri is optional and will be null by default

            Log.d(TAG, "User object created: " + user.toString());

            // FIX: Change this to Call<JsonObject> to match the SupabaseService
            Call<JsonObject> call = supabaseService.insertUser(user);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "User created successfully");
                        Toast.makeText(SignupActivity.this, "Account created! Please log in.", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                        finish();
                    } else {
                        // Enhanced error handling
                        String errorMessage = "Failed to create account: ";
                        if (response.code() == 401) {
                            errorMessage += "Unauthorized - Check your Supabase API key";
                        } else if (response.code() == 403) {
                            errorMessage += "Forbidden - Check RLS policies";
                        } else if (response.code() == 404) {
                            errorMessage += "Endpoint not found";
                        } else {
                            errorMessage += "HTTP " + response.code();
                        }

                        try {
                            if (response.errorBody() != null) {
                                String errorBody = response.errorBody().string();
                                errorMessage += " - " + errorBody;
                                Log.e(TAG, "Error response: " + errorBody);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body", e);
                        }

                        Log.e(TAG, errorMessage);
                        Toast.makeText(SignupActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        buttonSignUp.setEnabled(true);
                        buttonSignUp.setText("Sign Up");
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.e(TAG, "Network error during signup: " + t.getMessage());
                    Toast.makeText(SignupActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    buttonSignUp.setEnabled(true);
                    buttonSignUp.setText("Sign Up");
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception during user creation: " + e.getMessage());
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            buttonSignUp.setEnabled(true);
            buttonSignUp.setText("Sign Up");
        }
    }
}