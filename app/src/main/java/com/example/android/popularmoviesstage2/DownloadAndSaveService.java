package com.example.android.popularmoviesstage2;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v7.preference.PreferenceManager;

import com.example.android.popularmoviesstage2.database.MovieContract;
import com.example.android.popularmoviesstage2.utilities.NetworkUtilities;

import java.io.IOException;
import java.net.URL;

/**
 * This service will be used to download and save Movie info in the DB through an intent provider
 * A service is good for running long task in the background which don't have visual components.
 * Downloading and saving data is that case..
 * A service does not have the same lifecycle as an activity, it continues to run when the activity
 * is destroyed
 * Intent service runs on a separate background thread
 * All intentService instances are run on the same thread and issued in order
 * IntentService stops itself when it runs out of work.
 */

public class DownloadAndSaveService extends IntentService {

    public static final String MESSENGER_KEY = "MESSENGER";
    private static final String TAG = "Service: ";
    //Handler message constants
    private static final int MESSAGE_SUCCESS = 1;
    private static final int MESSAGE_FAIL = 2;

    //object ot communicate to the main activity
    private Messenger messageHandler;

    public DownloadAndSaveService() {
        super("DownloadAndSaveService");
    }

    //This is what will run in the background
    //similar to loadInBackground() method from asyncTask
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        //create the messenger object to send info to the main activity
        Bundle extras = intent.getExtras();
        messageHandler = (Messenger) extras.get(MESSENGER_KEY);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String sortOrderPrefValue = sharedPreferences.getString(getString(R.string.pref_sort_key),
                getString(R.string.pref_by_popularity_value));
        int pageNumber = sharedPreferences.getInt(getString(R.string.pref_page_to_open), 1);
        String queryType;
        ContentValues[] moviesValues;

        if (sortOrderPrefValue.equals(getString(R.string.pref_by_popularity_value))) {
            //if sort by popularity
            queryType = NetworkUtilities.QUERY_TYPE_POPULAR;
        } else {
            //if sort by highest rated
            queryType = NetworkUtilities.QUERY_TYPE_TOP_RATED;
        }

        URL queryUrl = NetworkUtilities.buildPopularMovieUrl(queryType, pageNumber);
        if (queryUrl == null) {
            //if there received string is empty just return
            sendMessage(MESSAGE_FAIL);
            return;
        }

        try {
            String jsonDataString = NetworkUtilities.getResponseFromHttpUrl(queryUrl);
            moviesValues = NetworkUtilities.getMovieContentValuesFromJson(jsonDataString);
        } catch (IOException e) {
            e.printStackTrace();
            sendMessage(MESSAGE_FAIL);
            return;
        }
        if (moviesValues != null && moviesValues.length > 0) {
            //Delete the previous list of popular movies before inserting new ones
            getContentResolver().delete(MovieContract.MovieEntry.CONTENT_URI, null, null);
            //insert all the values in the db through the ContentProvider
            getContentResolver().bulkInsert(MovieContract.MovieEntry.CONTENT_URI, moviesValues);
            sendMessage(MESSAGE_SUCCESS);
        } else {
            sendMessage(MESSAGE_FAIL);
        }
    }

    /*
    *A method to create messages and send to the MainActivity
    */
    private void sendMessage(int handlerMessage) {
        /*
        * Defines a message containing a description and arbitrary data object that
        * can be sent to a Handler. This object contains two extra int fields and an
         * extra object field that allow you to not do allocations in many cases.
        * */
        Message message = Message.obtain();
        message.arg1 = handlerMessage;
        try {
            messageHandler.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
