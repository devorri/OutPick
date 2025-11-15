package com.example.outpick.database.models;

public class ClosetContentItem {

    public enum ItemType {
        CLOTHING,
        SNAPSHOT
    }

    private ItemType type;
    private String clothingId;
    private String imageUri;
    private String snapshotPath;
    private String name;
    private String category;

    // ✅ ADD THESE FIELDS to fix the errors:
    private String season;
    private String style;

    // === Constructors (keep all existing ones) ===
    public ClosetContentItem() {
    }

    public ClosetContentItem(String clothingId, String imageUri) {
        this.type = ItemType.CLOTHING;
        this.clothingId = clothingId;
        this.imageUri = imageUri;
    }

    public ClosetContentItem(String clothingId, String imageUri, String name, String category) {
        this.type = ItemType.CLOTHING;
        this.clothingId = clothingId;
        this.imageUri = imageUri;
        this.name = name;
        this.category = category;
    }

    public ClosetContentItem(String snapshotPath) {
        this.type = ItemType.SNAPSHOT;
        this.snapshotPath = snapshotPath;
    }

    // ✅ ADD THIS CONSTRUCTOR for snapshots with season and style
    public ClosetContentItem(String snapshotPath, String name, String category, String season, String style) {
        this.type = ItemType.SNAPSHOT;
        this.snapshotPath = snapshotPath;
        this.name = name;
        this.category = category;
        this.season = season;
        this.style = style;
    }

    // === Getters ===
    public ItemType getType() {
        return type;
    }

    public String getClothingId() {
        return clothingId;
    }

    public String getImageUri() {
        return imageUri;
    }

    public String getSnapshotPath() {
        return snapshotPath;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    // ✅ ADD THESE GETTERS:
    public String getSeason() {
        return season;
    }

    public String getStyle() {
        return style;
    }

    // === Setters ===
    public void setType(ItemType type) {
        this.type = type;
    }

    public void setClothingId(String clothingId) {
        this.clothingId = clothingId;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

    public void setSnapshotPath(String snapshotPath) {
        this.snapshotPath = snapshotPath;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    // ✅ ADD THESE SETTERS:
    public void setSeason(String season) {
        this.season = season;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    // ... keep the rest of your existing methods (equals, hashCode, toString)
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ClosetContentItem that = (ClosetContentItem) obj;

        if (type != that.type) return false;

        if (type == ItemType.CLOTHING) {
            return clothingId != null ? clothingId.equals(that.clothingId) : that.clothingId == null;
        } else if (type == ItemType.SNAPSHOT) {
            return snapshotPath != null ? snapshotPath.equals(that.snapshotPath) : that.snapshotPath == null;
        }

        return false;
    }

    @Override
    public int hashCode() {
        if (type == ItemType.CLOTHING) {
            return clothingId != null ? clothingId.hashCode() : 0;
        } else if (type == ItemType.SNAPSHOT) {
            return snapshotPath != null ? snapshotPath.hashCode() : 0;
        }
        return 0;
    }

    @Override
    public String toString() {
        return "ClosetContentItem{" +
                "type=" + type +
                ", clothingId='" + clothingId + '\'' +
                ", imageUri='" + imageUri + '\'' +
                ", snapshotPath='" + snapshotPath + '\'' +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", season='" + season + '\'' +
                ", style='" + style + '\'' +
                '}';
    }
}