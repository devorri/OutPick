package com.example.outpick.database.repositories;

import com.example.outpick.database.models.ClothingItem;
import com.example.outpick.database.supabase.SupabaseService;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class ClothingRepository {
    private static ClothingRepository instance;
    private List<ClothingItem> items;
    private SupabaseService supabaseService;

    // Private constructor
    private ClothingRepository(SupabaseService supabaseService) {
        this.supabaseService = supabaseService;
        this.items = new ArrayList<>();
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
                    items.add(item);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }

    public boolean addClothingItem(String name, String imageUri, String category,
                                   String season, String occasion) {
        try {
            JsonObject clothing = new JsonObject();
            clothing.addProperty("name", name);
            clothing.addProperty("image_uri", imageUri);
            clothing.addProperty("category", category);
            clothing.addProperty("season", season);
            clothing.addProperty("occasion", occasion);

            Call<JsonObject> call = supabaseService.insertClothing(clothing);
            Response<JsonObject> response = call.execute();
            return response.isSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateClothingItem(String clothingId, String category, String season, String occasion) {
        try {
            JsonObject updates = new JsonObject();
            updates.addProperty("category", category);
            updates.addProperty("season", season);
            updates.addProperty("occasion", occasion);

            Call<JsonObject> call = supabaseService.updateClothing(clothingId, updates);
            Response<JsonObject> response = call.execute();
            return response.isSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteClothingItem(String clothingId) {
        try {
            Call<Void> call = supabaseService.deleteClothing(clothingId);
            Response<Void> response = call.execute();
            return response.isSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<ClothingItem> getFilteredClothing(String category, String season, String occasion) {
        List<ClothingItem> items = new ArrayList<>();
        try {
            Call<List<JsonObject>> call = supabaseService.getFilteredClothing(category, season, occasion);
            Response<List<JsonObject>> response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                for (JsonObject json : response.body()) {
                    ClothingItem item = convertJsonToClothingItem(json);
                    items.add(item);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }

    private ClothingItem convertJsonToClothingItem(JsonObject json) {
        ClothingItem item = new ClothingItem();

        if (json.has("id")) item.setId(json.get("id").getAsString());
        if (json.has("name")) item.setName(json.get("name").getAsString());

        // Handle both image_uri and image_path fields
        if (json.has("image_uri")) {
            item.setImagePath(json.get("image_uri").getAsString());
        } else if (json.has("image_path")) {
            item.setImagePath(json.get("image_path").getAsString());
        }

        if (json.has("category")) item.setCategory(json.get("category").getAsString());
        if (json.has("season")) item.setSeason(json.get("season").getAsString());
        if (json.has("occasion")) item.setOccasion(json.get("occasion").getAsString());

        // Set default values if fields are missing
        if (item.getName() == null) item.setName("Unnamed Item");
        if (item.getSeason() == null) item.setSeason("All-Season");
        if (item.getOccasion() == null) item.setOccasion("Casual");
        if (item.getClosetName() == null) item.setClosetName("My Closet");

        return item;
    }

    // For local caching
    public List<ClothingItem> getItems() {
        return items;
    }

    public void setItems(List<ClothingItem> items) {
        this.items = items;
    }
}