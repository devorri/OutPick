package com.example.outpick.database.models;

public class Outfit {

    // --- Core fields ---
    private String id; // CHANGED FROM int TO String for Supabase UUID
    private String imageUri; // Should contain cloud URLs like "https://xyz.supabase.co/..."
    private String name;        // general name
    private String outfitName;  // for SnapshotDetailsActivity
    private String category;
    private String description;
    private String gender;
    private boolean isFavorite;

    // --- Additional metadata (CRITICAL FIELDS) ---
    private String event;
    private String season; // NEW FIELD
    private String style;  // NEW FIELD

    // --- Optional fields ---
    private byte[] snapshot;
    private String categories;
    private byte[] imageBytes;

    // --- New field for snapshot path ---
    private String path;

    // ------------------- METADATA INITIALIZATION -------------------
    private void initializeMetadata(String event, String season, String style) {
        this.event = (event != null && !event.isEmpty()) ? event : "Casual";
        this.season = (season != null && !season.isEmpty()) ? season : "All-Season";
        this.style = (style != null && !style.isEmpty()) ? style : "Casual";
    }

    // ------------------- CONSTRUCTORS -------------------

    // Default constructor
    public Outfit() {
        this.id = "";
        this.name = "Unnamed Outfit";
        this.outfitName = "Unnamed Outfit";
        this.category = "General";
        this.description = "";
        this.gender = "Unisex";
        this.isFavorite = false;
        initializeMetadata("Casual", "All-Season", "Casual");
    }

    // ✅ IMPROVED: Constructor specifically for cloud data
    public Outfit(String id, String cloudImageUrl, String name, String category,
                  String description, String gender, String event,
                  String season, String style) {
        this.id = id;
        this.imageUri = cloudImageUrl; // Explicitly named for clarity
        this.name = name;
        this.outfitName = name;
        this.category = category;
        this.description = description;
        this.gender = gender;
        this.isFavorite = false;
        initializeMetadata(event, season, style);
    }

    // Constructor with String ID for Supabase
    public Outfit(String id, String imageUri, String name, String category,
                  String description, String gender, boolean isFavorite) {
        this.id = id;
        this.imageUri = imageUri;
        this.name = name;
        this.outfitName = name;
        this.category = category;
        this.description = description;
        this.gender = gender;
        this.isFavorite = isFavorite;
        initializeMetadata("", "", "");
    }

    // Constructor with String ID and event
    public Outfit(String id, String imageUri, String name, String category,
                  String description, String gender, boolean isFavorite,
                  String event) {
        this(id, imageUri, name, category, description, gender, isFavorite);
        initializeMetadata(event, "", "");
    }

    // PRIMARY CONSTRUCTOR FOR SUPABASE WITH SEASON & STYLE
    public Outfit(String id, String imageUri, String name, String category,
                  String description, String gender, boolean isFavorite,
                  String event, String season, String style) {
        this(id, imageUri, name, category, description, gender, isFavorite);
        initializeMetadata(event, season, style);
    }

    // Constructor with int ID (for backward compatibility)
    public Outfit(int id, String imageUri, String name, String category,
                  String description, String gender, boolean isFavorite) {
        this.id = String.valueOf(id);
        this.imageUri = imageUri;
        this.name = name;
        this.outfitName = name;
        this.category = category;
        this.description = description;
        this.gender = gender;
        this.isFavorite = isFavorite;
        initializeMetadata("", "", "");
    }

    // Constructor with int ID and event (for backward compatibility)
    public Outfit(int id, String imageUri, String name, String category,
                  String description, String gender, boolean isFavorite,
                  String event) {
        this(id, imageUri, name, category, description, gender, isFavorite);
        initializeMetadata(event, "", "");
    }

    // Constructor with int ID, season & style (for backward compatibility)
    public Outfit(int id, String imageUri, String name, String category,
                  String description, String gender, boolean isFavorite,
                  String event, String season, String style) {
        this(id, imageUri, name, category, description, gender, isFavorite);
        initializeMetadata(event, season, style);
    }

    public Outfit(int id, String imageUri, String name, String category,
                  String description, String gender, boolean isFavorite,
                  byte[] snapshot, String categories) {
        this.id = String.valueOf(id);
        this.imageUri = imageUri;
        this.name = name;
        this.outfitName = name;
        this.category = category;
        this.description = description;
        this.gender = gender;
        this.isFavorite = isFavorite;
        this.snapshot = snapshot;
        this.categories = categories;
        initializeMetadata("", "", "");
    }

