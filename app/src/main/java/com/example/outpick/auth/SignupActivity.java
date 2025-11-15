package com.example.outpick.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.*;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Supabase service
        supabaseService = SupabaseClient.getService();

        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        togglePassword = findViewById(R.id.togglePassword);
        toggleConfirmPassword = findViewById(R.id.toggleConfirmPassword);
        buttonMale = findViewById(R.id.buttonMale);
        buttonFemale = findViewById(R.id.buttonFemale);
        buttonSignUp = findViewById(R.id.buttonSignUp);
        textLoginLink = findViewById(R.id.textLoginLink);

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

        // Sign-up logic
        buttonSignUp.setOnClickListener(v -> {
            String username = editTextUsername.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();
            String confirmPassword = editTextConfirmPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedGender == null) {
                Toast.makeText(this, "Please select a gender", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if username exists in Supabase
            checkUsernameAndSignup(username, password);
        });

        // Go to Login screen
        textLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void checkUsernameAndSignup(String username, String password) {
        // Check if username already exists in Supabase
        Call<List<JsonObject>> call = supabaseService.getUserByUsername(username);
        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (!response.body().isEmpty()) {
                        // Username already exists
                        Toast.makeText(SignupActivity.this, "Username already taken", Toast.LENGTH_SHORT).show();
                    } else {
                        // Username is available, proceed with signup
                        createUserInSupabase(username, password);
                    }
                } else {
                    // If there's an error, still try to create the user
                    createUserInSupabase(username, password);
                }
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                Toast.makeText(SignupActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createUserInSupabase(String username, String password) {
        // Generate signup date
        String signupDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        // Create user object for Supabase
        JsonObject user = new JsonObject();
        user.addProperty("username", username);
        user.addProperty("password", password);
        user.addProperty("gender", selectedGender);
        user.addProperty("role", "User");
        user.addProperty("created_at", signupDate);
        user.addProperty("last_login", currentTime);
        user.addProperty("status", "Active");
        user.addProperty("is_active", true);

        // Insert user into Supabase
        Call<JsonObject> call = supabaseService.insertUser(user);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(SignupActivity.this, "Account created! Please log in.", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                    finish();
                } else {
                    Toast.makeText(SignupActivity.this, "Failed to create account", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(SignupActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}