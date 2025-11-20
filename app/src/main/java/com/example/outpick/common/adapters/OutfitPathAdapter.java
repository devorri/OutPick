package com.example.outpick.common.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.outpick.database.models.ClosetContentItem;
import com.example.outpick.database.models.Outfit;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.example.outpick.outfits.OutfitCombinationActivity;
import com.example.outpick.R;
import com.example.outpick.outfits.SnapshotDetailsActivity;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OutfitPathAdapter extends RecyclerView.Adapter<OutfitPathAdapter.ViewHolder> {

    private static final String TAG = "OutfitPathAdapter";
    private final Context context;
    private final List<ClosetContentItem> closetItems;
    private final SupabaseService supabaseService;

    private boolean multiSelectEnabled = false;
    private final Set<Integer> selectedItems = new HashSet<>();

    private static final int SNAPSHOT_PADDING_DP = 16;
    private static final int CLOTHING_PADDING_DP = 4;

    public OutfitPathAdapter(Context context, List<ClosetContentItem> closetItems) {
        this.context = context;
        this.closetItems = closetItems;
        this.supabaseService = SupabaseClient.getService();
    }

    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_outfit_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClosetContentItem item = closetItems.get(position);

        Log.d(TAG, "Binding item: " + item.getName() + " | Type: " + item.getType() + " | Image: " +
                (item.getType() == ClosetContentItem.ItemType.SNAPSHOT ? item.getSnapshotPath() : item.getImageUri()));

        // --- STYLING AND LOADING LOGIC ---
        if (item.getType() == ClosetContentItem.ItemType.CLOTHING) {
            holder.itemView.setBackground(null);
            holder.imageView.setBackgroundResource(R.drawable.bg_rounded_card);
            int padding = dpToPx(CLOTHING_PADDING_DP);
            holder.imageView.setPadding(padding, padding, padding, padding);

            String imageUri = item.getImageUri();
            if (imageUri != null && !imageUri.isEmpty()) {
                loadImageWithPicasso(imageUri, holder.imageView);
            } else {
                holder.imageView.setImageResource(R.drawable.placeholder_image);
            }

        } else if (item.getType() == ClosetContentItem.ItemType.SNAPSHOT) {
            holder.itemView.setBackgroundResource(R.drawable.bg_snapshot_border_card);
            holder.imageView.setBackground(null);
            int padding = dpToPx(SNAPSHOT_PADDING_DP);
            holder.imageView.setPadding(padding, padding, padding, padding);

            String imageUri = item.getSnapshotPath();
            if (imageUri != null && !imageUri.isEmpty()) {
                loadImageWithPicasso(imageUri, holder.imageView);
            } else {
                holder.imageView.setImageResource(R.drawable.placeholder_image);
            }
        }

        // Multi-select UI
        if (multiSelectEnabled) {
            holder.selectionOverlay.setVisibility(View.VISIBLE);
            if (selectedItems.contains(position)) {
                holder.selectionOverlay.setAlpha(0.6f);
                holder.checkIcon.setVisibility(View.VISIBLE);
            } else {
                holder.selectionOverlay.setAlpha(0f);
                holder.checkIcon.setVisibility(View.GONE);
            }
        } else {
            holder.selectionOverlay.setVisibility(View.GONE);
            holder.checkIcon.setVisibility(View.GONE);
        }

        // Click listeners
        holder.itemView.setOnClickListener(v -> {
            if (multiSelectEnabled) {
                toggleSelection(position);
            } else {
                if (item.getType() == ClosetContentItem.ItemType.SNAPSHOT) {
                    String clickedPath = item.getSnapshotPath();
                    loadOutfitFromSupabase(clickedPath, position);
                }
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!multiSelectEnabled) {
                enableMultiSelect();
                selectedItems.add(position);
                notifyItemChanged(position);
            }
            return true;
        });
    }

    private void loadImageWithPicasso(String imageUrl, ImageView imageView) {
        try {
            // Add cache busting parameter
            String cacheBusterUrl = imageUrl + "?t=" + System.currentTimeMillis();

            Picasso.get()
                    .load(cacheBusterUrl)
                    .networkPolicy(NetworkPolicy.NO_CACHE, NetworkPolicy.NO_STORE)
                    .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(imageView);

            Log.d(TAG, "Loading image with Picasso: " + cacheBusterUrl);
        } catch (Exception e) {
            Log.e(TAG, "Picasso load error: " + e.getMessage());
            imageView.setImageResource(R.drawable.error_image);
        }
    }

    private void loadOutfitFromSupabase(String snapshotPath, int position) {
        Call<List<JsonObject>> call = supabaseService.getOutfits();

        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(@NonNull Call<List<JsonObject>> call, @NonNull Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Outfit outfit = findOutfitByPath(response.body(), snapshotPath);
                    openSnapshotDetails(snapshotPath, outfit, position);
                } else {
                    openSnapshotDetails(snapshotPath, null, position);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<JsonObject>> call, @NonNull Throwable t) {
                openSnapshotDetails(snapshotPath, null, position);
            }
        });
    }

    private Outfit findOutfitByPath(List<JsonObject> outfitJsonList, String snapshotPath) {
        Gson gson = new Gson();

        for (JsonObject jsonObject : outfitJsonList) {
            try {
                Outfit outfit = gson.fromJson(jsonObject, Outfit.class);
                if (outfit != null && snapshotPath.equals(outfit.getImageUri())) {
                    return outfit;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void openSnapshotDetails(String snapshotPath, Outfit outfit, int position) {
        Intent intent = new Intent(context, SnapshotDetailsActivity.class);
        intent.putExtra("path", snapshotPath);

        if (outfit != null) {
            intent.putExtra("snapshotId", outfit.getId());
            intent.putExtra("outfitName", safeString(outfit.getOutfitName(), "Unknown Outfit"));
            intent.putExtra("event", safeString(outfit.getEvent(), ""));
            intent.putExtra("season", safeString(outfit.getSeason(), ""));
            intent.putExtra("style", safeString(outfit.getStyle(), ""));
        } else {
            intent.putExtra("outfitName", "Unknown Outfit");
            intent.putExtra("event", "");
            intent.putExtra("season", "");
            intent.putExtra("style", "");
        }

        if (context instanceof OutfitCombinationActivity) {
            ((OutfitCombinationActivity) context).startSnapshotDetailsForResult(intent, position);
        } else {
            context.startActivity(intent);
        }
    }

    private static String safeString(String s, String fallback) {
        return (s != null && !s.trim().isEmpty()) ? s : fallback;
    }

    @Override
    public int getItemCount() {
        return closetItems.size();
    }

    private void toggleSelection(int position) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(position);
        } else {
            selectedItems.add(position);
        }
        notifyItemChanged(position);
    }

    public void enableMultiSelect() {
        multiSelectEnabled = true;
        selectedItems.clear();
        notifyDataSetChanged();
    }

    public void disableMultiSelect() {
        multiSelectEnabled = false;
        selectedItems.clear();
        notifyDataSetChanged();
    }

    public Set<Integer> getSelectedItems() {
        return selectedItems;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        View selectionOverlay;
        ImageView checkIcon;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_outfit);
            selectionOverlay = itemView.findViewById(R.id.selection_overlay);
            checkIcon = itemView.findViewById(R.id.check_icon);
        }
    }
}