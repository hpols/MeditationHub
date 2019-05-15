package com.example.android.meditationhub.model;

public class MeditationFireBase {

    private String filename;
    private String location;
    private String subtitle;
    private String title;

    public MeditationFireBase() {
    }

    public MeditationFireBase(String filename, String location, String subtitle, String title) {
        this.filename = filename;
        this.location = location;
        this.subtitle = subtitle;
        this.title = title;
    }

    public String getFilename() {
        return filename;
    }

    public MeditationFireBase setFilename(String filename) {
        this.filename = filename;
        return this;
    }

    public String getLocation() {
        return location;
    }

    public MeditationFireBase setLocation(String location) {
        this.location = location;
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
}
