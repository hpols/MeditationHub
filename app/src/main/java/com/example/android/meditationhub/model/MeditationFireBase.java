package com.example.android.meditationhub.model;

public class MeditationFireBase {

    private String filename;
    private String subtitle;
    private String title;
    private String category;

    public MeditationFireBase() {
    }

    public MeditationFireBase(String filename, String subtitle, String title,
                              String category) {
        this.filename = filename;
        this.subtitle = subtitle;
        this.title = title;
        this.category = category;
    }

    public String getFilename() {
        return filename;
    }

    public MeditationFireBase setFilename(String filename) {
        this.filename = filename;
        return this;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public MeditationFireBase setSubtitle(String subtitle) {
        this.subtitle = subtitle;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public MeditationFireBase setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getCategory() {
        return category;
    }
}
