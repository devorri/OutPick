package com.example.outpick.common.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.outpick.database.models.OutfitCombination;
import com.example.outpick.R;

import java.util.List;

public class OutfitCombinationAdapter extends RecyclerView.Adapter<OutfitCombinationAdapter.ViewHolder> {

    private Context context;
    private List<OutfitCombination> outfitList;

    public OutfitCombinationAdapter(Context context, List<OutfitCombination> outfitList) {
        this.context = context;
        this.outfitList = outfitList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_outfit_combination_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OutfitCombination outfit = outfitList.get(position);
        holder.title.setText(outfit.getTitle());
        holder.imgTop.setImageResource(outfit.getTopImageResId());
        holder.imgBottom.setImageResource(outfit.getBottomImageResId());
    }

    @Override
    public int getItemCount() {
        return outfitList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        ImageView imgTop, imgBottom;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.textTitle);
            imgTop = itemView.findViewById(R.id.imageTop);
            imgBottom = itemView.findViewById(R.id.imageBottom);
        }
    }
}
