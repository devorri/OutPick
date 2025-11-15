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

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.ViewHolder> {

    private final Context context;
    private List<Outfit> outfitList;
    private final SupabaseService supabaseService;

    // ⭐ CRITICAL NEW FIELD: User ID is required for the toggle logic (String for Supabase UUID)
    private final String currentUserId;

    // ✅ FIX: CONSTRUCTOR NOW USES SupabaseService INSTEAD OF OutfitDatabaseHelper
    public FavoritesAdapter(Context context, List<Outfit> outfitList, SupabaseService supabaseService, String currentUserId) {
        this.context = context;
        this.outfitList = outfitList;
        this.supabaseService = supabaseService;
        this.currentUserId = currentUserId; // Store the user ID received from FavoritesActivity
    }

    /** Method to update the adapter data list */
    public void updateData(List<Outfit> newOutfits) {
        this.outfitList = newOutfits;
        notifyDataSetChanged();
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
            // Simplified URI check
            if (outfit.getImageUri().contains("://")) {
                uriToLoad = Uri.parse(outfit.getImageUri());
            } else {
                uriToLoad = Uri.fromFile(new File(outfit.getImageUri()));
            }

            Glide.with(context)
                    .load(uriToLoad)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(holder.imageView);
        } else {
            holder.imageView.setImageResource(R.drawable.placeholder_image);
        }

        // --- Favorite Button State ---
        holder.btnFavorite.setVisibility(View.VISIBLE);
        holder.btnFavorite.setImageResource(R.drawable.ic_favorite);

        // --- Favorite button toggle logic (Unfavoriting) ---
        holder.btnFavorite.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION) return;

            if (currentUserId.isEmpty()) {
                Toast.makeText(context, "Error: User ID missing. Cannot remove favorite.", Toast.LENGTH_SHORT).show();
                return;
            }

            Outfit outfitToRemove = outfitList.get(currentPosition);

            // Remove from favorites in Supabase
            removeFromFavorites(outfitToRemove.getId(), currentPosition);
        });

        // --- Open outfit details on item click ---
        holder.itemView.setOnClickListener(v -> {
            Outfit clickedOutfit = outfitList.get(position);
            Intent intent = new Intent(context, OutfitSuggestionDetailsActivity.class);

            // Pass outfit details
            intent.putExtra("id", clickedOutfit.getId());
            intent.putExtra("imageUri", clickedOutfit.getImageUri());
            intent.putExtra("name", clickedOutfit.getName());
            intent.putExtra("category", clickedOutfit.getCategory());
            intent.putExtra("description", clickedOutfit.getDescription());
            intent.putExtra("gender", clickedOutfit.getGender());
            intent.putExtra("event", clickedOutfit.getEvent());
            intent.putExtra("season", clickedOutfit.getSeason());
            intent.putExtra("style", clickedOutfit.getStyle());
            intent.putExtra("isFavorite", true);

            // ✅ FIX: Use the new key for user ID
            intent.putExtra("user_id", currentUserId);

            context.startActivity(intent);
        });
    }

    private void removeFromFavorites(String outfitId, int position) {
        Call<Void> call = supabaseService.removeFavorite(currentUserId, outfitId);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // Remove the item from the local list and update the view
                    outfitList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, outfitList.size());

                    Toast.makeText(
                            context,
                            "Removed from favorites",
                            Toast.LENGTH_SHORT
                    ).show();
                } else {
                    Toast.makeText(
                            context,
                            "Failed to remove from favorites",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(
                        context,
                        "Network error: " + t.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
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