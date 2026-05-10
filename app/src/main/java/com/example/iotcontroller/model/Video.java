package com.example.iotcontroller.model;

import android.net.Uri;

public class Video {
    private long id;
    private Uri uri;
    private String name;

    public Video(long id, Uri uri, String name) {
        this.id = id;
        this.uri = uri;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
