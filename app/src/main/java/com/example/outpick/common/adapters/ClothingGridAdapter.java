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
import com.example.outpick.database.models.ClothingItem;
import com.example.outpick.R;

import java.util.List;

public class ClothingGridAdapter extends RecyclerView.Adapter<ClothingGridAdapter.ClothingViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(ClothingItem clothingItem);
    }

    private Context context;
    private List<ClothingItem> clothingItems;
    private OnItemClickListener listener;

    public ClothingGridAdapter(List<ClothingItem> clothingItems, OnItemClickListener listener) {
        this.clothingItems = clothingItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ClothingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.clothing_item_layout, parent, false);
        return new ClothingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClothingViewHolder holder, int position) {
        ClothingItem item = clothingItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return clothingItems.size();
    }

    // Remove the override of notifyDataSetChanged() since it's final
    public void updateData(List<ClothingItem> newItems) {
        this.clothingItems = newItems;
        notifyDataSetChanged(); // Call the parent's final method
    }

    class ClothingViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView nameText;
        TextView categoryText;

        public ClothingViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            nameText = itemView.findViewById(R.id.categoryText); // Reusing category text for name
            categoryText = itemView.findViewById(R.id.seasonText); // Reusing season text for category

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(clothingItems.get(position));
                }
            });
        }

        public void bind(ClothingItem item) {
            // Set name and category
            nameText.setText(item.getName() != null ? item.getName() : "Unnamed");
            categoryText.setText(item.getCategory() != null ? item.getCategory() : "No Category");

            // Load image
            String imageUri = item.getImageUri();
            if (imageUri != null && !imageUri.isEmpty()) {
                Glide.with(context)
                        .load(imageUri)
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_error)
                        .into(imageView);
            } else {
                imageView.setImageResource(R.drawable.ic_placeholder);
            }
        }
    }
}