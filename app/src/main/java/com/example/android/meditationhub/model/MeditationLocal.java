package com.example.android.meditationhub.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;

@Entity(tableName = "meditations")
public class MeditationLocal {

    private static final String ID = "id";
    private static final String FILENAME = "filename";
    private static final String LOCATION = "location";
    private static final String SUBTITLE = "subtitle";
    private static final String TITLE = "title";
    private static final String STORAGE = "storage";

    @PrimaryKey
    @ColumnInfo(name = ID)
    private int id;
    @ColumnInfo(name = FILENAME)
    private String filename;
    @ColumnInfo(name = LOCATION)
    private String location;
    @ColumnInfo(name = SUBTITLE)
    private String subtitle;
    @ColumnInfo(name = TITLE)
    private String title;
    @ColumnInfo(name = STORAGE)
    private String storage;

    @Ignore //without ID
    public MeditationLocal() {

    }

    //with ID for Room
    public MeditationLocal(int id, String filename, String location, String subtitle, String title, String storage) {
        this.id = id;
        this.filename = filename;
        this.location = location;
        this.subtitle = subtitle;
        this.title = title;
        this.storage = storage;
    }

    public String getFilename() {
        return filename;
    }

    public MeditationLocal setFilename(String filename) {
        this.filename = filename;
        return this;
    }

    public String getLocation() {
        return location;
    }

    public MeditationLocal setLocation(String location) {
        this.location = location;
        return this;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public MeditationLocal setSubtitle(String subtitle) {
        this.subtitle = subtitle;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public MeditationLocal setTitle(String title) {
        this.title = title;
        return this;
    }

    public int getId() {
        return id;
    }

    public MeditationLocal setId(int id) {
        this.id = id;
        return this;
    }

    public String getStorage() {
        return storage;
    }

    public MeditationLocal setStorage(String storage) {
        this.storage = storage;
        return this;
    }
}
