package com.example.outpick.common.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.outpick.R;
import com.example.outpick.database.models.UserModel;
import java.util.List;

public class UserActivityLogsAdapter extends RecyclerView.Adapter<UserActivityLogsAdapter.UserActivityViewHolder> {

    private List<UserModel> userList;

    public UserActivityLogsAdapter(List<UserModel> userList) {
        this.userList = userList != null ? userList : new java.util.ArrayList<>();
    }

    @NonNull
    @Override
    public UserActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_activity_log, parent, false);
        return new UserActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserActivityViewHolder holder, int position) {
        UserModel user = userList.get(position);

        // ✅ Set data to views - using the CORRECT IDs from your layout
        setTextSafe(holder.tvUsername, user.getUsername());
        setTextSafe(holder.tvSignupDate, user.getSignupDate());
        setTextSafe(holder.tvLastLogin, user.getLastLogin());
        setTextSafe(holder.tvLastLogout, user.getLastLogout());
        setTextSafe(holder.tvRole, user.getRole());

        // Optional: Set gender if you add it to your layout
        // setTextSafe(holder.tvGender, user.getGender());
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    // ✅ Safe method to set text and avoid NullPointerException
    private void setTextSafe(TextView textView, String text) {
        if (textView != null) {
            textView.setText(text != null ? text : "N/A");
        }
    }

    // ✅ Update data method
    public void updateData(List<UserModel> newUserList) {
        this.userList = newUserList != null ? newUserList : new java.util.ArrayList<>();
        notifyDataSetChanged();
    }

    // ✅ FIXED ViewHolder - IDs MUST MATCH your layout file
    public static class UserActivityViewHolder extends RecyclerView.ViewHolder {
        // ✅ CORRECT IDs from your layout file
        TextView tvUsername, tvSignupDate, tvLastLogin, tvLastLogout, tvRole;

        public UserActivityViewHolder(@NonNull View itemView) {
            super(itemView);

            // ✅ Initialize with CORRECT IDs that match your XML
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvSignupDate = itemView.findViewById(R.id.tvSignupDate);
            tvLastLogin = itemView.findViewById(R.id.tvLastLogin);
            tvLastLogout = itemView.findViewById(R.id.tvLastLogout);
            tvRole = itemView.findViewById(R.id.tvRole);
        }
    }
}