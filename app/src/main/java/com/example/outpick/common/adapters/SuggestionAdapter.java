package com.example.outpick.common.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.outpick.database.models.Outfit;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.example.outpick.outfits.OutfitSuggestionDetailsActivity;
import com.example.outpick.R;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.ViewHolder> {

    private final Context context;
    private final List<Outfit> outfitList;
    private final boolean showFavoriteToggle;

    // ⭐ CRITICAL NEW FIELDS for User-Scoped Favorites with Supabase
    private final String currentUserId; // The user ID of the currently logged-in user

    // --- Multi-select mode ---
    private boolean multiSelectMode = false;
    private final Set<Integer> selectedPositions = new HashSet<>();

    // Supabase service
    private final SupabaseService supabaseService;

    // --- Constructor (default: show favorites, e.g. for OutfitSuggestionActivity) ---
    public SuggestionAdapter(Context context, List<Outfit> outfitList, String currentUserId) {
        this(context, outfitList, true, currentUserId);
    }

    // --- Constructor allowing control over favorite toggle visibility ---
    public SuggestionAdapter(Context context, List<Outfit> outfitList, boolean showFavoriteToggle, String currentUserId) {
        this.context = context;
        this.outfitList = outfitList;
        this.showFavoriteToggle = showFavoriteToggle;
        this.currentUserId = currentUserId;
        this.supabaseService = SupabaseClient.getService();

        // Log an error if the user ID is invalid, as favorites won't work
        if (this.currentUserId == null || this.currentUserId.isEmpty()) {
            Toast.makeText(context, "Error: User not logged in for favorites.", Toast.LENGTH_LONG).show();
        }
    }

    /** Enable or disable multi-select mode */
    public void enableMultiSelectMode(boolean enable) {
        multiSelectMode = enable;
        selectedPositions.clear();
        notifyDataSetChanged();
    }

    /** Delete all selected outfits (from Supabase + list) */
    public void deleteSelected() {
        List<Outfit> toDelete = new ArrayList<>();
        List<String> idsToDelete = new ArrayList<>();

        for (Integer pos : selectedPositions) {
            if (pos >= 0 && pos < outfitList.size()) {
                Outfit outfit = outfitList.get(pos);
                toDelete.add(outfit);
                idsToDelete.add(outfit.getId());
            }
        }

        if (!idsToDelete.isEmpty()) {
            // Delete from Supabase
            deleteOutfitsFromSupabase(idsToDelete);
        }

        outfitList.removeAll(toDelete);
        selectedPositions.clear();
        notifyDataSetChanged();
    }

    /** Get selected outfits for deletion */
    public List<Outfit> getSelectedOutfits() {
        List<Outfit> selected = new ArrayList<>();
        for (Integer pos : selectedPositions) {
            if (pos >= 0 && pos < outfitList.size()) {
                selected.add(outfitList.get(pos));
            }
        }
        return selected;
    }

    /** Remove specific outfit */
    public void removeOutfit(Outfit outfit) {
        int position = outfitList.indexOf(outfit);
        if (position != -1) {
            outfitList.remove(position);
            notifyItemRemoved(position);
        }
    }

    /** Clear all selections */
    public void clearSelections() {
        selectedPositions.clear();
        notifyDataSetChanged();
    }

    /** Get number of selected items */
    public int getSelectedCount() {
        return selectedPositions.size();
    }

    private void deleteOutfitsFromSupabase(List<String> outfitIds) {
        for (String outfitId : outfitIds) {
            Call<Void> call = supabaseService.deleteOutfit(outfitId);
            call.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    if (!response.isSuccessful()) {
                        Toast.makeText(context, "Failed to delete outfit from server", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    Toast.makeText(context, "Network error deleting outfit", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_outfit, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Outfit outfit = outfitList.get(position);

        // --- Load outfit image using Glide ---
        if (outfit.getImageUri() != null && !outfit.getImageUri().isEmpty()) {
            Uri uriToLoad;
            if (outfit.getImageUri().contains("://")) {
                uriToLoad = Uri.parse(outfit.getImageUri());
            } else {
                uriToLoad = Uri.fromFile(new java.io.File(outfit.getImageUri()));
            }

            Glide.with(context)
                    .load(uriToLoad)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(holder.imageView);
        } else {
            holder.imageView.setImageResource(R.drawable.placeholder_image);
        }

        // --- Show or hide favorite button ---
        if (showFavoriteToggle) {
            holder.btnFavorite.setVisibility(View.VISIBLE);

            // Check favorite status from Supabase for this user/outfit pair
            checkFavoriteStatus(outfit, holder, position);

            // --- Favorite button toggle logic ---
            holder.btnFavorite.setOnClickListener(v -> {
                if (currentUserId == null || currentUserId.isEmpty()) {
                    Toast.makeText(context, "Error: Cannot save favorite. User not logged in.", Toast.LENGTH_SHORT).show();
                    return;
                }

                toggleFavoriteStatus(outfit, holder, position);
            });
        } else {
            // Hide favorite button completely for screens like ContentOutfitsActivity
            holder.btnFavorite.setVisibility(View.GONE);
        }

        // --- Multi-select highlight ---
        if (multiSelectMode && selectedPositions.contains(position)) {
            holder.itemView.setBackgroundColor(Color.LTGRAY);
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }

        // --- Item click behavior ---
        holder.itemView.setOnClickListener(v -> {
            if (multiSelectMode) {
                // Toggle selection
                if (selectedPositions.contains(position)) {
                    selectedPositions.remove(position);
                } else {
                    selectedPositions.add(position);
                }
                notifyItemChanged(position);
            } else {
                // Normal click → open details
                openOutfitDetails(outfit);
            }
        });
    }

    private void checkFavoriteStatus(Outfit outfit, ViewHolder holder, int position) {
        if (currentUserId == null || currentUserId.isEmpty()) {
            holder.btnFavorite.setImageResource(R.drawable.ic_favorite_border);
            return;
        }

        Call<List<JsonObject>> call = supabaseService.checkFavorite(currentUserId, outfit.getId());
        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(@NonNull Call<List<JsonObject>> call, @NonNull Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    // Outfit is favorited
                    outfit.setFavorite(true);
                    holder.btnFavorite.setImageResource(R.drawable.ic_favorite);
                } else {
                    // Outfit is not favorited
                    outfit.setFavorite(false);
                    holder.btnFavorite.setImageResource(R.drawable.ic_favorite_border);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<JsonObject>> call, @NonNull Throwable t) {
                // On failure, assume not favorited
                outfit.setFavorite(false);
                holder.btnFavorite.setImageResource(R.drawable.ic_favorite_border);
            }
        });
    }

    private void toggleFavoriteStatus(Outfit outfit, ViewHolder holder, int position) {
        boolean newState = !outfit.isFavorite();
        outfit.setFavorite(newState);

        // Update UI immediately for better UX
        holder.btnFavorite.setImageResource(
                newState ? R.drawable.ic_favorite : R.drawable.ic_favorite_border
        );

        if (newState) {
            // Add to favorites
            addToFavorites(outfit, holder, position);
        } else {
            // Remove from favorites
            removeFromFavorites(outfit, holder, position);
        }
    }

    private void addToFavorites(Outfit outfit, ViewHolder holder, int position) {
        JsonObject favorite = new JsonObject();
        favorite.addProperty("user_id", currentUserId);
        favorite.addProperty("outfit_id", outfit.getId());

        Call<JsonObject> call = supabaseService.addFavorite(favorite);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(context, "Added to favorites", Toast.LENGTH_SHORT).show();
                } else {
                    // Revert on failure
                    outfit.setFavorite(false);
                    holder.btnFavorite.setImageResource(R.drawable.ic_favorite_border);
                    Toast.makeText(context, "Failed to add to favorites", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                // Revert on failure
                outfit.setFavorite(false);
                holder.btnFavorite.setImageResource(R.drawable.ic_favorite_border);
                Toast.makeText(context, "Network error adding favorite", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void removeFromFavorites(Outfit outfit, ViewHolder holder, int position) {
        Call<Void> call = supabaseService.removeFavorite(currentUserId, outfit.getId());
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show();
                } else {
                    // Revert on failure
                    outfit.setFavorite(true);
                    holder.btnFavorite.setImageResource(R.drawable.ic_favorite);
                    Toast.makeText(context, "Failed to remove from favorites", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                // Revert on failure
                outfit.setFavorite(true);
                holder.btnFavorite.setImageResource(R.drawable.ic_favorite);
                Toast.makeText(context, "Network error removing favorite", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openOutfitDetails(Outfit outfit) {
        Intent intent = new Intent(context, OutfitSuggestionDetailsActivity.class);

        // Pass all outfit details
        intent.putExtra("id", outfit.getId());
        intent.putExtra("imageUri", outfit.getImageUri());
        intent.putExtra("name", outfit.getName());
        intent.putExtra("description", outfit.getDescription());
        intent.putExtra("gender", outfit.getGender());
        intent.putExtra("event", outfit.getEvent());
        intent.putExtra("season", outfit.getSeason());
        intent.putExtra("style", outfit.getStyle());
        intent.putExtra("isFavorite", outfit.isFavorite());
        intent.putExtra("category", outfit.getCategory());

        // Pass the user ID for favorite functionality in details activity
        intent.putExtra("currentUserId", currentUserId);

        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return outfitList.size();
    }

    // --- ViewHolder ---
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageButton btnFavorite;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.iv_outfit_image);
            btnFavorite = itemView.findViewById(R.id.btn_favorite);
        }
    }
}