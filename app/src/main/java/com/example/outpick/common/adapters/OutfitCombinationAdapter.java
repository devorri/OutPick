package com.example.outpick.common.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.outpick.database.models.Outfit;
import com.example.outpick.R;

import java.util.List;

public class OutfitCombinationAdapter extends RecyclerView.Adapter<OutfitCombinationAdapter.ViewHolder> {

    private Context context;
    private List<Outfit> outfitList;

    public OutfitCombinationAdapter(Context context, List<Outfit> outfitList) {
        this.context = context;
        this.outfitList = outfitList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_outfit_combination_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Outfit outfit = outfitList.get(position);

        // Set outfit title
        holder.title.setText(outfit.getName() != null ? outfit.getName() : "Outfit");

        // ✅ FIXED: Use Glide to load cloud images
        if (outfit.getImageUri() != null && !outfit.getImageUri().isEmpty()) {
            // Load the main outfit image (cloud URL) into both image views
            // or you can modify your layout to show a single image if that makes more sense
            Glide.with(context)
                    .load(outfit.getImageUri())
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_error)
                    .into(holder.imgTop);

            // If you want to show the same image in both spots, or leave bottom empty
            Glide.with(context)
                    .load(outfit.getImageUri())
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_error)
                    .into(holder.imgBottom);
        } else {
            // Fallback to placeholder images
            holder.imgTop.setImageResource(R.drawable.ic_placeholder);
            holder.imgBottom.setImageResource(R.drawable.ic_placeholder);
        }

        // Optional: Set click listener
        holder.itemView.setOnClickListener(v -> {
            // You can add click handling here to open outfit details
            // Intent intent = new Intent(context, OutfitDetailsActivity.class);
            // intent.putExtra("outfit_id", outfit.getId());
            // context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return outfitList.size();
    }

    /**
     * Update the adapter with new data
     */
    public void updateList(List<Outfit> newList) {
        outfitList.clear();
        outfitList.addAll(newList);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        ImageView imgTop, imgBottom;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.textTitle);
            imgTop = itemView.findViewById(R.id.imageTop);
            imgBottom = itemView.findViewById(R.id.imageBottom);
        }
    }
}