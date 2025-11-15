package com.example.outpick.common.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.outpick.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Adapter for selecting closets in the "Add to Closet" bottom sheet.
 * Does not modify the database directly.
 * Selected closets are applied only when the "Add Item List" button is clicked.
 */
public class ClosetSelectionAdapter extends RecyclerView.Adapter<ClosetSelectionAdapter.ViewHolder> {

    private final ArrayList<String> closets;      // List of all closets
    private final Set<String> selectedClosets = new HashSet<>(); // Tracks user selection

    public ClosetSelectionAdapter(ArrayList<String> closets) {
        this.closets = closets;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_closet_selection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String closetName = closets.get(position);
        holder.closetNameText.setText(closetName);

        // Set checkbox state based on selection
        holder.checkCircle.setChecked(selectedClosets.contains(closetName));

        // Toggle selection on item click
        holder.itemView.setOnClickListener(v -> {
            if (selectedClosets.contains(closetName)) {
                selectedClosets.remove(closetName);
                holder.checkCircle.setChecked(false);
            } else {
                selectedClosets.add(closetName);
                holder.checkCircle.setChecked(true);
            }
        });
    }

    @Override
    public int getItemCount() {
        return closets.size();
    }

    /**
     * Returns the set of closets selected by the user.
     */
    public Set<String> getSelectedClosets() {
        return selectedClosets;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView closetNameText;
        CheckBox checkCircle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            closetNameText = itemView.findViewById(R.id.closetNameText);
            checkCircle = itemView.findViewById(R.id.checkCircle);
        }
    }
}
