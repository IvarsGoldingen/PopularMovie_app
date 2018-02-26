package com.example.android.popularmoviesstage2.ExtraDataObjects;
/**
 * A  video object created from json data
 */

public class Video {
    private String mName;
    private String mKey;

    public Video(String name, String key) {
        mName = name;
        mKey = key;
    }

    public String getName() {
        return mName;
    }

    public String getKey() {
        return mKey;
    }
}
