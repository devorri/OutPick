package com.example.outpick.common.adapters;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.outpick.MainActivity;
import com.example.outpick.R;
import com.example.outpick.closet.ClosetDetailActivity;
import com.example.outpick.closet.CreateClosetActivity;
import com.example.outpick.database.models.ClosetItem;
import com.example.outpick.database.models.Outfit;
import com.example.outpick.database.repositories.OutfitRepository;
import com.example.outpick.database.repositories.UserOutfitRepository;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.example.outpick.outfits.CreateOutfitActivity;
import com.example.outpick.outfits.OutfitCombinationActivity;
import com.example.outpick.outfits.OutfitCreationActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;

import com.example.outpick.common.BaseDrawerActivity;

public class ClosetAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_OUTFIT = 0;
    private static final int TYPE_CREATE = 1;
    private static final int TYPE_DEFAULT = 2;
    private static final String TAG = "ClosetAdapter";

    private final MainActivity mainActivity;
    private final List<ClosetItem> closetList;
    private final SupabaseService supabaseService;
    private final UserOutfitRepository userOutfitRepository;
    private final OutfitRepository outfitRepository;
    private final String currentUserId;

    public ClosetAdapter(MainActivity mainActivity, List<ClosetItem> closetList) {
        this.mainActivity = mainActivity;
        this.closetList = closetList;
        this.supabaseService = SupabaseClient.getService();

        // Initialize repositories
        this.outfitRepository = new OutfitRepository(supabaseService);
        this.userOutfitRepository = new UserOutfitRepository(supabaseService, outfitRepository);

        // Get current user ID
        SharedPreferences prefs = mainActivity.getSharedPreferences("UserPrefs", MainActivity.MODE_PRIVATE);
        this.currentUserId = prefs.getString("user_id", "");
    }

    @Override
    public int getItemViewType(int position) {
        String type = closetList.get(position).getType();
        if ("outfit_card".equals(type)) return TYPE_OUTFIT;
        if ("create_card".equals(type)) return TYPE_CREATE;
        return TYPE_DEFAULT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mainActivity);

        if (viewType == TYPE_OUTFIT) {
            View view = inflater.inflate(R.layout.item_outfit_combination, parent, false);
            return new OutfitViewHolder(view);
        } else if (viewType == TYPE_CREATE) {
            View view = inflater.inflate(R.layout.item_create_closet, parent, false);
            return new CreateViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_closet, parent, false);
            return new ClosetViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ClosetItem item = closetList.get(position);

        if (holder instanceof OutfitViewHolder) {
            OutfitViewHolder h = (OutfitViewHolder) holder;
            h.outfitLabel.setText("Outfit Combinations");

            // 🔁 Load dynamic snapshot count from Supabase FOR CURRENT USER ONLY
            loadOutfitSnapshotCountForCurrentUser(h);

            h.plusBtn.setImageResource(R.drawable.ic_plus);
            h.plusBtn.setBackgroundResource(R.drawable.circle_background_white);

            h.plusBtn.setOnClickListener(v -> showBottomSheetMenu());

            h.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(mainActivity, OutfitCombinationActivity.class);

                // FIX: Cast mainActivity (MainActivity) to BaseDrawerActivity
                String immutableId = "";
                if (mainActivity instanceof BaseDrawerActivity) {
                    immutableId = ((BaseDrawerActivity) mainActivity).getImmutableLoginId();
                } else {
                    Log.e(TAG, "MainActivity does not extend BaseDrawerActivity. Cannot retrieve immutable ID.");
                }

                // Pass the immutable ID
                intent.putExtra("username", immutableId);
                mainActivity.startActivity(intent);
            });

        } else if (holder instanceof CreateViewHolder) {
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(mainActivity, CreateClosetActivity.class);
                mainActivity.startActivity(intent);
            });

        } else if (holder instanceof ClosetViewHolder) {
            ClosetViewHolder h = (ClosetViewHolder) holder;
            h.textTitle.setText(item.getName());
            h.textSub.setText(item.getDescription());

            String imageUri = item.getCoverImageUri();
            Glide.with(mainActivity)
                    .load(imageUri != null && !imageUri.isEmpty() ? Uri.parse(imageUri) : R.drawable.ic_closet)
                    .placeholder(R.drawable.ic_placeholder)
                    .into(h.imageClothing);

            h.textSub.setVisibility(View.VISIBLE);
            h.btnAdd.setVisibility(View.GONE);
            h.btnMenu.setVisibility(View.VISIBLE);

            h.btnMenu.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(mainActivity, h.btnMenu);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.menu_closet_item, popup.getMenu());

                popup.setOnMenuItemClickListener(menuItem -> {
                    if (menuItem.getItemId() == R.id.action_delete) {
                        deleteClosetFromSupabase(item, position);
                        return true;
                    }
                    return false;
                });

                popup.show();
            });

            h.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(mainActivity, ClosetDetailActivity.class);
                intent.putExtra("closet_name", item.getName());
                mainActivity.startActivity(intent);
            });
        }
    }

    // ✅ UPDATED: Load outfit count for CURRENT USER only
    private void loadOutfitSnapshotCountForCurrentUser(OutfitViewHolder holder) {
        if (currentUserId == null || currentUserId.isEmpty()) {
            holder.outfitSub.setText("Tap to View");
            return;
        }

        // Run in background thread to avoid NetworkOnMainThreadException
        new Thread(() -> {
            try {
                List<Outfit> userOutfits = userOutfitRepository.getOutfitsForUser(currentUserId);
                int snapshotCount = userOutfits.size();

                mainActivity.runOnUiThread(() -> {
                    if (snapshotCount > 0) {
                        holder.outfitSub.setText(snapshotCount + " Outfit" + (snapshotCount > 1 ? "s" : ""));
                    } else {
                        holder.outfitSub.setText("Tap to View");
                    }
                });

                Log.d(TAG, "Loaded " + snapshotCount + " outfits for user: " + currentUserId);

            } catch (Exception e) {
                Log.e(TAG, "Error loading user outfit count: " + e.getMessage());
                mainActivity.runOnUiThread(() -> {
                    holder.outfitSub.setText("Tap to View");
                });
            }
        }).start();
    }

    private void deleteClosetFromSupabase(ClosetItem closetItem, int position) {
        if (currentUserId.isEmpty()) {
            Toast.makeText(mainActivity, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use the ID from the updated ClosetItem model
        String closetId = closetItem.getId();

        if (closetId == null || closetId.isEmpty()) {
            // For static cards or items without ID, just remove from UI
            if ("outfit_card".equals(closetItem.getType()) || "create_card".equals(closetItem.getType())) {
                // These are static cards, just show message
                Toast.makeText(mainActivity, "Cannot delete system item", Toast.LENGTH_SHORT).show();
                return;
            }

            // Try to delete by name as fallback
            deleteClosetByName(closetItem.getName(), position);
            return;
        }

        // Use Retrofit call for deletion
        retrofit2.Call<Void> call = supabaseService.deleteCloset(closetId);
        call.enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<Void> call, @NonNull retrofit2.Response<Void> response) {
                if (response.isSuccessful()) {
                    // Remove from local list and update UI
                    closetList.remove(position);
                    notifyItemRemoved(position);
                    Toast.makeText(mainActivity, "Closet deleted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mainActivity, "Failed to delete closet", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<Void> call, @NonNull Throwable t) {
                Toast.makeText(mainActivity, "Network error deleting closet", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteClosetByName(String closetName, int position) {
        // This is a fallback method - you might need to implement a proper query by name
        // For now, just remove from local list
        closetList.remove(position);
        notifyItemRemoved(position);
        Toast.makeText(mainActivity, "Closet removed locally", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int getItemCount() {
        return closetList != null ? closetList.size() : 0;
    }

    private void showBottomSheetMenu() {
        View sheetView = LayoutInflater.from(mainActivity).inflate(R.layout.popup_outfit_menu, null);
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(mainActivity);
        bottomSheetDialog.setContentView(sheetView);

        Button btnAddItem = sheetView.findViewById(R.id.btn_add_item);
        Button btnCreateOutfit = sheetView.findViewById(R.id.btn_create_outfit);
        ImageView iconClose = sheetView.findViewById(R.id.icon_close);

        btnCreateOutfit.setOnClickListener(v -> {
            mainActivity.startActivity(new Intent(mainActivity, OutfitCreationActivity.class));
            bottomSheetDialog.dismiss();
        });

        btnAddItem.setOnClickListener(v -> {
            mainActivity.startActivity(new Intent(mainActivity, CreateOutfitActivity.class));
            bottomSheetDialog.dismiss();
        });

        iconClose.setOnClickListener(v -> bottomSheetDialog.dismiss());
        bottomSheetDialog.show();
    }

    // ✅ Called from MainActivity.onResume() to refresh the "X Outfits" label
    public void updateOutfitSnapshotCount() {
        // This will automatically refresh when the adapter rebinds
        notifyDataSetChanged();
    }

    // ViewHolders
    static class ClosetViewHolder extends RecyclerView.ViewHolder {
        ImageView imageClothing;
        TextView textTitle, textSub;
        ImageButton btnAdd, btnMenu;

        public ClosetViewHolder(@NonNull View itemView) {
            super(itemView);
            imageClothing = itemView.findViewById(R.id.imageClothing);
            textTitle = itemView.findViewById(R.id.textTitle);
            textSub = itemView.findViewById(R.id.textSub);
            btnAdd = itemView.findViewById(R.id.btnAdd);
            btnMenu = itemView.findViewById(R.id.btnMenu);
        }
    }

    static class CreateViewHolder extends RecyclerView.ViewHolder {
        public CreateViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    static class OutfitViewHolder extends RecyclerView.ViewHolder {
        ImageView plusBtn;
        TextView outfitLabel, outfitSub;

        public OutfitViewHolder(@NonNull View itemView) {
            super(itemView);
            plusBtn = itemView.findViewById(R.id.img_plus);
            outfitLabel = itemView.findViewById(R.id.text_outfit_title);
            outfitSub = itemView.findViewById(R.id.text_outfit_sub);
        }
    }
}