package com.example.outpick.common;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.outpick.R;
import com.example.outpick.auth.ChangePasswordActivity;
import com.example.outpick.auth.EditProfileActivity;

public class SettingActivity extends AppCompatActivity {

    private LinearLayout personalDataLine;
    private LinearLayout passwordLine;
    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        // 🔹 Back Button (returns to previous screen)
        backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        // 🔹 Personal Data Line → Go to EditProfileActivity
        personalDataLine = findViewById(R.id.personalDataLine);
        if (personalDataLine != null) {
            personalDataLine.setOnClickListener(v -> {
                Intent intent = new Intent(SettingActivity.this, EditProfileActivity.class);
                startActivity(intent);
            });
        }

        // 🔹 Password Line → Go to ChangePasswordActivity
        passwordLine = findViewById(R.id.passwordLine);
        if (passwordLine != null) {
            passwordLine.setOnClickListener(v -> {
                Intent intent = new Intent(SettingActivity.this, ChangePasswordActivity.class);
                startActivity(intent);
            });
        }
    }
}
