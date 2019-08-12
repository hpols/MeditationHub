package com.example.android.meditationhub.model;

public class Header implements ItemList {

    String name;

    public Header(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int getItemType() {
        return ItemList.TYPE_HEADER;
    }

    @Override
    public String toString() {
        return "Header{" +
                "name='" + name + '\'' +
                '}';
    }
}
