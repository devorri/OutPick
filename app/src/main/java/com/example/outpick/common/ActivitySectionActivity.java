package com.example.outpick.common;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.cardview.widget.CardView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.outpick.R;
import com.example.outpick.admin.AdminDashboardActivity;
import com.example.outpick.admin.UserActivityLogsActivity;

public class ActivitySectionActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private CardView cardActivityLogs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity_section);

        // --- Back Button ---
        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> navigateToAdminDashboard());

        // --- User Activity Logs Card ---
        cardActivityLogs = findViewById(R.id.cardActivityLogs);
        cardActivityLogs.setOnClickListener(v -> {
            Intent intent = new Intent(ActivitySectionActivity.this, UserActivityLogsActivity.class);
            startActivity(intent);
        });
    }

    // Override the system back button
    @SuppressLint("MissingSuperCall") // suppresses the lint warning
    @Override
    public void onBackPressed() {
        navigateToAdminDashboard();
    }

    // Navigate back to AdminDashboardActivity
    private void navigateToAdminDashboard() {
        Intent intent = new Intent(ActivitySectionActivity.this, AdminDashboardActivity.class);
        // Ensures no duplicate dashboard in back stack
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish(); // Close this activity
    }
}
