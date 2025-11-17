package com.example.outpick.common.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.outpick.R;
import com.example.outpick.admin.UserAdminCreateOutfitActivity;
import com.example.outpick.admin.UserEditActivity;
import com.example.outpick.database.supabase.SupabaseService;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.UserViewHolder> {

    private Context context;
    private List<JsonObject> userList;
    private SupabaseService supabaseService;
    private Runnable refreshCallback;
    private static final int EDIT_USER_REQUEST = 100;

    public AdminUserAdapter(Context context, List<JsonObject> userList, SupabaseService supabaseService, Runnable refreshCallback) {
        this.context = context;
        this.userList = userList;
        this.supabaseService = supabaseService;
        this.refreshCallback = refreshCallback;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        JsonObject user = userList.get(position);

        // ✅ SAFELY Extract data from JsonObject
        String userId = getSafeString(user, "id", "");
        String username = getSafeString(user, "username", "N/A");
        String gender = getSafeString(user, "gender", "Not specified");
        String role = getSafeString(user, "role", "User");
        String signupDate = getSafeString(user, "created_at", "Unknown");
        String lastLogin = getSafeString(user, "last_login", "Never");

        // Handle boolean field safely
        boolean isActive = getSafeBoolean(user, "is_active", true);
        String status = isActive ? "Active" : "Suspended";

        // ✅ Display user info
        holder.txtUserId.setText("ID: " + (position + 1));
        holder.txtUsername.setText("Username: " + username);
        holder.txtGender.setText("Gender: " + gender);
        holder.txtSignupDate.setText("Signup: " + formatDate(signupDate));
        holder.txtRole.setText("Role: " + role);
        holder.txtLastLogin.setText("Last login: " + formatDate(lastLogin));
        holder.txtStatus.setText("Status: " + status);

        // ✅ Status text color
        switch (status.toLowerCase()) {
            case "active":
                holder.txtStatus.setTextColor(Color.parseColor("#388E3C")); // Green
                break;
            case "suspended":
                holder.txtStatus.setTextColor(Color.parseColor("#FFA000")); // Orange
                break;
            default:
                holder.txtStatus.setTextColor(Color.RED);
                break;
        }

        // ✅ Delete user
        holder.btnDelete.setOnClickListener(v -> {
            if (supabaseService != null && !userId.isEmpty()) {
                Call<Void> call = supabaseService.deleteUser(userId);
                call.enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(context, "User deleted permanently", Toast.LENGTH_SHORT).show();
                            if (refreshCallback != null) refreshCallback.run();
                        } else {
                            Toast.makeText(context, "Failed to delete user", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(context, "Error deleting user: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(context, "Cannot delete user: Service not available", Toast.LENGTH_SHORT).show();
            }
        });

        // ✅ Edit user
        holder.btnEdit.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                JsonObject editUser = userList.get(pos);
                String editUserId = getSafeString(editUser, "id", "");
                Intent intent = new Intent(context, UserEditActivity.class);
                intent.putExtra("userId", editUserId);
                if (context instanceof Activity) {
                    ((Activity) context).startActivityForResult(intent, EDIT_USER_REQUEST);
                }
            }
        });

        // ✅ Create Outfit → PASS BOTH USER ID AND USERNAME TO CREATE OUTFIT ACTIVITY
        holder.btnCreateOutfit.setOnClickListener(v -> {
            String outfitUserId = getSafeString(user, "id", "");
            String outfitUsername = getSafeString(user, "username", "");

            if (!outfitUserId.isEmpty() && !outfitUsername.isEmpty()) {
                Intent intent = new Intent(context, UserAdminCreateOutfitActivity.class);
                intent.putExtra("user_id", outfitUserId); // ✅ CRITICAL: Add this line
                intent.putExtra("username", outfitUsername);
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "Cannot create outfit: User information incomplete", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList != null ? userList.size() : 0;
    }

    // ✅ Update RecyclerView data
    public void updateUsers(List<JsonObject> newUsers) {
        this.userList = newUsers;
        notifyDataSetChanged();
    }

    // ✅ Safe method to get string values from JsonObject
    private String getSafeString(JsonObject json, String key, String defaultValue) {
        if (json == null || !json.has(key)) {
            return defaultValue;
        }

        JsonElement element = json.get(key);
        if (element == null || element.isJsonNull()) {
            return defaultValue;
        }

        return element.getAsString();
    }

    // ✅ Safe method to get boolean values
    private boolean getSafeBoolean(JsonObject json, String key, boolean defaultValue) {
        if (json == null || !json.has(key)) {
            return defaultValue;
        }

        JsonElement element = json.get(key);
        if (element == null || element.isJsonNull()) {
            return defaultValue;
        }

        try {
            return element.getAsBoolean();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    // ✅ Format date
    private String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty() || dateString.equals("Unknown") || dateString.equals("Never")) {
            return dateString;
        }
        try {
            if (dateString.contains("T")) {
                return dateString.split("T")[0];
            }
            return dateString;
        } catch (Exception e) {
            return dateString;
        }
    }

    // ✅ ViewHolder
    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView txtUserId, txtUsername, txtGender, txtSignupDate, txtLastLogin, txtStatus, txtRole;
        Button btnDelete, btnEdit, btnCreateOutfit;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            txtUserId = itemView.findViewById(R.id.txtUserId);
            txtUsername = itemView.findViewById(R.id.txtUsername);
            txtGender = itemView.findViewById(R.id.txtGender);
            txtSignupDate = itemView.findViewById(R.id.txtSignupDate);
            txtLastLogin = itemView.findViewById(R.id.txtLastLogin);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            txtRole = itemView.findViewById(R.id.txtRole);
            btnDelete = itemView.findViewById(R.id.btnDeleteUser);
            btnEdit = itemView.findViewById(R.id.btnEditUser);
            btnCreateOutfit = itemView.findViewById(R.id.btnCreateOutfit);
        }
    }
}