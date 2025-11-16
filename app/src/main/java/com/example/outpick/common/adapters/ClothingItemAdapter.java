package com.example.outpick.common.adapters;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
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

/**
 * RecyclerView Adapter for displaying a grid of ClothingItem objects.
 * Updated for Supabase compatibility (String IDs)
 */
public class ClothingItemAdapter extends RecyclerView.Adapter<ClothingItemAdapter.ItemViewHolder> {

    private final List<ClothingItem> mItems;
    private final Context mContext;
    private final OnItemClickListener mListener;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public interface OnItemClickListener {
        void onItemClick(ClothingItem item);
    }

    public ClothingItemAdapter(Context context, List<ClothingItem> items, OnItemClickListener listener) {
        mContext = context;
        mItems = items;
        mListener = listener;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_clothing_grid, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        ClothingItem currentItem = mItems.get(position);

        // ✅ IMPROVED: Enhanced image loading with better cloud URL handling
        String imagePath = currentItem.getImagePath();

        if (currentItem.getMockDrawableId() != 0) {
            // Use mock drawable for testing
            Glide.with(mContext)
                    .load(currentItem.getMockDrawableId())
                    .centerCrop()
                    .into(holder.itemImage);
        } else if (imagePath != null && !imagePath.isEmpty()) {
            // ✅ Use Glide for both cloud URLs and local files
            Glide.with(mContext)
                    .load(imagePath)
                    .centerCrop()
                    .placeholder(R.drawable.ic_placeholder) // Use your custom placeholder
                    .error(R.drawable.ic_error) // Use your custom error image
                    .into(holder.itemImage);

            // ✅ OPTIONAL: Log cloud URL detection for debugging
            if (imagePath.startsWith("http")) {
                Log.d("ClothingItemAdapter", "Loading cloud image: " + imagePath);
            }
        } else {
            // Fallback to default image
            holder.itemImage.setImageResource(R.drawable.ic_placeholder);
        }

        // Set display name
        String subcategory = currentItem.getSubcategory();
        String name = currentItem.getName();

        String displayLabel;
        if (subcategory != null && !subcategory.trim().isEmpty()) {
            displayLabel = subcategory;
        } else if (name != null && !name.trim().isEmpty()) {
            displayLabel = name;
        } else {
            displayLabel = "Unnamed Item";
        }

        holder.itemName.setText(displayLabel);

        // Handle selection state
        holder.itemView.setBackgroundColor(position == selectedPosition ? Color.parseColor("#40C4FF") : Color.TRANSPARENT);

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            final int clickedPosition = holder.getAdapterPosition();
            if (clickedPosition != RecyclerView.NO_POSITION) {
                ClothingItem clickedItem = mItems.get(clickedPosition);

                // ✅ OPTIONAL: Log selection for debugging
                Log.d("ClothingItemAdapter", "Item selected: " + clickedItem.getName() +
                        " | Image: " + clickedItem.getImagePath());

                mListener.onItemClick(clickedItem);

                int previousSelectedPosition = selectedPosition;
                selectedPosition = clickedPosition;

                if (previousSelectedPosition != RecyclerView.NO_POSITION) {
                    notifyItemChanged(previousSelectedPosition);
                }
                notifyItemChanged(selectedPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        public ImageView itemImage;
        public TextView itemName;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.item_image);
            itemName = itemView.findViewById(R.id.item_name);
        }
    }

    public ClothingItem getSelectedItem() {
        if (selectedPosition != RecyclerView.NO_POSITION) {
            return mItems.get(selectedPosition);
        }
        return null;
    }

    public void updateList(List<ClothingItem> newList) {
        mItems.clear();
        mItems.addAll(newList);
        selectedPosition = RecyclerView.NO_POSITION; // Reset selection
        notifyDataSetChanged();
    }

    // ✅ ADDED: Helper method to clear selection
    public void clearSelection() {
        int previousPosition = selectedPosition;
        selectedPosition = RecyclerView.NO_POSITION;
        if (previousPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(previousPosition);
        }
    }
}