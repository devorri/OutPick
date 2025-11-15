package com.example.outpick.outfits;

import android.os.Bundle;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.outpick.database.models.OutfitCombination;
import com.example.outpick.R;
import com.example.outpick.common.BaseDrawerActivity;
import com.example.outpick.common.adapters.OutfitCombinationAdapter;

import java.util.ArrayList;
import java.util.List;

public class ViewOutfitCombinationsActivity extends BaseDrawerActivity {

    private RecyclerView recyclerView;
    private OutfitCombinationAdapter adapter;
    private List<OutfitCombination> outfitList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_outfit_combinations);

        setupDrawer(R.id.drawer_layout, R.id.nav_view);

        recyclerView = findViewById(R.id.recyclerOutfits);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2)); // 2-column grid

        outfitList = new ArrayList<>();
        loadSampleOutfits();

        adapter = new OutfitCombinationAdapter(this, outfitList);
        recyclerView.setAdapter(adapter);
    }

    private void loadSampleOutfits() {
        outfitList.add(new OutfitCombination("Casual Day", R.drawable.top_test3, R.drawable.bottom_test2));
        outfitList.add(new OutfitCombination("Work Ready", R.drawable.top_test3, R.drawable.bottom_test2));
        outfitList.add(new OutfitCombination("Date Night", R.drawable.top_test3, R.drawable.bottom_test2));
    }
}
