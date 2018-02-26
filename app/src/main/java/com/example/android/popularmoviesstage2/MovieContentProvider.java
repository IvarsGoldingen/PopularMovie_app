package com.example.android.popularmoviesstage2;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.example.android.popularmoviesstage2.database.MovieContract;
import com.example.android.popularmoviesstage2.database.MovieContract.*;
import com.example.android.popularmoviesstage2.database.MovieDbHelper;

/**
 * Content providers can help an application manage access to data stored by itself, stored by other
 * apps, and provide a way to share data with other apps.
 */

public class MovieContentProvider extends ContentProvider {

    private static final String TAG = "ContentProvider: ";

    //these constants together with the Uri  matcher will be used to show which data needs to be accessed
    //constants for the temporary DB
    private static final int POPULAR_MOVIES = 100;
    private static final int POPULAR_MOVIES_WITH_ID = 101;
    //constants for favorite movie DB
    private static final int FAVORITE_MOVIES = 200;
    private static final int FAVORITE_MOVIES_WITH_ID = 201;

    //The UriMatcher will be used throughout the content provider code to understand which parts of
    //the database need to be accessed, deleted or updated
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    //this variable will help the provider work with the data
    private MovieDbHelper movieDbHelper;

    private static UriMatcher buildUriMatcher() {
        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        //add all the Uris we want to recognise to the uriMatcher
        //Uri for all the popular movies
        //if the Uri will contain the authority and the path for the movies it will match it with the int value POPULAR_MOVIES
        uriMatcher.addURI(MovieContract.AUTHORITY, MovieContract.PATH_POPULAR_MOVIES, POPULAR_MOVIES);
        //Uri for a single popular movie
        //# is a wildcard symbol which can be any numerical value. It will be used to specify which movie from the db we want
        uriMatcher.addURI(MovieContract.AUTHORITY, MovieContract.PATH_POPULAR_MOVIES + "/#", POPULAR_MOVIES_WITH_ID);
        //Uri for all the favorite movies
        //if the Uri will contain the authority and the path for the favorite movies it will match it with the int value FAVORITE_MOVIES
        uriMatcher.addURI(MovieContract.AUTHORITY, MovieContract.PATH_FAVORITE_MOVIES, FAVORITE_MOVIES);
        //Uri for a single favorite movie
        //# is a wildcard symbol which can be any numerical value. It will be used to specify which movie from the db we want
        uriMatcher.addURI(MovieContract.AUTHORITY, MovieContract.PATH_FAVORITE_MOVIES + "/#", FAVORITE_MOVIES_WITH_ID);
        return uriMatcher;
    }

    /*
    * is called when the provide ris first initialized
    * The base data source should be initialized here, in this case we need to initialize a dbhelper
    * because we are using a database*/
    @Override
    public boolean onCreate() {
        Context context = getContext();
        movieDbHelper = new MovieDbHelper(context);
        return true;
    }

