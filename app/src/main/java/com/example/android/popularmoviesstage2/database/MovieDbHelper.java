package com.example.android.popularmoviesstage2.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.android.popularmoviesstage2.database.MovieContract.MovieEntry;
import com.example.android.popularmoviesstage2.database.MovieContract.FavoriteMovieEntry;

/**
 * A class that helps with database functions
 */

public class MovieDbHelper extends SQLiteOpenHelper {
    //the name of the database. It will be a local file on Android
    private static final String DATABASE_NAME = "movie.db";
    //everytime the database schema is changed this should be incremented
    private static final int DATABASE_VERSION = 1;

    public MovieDbHelper(Context context) {
        //use the default constructor
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    //creates the database for the first time
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        //create a sql statement that creates a table
        //CREATE TABLE table_name (column_name column_type, ...);
        //table for popular movies
        final String SQL_CREATE_POPULAR_MOVIES_TABLE =
                "CREATE TABLE " +
                        MovieEntry.TABLE_NAME +
                        " (" +
                        //autoincrement means that these values will be generated automatically when new items are added to the table
                        MovieEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        MovieEntry.COLUMN_TITLE + " TEXT NOT NULL, " +
                        MovieEntry.COLUMN_OVERVIEW + " TEXT NOT NULL, " +
                        MovieEntry.COLUMN_POSTER_PATH + " TEXT NOT NULL, " +
                        MovieEntry.COLUMN_REALEASE_DATE + " TEXT NOT NULL, " +
                        MovieEntry.COLUMN_VOTE_AVARAGE + " REAL NOT NULL, " +
                        MovieEntry.COLUMN_MOVIE_ID + " INTEGER NOT NULL" +
                        ");";
        //execute the SQL statement that was just created
        sqLiteDatabase.execSQL(SQL_CREATE_POPULAR_MOVIES_TABLE);

        //table for favorite movies
        final String SQL_CREATE_FAVORITE_MOVIES_TABLE =
                "CREATE TABLE " +
                        FavoriteMovieEntry.TABLE_NAME +
                        " (" +
                        //autoincrement means that these values will be generated automatically when new items are added to the table
                        FavoriteMovieEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        FavoriteMovieEntry.COLUMN_TITLE + " TEXT NOT NULL, " +
                        FavoriteMovieEntry.COLUMN_OVERVIEW + " TEXT NOT NULL, " +
                        FavoriteMovieEntry.COLUMN_POSTER_PATH + " TEXT NOT NULL, " +
                        FavoriteMovieEntry.COLUMN_REALEASE_DATE + " TEXT NOT NULL, " +
                        FavoriteMovieEntry.COLUMN_VOTE_AVARAGE + " REAL NOT NULL, " +
                        FavoriteMovieEntry.COLUMN_MOVIE_ID + " INTEGER NOT NULL" +
                        ");";
        sqLiteDatabase.execSQL(SQL_CREATE_FAVORITE_MOVIES_TABLE);
    }

    //updates the database schema when needed
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        //this is called only when the database version number is larger than the on stored on the device
        //DROP TABLE removes the table specified
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + MovieEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + FavoriteMovieEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
        //a better way for this would be to modify the existing table and not removing the old one
    }
}
