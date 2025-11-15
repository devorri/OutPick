package com.example.outpick.common.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.outpick.database.models.Outfit;
import com.example.outpick.R;
import com.example.outpick.outfits.SnapshotDetailsActivity;

import java.util.List;

public class OutfitAdapter extends RecyclerView.Adapter<OutfitAdapter.OutfitViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Outfit outfit);
    }

    private final Context context;
    private final List<Outfit> outfitList;
    private final OnItemClickListener listener;

    public OutfitAdapter(Context context, List<Outfit> outfitList, OnItemClickListener listener) {
        this.context = context;
        this.outfitList = outfitList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OutfitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_outfit, parent, false);
        return new OutfitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OutfitViewHolder holder, int position) {
        Outfit outfit = outfitList.get(position);

        // Load image (from Uri or snapshot bytes)
        if (outfit.getImageUri() != null) {
            Glide.with(context)
                    .load(Uri.parse(outfit.getImageUri()))
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(holder.ivImage);
        } else if (outfit.getSnapshot() != null) {
            holder.ivImage.setImageBitmap(
                    BitmapFactory.decodeByteArray(outfit.getSnapshot(), 0, outfit.getSnapshot().length)
            );
        } else {
            holder.ivImage.setImageResource(R.drawable.placeholder_image);
        }

        // Favorite toggle
        holder.btnFavorite.setImageResource(
                outfit.isFavorite() ? R.drawable.ic_favorite : R.drawable.ic_favorite_border
        );
        holder.btnFavorite.setOnClickListener(v -> {
            outfit.setFavorite(!outfit.isFavorite());
            notifyItemChanged(position);
        });

        // On click → open SnapshotDetailsActivity
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, SnapshotDetailsActivity.class);

            // Send either imageUri or snapshot bytes
            if (outfit.getImageUri() != null) {
                intent.putExtra("snapshotImage", outfit.getImageUri());
            } else if (outfit.getSnapshot() != null) {
                intent.putExtra("snapshotBytes", outfit.getSnapshot());
            }

            // Pass name and categories
            intent.putExtra("snapshotName", outfit.getName());
            intent.putExtra("snapshotCategories", outfit.getCategories());

            context.startActivity(intent);

            if (listener != null) listener.onItemClick(outfit);
        });
    }

    @Override
    public int getItemCount() {
        return outfitList.size();
    }

    static class OutfitViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        ImageButton btnFavorite;

        public OutfitViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_outfit_image);
            btnFavorite = itemView.findViewById(R.id.btn_favorite);
        }
    }
}