    /*
    * This method retruns a cursor with the data that is necessary
    * projection - columns that should be returned
    * selection, selection args - rows that should be returned
    * sortOrder - sort order
    * */
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        final SQLiteDatabase db = movieDbHelper.getReadableDatabase();
        //get the appropriate int value from the Uri matcher by passing the Uri to it.
        int match = sUriMatcher.match(uri);
        Cursor returnCursor;
        switch (match) {
            //when all movies will be returned for the main activity
            case POPULAR_MOVIES:
                returnCursor = db.query(
                        MovieContract.MovieEntry.TABLE_NAME,
                        projection,//columns
                        selection,//rows
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            //a single movie will be returned for the details activity
            case POPULAR_MOVIES_WITH_ID:
                //the uriMatcher has matched this case to a content://<authority>/movies/#
                //the # can be retrieved from the uri
                String id = uri.getPathSegments().get(1);//index 0 would be the movies part of the uri
                //create the selection so that the id is found in the _ID column
                String mSelection = MovieContract.MovieEntry._ID + "=?";
                String[] mSelectionArgs = new String[]{id};
                returnCursor = db.query(
                        MovieContract.MovieEntry.TABLE_NAME,
                        projection,//columns
                        mSelection,//rows
                        mSelectionArgs,
                        null,
                        null,
                        sortOrder
                );
                /*
                * this creates a sql statement something like this:
                * SELECT _id, title, rating, ..., FROM movies_table WHERE _id=?
                * where the ? is the id number from our query
                * */
                break;
            //when the movie list needs to be populated with the local favorite movies
            case FAVORITE_MOVIES:
                returnCursor = db.query(
                        MovieContract.FavoriteMovieEntry.TABLE_NAME,
                        projection,//columns
                        selection,//rows
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            //when a single favorite movie needs to be displayed
            case FAVORITE_MOVIES_WITH_ID:
                //the uriMatcher has matched this case to a content://<authority>/favorite_movies/#
                //the # can be retrieved from the uri
                String favId = uri.getPathSegments().get(1);//1 is the # part of the uri
                //create the selection so that the favId is found in the _ID column
                String mFavSelection = MovieContract.FavoriteMovieEntry._ID + "=?";
                String[] mFavSelectionArgs = new String[]{favId};
                returnCursor = db.query(
                        MovieContract.FavoriteMovieEntry.TABLE_NAME,
                        projection,
                        mFavSelection,
                        mFavSelectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        //Lets the cursor know what Uri it was created for.
        //It is needed so that when we change data with insert/update/delete methods and call notify();
        //this cursor will update itself
        returnCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return returnCursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    /*
    * This function will be called when a list of movies will be received from the internet
    * */
    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        final SQLiteDatabase db = movieDbHelper.getWritableDatabase();
        /*These are used when large amounts of data are being inserted in a db
        db.beginTransaction();
        db.setTransactionSuccessful();
        db.endTransaction();
        */
        //get the appropriate int value from the Uri matcher by passing the Uri to it.
        int match = sUriMatcher.match(uri);
        //complete the appropriate action on the db depending on the data that need to be accessed
        int valuesInserted = 0;
        switch (match) {
            case POPULAR_MOVIES:
                db.beginTransaction();
                try {
                    for (ContentValues singMovieContentValues : values) {
                        long _id = db.insert(MovieContract.MovieEntry.TABLE_NAME, null, singMovieContentValues);
                        if (_id != -1) {
                            valuesInserted++;
                        }
                    }
                    if (valuesInserted > 0) {
                        //Notify the cursorLoader in the main activity that data has changed
                        getContext().getContentResolver().notifyChange(uri, null);
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                break;
            default:
                return super.bulkInsert(uri, values);
        }
        return valuesInserted;
    }

    /*
    * This function is called to insert a single value in the db
    * This functionality is not needed for the popular movies DB, because several movies at a time are received.
    * It will be needed for the Favorite movies db
    * */
    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        final SQLiteDatabase db = movieDbHelper.getWritableDatabase();
        //get the appropriate int value from the Uri matcher by passing the Uri to it.
        int match = sUriMatcher.match(uri);
        //the uri which is returned to the caller
        Uri returnUri;
        //complete the appropriate action on the db depending on the data that need to be accessed
        switch (match) {
            case FAVORITE_MOVIES:
                //this will be called when a movie will be added to the db from the detail view
                long _id = db.insert(MovieContract.FavoriteMovieEntry.TABLE_NAME, null, contentValues);
                if (_id != -1){
                    //if the insertion was successful create the return uri
                    returnUri = ContentUris.withAppendedId(MovieContract.FavoriteMovieEntry.CONTENT_URI, _id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into: " + uri);
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        //Notify the resolver that a change has been made at this particular Uri
        //The resolver will update the DB and any associated UI
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] strings) {
        //this is used to delete the previous values in the temporary movie database
        final SQLiteDatabase db = movieDbHelper.getWritableDatabase();
        //get the appropriate int value from the Uri matcher by passing the Uri to it.
        int match = sUriMatcher.match(uri);
        int itemsDeleted = 0;
        switch (match) {
            case POPULAR_MOVIES:
                //We are deleting all movies so delete everything and do not add the where clause
                itemsDeleted = db.delete(MovieContract.MovieEntry.TABLE_NAME, null, null);
                break;
            case FAVORITE_MOVIES:
                //when deleting a movie from the favorites list
                itemsDeleted = db.delete(FavoriteMovieEntry.TABLE_NAME, s, strings);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);

        }
        if (itemsDeleted > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return itemsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
        return 0;

    }
}
