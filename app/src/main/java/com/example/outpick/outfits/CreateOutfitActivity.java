package com.example.outpick.outfits;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.outpick.R;
import com.example.outpick.common.PreviewImageActivity;
import com.example.outpick.utils.ImageUploader;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CreateOutfitActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1001;
    private static final int PICK_IMAGE_REQUEST = 101;
    private static final int PERMISSION_REQUEST_CODE = 200;

    private Uri photoUri;
    private ImageUploader imageUploader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_outfit);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize ImageUploader
        imageUploader = new ImageUploader(this);

        // Back arrow
        ImageView backArrow = findViewById(R.id.backArrow);
        backArrow.setOnClickListener(v -> onBackPressed());

        // Album button
        LinearLayout btnAlbum = findViewById(R.id.btnAlbum);
        btnAlbum.setOnClickListener(v -> openGallery());

        // Camera button
        LinearLayout btnCamera = findViewById(R.id.btnCamera);
        btnCamera.setOnClickListener(v -> {
            if (checkPermissions()) {
                openCamera();
            }
        });
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES},
                        PERMISSION_REQUEST_CODE);
                return false;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
                return false;
            }
        }
        return true;
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".fileprovider",
                        photoFile
                );
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                if (photoUri != null) {
                    // Upload to cloud first, then go to preview
                    uploadImageToCloud(photoUri, "camera_capture");
                }
            } else if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
                Uri selectedImageUri = data.getData();
                // Upload to cloud first, then go to preview
                uploadImageToCloud(selectedImageUri, "gallery_selection");
            }
        }
    }

    /**
     * Upload image to cloud storage and then proceed to preview
     */
    private void uploadImageToCloud(Uri imageUri, String source) {
        showLoading("Uploading image...");

        String fileName = generateFileName(source);
        // FIXED: Use the correct bucket name "clothing"
        String folder = "clothing"; // Use the actual working bucket name

        imageUploader.uploadImage(imageUri, folder, fileName, new ImageUploader.UploadCallback() {
            @Override
            public void onSuccess(String publicImageUrl) {
                runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(CreateOutfitActivity.this, "Image uploaded successfully!", Toast.LENGTH_SHORT).show();

                    // Go to preview with both cloud URL and local URI
                    goToPreview(publicImageUrl, imageUri.toString());
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(CreateOutfitActivity.this,
                            "Upload failed: " + error + ". Using local image.", Toast.LENGTH_LONG).show();

                    // Fallback: proceed with local URI only
                    goToPreview(null, imageUri.toString());
                });
            }
        });
    }

    private String generateFileName(String source) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return source + "_" + timeStamp + ".jpg";
    }

    private void goToPreview(String cloudImageUrl, String localImageUri) {
        Intent intent = new Intent(CreateOutfitActivity.this, PreviewImageActivity.class);

        // Pass BOTH parameters with clear names
        if (cloudImageUrl != null) {
            intent.putExtra("cloud_image_url", cloudImageUrl);
            Log.d("CreateOutfitActivity", "Passing cloud URL: " + cloudImageUrl);
        }

        intent.putExtra("local_image_uri", localImageUri);
        Log.d("CreateOutfitActivity", "Passing local URI: " + localImageUri);

        startActivity(intent);
    }

    private void showLoading(String message) {
        // You can implement a progress dialog here
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        // Disable buttons during upload
        findViewById(R.id.btnCamera).setEnabled(false);
        findViewById(R.id.btnAlbum).setEnabled(false);
    }

    private void hideLoading() {
        // Re-enable buttons
        findViewById(R.id.btnCamera).setEnabled(true);
        findViewById(R.id.btnAlbum).setEnabled(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                openCamera();
            } else {
                Toast.makeText(this, "Permissions are required to use the camera", Toast.LENGTH_SHORT).show();
            }
        }
    }
}