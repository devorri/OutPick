package com.example.outpick.common;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.outpick.R;
import com.example.outpick.admin.AdminDashboardActivity;

public class ContentBoardActivity extends AppCompatActivity {

    private CardView cardOutfits;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content_board);

        // Back button
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ContentBoardActivity.this, AdminDashboardActivity.class);
                startActivity(intent);
                finish(); // Close this activity
            }
        });

        // CardView navigation to ContentOutfitsActivity
        cardOutfits = findViewById(R.id.cardOutfits);
        cardOutfits.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ContentBoardActivity.this, ContentOutfitsActivity.class);
                startActivity(intent);
            }
        });
    }
}
