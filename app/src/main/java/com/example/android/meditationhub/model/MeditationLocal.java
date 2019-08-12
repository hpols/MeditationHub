package com.example.android.meditationhub.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

@Entity(tableName = "meditations")
public class MeditationLocal implements Parcelable, ItemList, Comparable<MeditationLocal> {

    private static final String ID = "id";
    private static final String FILENAME = "filename";
    private static final String SUBTITLE = "subtitle";
    private static final String TITLE = "title";
    private static final String STORAGE = "storage";
    private static final String CATEGORY = "category";

    @NonNull
    @PrimaryKey
    @ColumnInfo(name = ID)
    private String id;
    @ColumnInfo(name = FILENAME)
    private String filename;
    @ColumnInfo(name = SUBTITLE)
    private String subtitle;
    @ColumnInfo(name = TITLE)
    private String title;
    @ColumnInfo(name = STORAGE)
    private String storage;

    @ColumnInfo(name = CATEGORY)
    private String category;

    @Ignore //without ID
    public MeditationLocal() {

    }

    //with ID for Room
    MeditationLocal(String id, String filename, String subtitle, String title,
                    String storage, String category) {
        this.id = id;
        this.filename = filename;
        this.subtitle = subtitle;
        this.title = title;
        this.storage = storage;
        this.category = category;
    }

    public String getFilename() {
        return filename;
    }

    public MeditationLocal setFilename(String filename) {
        this.filename = filename;
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

    public String getId() {
        return id;
    }

    public MeditationLocal setId(String id) {
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @NonNull
    @Override
    public String toString() {
        return "MeditationLocal{" +
                "id='" + id + '\'' +
                ", filename='" + filename + '\'' +
                ", subtitle='" + subtitle + '\'' +
                ", title='" + title + '\'' +
                ", storage='" + storage + '\'' +
                ", category ='" + category + '\'' +
                '}';
    }

    /**
     * Parcable Functionality
     **/

    public static final Creator<MeditationLocal> CREATOR = new Creator<MeditationLocal>() {
        @Override
        public MeditationLocal createFromParcel(Parcel in) {
            return new MeditationLocal(in);
        }

        @Override
        public MeditationLocal[] newArray(int size) {
            return new MeditationLocal[size];
        }
    };


    private MeditationLocal(Parcel in) {
        id = in.readString();
        filename = in.readString();
        subtitle = in.readString();
        title = in.readString();
        storage = in.readString();
        category = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(filename);
        dest.writeString(title);
        dest.writeString(subtitle);
        dest.writeString(storage);
        dest.writeString(category);

    }

    @Override
    public int getItemType() {
        return ItemList.TYPE_ITEM;
    }

    @Override
    public int compareTo(MeditationLocal o) {
        if (getCategory() == null || o.getCategory() == null) {
            return 0;
        }
        return getCategory().compareTo(o.getCategory());
    }
}
