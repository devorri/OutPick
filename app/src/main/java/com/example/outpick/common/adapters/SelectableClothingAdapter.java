package com.example.outpick.common.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.outpick.database.models.ClothingItem;
import com.example.outpick.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SelectableClothingAdapter extends RecyclerView.Adapter<SelectableClothingAdapter.ViewHolder> {

    private final List<ClothingItem> items;
    private final List<ClothingItem> selectedItems = new ArrayList<>();
    private final OnSelectionChangedListener listener;
    private boolean isSelectable = true;

    public interface OnSelectionChangedListener {
        void onSelectionChanged(List<ClothingItem> selectedItems);
    }

    public SelectableClothingAdapter(List<ClothingItem> items, OnSelectionChangedListener listener) {
        this.items = new ArrayList<>(items);
        this.listener = listener;
    }

    public List<ClothingItem> getSelectedItems() {
        return new ArrayList<>(selectedItems);
    }

    public void updateItems(List<ClothingItem> newItems) {
        items.clear();
        items.addAll(newItems);
        selectedItems.clear();
        for (ClothingItem item : items) {
            item.setSelected(false);
        }
        notifyDataSetChanged();
    }

    public void setSelectable(boolean selectable) {
        this.isSelectable = selectable;
        selectedItems.clear();
        for (ClothingItem item : items) {
            item.setSelected(false);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_clothing_selectable, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClothingItem item = items.get(position);
        String uriString = item.getImageUri();

        // Load image using Glide
        if (uriString != null && !uriString.isEmpty()) {
            File imageFile = new File(uriString);
            if (imageFile.exists()) {
                Glide.with(holder.itemView.getContext())
                        .load(imageFile)
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_error)
                        .into(holder.imageView);
            } else {
                holder.imageView.setImageResource(R.drawable.ic_error);
            }
        } else {
            holder.imageView.setImageResource(R.drawable.ic_error);
        }

        // Show selection overlay and check icon if selected
        if (isSelectable) {
            holder.selectionOverlay.setVisibility(item.isSelected() ? View.VISIBLE : View.GONE);
            holder.checkIcon.setVisibility(item.isSelected() ? View.VISIBLE : View.GONE);
        } else {
            holder.selectionOverlay.setVisibility(View.GONE);
            holder.checkIcon.setVisibility(View.GONE);
        }

        // Handle item click for selection
        holder.itemView.setOnClickListener(v -> {
            if (!isSelectable) return;

            boolean selected = !item.isSelected();
            item.setSelected(selected);

            if (selected) {
                selectedItems.add(item);
            } else {
                selectedItems.remove(item);
            }

            notifyItemChanged(position);
            listener.onSelectionChanged(new ArrayList<>(selectedItems));
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        View selectionOverlay;
        ImageView checkIcon;

        ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewClothing);
            selectionOverlay = itemView.findViewById(R.id.selection_overlay);
            checkIcon = itemView.findViewById(R.id.check_icon);
        }
    }
}
