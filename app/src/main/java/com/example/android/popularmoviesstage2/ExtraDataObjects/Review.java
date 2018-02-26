package com.example.android.popularmoviesstage2.ExtraDataObjects;

import java.net.URL;

/**
 * A  review object created from json data
 */

public class Review {
    private String mAuthor;
    private String mReview;
    private URL mReviewUrl;

    public Review(String author, String review, URL reviewUrl) {
        mAuthor = author;
        mReview = review;
        mReviewUrl = reviewUrl;
    }

    public String getAuthor() {
        return mAuthor;
    }

    public String getReview() {
        return mReview;
    }

    public URL getReviewUrl(){
        return mReviewUrl;
    }
}
