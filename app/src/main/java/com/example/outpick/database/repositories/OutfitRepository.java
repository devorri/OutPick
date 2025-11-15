package com.example.outpick.database.repositories;

import com.example.outpick.database.models.Outfit;
import com.example.outpick.database.supabase.SupabaseService;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class OutfitRepository {
    private SupabaseService supabase;

    public OutfitRepository(SupabaseService supabase) {
        this.supabase = supabase;
    }

    public List<Outfit> getAllOutfits() {
        List<Outfit> outfits = new ArrayList<>();
        try {
            Response<List<JsonObject>> response = supabase.getOutfits().execute();
            if (response.isSuccessful() && response.body() != null) {
                for (JsonObject json : response.body()) {
                    Outfit outfit = convertJsonToOutfit(json);
                    outfits.add(outfit);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return outfits;
    }

    public List<Outfit> getOutfitsByCategory(String category) {
        List<Outfit> outfits = new ArrayList<>();
        try {
            Response<List<JsonObject>> response = supabase.getOutfitsByCategory(category).execute();
            if (response.isSuccessful() && response.body() != null) {
                for (JsonObject json : response.body()) {
                    Outfit outfit = convertJsonToOutfit(json);
                    outfits.add(outfit);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return outfits;
    }

    public List<Outfit> getOutfitsByGender(String gender) {
        List<Outfit> outfits = new ArrayList<>();
        try {
            Response<List<JsonObject>> response = supabase.getOutfitsByGender(gender).execute();
            if (response.isSuccessful() && response.body() != null) {
                for (JsonObject json : response.body()) {
                    Outfit outfit = convertJsonToOutfit(json);
                    outfits.add(outfit);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return outfits;
    }

    public List<Outfit> getFilteredOutfits(String category, String gender, String event, String season, String style) {
        List<Outfit> outfits = new ArrayList<>();
        try {
            Response<List<JsonObject>> response = supabase.getFilteredOutfits(category, gender, event, season, style).execute();
            if (response.isSuccessful() && response.body() != null) {
                for (JsonObject json : response.body()) {
                    Outfit outfit = convertJsonToOutfit(json);
                    outfits.add(outfit);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return outfits;
    }

    public boolean addOutfit(String imageUri, String name, String category,
                             String description, String gender, String event,
                             String season, String style) {
        try {
            JsonObject outfit = new JsonObject();
            outfit.addProperty("image_uri", imageUri);
            outfit.addProperty("name", name);
            outfit.addProperty("category", category);
            outfit.addProperty("description", description);
            outfit.addProperty("gender", gender);
            outfit.addProperty("event", event);
            outfit.addProperty("season", season);
            outfit.addProperty("style", style);

            Response<JsonObject> response = supabase.insertOutfit(outfit).execute();
            return response.isSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateOutfit(String outfitId, String name, String category,
                                String description, String gender, String event,
                                String season, String style) {
        try {
            JsonObject updates = new JsonObject();
            if (name != null) updates.addProperty("name", name);
            if (category != null) updates.addProperty("category", category);
            if (description != null) updates.addProperty("description", description);
            if (gender != null) updates.addProperty("gender", gender);
            if (event != null) updates.addProperty("event", event);
            if (season != null) updates.addProperty("season", season);
            if (style != null) updates.addProperty("style", style);

            Response<JsonObject> response = supabase.updateOutfit(outfitId, updates).execute();
            return response.isSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteOutfit(String outfitId) {
        try {
            Response<Void> response = supabase.deleteOutfit(outfitId).execute();
            return response.isSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private Outfit convertJsonToOutfit(JsonObject json) {
        Outfit outfit = new Outfit();

        // Map Supabase fields to Outfit properties
        if (json.has("id")) outfit.setId(json.get("id").getAsString());
        if (json.has("name")) outfit.setName(json.get("name").getAsString());
        if (json.has("image_uri")) outfit.setImageUri(json.get("image_uri").getAsString());
        if (json.has("category")) outfit.setCategory(json.get("category").getAsString());

        // Handle both 'description' and 'occasion' fields - they might be used interchangeably
        if (json.has("description")) {
            outfit.setDescription(json.get("description").getAsString());
        } else if (json.has("occasion")) {
            outfit.setDescription(json.get("occasion").getAsString());
        }

        if (json.has("gender")) outfit.setGender(json.get("gender").getAsString());

        // Handle both 'event' and 'occasion' fields
        if (json.has("event")) {
            outfit.setEvent(json.get("event").getAsString());
        } else if (json.has("occasion")) {
            outfit.setEvent(json.get("occasion").getAsString());
        }

        if (json.has("season")) outfit.setSeason(json.get("season").getAsString());
        if (json.has("style")) outfit.setStyle(json.get("style").getAsString());

        // Set default values if fields are missing
        if (outfit.getName() == null) outfit.setName("Unnamed Outfit");
        if (outfit.getCategory() == null) outfit.setCategory("General");
        if (outfit.getDescription() == null) outfit.setDescription("No description");
        if (outfit.getGender() == null) outfit.setGender("Unisex");
        if (outfit.getEvent() == null) outfit.setEvent("Casual");
        if (outfit.getSeason() == null) outfit.setSeason("All-Season");
        if (outfit.getStyle() == null) outfit.setStyle("Casual");

        return outfit;
    }
}