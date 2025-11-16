package com.example.outpick.database.repositories;

import android.util.Log;

import com.example.outpick.database.models.ClothingItem;
import com.example.outpick.database.supabase.SupabaseService;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class ClothingRepository {
    private static final String TAG = "ClothingRepository";
    private static ClothingRepository instance;
    private List<ClothingItem> items;
    private SupabaseService supabaseService;

    // Public constructor - CHANGED FROM PRIVATE TO PUBLIC
    public ClothingRepository(SupabaseService supabaseService) {
        this.supabaseService = supabaseService;
        this.items = new ArrayList<>();
    }

    // Singleton instance getter (optional - you can remove this if not needed)
    public static synchronized ClothingRepository getInstance(SupabaseService supabaseService) {
        if (instance == null) {
            instance = new ClothingRepository(supabaseService);
        }
        return instance;
    }

    // For existing code compatibility (optional)
    public static ClothingRepository getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ClothingRepository must be initialized with SupabaseService first");
        }
        return instance;
    }

    public List<ClothingItem> getAllClothing() {
        List<ClothingItem> items = new ArrayList<>();
        try {
            Call<List<JsonObject>> call = supabaseService.getClothing();
            Response<List<JsonObject>> response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                for (JsonObject json : response.body()) {
                    ClothingItem item = convertJsonToClothingItem(json);
                    if (item != null) {
                        items.add(item);

                        // ✅ ADDED: Log cloud URL detection
                        if (item.getImagePath() != null && item.getImagePath().startsWith("http")) {
                            Log.d(TAG, "Cloud image detected for: " + item.getName());
                        }
                    }
                }
                Log.d(TAG, "Successfully loaded " + items.size() + " clothing items from cloud");
            } else {
                Log.e(TAG, "Failed to load clothing items: " + response.message());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading clothing items: " + e.getMessage());
            e.printStackTrace();
        }
        return items;
    }

    /**
     * ✅ ADDED: Get clothing items by closet name
     */
    public List<ClothingItem> getClothingByCloset(String closetName) {
        List<ClothingItem> items = new ArrayList<>();
        try {
            // If your Supabase service has a specific method for this, use it
            // Otherwise, we'll filter from all items
            Call<List<JsonObject>> call = supabaseService.getClothing();
            Response<List<JsonObject>> response = call.execute();

            if (response.isSuccessful() && response.body() != null) {
                for (JsonObject json : response.body()) {
                    ClothingItem item = convertJsonToClothingItem(json);
                    if (item != null && closetName.equals(item.getClosetName())) {
                        items.add(item);
                    }
                }
                Log.d(TAG, "Found " + items.size() + " items in closet: " + closetName);
            } else {
                Log.e(TAG, "Failed to load clothing items for closet: " + response.message());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading clothing items by closet: " + e.getMessage());
            e.printStackTrace();
        }
        return items;
    }

    /**
     * ✅ ADDED: Get clothing item by ID
     */
    public ClothingItem getClothingById(String clothingId) {
        try {
            // ✅ FIXED: Changed from Call<JsonObject> to Call<List<JsonObject>>
            Call<List<JsonObject>> call = supabaseService.getClothingById(clothingId);
            Response<List<JsonObject>> response = call.execute();

            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                // Get the first item from the list
                return convertJsonToClothingItem(response.body().get(0));
            } else {
                Log.e(TAG, "Failed to get clothing item by ID: " + response.message());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting clothing item by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * ✅ IMPROVED: Added parameter validation and better logging
     */
    public boolean addClothingItem(String name, String imageUri, String category,
                                   String season, String occasion) {
        return addClothingItem(name, imageUri, category, season, occasion, "My Closet");
    }

    /**
     * ✅ ADDED: Overloaded method with closet name
     */
    public boolean addClothingItem(String name, String imageUri, String category,
                                   String season, String occasion, String closetName) {
        try {
            // ✅ ADDED: Parameter validation
            if (name == null || name.trim().isEmpty()) {
                Log.e(TAG, "Cannot add clothing item: name is required");
                return false;
            }

            if (imageUri == null || imageUri.trim().isEmpty()) {
                Log.e(TAG, "Cannot add clothing item: imageUri is required");
                return false;
            }

            // ✅ ADDED: Log cloud URL verification
            if (imageUri.startsWith("http")) {
                Log.d(TAG, "Adding clothing item with cloud URL: " + name);
            } else {
                Log.w(TAG, "Adding clothing item with local URI (should be cloud URL): " + name);
            }

            JsonObject clothing = new JsonObject();
            clothing.addProperty("name", name);
            clothing.addProperty("image_uri", imageUri); // This should be a CLOUD URL
            clothing.addProperty("category", category);
            clothing.addProperty("season", season);
            clothing.addProperty("occasion", occasion);
            clothing.addProperty("closet_name", closetName != null ? closetName : "My Closet");
            clothing.addProperty("created_at", new java.util.Date().toString());

            Call<JsonObject> call = supabaseService.insertClothing(clothing);
            Response<JsonObject> response = call.execute();

            boolean success = response.isSuccessful();
            if (success) {
                Log.d(TAG, "Successfully added clothing item to cloud: " + name);
                // Clear cache to ensure fresh data on next load
                clearCache();
            } else {
                Log.e(TAG, "Failed to add clothing item: " + response.message());
            }
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Error adding clothing item: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ✅ ADDED: Method to add ClothingItem object directly
     */
    public boolean addClothingItem(ClothingItem clothingItem) {
        if (clothingItem == null) {
            Log.e(TAG, "Cannot add null clothing item");
            return false;
        }

        return addClothingItem(
                clothingItem.getName(),
                clothingItem.getImagePath(),
                clothingItem.getCategory(),
                clothingItem.getSeason(),
                clothingItem.getOccasion(),
                clothingItem.getClosetName()
        );
    }

    public boolean updateClothingItem(String clothingId, String category, String season, String occasion) {
        try {
            JsonObject updates = new JsonObject();
            updates.addProperty("category", category);
            updates.addProperty("season", season);
            updates.addProperty("occasion", occasion);

            Call<JsonObject> call = supabaseService.updateClothing(clothingId, updates);
            Response<JsonObject> response = call.execute();

            boolean success = response.isSuccessful();
            if (success) {
                Log.d(TAG, "Successfully updated clothing item: " + clothingId);
                clearCache(); // Clear cache to ensure fresh data
            } else {
                Log.e(TAG, "Failed to update clothing item: " + response.message());
            }
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Error updating clothing item: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ✅ ADDED: Update clothing item with full object
     */
    public boolean updateClothingItem(ClothingItem clothingItem) {
        if (clothingItem == null || clothingItem.getId() == null) {
            Log.e(TAG, "Cannot update clothing item: item or ID is null");
            return false;
        }

        try {
            JsonObject updates = new JsonObject();
            if (clothingItem.getName() != null) updates.addProperty("name", clothingItem.getName());
            if (clothingItem.getImagePath() != null) updates.addProperty("image_uri", clothingItem.getImagePath());
            if (clothingItem.getCategory() != null) updates.addProperty("category", clothingItem.getCategory());
            if (clothingItem.getSeason() != null) updates.addProperty("season", clothingItem.getSeason());
            if (clothingItem.getOccasion() != null) updates.addProperty("occasion", clothingItem.getOccasion());
            if (clothingItem.getClosetName() != null) updates.addProperty("closet_name", clothingItem.getClosetName());

            Call<JsonObject> call = supabaseService.updateClothing(clothingItem.getId(), updates);
            Response<JsonObject> response = call.execute();

            boolean success = response.isSuccessful();
            if (success) {
                Log.d(TAG, "Successfully updated clothing item: " + clothingItem.getId());
                clearCache();
            } else {
                Log.e(TAG, "Failed to update clothing item: " + response.message());
            }
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Error updating clothing item: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteClothing(String clothingId) {
        try {
            Call<Void> call = supabaseService.deleteClothing(clothingId);
            Response<Void> response = call.execute();

            boolean success = response.isSuccessful();
            if (success) {
                Log.d(TAG, "Successfully deleted clothing item: " + clothingId);
                clearCache(); // Clear cache to ensure fresh data
            } else {
                Log.e(TAG, "Failed to delete clothing item: " + response.message());
            }
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting clothing item: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ✅ ADDED: Alias for deleteClothing for consistency
     */
    public boolean deleteClothingItem(String clothingId) {
        return deleteClothing(clothingId);
    }

    public List<ClothingItem> getFilteredClothing(String category, String season, String occasion) {
        List<ClothingItem> items = new ArrayList<>();
        try {
            Call<List<JsonObject>> call = supabaseService.getFilteredClothing(category, season, occasion);
            Response<List<JsonObject>> response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                for (JsonObject json : response.body()) {
                    ClothingItem item = convertJsonToClothingItem(json);
                    if (item != null) {
                        items.add(item);
                    }
                }
                Log.d(TAG, "Filtered clothing: " + items.size() + " items found");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error filtering clothing items: " + e.getMessage());
            e.printStackTrace();
        }
        return items;
    }

    private ClothingItem convertJsonToClothingItem(JsonObject json) {
        try {
            ClothingItem item = new ClothingItem();

            // Safe field access with null checks
            if (json.has("id") && !json.get("id").isJsonNull()) {
                item.setId(json.get("id").getAsString());
            }

            if (json.has("name") && !json.get("name").isJsonNull()) {
                item.setName(json.get("name").getAsString());
            }

            // ✅ IMPROVED: Better field handling with priority
            String imageUrl = null;
            if (json.has("image_uri") && !json.get("image_uri").isJsonNull()) {
                imageUrl = json.get("image_uri").getAsString();
            } else if (json.has("image_path") && !json.get("image_path").isJsonNull()) {
                imageUrl = json.get("image_path").getAsString();
            }
            item.setImagePath(imageUrl);

            if (json.has("category") && !json.get("category").isJsonNull()) {
                item.setCategory(json.get("category").getAsString());
            }

            if (json.has("season") && !json.get("season").isJsonNull()) {
                item.setSeason(json.get("season").getAsString());
            }

            if (json.has("occasion") && !json.get("occasion").isJsonNull()) {
                item.setOccasion(json.get("occasion").getAsString());
            }

            if (json.has("closet_name") && !json.get("closet_name").isJsonNull()) {
                item.setClosetName(json.get("closet_name").getAsString());
            }

            // Set default values if fields are missing
            if (item.getName() == null) item.setName("Unnamed Item");
            if (item.getSeason() == null) item.setSeason("All-Season");
            if (item.getOccasion() == null) item.setOccasion("Casual");
            if (item.getClosetName() == null) item.setClosetName("My Closet");

            return item;
        } catch (Exception e) {
            Log.e(TAG, "Error converting JSON to ClothingItem: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // For local caching
    public List<ClothingItem> getItems() {
        return items;
    }

    public void setItems(List<ClothingItem> items) {
        this.items = items;
    }

    // ✅ ADDED: Clear cache method
    public void clearCache() {
        this.items.clear();
        Log.d(TAG, "Clothing repository cache cleared");
    }

    /**
     * ✅ ADDED: Refresh data from server
     */
    public List<ClothingItem> refreshData() {
        clearCache();
        return getAllClothing();
    }
}