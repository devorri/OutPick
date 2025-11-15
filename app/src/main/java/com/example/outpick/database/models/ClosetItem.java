package com.example.outpick.database.models;

public class ClosetItem {

    private String id;             // NEW: Supabase UUID
    private String name;           // Closet name
    private String description;    // Optional subtext/description
    private String coverImageUri;  // Image URI or resource URI
    private String type;           // "outfit_card", "create_card", or ""

    // Main constructor (with type)
    public ClosetItem(String name, String description, String coverImageUri, String type) {
        this.id = ""; // Default empty ID
        this.name = name != null ? name : "";
        this.description = description != null ? description : "";
        this.coverImageUri = coverImageUri != null ? coverImageUri : "";
        this.type = type != null ? type : "";
    }

    // Constructor with ID for Supabase
    public ClosetItem(String id, String name, String description, String coverImageUri, String type) {
        this.id = id != null ? id : "";
        this.name = name != null ? name : "";
        this.description = description != null ? description : "";
        this.coverImageUri = coverImageUri != null ? coverImageUri : "";
        this.type = type != null ? type : "";
    }

    // Optional fallback constructor (no type)
    public ClosetItem(String name, String description, String coverImageUri) {
        this("", name, description, coverImageUri, "");
    }

    // ===== Getters =====
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCoverImageUri() {
        return coverImageUri;
    }

    public String getType() {
        return type;
    }

    // ===== Setters =====
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCoverImageUri(String coverImageUri) {
        this.coverImageUri = coverImageUri;
    }

    public void setType(String type) {
        this.type = type;
    }
}