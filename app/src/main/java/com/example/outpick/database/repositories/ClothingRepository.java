package com.example.outpick.database.repositories;

import android.util.Log;

import com.example.outpick.database.models.ClothingItem;
import com.example.outpick.database.supabase.SupabaseService;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

public class ClothingRepository {
    private static final String TAG = "ClothingRepository";
    private static ClothingRepository instance;
    private List<ClothingItem> items;
    private SupabaseService supabaseService;
    private ExecutorService executorService;

    // Public constructor
    public ClothingRepository(SupabaseService supabaseService) {
        this.supabaseService = supabaseService;
        this.items = new ArrayList<>();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    // Singleton instance getter
    public static synchronized ClothingRepository getInstance(SupabaseService supabaseService) {
        if (instance == null) {
            instance = new ClothingRepository(supabaseService);
        }
        return instance;
    }

    // For existing code compatibility
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

                        if (item.getImagePath() != null && item.getImagePath().startsWith("http")) {
                            Log.d(TAG, "Cloud image detected for: " + item.getName());
                        }
                    }
                }
                Log.d(TAG, "Successfully loaded " + items.size() + " clothing items from cloud");
            } else {
                Log.e(TAG, "Failed to load clothing items: " + response.code() + " - " + response.message());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading clothing items: " + e.getMessage(), e);
        }
        return items;
    }

    /**
     * ✅ FIXED: Get clothing items by user ID with proper debugging
     */
    public List<ClothingItem> getClothingByUserId(String userId) {
        List<ClothingItem> items = new ArrayList<>();
        try {
            Log.d(TAG, "🔄 START: Getting clothing for user ID: " + userId);

            // Get all clothes first (most reliable method)
            List<ClothingItem> allClothes = getAllClothing();
            Log.d(TAG, "📊 Total clothes in database: " + allClothes.size());

            // Filter by user_id
            for (ClothingItem item : allClothes) {
                String itemUserId = item.getUserId();
                Log.d(TAG, "🔍 Checking item: " + item.getName() + " | User ID: '" + itemUserId + "'");

                if (userId.equals(itemUserId)) {
                    items.add(item);
                    Log.d(TAG, "✅ ADDED: " + item.getName() + " for user " + userId);
                } else if (itemUserId == null || itemUserId.isEmpty()) {
                    Log.d(TAG, "❌ SKIPPED: No user_id for item: " + item.getName());
                } else {
                    Log.d(TAG, "❌ SKIPPED: User ID mismatch - Expected: " + userId + ", Got: " + itemUserId);
                }
            }

            Log.d(TAG, "✅ FINAL: Found " + items.size() + " items for user ID: " + userId);

        } catch (Exception e) {
            Log.e(TAG, "❌ ERROR in getClothingByUserId: " + e.getMessage(), e);
        }
        return items;
    }

    /**
     * Get clothing items by closet name
     */
    public List<ClothingItem> getClothingByCloset(String closetName) {
        List<ClothingItem> items = new ArrayList<>();
        try {
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
                Log.e(TAG, "Failed to load clothing items for closet: " + response.code() + " - " + response.message());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading clothing items by closet: " + e.getMessage(), e);
        }
        return items;
    }

    /**
     * Get clothing item by ID
     */
    public ClothingItem getClothingById(String clothingId) {
        if (clothingId == null || clothingId.isEmpty()) {
            Log.e(TAG, "Cannot get clothing item: clothingId is null or empty");
            return null;
        }

        try {
            Call<List<JsonObject>> call = supabaseService.getClothingById(clothingId);
            Response<List<JsonObject>> response = call.execute();

            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                ClothingItem item = convertJsonToClothingItem(response.body().get(0));
                Log.d(TAG, "Successfully retrieved clothing item: " + clothingId);
                return item;
            } else {
                Log.e(TAG, "Failed to get clothing item by ID: " + response.code() + " - " + response.message());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting clothing item by ID: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Add clothing item with default closet
     */
    public boolean addClothingItem(String name, String imageUri, String category,
                                   String season, String occasion) {
        // Use user-specific closet name instead of default "My Closet"
        return addClothingItem(name, imageUri, category, season, occasion, "user_default");
    }

    /**
     * Add clothing item with custom closet name
     */
    public boolean addClothingItem(String name, String imageUri, String category,
                                   String season, String occasion, String closetName) {
        try {
            // Parameter validation
            if (name == null || name.trim().isEmpty()) {
                Log.e(TAG, "Cannot add clothing item: name is required");
                return false;
            }

            if (imageUri == null || imageUri.trim().isEmpty()) {
                Log.e(TAG, "Cannot add clothing item: imageUri is required");
                return false;
            }

            // Log cloud URL verification
            if (imageUri.startsWith("http")) {
                Log.d(TAG, "Adding clothing item with cloud URL: " + name);
            } else {
                Log.w(TAG, "Adding clothing item with local URI (should be cloud URL): " + name);
            }

            // Use proper date formatting
            String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(new java.util.Date());

            JsonObject clothing = new JsonObject();
            clothing.addProperty("name", name);
            clothing.addProperty("image_uri", imageUri);
            clothing.addProperty("category", category != null ? category : "Other");
            clothing.addProperty("season", season != null ? season : "All-Season");
            clothing.addProperty("occasion", occasion != null ? occasion : "Casual");
            clothing.addProperty("closet_name", closetName != null ? closetName : "user_default");
            clothing.addProperty("created_at", timestamp);

            // ✅ FIXED: Changed to List<JsonObject>
            Call<List<JsonObject>> call = supabaseService.insertClothing(clothing);
            Response<List<JsonObject>> response = call.execute();

            boolean success = response.isSuccessful();
            if (success) {
                Log.d(TAG, "Successfully added clothing item to cloud: " + name + " in closet: " + closetName);
                clearCache();
            } else {
                Log.e(TAG, "Failed to add clothing item: " + response.code() + " - " + response.message());
                // Log response body for debugging
                try {
                    if (response.errorBody() != null) {
                        Log.e(TAG, "Error body: " + response.errorBody().string());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Could not read error body", e);
                }
            }
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Error adding clothing item: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * ✅ NEW: Add clothing item with user ID
     */
    public boolean addClothingItemWithUserId(String name, String imageUri, String category,
                                             String season, String occasion, String userId) {
        try {
            if (name == null || name.trim().isEmpty()) {
                Log.e(TAG, "Cannot add clothing item: name is required");
                return false;
            }

            if (imageUri == null || imageUri.trim().isEmpty()) {
                Log.e(TAG, "Cannot add clothing item: imageUri is required");
                return false;
            }

            String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                    .format(new java.util.Date());

            JsonObject clothing = new JsonObject();
            clothing.addProperty("name", name);
            clothing.addProperty("image_uri", imageUri);
            clothing.addProperty("category", category != null ? category : "Other");
            clothing.addProperty("season", season != null ? season : "All-Season");
            clothing.addProperty("occasion", occasion != null ? occasion : "Casual");
            clothing.addProperty("user_id", userId); // ✅ ADD USER ID
            clothing.addProperty("created_at", timestamp);

            // ✅ FIXED: Changed to List<JsonObject>
            Call<List<JsonObject>> call = supabaseService.insertClothing(clothing);
            Response<List<JsonObject>> response = call.execute();

            boolean success = response.isSuccessful();
            if (success) {
                Log.d(TAG, "Successfully added clothing item for user: " + userId);
                clearCache();
            } else {
                Log.e(TAG, "Failed to add clothing item: " + response.code() + " - " + response.message());
            }
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Error adding clothing item: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Add ClothingItem object directly
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

    /**
     * Update clothing item with specific fields - FIXED VERSION
     */
    public boolean updateClothingItem(String clothingId, String category, String season, String occasion) {
        if (clothingId == null || clothingId.isEmpty()) {
            Log.e(TAG, "Cannot update clothing item: clothingId is null or empty");
            return false;
        }

        try {
            JsonObject updates = new JsonObject();
            if (category != null) updates.addProperty("category", category);
            if (season != null) updates.addProperty("season", season);
            if (occasion != null) updates.addProperty("occasion", occasion);

            // ✅ FIXED: Use the corrected method that returns List<JsonObject>
            Call<List<JsonObject>> call = supabaseService.updateClothing(clothingId, updates);
            Response<List<JsonObject>> response = call.execute();

            boolean success = response.isSuccessful();
            if (success) {
                Log.d(TAG, "Successfully updated clothing item: " + clothingId);
                clearCache();
            } else {
                Log.e(TAG, "Failed to update clothing item: " + response.code() + " - " + response.message());
                // Log response body for debugging
                try {
                    if (response.errorBody() != null) {
                        Log.e(TAG, "Error body: " + response.errorBody().string());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Could not read error body", e);
                }
            }
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Error updating clothing item: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Update clothing item with full object - FIXED VERSION
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

            // ✅ FIXED: Use the corrected method that returns List<JsonObject>
            Call<List<JsonObject>> call = supabaseService.updateClothing(clothingItem.getId(), updates);
            Response<List<JsonObject>> response = call.execute();

            boolean success = response.isSuccessful();
            if (success) {
                Log.d(TAG, "Successfully updated clothing item: " + clothingItem.getId());
                clearCache();
            } else {
                Log.e(TAG, "Failed to update clothing item: " + response.code() + " - " + response.message());
                try {
                    if (response.errorBody() != null) {
                        Log.e(TAG, "Error body: " + response.errorBody().string());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Could not read error body", e);
                }
            }
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Error updating clothing item: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * ✅ FIXED: Delete clothing item with proper PostgREST filter syntax
     */
    public boolean deleteClothing(String clothingId) {
        // Add parameter validation
        if (clothingId == null || clothingId.isEmpty()) {
            Log.e(TAG, "Cannot delete clothing item: clothingId is null or empty");
            return false;
        }

        try {
            // ✅ FIXED: Use proper PostgREST filter syntax - this will create: clothing?id=eq.UUID
            Call<Void> call = supabaseService.deleteClothing("eq." + clothingId);
            Response<Void> response = call.execute();

            boolean success = response.isSuccessful();
            if (success) {
                Log.d(TAG, "Successfully deleted clothing item: " + clothingId);
                clearCache();
            } else {
                Log.e(TAG, "Failed to delete clothing item: " + response.code() + " - " + response.message());
                // Log response body for debugging
                try {
                    if (response.errorBody() != null) {
                        Log.e(TAG, "Error body: " + response.errorBody().string());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Could not read error body", e);
                }
            }
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting clothing item: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Alias for deleteClothing
     */
    public boolean deleteClothingItem(String clothingId) {
        return deleteClothing(clothingId);
    }

    /**
     * Get filtered clothing items
     */
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
            } else {
                Log.e(TAG, "Failed to filter clothing: " + response.code() + " - " + response.message());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error filtering clothing items: " + e.getMessage(), e);
        }
        return items;
    }

    /**
     * ✅ FIXED: Convert JSON to ClothingItem object with proper user_id handling
     */
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

            // Better field handling with priority
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

            // ✅ CRITICAL FIX: Properly handle user_id field
            if (json.has("user_id") && !json.get("user_id").isJsonNull()) {
                String userId = json.get("user_id").getAsString();
                item.setUserId(userId);
                Log.d(TAG, "✅ Set user_id: " + userId + " for item: " + item.getName());
            } else {
                // ✅ Log when user_id is missing
                Log.w(TAG, "⚠️ user_id is null or missing for item: " + item.getName());
            }

            // Set default values if fields are missing
            if (item.getName() == null) item.setName("Unnamed Item");
            if (item.getSeason() == null) item.setSeason("All-Season");
            if (item.getOccasion() == null) item.setOccasion("Casual");
            if (item.getClosetName() == null) item.setClosetName("user_default");

            return item;
        } catch (Exception e) {
            Log.e(TAG, "Error converting JSON to ClothingItem: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get cached items
     */
    public List<ClothingItem> getItems() {
        return items;
    }

    /**
     * Set cached items
     */
    public void setItems(List<ClothingItem> items) {
        this.items = items;
    }

    /**
     * Clear local cache
     */
    public void clearCache() {
        this.items.clear();
        Log.d(TAG, "Clothing repository cache cleared");
    }

    /**
     * Refresh data from server
     */
    public List<ClothingItem> refreshData() {
        clearCache();
        return getAllClothing();
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}