    public Outfit(int id, String imageUri, String name, String category,
                  String description, String gender, boolean isFavorite,
                  byte[] imageBytes, String event, boolean useImageBytes) {
        this.id = String.valueOf(id);
        this.imageUri = imageUri;
        this.name = name;
        this.outfitName = name;
        this.category = category;
        this.description = description;
        this.gender = gender;
        this.isFavorite = isFavorite;
        this.imageBytes = imageBytes;
        initializeMetadata(event, "", "");
    }

    public Outfit(int id, String path, String outfitName, String event, String season, String style) {
        this.id = String.valueOf(id);
        this.path = path;
        this.outfitName = outfitName;
        this.name = outfitName;
        initializeMetadata(event, season, style);
        this.category = "";
        this.description = "";
        this.gender = "";
        this.isFavorite = false;
    }

    public Outfit(String imageUri, String name, String category, String description,
                  String gender, byte[] snapshot, String categories) {
        this.id = "";
        this.imageUri = imageUri;
        this.name = name;
        this.outfitName = name;
        this.category = category;
        this.description = description;
        this.gender = gender;
        this.isFavorite = false;
        this.snapshot = snapshot;
        this.categories = categories;
        initializeMetadata("", "", "");
    }

    public Outfit(String imageUri, String outfitName, String category, String description,
                  String gender, byte[] snapshot, String event, String season, String style) {
        this.id = "";
        this.imageUri = imageUri;
        this.name = outfitName;
        this.outfitName = outfitName;
        this.category = category;
        this.description = description;
        this.gender = gender;
        this.isFavorite = false;
        this.snapshot = snapshot;
        initializeMetadata(event, season, style);
    }

    public Outfit(String imageUri, String name, String category, String description) {
        this.id = "";
        this.imageUri = imageUri;
        this.name = name;
        this.outfitName = name;
        this.category = category;
        this.description = description;
        this.gender = "";
        this.isFavorite = false;
        initializeMetadata("", "", "");
    }

    public Outfit(String imageUri, String name, String category, String description, String gender) {
        this(imageUri, name, category, description);
        this.gender = gender;
        this.outfitName = name;
    }

    // ------------------- GETTERS -------------------
    public String getId() { return id; }
    public String getImageUri() { return imageUri; }
    public String getName() { return name; }
    public String getOutfitName() { return outfitName != null ? outfitName : name; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getGender() { return gender; }
    public boolean isFavorite() { return isFavorite; }
    public String getEvent() { return event; }
    public String getSeason() { return season; }
    public String getStyle() { return style; }
    public byte[] getSnapshot() { return snapshot; }
    public String getCategories() { return categories; }
    public byte[] getImageBytes() { return imageBytes; }
    public String getPath() { return path; }

    // ------------------- SETTERS -------------------
    public void setId(String id) { this.id = id; }
    public void setId(int id) { this.id = String.valueOf(id); } // Overloaded for backward compatibility
    public void setImageUri(String imageUri) { this.imageUri = imageUri; }
    public void setName(String name) {
        this.name = name;
        this.outfitName = name;
    }
    public void setOutfitName(String outfitName) {
        this.outfitName = outfitName;
        this.name = outfitName;
    }
    public void setCategory(String category) { this.category = category; }
    public void setDescription(String description) { this.description = description; }
    public void setGender(String gender) { this.gender = gender; }
    public void setFavorite(boolean favorite) { this.isFavorite = favorite; }
    public void setEvent(String event) { this.event = event; }
    public void setSeason(String season) { this.season = season; }
    public void setStyle(String style) { this.style = style; }
    public void setSnapshot(byte[] snapshot) { this.snapshot = snapshot; }
    public void setCategories(String categories) { this.categories = categories; }
    public void setImageBytes(byte[] imageBytes) { this.imageBytes = imageBytes; }
    public void setPath(String path) { this.path = path; }

    // ✅ ADDED: Helper method to check if image is from cloud
    public boolean isCloudImage() {
        return imageUri != null && imageUri.startsWith("http");
    }

    // ✅ ADDED: Helper method to check if image is local file
    public boolean isLocalImage() {
        return imageUri != null && (imageUri.startsWith("file://") || imageUri.startsWith("/"));
    }

    @Override
    public String toString() {
        return getOutfitName() + " (" + category + ", " + gender + ", " + event + ", " + season + ", " + style + ")";
    }
}