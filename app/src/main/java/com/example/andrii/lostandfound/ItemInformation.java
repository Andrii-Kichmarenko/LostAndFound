package com.example.andrii.lostandfound;

import java.io.File;

public class ItemInformation {

    private String id, title, name, description, phoneNumber;
    private File imageFile;
    private double latitude;
    private double longitude;

    public ItemInformation() {
    }

    public ItemInformation(String id, String title, String name, String description, String phoneNumber, double latitude, double longitude) {
        this.id = id;
        this.title = title;
        this.name = name;
        this.description = description;
        this.phoneNumber = phoneNumber;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public File getImageFile() {
        return imageFile;
    }

    public void setImageFile(File file) {
        this.imageFile = file;
    }
}
