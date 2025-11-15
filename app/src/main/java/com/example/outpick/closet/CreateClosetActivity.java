package com.example.outpick.closet;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.outpick.R;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateClosetActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private ImageView imageCover;
    private EditText editClosetName;
    private Uri selectedImageUri = null;

    private SupabaseService supabaseService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_closet);

        // Initialize Supabase
        supabaseService = SupabaseClient.getService();

        imageCover = findViewById(R.id.imageCover);
        editClosetName = findViewById(R.id.editClosetName);
        Button btnDone = findViewById(R.id.btnDone);
        ImageView btnBack = findViewById(R.id.btnBack);

        // Open gallery to pick image
        imageCover.setOnClickListener(v -> openImagePicker());

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Done button saves the closet to Supabase
        btnDone.setOnClickListener(v -> {
            String closetName = editClosetName.getText().toString().trim();

            if (closetName.isEmpty()) {
                editClosetName.setError("Please enter a closet name");
                return;
            }

            // Check for duplicate closet name in Supabase
            checkClosetNameExists(closetName);
        });
    }

    private void checkClosetNameExists(String closetName) {
        // First check if closet name already exists
        Call<List<JsonObject>> call = supabaseService.getClosets();
        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (JsonObject closet : response.body()) {
                        if (closet.has("name") && closet.get("name").getAsString().equalsIgnoreCase(closetName)) {
                            Toast.makeText(CreateClosetActivity.this, "A closet with this name already exists!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    // No duplicate found, proceed to save
                    saveClosetToSupabase(closetName);
                } else {
                    // If we can't check, still try to save (let Supabase handle unique constraint)
                    saveClosetToSupabase(closetName);
                }
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                // If check fails, still try to save
                saveClosetToSupabase(closetName);
            }
        });
    }

    private void saveClosetToSupabase(String closetName) {
        // Use default image if none selected
        String imageUri;
        if (selectedImageUri != null) {
            imageUri = selectedImageUri.toString();
        } else {
            imageUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.ic_closet).toString();
        }

        // Create closet object for Supabase
        JsonObject closet = new JsonObject();
        closet.addProperty("name", closetName);
        closet.addProperty("image_uri", imageUri);
        closet.addProperty("created_at", new java.util.Date().toString());

        // Save to Supabase
        Call<JsonObject> call = supabaseService.insertCloset(closet);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(CreateClosetActivity.this, "Closet saved to cloud!", Toast.LENGTH_SHORT).show();
                    finish(); // Go back to main screen
                } else {
                    Toast.makeText(CreateClosetActivity.this, "Failed to save closet", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(CreateClosetActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Open gallery
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    // Handle gallery result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            imageCover.setImageURI(selectedImageUri);
        }
    }
}