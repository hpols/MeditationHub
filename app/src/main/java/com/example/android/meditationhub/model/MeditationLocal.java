package com.example.android.meditationhub.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

@Entity(tableName = "meditations")
public class MeditationLocal implements Parcelable {

    private static final String ID = "id";
    private static final String FILENAME = "filename";
    private static final String LOCATION = "location";
    private static final String SUBTITLE = "subtitle";
    private static final String TITLE = "title";
    private static final String STORAGE = "storage";

    @NonNull
    @PrimaryKey
    @ColumnInfo(name = ID)
    private String id;
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
    MeditationLocal(String id, String filename, String location, String subtitle, String title, String storage) {
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

    @Override
    public String toString() {
        return "MeditationLocal{" +
                "id='" + id + '\'' +
                ", filename='" + filename + '\'' +
                ", location='" + location + '\'' +
                ", subtitle='" + subtitle + '\'' +
                ", title='" + title + '\'' +
                ", storage='" + storage + '\'' +
                '}';
    }

    /** Parcable Functionality **/

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
        location = in.readString();
        subtitle = in.readString();
        title = in.readString();
        storage = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(filename);
        dest.writeString(location);
        dest.writeString(title);
        dest.writeString(subtitle);
        dest.writeString(storage);

    }
}
