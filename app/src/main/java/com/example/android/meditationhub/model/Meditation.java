package com.example.android.meditationhub.model;

public class Meditation {

    private String filename;
    private String location;
    private String subtitle;
    private String title;

    public Meditation() {
    }

    public Meditation(String filename, String location, String subtitle, String title) {
        this.filename = filename;
        this.location = location;
        this.subtitle = subtitle;
        this.title = title;
    }

    public String getFilename() {
        return filename;
    }

    public Meditation setFilename(String filename) {
        this.filename = filename;
        return this;
    }

    public String getLocation() {
        return location;
    }

    public Meditation setLocation(String location) {
        this.location = location;
        return this;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public Meditation setSubtitle(String subtitle) {
        this.subtitle = subtitle;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public Meditation setTitle(String title) {
        this.title = title;
        return this;
    }
}
