package com.example.outpick.common.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.outpick.R;

import java.util.ArrayList;

public class ImageAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<Bitmap> images;
    private ArrayList<String> imageUrls; // ✅ ADDED: Support for cloud URLs

    // ✅ UPDATED: Constructor for backward compatibility (bitmaps only)
    public ImageAdapter(Context context, ArrayList<Bitmap> images) {
        this.context = context;
        this.images = images;
        this.imageUrls = new ArrayList<>(); // Initialize empty URLs list
    }

    // ✅ ADDED: New constructor that accepts both bitmaps and URLs
    public ImageAdapter(Context context, ArrayList<Bitmap> images, ArrayList<String> imageUrls) {
        this.context = context;
        this.images = images;
        this.imageUrls = imageUrls;

        // Ensure lists are the same size for safety
        if (images.size() != imageUrls.size()) {
            // If sizes don't match, create placeholder bitmaps for URLs
            if (imageUrls.size() > images.size()) {
                for (int i = images.size(); i < imageUrls.size(); i++) {
                    images.add(null); // Add null placeholders
                }
            }
        }
    }

    @Override
    public int getCount() {
        // Return the larger of the two lists to ensure all items are shown
        return Math.max(images.size(), imageUrls.size());
    }

    @Override
    public Object getItem(int position) {
        // Return the URL if available, otherwise return bitmap
        if (position < imageUrls.size() && imageUrls.get(position) != null) {
            return imageUrls.get(position);
        } else if (position < images.size()) {
            return images.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;

        if (convertView == null) {
            imageView = new ImageView(context);
            imageView.setLayoutParams(new ViewGroup.LayoutParams(300, 300));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            imageView = (ImageView) convertView;
        }

        // ✅ FIXED: Priority loading - Use URL first, then bitmap as fallback
        if (position < imageUrls.size() && imageUrls.get(position) != null && !imageUrls.get(position).isEmpty()) {
            // Load from cloud URL using Glide
            String imageUrl = imageUrls.get(position);
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_error)
                    .into(imageView);
        } else if (position < images.size() && images.get(position) != null) {
            // Fallback to bitmap
            imageView.setImageBitmap(images.get(position));
        } else {
            // Final fallback - error image
            imageView.setImageResource(R.drawable.ic_error);
        }

        return imageView;
    }

    // ✅ ADDED: Method to update URLs
    public void updateImageUrls(ArrayList<String> newImageUrls) {
        this.imageUrls.clear();
        this.imageUrls.addAll(newImageUrls);
        notifyDataSetChanged();
    }

    // ✅ ADDED: Method to update both images and URLs
    public void updateData(ArrayList<Bitmap> newImages, ArrayList<String> newImageUrls) {
        this.images.clear();
        this.images.addAll(newImages);
        this.imageUrls.clear();
        this.imageUrls.addAll(newImageUrls);
        notifyDataSetChanged();
    }

    // ✅ ADDED: Method to get image URL at position
    public String getImageUrl(int position) {
        if (position >= 0 && position < imageUrls.size()) {
            return imageUrls.get(position);
        }
        return null;
    }

    // ✅ ADDED: Method to check if item at position is a cloud image
    public boolean isCloudImage(int position) {
        if (position >= 0 && position < imageUrls.size()) {
            String url = imageUrls.get(position);
            return url != null && url.startsWith("http");
        }
        return false;
    }
}