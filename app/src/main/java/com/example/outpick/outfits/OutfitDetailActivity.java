package com.example.outpick.outfits;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class OutfitDetailActivity extends AppCompatActivity {

    private TextView outfitTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        String outfitName = getIntent().getStringExtra("outfit_name");
        if (outfitName != null && !outfitName.trim().isEmpty()) {
            outfitTitle.setText(outfitName);
        } else {
            Toast.makeText(this, "No outfit name received!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
