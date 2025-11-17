package com.example.outpick.common.adapters;

import android.content.Context;
import android.content.Intent;
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
import com.example.outpick.outfits.OutfitSuggestionDetailsActivity;
import com.example.outpick.R;
import com.example.outpick.database.supabase.SupabaseService;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OutfitSuggestionAdapter extends RecyclerView.Adapter<OutfitSuggestionAdapter.ViewHolder> {

    public interface OnOutfitClickListener {
        void onOutfitClick(Outfit outfit);
    }

    private final Context context;
    private final List<Outfit> outfitList;
    private final SupabaseService supabaseService;
    private final OnOutfitClickListener listener;

    // ⭐ CHANGED: Now using String for Supabase UUID
    private final String currentUserId;

    // ---------------- CONSTRUCTOR ----------------
    // ✅ FIX: Constructor now uses String for currentUserId and SupabaseService
    public OutfitSuggestionAdapter(Context context, List<Outfit> outfitList, OnOutfitClickListener listener, String currentUserId, SupabaseService supabaseService) {
        this.context = context;
        this.outfitList = outfitList;
        this.supabaseService = supabaseService;
        this.listener = listener;
        this.currentUserId = currentUserId;

        // Initial check for debugging
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(context, "Warning: User ID not found. Favorites are disabled.", Toast.LENGTH_LONG).show();
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

        // 1. Check/Refresh Favorite Status from Supabase for this user before binding
        checkFavoriteStatus(outfit, holder);

        // --- Load image safely ---
        String imageUri = outfit.getImageUri();
        if (imageUri != null && !imageUri.isEmpty()) {
            Uri uriToLoad;
            if (imageUri.contains("://")) {
                uriToLoad = Uri.parse(imageUri);
            } else {
                uriToLoad = Uri.fromFile(new File(imageUri));
            }

            Glide.with(context)
                    .load(uriToLoad)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(holder.imageView);
        } else {
            holder.imageView.setImageResource(R.drawable.placeholder_image);
        }

        // --- Favorite icon (Uses the refreshed status) ---
        holder.btnFavorite.setVisibility(View.VISIBLE);
        holder.btnFavorite.setImageResource(outfit.isFavorite() ?
                R.drawable.ic_favorite : R.drawable.ic_favorite_border);

        // --- Toggle favorite with Supabase ---
        holder.btnFavorite.setOnClickListener(v -> {
            if (currentUserId == null || currentUserId.isEmpty()) {
                Toast.makeText(context, "Please log in to save favorites.", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean newStatus = !outfit.isFavorite();
            outfit.setFavorite(newStatus);

            // ⭐ CRITICAL FIX: Use Supabase instead of SQLite
            if (newStatus) {
                addToFavorites(outfit.getId(), holder);
            } else {
                removeFromFavorites(outfit.getId(), holder);
            }

            holder.btnFavorite.setImageResource(newStatus ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
        });

        // --- Open outfit details using listener ---
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOutfitClick(outfit);
            } else {
                // Fallback: open directly if listener not provided
                Intent intent = new Intent(context, OutfitSuggestionDetailsActivity.class);

                // Pass essential outfit data
                intent.putExtra("id", outfit.getId());
                intent.putExtra("imageUri", outfit.getImageUri());
                intent.putExtra("name", outfit.getName());
                intent.putExtra("category", outfit.getCategory());
                intent.putExtra("description", outfit.getDescription());
                intent.putExtra("gender", outfit.getGender());
                intent.putExtra("event", outfit.getEvent());
                intent.putExtra("season", outfit.getSeason());
                intent.putExtra("style", outfit.getStyle());
                intent.putExtra("isFavorite", outfit.isFavorite());

                // CRITICAL: Pass the User ID for the Details Activity to use
                intent.putExtra("user_id", currentUserId);

                context.startActivity(intent);
            }
        });
    }

    private void checkFavoriteStatus(Outfit outfit, ViewHolder holder) {
        if (currentUserId == null || currentUserId.isEmpty() || outfit.getId() == null) {
            outfit.setFavorite(false);
            return;
        }

        Call<List<JsonObject>> call = supabaseService.checkFavorite(currentUserId, outfit.getId());
        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    boolean isFavorite = !response.body().isEmpty();
                    outfit.setFavorite(isFavorite);
                    holder.btnFavorite.setImageResource(isFavorite ?
                            R.drawable.ic_favorite : R.drawable.ic_favorite_border);
                }
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                outfit.setFavorite(false);
                holder.btnFavorite.setImageResource(R.drawable.ic_favorite_border);
            }
        });
    }

    private void addToFavorites(String outfitId, ViewHolder holder) {
        JsonObject favorite = new JsonObject();
        favorite.addProperty("user_id", currentUserId);
        favorite.addProperty("outfit_id", outfitId);

        // ✅ FIXED: Changed to List<JsonObject>
        Call<List<JsonObject>> call = supabaseService.addFavorite(favorite);
        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(context, "Added to favorites", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Failed to add to favorites", Toast.LENGTH_SHORT).show();
                    // Revert UI state
                    outfitList.get(holder.getAdapterPosition()).setFavorite(false);
                    holder.btnFavorite.setImageResource(R.drawable.ic_favorite_border);
                }
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                Toast.makeText(context, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                // Revert UI state
                outfitList.get(holder.getAdapterPosition()).setFavorite(false);
                holder.btnFavorite.setImageResource(R.drawable.ic_favorite_border);
            }
        });
    }

    private void removeFromFavorites(String outfitId, ViewHolder holder) {
        Call<Void> call = supabaseService.removeFavorite(currentUserId, outfitId);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Failed to remove from favorites", Toast.LENGTH_SHORT).show();
                    // Revert UI state
                    outfitList.get(holder.getAdapterPosition()).setFavorite(true);
                    holder.btnFavorite.setImageResource(R.drawable.ic_favorite);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(context, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                // Revert UI state
                outfitList.get(holder.getAdapterPosition()).setFavorite(true);
                holder.btnFavorite.setImageResource(R.drawable.ic_favorite);
            }
        });
    }

    @Override
    public int getItemCount() {
        return outfitList.size();
    }

    // ------------------- UPDATE LIST -------------------
    public void updateList(List<Outfit> newList) {
        outfitList.clear();
        outfitList.addAll(newList);
        notifyDataSetChanged();
    }

    // ------------------- VIEW HOLDER -------------------
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageButton btnFavorite;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.iv_outfit_image);
            btnFavorite = itemView.findViewById(R.id.btn_favorite);
        }
    }
}