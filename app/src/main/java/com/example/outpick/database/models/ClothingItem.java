package com.example.outpick.database.models;

import android.net.Uri;
import java.io.Serializable;

/**
 * Data class for a clothing item, updated for Supabase compatibility (String IDs)
 */
public class ClothingItem implements Serializable {

    // --- Core Database Fields ---
    private String id; // Changed from int to String for Supabase UUID
    private String name;
    private String category;
    private String imagePath;
    private boolean isFavorite;

    // --- Categorization & State Fields ---
    private String season;
    private String occasion;
    private boolean isSelected;
    private String closetName;
    private int mockDrawableId;

    // ✅ NEW: User relationship field
    private String userId;

    // === Constructors ===

    // Default empty constructor
    public ClothingItem() {
        this.closetName = "My Closet";
        this.isSelected = false;
        this.isFavorite = false;
        this.name = "Unnamed Item";
        this.id = "";
        this.userId = "";
    }

    // Full constructor with String ID
    public ClothingItem(String id, String name, String category, String imagePath, boolean isFavorite) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.imagePath = imagePath;
        this.isFavorite = isFavorite;
        this.closetName = "My Closet";
        this.isSelected = false;
        this.season = "All-Season";
        this.occasion = "Casual";
        this.userId = "";
    }

    // Constructor for mock data
    public ClothingItem(String mockId, String name, String category, Uri uri, int mockDrawableId) {
        this.name = name;
        this.category = category;
        this.mockDrawableId = mockDrawableId;
        this.id = mockId; // Use the mock ID as string
        this.imagePath = uri != null ? uri.toString() : null;
        this.closetName = "TryOn Mock Data";
        this.isSelected = false;
        this.isFavorite = false;
        this.occasion = "Casual";
        this.season = "All-Season";
        this.userId = "";
    }

    // Constructor without ID
    public ClothingItem(String imagePath, String category, String season, String occasion) {
        this.imagePath = imagePath;
        this.category = category;
        this.season = season;
        this.occasion = occasion;
        this.isSelected = false;
        this.isFavorite = false;
        this.closetName = "My Closet";
        this.name = "Unnamed Item";
        this.id = "";
        this.userId = "";
    }

    // Full constructor with ID and occasion
    public ClothingItem(String id, String imagePath, String category, String season, String occasion) {
        this.id = id;
        this.imagePath = imagePath;
        this.category = category;
        this.season = season;
        this.occasion = occasion;
        this.isSelected = false;
        this.isFavorite = false;
        this.closetName = "My Closet";
        this.name = "Item " + id;
        this.userId = "";
    }

    // Full constructor for retrieving from DB with closetName
    public ClothingItem(String id, String imagePath, String category, String season, String occasion, String closetName) {
        this.id = id;
        this.imagePath = imagePath;
        this.category = category;
        this.season = season;
        this.occasion = occasion;
        this.closetName = closetName;
        this.isSelected = false;
        this.isFavorite = false;
        this.name = "Item " + id;
        this.userId = "";
    }

    // ✅ NEW: Constructor with userId
    public ClothingItem(String id, String imagePath, String category, String season, String occasion, String closetName, String userId) {
        this.id = id;
        this.imagePath = imagePath;
        this.category = category;
        this.season = season;
        this.occasion = occasion;
        this.closetName = closetName;
        this.userId = userId;
        this.isSelected = false;
        this.isFavorite = false;
        this.name = "Item " + id;
    }

    // Minimal constructor with only image
    public ClothingItem(String imagePath) {
        this.imagePath = imagePath;
        this.isSelected = false;
        this.isFavorite = false;
        this.closetName = "My Closet";
        this.name = "Unnamed Item";
        this.id = "";
        this.userId = "";
    }

    // Full constructor with closetName, season, category and selection
    public ClothingItem(String id, String imagePath, String category, String season, boolean isSelected, String closetName) {
        this.id = id;
        this.imagePath = imagePath;
        this.category = category;
        this.season = season;
        this.isSelected = isSelected;
        this.isFavorite = false;
        this.closetName = closetName;
        this.name = "Item " + id;
        this.userId = "";
    }

    // === Getters ===
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getMockDrawableId() {
        return mockDrawableId;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getImageUri() {
        return getImagePath();
    }

    public String getCategory() {
        return category;
    }

    public String getSubcategory() {
        if (category == null) {
            return null;
        }
        int index = category.lastIndexOf('>');
        if (index > -1 && index < category.length() - 1) {
            return category.substring(index + 1).trim();
        }
        return category.trim();
    }

    public String getSeason() {
        return season;
    }

    public String getOccasion() {
        return occasion;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public String getClosetName() {
        return closetName;
    }

    // ✅ NEW: Getter for userId
    public String getUserId() {
        return userId;
    }

    // === Setters ===
    public void setId(String id) {
        this.id = id;
        if (this.name == null || this.name.startsWith("Item ")) {
            this.name = "Item " + id;
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMockDrawableId(int mockDrawableId) {
        this.mockDrawableId = mockDrawableId;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public void setImagePath(Uri imageUri) {
        this.imagePath = imageUri != null ? imageUri.toString() : null;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public void setOccasion(String occasion) {
        this.occasion = occasion;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }

    public void setClosetName(String closetName) {
        this.closetName = closetName;
    }

    // ✅ NEW: Setter for userId
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "ClothingItem{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", imagePath='" + imagePath + '\'' +
                ", isFavorite=" + isFavorite +
                ", season='" + season + '\'' +
                ", occasion='" + occasion + '\'' +
                ", isSelected=" + isSelected +
                ", closetName='" + closetName + '\'' +
                ", userId='" + userId + '\'' + // ✅ ADDED userId to toString
                '}';
    }
}