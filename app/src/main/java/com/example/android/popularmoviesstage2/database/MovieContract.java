package com.example.android.popularmoviesstage2.database;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * The contract class defines how the database will look. All the tables and columns of the database
 */

public class MovieContract {
    //all data related to the contract it self goes in the MovieContract class itself
    //and an inner class for each table should be made

    //This is the part of the URI which dirrects to the correct content provider
    //it is the same as in the manifest
    public static final String AUTHORITY = "com.example.android.popularmoviesstage2";
    //A string that will be used to access the popular_movies table
    public static final String PATH_POPULAR_MOVIES = "popular_movies";
    //A string that will be used to access the favorite_movies table
    public static final String PATH_FAVORITE_MOVIES = "favorite_movies";
    //this is the start of the Uri which will be used to access this app's provider
    //"content://" is how Uris start in Android
    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    private MovieContract() {
        //a private constructor so a object can NOT be made
    }

    /*
    * An inner class that describes the table used for saving movies downloaded from the web.
    * Is used to reduce number of network calls and to save a page while not connected to internet
    * */
    public static class MovieEntry implements BaseColumns {
        //the uri which must be used to get access to this table
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_POPULAR_MOVIES).build();

        public static final String TABLE_NAME = "popular_movies";
        //this will define all the columns that are needed
        //baseColumns already implements the _ID column so that does not need to be written
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_POSTER_PATH = "poster_path";
        public static final String COLUMN_OVERVIEW = "overview";
        public static final String COLUMN_VOTE_AVARAGE = "vote_average";
        public static final String COLUMN_REALEASE_DATE = "release_date";
        public static final String COLUMN_MOVIE_ID = "movie_id";
    }
    public static class FavoriteMovieEntry implements BaseColumns {
        //the uri which must be used to get access to this table
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_FAVORITE_MOVIES).build();

        public static final String TABLE_NAME = "favorite_movies";
        //this will define all the columns that are needed
        //baseColumns already implements the _ID column so that does not need to be written
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_POSTER_PATH = "poster_path";
        public static final String COLUMN_OVERVIEW = "overview";
        public static final String COLUMN_VOTE_AVARAGE = "vote_average";
        public static final String COLUMN_REALEASE_DATE = "release_date";
        public static final String COLUMN_MOVIE_ID = "movie_id";
    }
}