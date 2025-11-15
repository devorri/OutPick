package com.example.outpick.database.models;

public class OutfitHistoryItem {

    private int id;
    private String outfitName;
    private String category;
    private String dateUsed;
    private String actionTaken;
    private String imagePath; // NEW: path to outfit image
    private String event;     // NEW: event associated with outfit
    private String season;    // NEW: season associated with outfit
    private String gender;    // NEW: gender associated with outfit

    // Default constructor
    public OutfitHistoryItem() {
    }

    // Full constructor
    public OutfitHistoryItem(int id, String outfitName, String category, String dateUsed,
                             String actionTaken, String imagePath, String event,
                             String season, String gender) {
        this.id = id;
        this.outfitName = outfitName;
        this.category = category;
        this.dateUsed = dateUsed;
        this.actionTaken = actionTaken;
        this.imagePath = imagePath;
        this.event = event;
        this.season = season;
        this.gender = gender;
    }

    // ---------------- Getters & Setters ----------------

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getOutfitName() {
        return outfitName;
    }

    public void setOutfitName(String outfitName) {
        this.outfitName = outfitName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDateUsed() {
        return dateUsed;
    }

    public void setDateUsed(String dateUsed) {
        this.dateUsed = dateUsed;
    }

    public String getActionTaken() {
        return actionTaken;
    }

    public void setActionTaken(String actionTaken) {
        this.actionTaken = actionTaken;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
}
