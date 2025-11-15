package com.example.outpick.database.models;

public class OutfitCombination {
    private String title;
    private int topImageResId;
    private int bottomImageResId;

    public OutfitCombination(String title, int topImageResId, int bottomImageResId) {
        this.title = title;
        this.topImageResId = topImageResId;
        this.bottomImageResId = bottomImageResId;
    }

    public String getTitle() {
        return title;
    }

    public int getTopImageResId() {
        return topImageResId;
    }

    public int getBottomImageResId() {
        return bottomImageResId;
    }
}
