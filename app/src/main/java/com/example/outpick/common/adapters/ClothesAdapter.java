package com.example.outpick.common.adapters;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.outpick.database.models.ClothingItem;
import com.example.outpick.R;

import java.util.List;

public class ClothesAdapter extends RecyclerView.Adapter<ClothesAdapter.ViewHolder> {

    private final Context context;
    private final List<ClothingItem> clothingList;

    public ClothesAdapter(Context context, List<ClothingItem> clothingList) {
        this.context = context;
        this.clothingList = clothingList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_clothing, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ClothingItem item = clothingList.get(position);

        // ✅ FIXED: Use getImagePath() instead of getImageUri()
        if (item.getImagePath() != null && !item.getImagePath().isEmpty()) {
            Glide.with(context)
                    .load(item.getImagePath())
                    .into(holder.imageView);
        }

        holder.categoryText.setText(item.getCategory());
        holder.seasonText.setText(item.getSeason());
        holder.occasionText.setText(item.getOccasion());

        // ✅ Set clothing name if available
        if (holder.nameText != null && item.getName() != null) {
            holder.nameText.setText(item.getName());
        }
    }

    @Override
    public int getItemCount() {
        return clothingList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView categoryText, seasonText, occasionText, nameText;

        public ViewHolder(View itemView) {
            super(itemView);
            // ✅ FIXED: Match the IDs from the updated XML
            imageView = itemView.findViewById(R.id.clothingImage); // Changed from clothing_image
            categoryText = itemView.findViewById(R.id.clothing_category);
            seasonText = itemView.findViewById(R.id.clothing_season);
            occasionText = itemView.findViewById(R.id.clothing_occasion);
            nameText = itemView.findViewById(R.id.clothingName); // Added this
        }
    }
}