package com.example.outpick.common.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ClosetListAdapter extends RecyclerView.Adapter<ClosetListAdapter.ViewHolder> {

    private final List<String> closetNames;
    private int selectedPosition = -1;

    public ClosetListAdapter(List<String> closetNames) {
        this.closetNames = closetNames;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_single_choice, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.checkedTextView.setText(closetNames.get(position));
        holder.checkedTextView.setChecked(position == selectedPosition);

        holder.itemView.setOnClickListener(v -> {
            selectedPosition = position;
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return closetNames.size();
    }

    public String getSelectedCloset() {
        return (selectedPosition != -1) ? closetNames.get(selectedPosition) : null;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckedTextView checkedTextView;

        ViewHolder(View itemView) {
            super(itemView);
            checkedTextView = (CheckedTextView) itemView;
        }
    }
}
