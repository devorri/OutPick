package com.example.outpick.common.adapters;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.outpick.database.models.ClothingItem;
import com.example.outpick.R;

import java.io.File;
import java.util.ArrayList;

public class ClothingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_ADD = 0;
    private static final int VIEW_TYPE_CLOTHING = 1;
    private static final String TAG = "ClothingAdapter";

    private final Context context;
    private ArrayList<ClothingItem> items = new ArrayList<>();
    private boolean showCheckboxes = false;
    private boolean showAddTile = true;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onClothingClick(ClothingItem item);
        void onAddItemClick();
    }

    public ClothingAdapter(Context context, ArrayList<ClothingItem> items) {
        this.context = context;
        if (items != null) this.items = items;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setShowCheckboxes(boolean show) {
        this.showCheckboxes = show;
        clearAllSelections();
        notifyDataSetChanged();
    }

    public void setShowAddTile(boolean show) {
        this.showAddTile = show;
        notifyDataSetChanged();
    }

    public void setItems(ArrayList<ClothingItem> newItems) {
        this.items = newItems != null ? newItems : new ArrayList<>();
        clearAllSelections();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return showAddTile ? items.size() + 1 : items.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (showAddTile && position == 0) return VIEW_TYPE_ADD;
        return VIEW_TYPE_CLOTHING;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == VIEW_TYPE_ADD) {
            View view = inflater.inflate(R.layout.item_add_button, parent, false);
            return new AddItemViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.clothing_item_layout, parent, false);
            return new ClothingViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof AddItemViewHolder) {
            ((AddItemViewHolder) holder).bind();
        } else if (holder instanceof ClothingViewHolder) {
            int actualPosition = showAddTile ? position - 1 : position;
            if (actualPosition >= 0 && actualPosition < items.size()) {
                ((ClothingViewHolder) holder).bind(items.get(actualPosition), position);
            }
        }
    }

    // ===== Clothing ViewHolder =====
    public class ClothingViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        CheckBox checkBox;
        TextView categoryText, seasonText;

        public ClothingViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            checkBox = itemView.findViewById(R.id.checkbox_selection);
            categoryText = itemView.findViewById(R.id.categoryText);
            seasonText = itemView.findViewById(R.id.seasonText);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && (!showAddTile || pos != 0)) {
                    int actualPos = showAddTile ? pos - 1 : pos;
                    ClothingItem item = items.get(actualPos);

                    if (showCheckboxes) {
                        item.setSelected(!item.isSelected());
                        notifyItemChanged(pos);
                    } else if (listener != null) {
                        listener.onClothingClick(item);
                    }
                }
            });

            checkBox.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    int actualPos = showAddTile ? pos - 1 : pos;
                    ClothingItem item = items.get(actualPos);
                    item.setSelected(!item.isSelected());
                    notifyItemChanged(pos);
                }
            });
        }

        public void bind(ClothingItem item, int adapterPosition) {
            String uriString = item.getImageUri();

            // ✅ DEBUG: Log what we're trying to load
            Log.d(TAG, "Loading image for: " + item.getName() + " | URI: " + uriString);

            if (uriString != null && !uriString.isEmpty()) {
                // ✅ Check if it's a cloud URL (starts with http/https)
                if (uriString.startsWith("http")) {
                    // Load from Supabase Storage URL
                    Log.d(TAG, "✅ Loading cloud image: " + uriString);

                    Glide.with(context)
                            .load(uriString)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.ic_placeholder)
                            .error(R.drawable.ic_error)
                            .into(imageView);

                } else {
                    // Check if it's a local file that exists
                    File imageFile = new File(uriString);
                    if (imageFile.exists()) {
                        // Load from local file
                        Log.d(TAG, "✅ Loading local file: " + uriString);

                        Glide.with(context)
                                .load(imageFile)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .skipMemoryCache(true)
                                .placeholder(R.drawable.ic_placeholder)
                                .error(R.drawable.ic_error)
                                .into(imageView);
                    } else {
                        // Try loading as URI (for content:// URIs)
                        try {
                            Log.d(TAG, "🔄 Trying to load as URI: " + uriString);

                            Uri uri = Uri.parse(uriString);
                            Glide.with(context)
                                    .load(uri)
                                    .placeholder(R.drawable.ic_placeholder)
                                    .error(R.drawable.ic_error)
                                    .into(imageView);
                        } catch (Exception e) {
                            // Fallback to error image
                            Log.e(TAG, "❌ Failed to load image: " + uriString, e);
                            imageView.setImageResource(R.drawable.ic_error);
                        }
                    }
                }
            } else {
                // No image available
                Log.w(TAG, "⚠️ No image URI for item: " + item.getName());
                imageView.setImageResource(R.drawable.ic_placeholder);
            }

            checkBox.setVisibility(showCheckboxes ? View.VISIBLE : View.GONE);
            checkBox.setChecked(item.isSelected());
            categoryText.setText(item.getCategory() != null ? item.getCategory() : "Unknown");
            seasonText.setText(item.getSeason() != null ? item.getSeason() : "Unknown");
        }
    }

    // ===== Add Item ViewHolder =====
    public class AddItemViewHolder extends RecyclerView.ViewHolder {
        LinearLayout addItemCard;

        public AddItemViewHolder(@NonNull View itemView) {
            super(itemView);
            addItemCard = itemView.findViewById(R.id.addItemCard);
        }

        public void bind() {
            addItemCard.setOnClickListener(v -> {
                if (listener != null) listener.onAddItemClick();
            });
        }
    }

    // ===== Selection Helpers =====
    public void clearAllSelections() {
        for (ClothingItem item : items) {
            item.setSelected(false);
        }
        notifyDataSetChanged();
    }

    public ArrayList<ClothingItem> getSelectedItems() {
        ArrayList<ClothingItem> selected = new ArrayList<>();
        for (ClothingItem item : items) {
            if (item.isSelected()) selected.add(item);
        }
        return selected;
    }

    public void deleteSelectedItems() {
        ArrayList<ClothingItem> toRemove = getSelectedItems();
        items.removeAll(toRemove);
        notifyDataSetChanged();
    }
}