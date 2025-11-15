package com.example.outpick.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.outpick.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class UserAdminCreateOutfitActivity extends AppCompatActivity {

    private ImageButton btnBack, btnWorkspace, btnMenu;
    private String targetUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_admin_create_outfit);

        // ✅ GET THE TARGET USERNAME FROM INTENT
        Intent intent = getIntent();
        targetUsername = intent.getStringExtra("username");

        if (targetUsername == null || targetUsername.isEmpty()) {
            Toast.makeText(this, "Error: No user selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        btnBack = findViewById(R.id.btnBack);
        btnWorkspace = findViewById(R.id.btn_workspace);
        btnMenu = findViewById(R.id.btnMenu);

        // Back button
        btnBack.setOnClickListener(v -> onBackPressed());

        // Workspace button - show which user we're working with
        btnWorkspace.setOnClickListener(v ->
                Toast.makeText(this, "Creating outfit for: " + targetUsername, Toast.LENGTH_SHORT).show()
        );

        // 3-dot menu
        btnMenu.setOnClickListener(v -> showSelectMultipleBottomSheet());

        // ✅ SIMPLE: Just show a message that we're creating outfit for this user
        Toast.makeText(this, "Ready to create outfit for " + targetUsername, Toast.LENGTH_LONG).show();
    }

    private void showSelectMultipleBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this)
                .inflate(R.layout.bottom_closet_options_menu, null);
        bottomSheetDialog.setContentView(view);

        TextView selectMultipleOption = view.findViewById(R.id.selectMultipleOption);
        selectMultipleOption.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            // ✅ When selecting multiple, we're selecting from THIS USER'S items only
            Toast.makeText(this, "Would select items from " + targetUsername + "'s closet", Toast.LENGTH_SHORT).show();
        });

        bottomSheetDialog.show();
    }
}