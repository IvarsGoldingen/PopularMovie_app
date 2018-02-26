package com.example.android.popularmoviesstage2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.popularmoviesstage2.database.MovieContract;
import com.example.android.popularmoviesstage2.utilities.NetworkUtilities;

import java.lang.ref.WeakReference;

/*
* implements on shared preferences change listener so that settings changes take effect immidiately
* */
public class MainActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks,
        SharedPreferences.OnSharedPreferenceChangeListener {

    //Time constants
    private static final int SECOND_IN_MILLS = 1000;
    private static final int MINUTE_IN_MILLS = SECOND_IN_MILLS * 60;
    private static final int HOUR_IN_MILLS = MINUTE_IN_MILLS * 60;
    private static final int DAY_IN_MILLS = HOUR_IN_MILLS * 24;
    //Handler message constants
    private static final int MESSAGE_SUCCESS = 1;
    private static final int MESSAGE_FAIL = 2;
    private static final String TAG = "Main activity: ";
    //loader id for quering the db through the content resolver
    private static final int CONTENT_LOADER_ID = 2;
    private static final int MAX_PAGE_NUMBER = 100;
    //Handler to communicate with the service
    private static Handler messageHandler;
    private MovieAdapter movieAdapter;

    private TextView noMoviesAvailableTextView;
    private View loadingIndicator;
    private ImageButton previousPageButton;
    private ImageButton nextPageButton;
    private TextView currentPageText;
    private GridView movieGridView;

    //a mToast object used to stop toasts of accumulating
    private Toast mToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //initialize the message handler used for communicating with the service
        messageHandler = new MessageHandler(this);

        noMoviesAvailableTextView = findViewById(R.id.no_movies_available_text);
        loadingIndicator = findViewById(R.id.loading_indicator);
        previousPageButton = findViewById(R.id.button_previous);
        nextPageButton = findViewById(R.id.button_next);
        currentPageText = findViewById(R.id.current_page_number_textview);

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        currentPageText.setText(String.valueOf(sharedPreferences.getInt(getString(R.string.pref_current_page), 1)));
        setupSharedPreferences();

        movieGridView = findViewById(R.id.movie_grid);
        movieAdapter = new MovieAdapter(this, null);
        movieGridView.setAdapter(movieAdapter);
        movieGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Cursor movieCursor = (Cursor) movieAdapter.getItem(position);
                int movieIdColumn = movieCursor.getColumnIndex(MovieContract.MovieEntry._ID);
                int movieId = movieCursor.getInt(movieIdColumn);
                //get the movie list we are currently looking at
                String movieList = sharedPreferences.getString(
                        getString(R.string.pref_sort_key), getString(R.string.pref_by_popularity_value));
                Uri selectedMovieUri = null;
                if (movieList.equals(getString(R.string.pref_by_favorites_value))){
                    //if the favorites movie list has to queried
                    selectedMovieUri = MovieContract.FavoriteMovieEntry.CONTENT_URI.buildUpon()
                            .appendPath(String.valueOf(movieId))
                            .build();
                }else {
                    //if the popular or best rated temporary table needs to be queried
                    selectedMovieUri = MovieContract.MovieEntry.CONTENT_URI.buildUpon()
                            .appendPath(String.valueOf(movieId))
                            .build();
                }
                Intent detailActivityIntent = new Intent(MainActivity.this, DetailMovieActivity.class);
                detailActivityIntent.setData(selectedMovieUri);
                startActivity(detailActivityIntent);
            }
        });

        previousPageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                //Get the list name currently open
                String movieList = sharedPreferences.getString(
                        getString(R.string.pref_sort_key),
                        getString(R.string.pref_by_popularity_value));
                if (!movieList.equals(getString(R.string.pref_by_favorites_value))){
                    //if popular or most rated movies are open allow switching pages
                    //check which page is open currently
                    int currentPage = sharedPreferences.getInt(getString(R.string.pref_current_page), 1);
                    //save the next page which should be open
                    int pageToOpen = currentPage - 1;
                    if (pageToOpen > 0) {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt(getString(R.string.pref_page_to_open), pageToOpen);
                        //must be called for the preferences to be saved
                        editor.apply();
                        //make the download request from API with the next page
                        getMoviesFromApi();
                    } else {
                        showToast("First page already displayed");
                    }
                } else {
                    showToast("Page browsing not allowed in favorites");
                }

            }
        });

        nextPageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                String movieList = sharedPreferences.getString(
                        getString(R.string.pref_sort_key),
                        getString(R.string.pref_by_popularity_value));
                if (!movieList.equals(getString(R.string.pref_by_favorites_value))) {
                    //check which page is open currently
                    int currentPage = sharedPreferences.getInt(getString(R.string.pref_current_page), 1);
                    //save the next page which should be open
                    int pageToOpen = currentPage + 1;
                    if (pageToOpen < MAX_PAGE_NUMBER) {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt(getString(R.string.pref_page_to_open), pageToOpen);
                        //must be called for the preferences to be saved
                        editor.apply();
                        //make the download request from API with the next page
                        getMoviesFromApi();

                    } else {
                        showToast("Max page allowed: " + (MAX_PAGE_NUMBER - 1));
                    }
                } else {
                    showToast("Page browsing not allowed in favorites");
                }
            }
        });

        initializeData();
    }

    //Method to show toast and not let them be accumulated
    private void showToast(String message) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        mToast.show();
    }

    /*
    * If database has values and refresh time has not yet passed, just get the old movie values
    * from the database
    * Else get the movies from the API
    * */
    private void initializeData() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        //check if there are values in the database, this value is updated after inserting them in the db
        boolean dbHasValues = sharedPreferences.getBoolean(getString(R.string.pref_has_data_key),
                false);
        //get the previous time when the data was downloaded
        long lastDownloadTime = sharedPreferences.getLong(getString(R.string.pref_update_time_key),
                0);
        //Get refresh rate from shared preferences which ca be changed in settings
        String refreshRateString = sharedPreferences.getString(getString(R.string.pref_refresh_rate_key),
                getString(R.string.pref_refresh_value_1day));
        long refreshTime = 0;
        if (refreshRateString.equals(getString(R.string.pref_refresh_value_always))) {
            refreshTime = 0;
        } else if (refreshRateString.equals(getString(R.string.pref_refresh_value_1hour))) {
            refreshTime = HOUR_IN_MILLS;
        } else if (refreshRateString.equals(getString(R.string.pref_refresh_label_12hours))) {
            refreshTime = HOUR_IN_MILLS * 12;
        } else if (refreshRateString.equals(getString(R.string.pref_refresh_value_1day))) {
            refreshTime = DAY_IN_MILLS;
        } else if (refreshRateString.equals(getString(R.string.pref_refresh_value_1week))) {
            refreshTime = DAY_IN_MILLS * 7;
        }

        //A loaderManager is needed to manage loaders
        LoaderManager loaderManager = getSupportLoaderManager();
        //The system automatically determines if the loader wuth the id exists. If yes it reuses the
        //old one elese creates a new one
        //Since this activity implements Loader callbacks we can pass this as the 3rd argument
        loaderManager.restartLoader(CONTENT_LOADER_ID, null, this);

        boolean timeToUpdate = false;
        if (System.currentTimeMillis() - lastDownloadTime > refreshTime) {
            //if more time has passed than the set refresh time, get new values
            timeToUpdate = true;
        }
        if (!dbHasValues || timeToUpdate) {
            // If database does not have values or it is time to update
            getMoviesFromApi();
        }
    }

    //Adds the options menu in the topRight corner of the app
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.movies_menu, menu);
        return true;
    }

    //Determines what happens when different options are pressed
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //chech if the id matches with one created in the menu XML folder
        switch (id) {
            case R.id.action_refresh:
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                //get the list value in shared preferences
                String movieList = sharedPreferences.getString(
                        getString(R.string.pref_sort_key),
                        getString(R.string.pref_by_popularity_value)
                );
                if (!movieList.equals(getString(R.string.pref_by_favorites_value))){
                    //if refreshed is pressed in the popular or best rated movie list, connect to the
                    //API again
                    getMoviesFromApi();
                } else {
                    showToast(getString(R.string.refresh_not_possible));
                }
                return true;
            case R.id.action_settings:
                Intent startSettingsActivity = new Intent(this, SettingsActivity.class);
                startActivity(startSettingsActivity);
                return true;
            default:
                Log.e(TAG, "Unknown options item");
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    //creates a loader that connects to the internet and retrievs movie data and inserts them in db
    private void getMoviesFromApi() {
        if (NetworkUtilities.checkInternetConnection(this)) {
            //if internet is available
            //show the loading indicator
            loadingIndicator.setVisibility(View.VISIBLE);
            Intent downloadMoviesIntent = new Intent(this, DownloadAndSaveService.class);
            //this will allow messages to be received from the service
            downloadMoviesIntent.putExtra(DownloadAndSaveService.MESSENGER_KEY, new Messenger(messageHandler));
            startService(downloadMoviesIntent);
        } else {
            showToast("No internet access");
        }

    }

    private void setupSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        //It is possible to pass this here because this activity implements the ChangeListener
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    /*
    The Loader API lets you load data from a content provider or other data source
    Loaders run on seperate threads
    Loaders take care of configuration changes
    */
    //Called when the system needs a new loader(when init or restart loader is called)
    @Override
    public Loader onCreateLoader(int loaderId, final Bundle args) {
        switch (loaderId) {
            case CONTENT_LOADER_ID:
                //get the table that needs to be accessed
                String tableToRead = PreferenceManager.getDefaultSharedPreferences(this).
                        getString(getString(R.string.pref_sort_key),
                        getString(R.string.pref_by_popularity_value)
                        );
                Log.d(TAG, tableToRead);
                if(tableToRead.equals(getString(R.string.pref_by_favorites_value))){
                    //if the table with favorite movies needs to be read
                    //Only the Poster Path is needed to show the movie images in the gridview
                    String[] projection = {
                            MovieContract.FavoriteMovieEntry._ID,
                            MovieContract.FavoriteMovieEntry.COLUMN_POSTER_PATH
                    };
                    //The CursorLoader automatically registers a ContentObserver to trigger a reload when data changes
                    return new CursorLoader(
                            this,
                            MovieContract.FavoriteMovieEntry.CONTENT_URI,
                            projection,//columns
                            null,//rows
                            null,
                            null
                    );
                } else {
                    //if the table wth the movies from the API need to be read
                    //Only the Poster Path is needed to show the movie images in the gridview
                    String[] projection = {
                            MovieContract.MovieEntry._ID,
                            MovieContract.MovieEntry.COLUMN_POSTER_PATH
                    };
                    //The CursorLoader automatically registers a ContentObserver to trigger a reload when data changes
                    return new CursorLoader(
                            this,
                            MovieContract.MovieEntry.CONTENT_URI,
                            projection,//columns
                            null,//rows
                            null,
                            null
                    );
                }
            default:
                Log.e("onCreateLoader:", "Unknown loader");
                return null;
        }
    }

    //called when loader has finished loading data
    @Override
        public void onLoadFinished(Loader loader, Object data) {
        //All use of previous data should be removed.
        int loaderId = loader.getId();
        switch (loaderId) {
            case CONTENT_LOADER_ID:
                //receive the cursor with movies from the background thread
                Cursor movieCursor = (Cursor) data;
                if (movieCursor != null && movieCursor.moveToFirst()) {
                    //Set the new cursor on the adapter
                    movieAdapter.swapCursor(movieCursor);
                    //notify the adapter that it should update
                    movieAdapter.notifyDataSetChanged();
                    noMoviesAvailableTextView.setVisibility(View.GONE);
                } else {
                    //this must be called so errors do not occur. If null is not passed here, the old
                    //cursor is closed but the methods in the content provider will try to access it
                    movieAdapter.swapCursor(null);
                    noMoviesAvailableTextView.setVisibility(View.VISIBLE);
                }
                break;
            default:
                Log.e("onLoadFinished:", "Unknown loader");
                break;
        }
    }

    /*
    *This is called when the previous called loader is reset (when destroyLoader() is called
    * or when the activity is destroyed), thus making its data unavailable. Any reference to that
    * data should be cleared.
    *In that case the data from the previous load is invalid and should be cleared
    */
    @Override
    public void onLoaderReset(Loader loader) {
        int loaderId = loader.getId();
        switch (loaderId) {
            case CONTENT_LOADER_ID:
                //this is called when the last cursor porvied to onLoadFinished is about to be closed
                //the adapter should be cleared here, so we do not try to ese the closed cursor
                movieAdapter.swapCursor(null);
                break;
            default:
                Log.e("onLoadFinished:", "Unknown loader");
                break;
        }
    }

    //allows changes in settings take effect immediately
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_sort_key))) {
            //Show the first page when changing sort type
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(getString(R.string.pref_page_to_open), 1);
            editor.apply();
            String sortOrder = sharedPreferences.getString(key, getString(R.string.pref_by_popularity_value));
            if(!sortOrder.equals(getString(R.string.pref_by_favorites_value))){
                //If the list has been changed to popular or most rated movies, get them from the web
                getMoviesFromApi();
                //favorite movies does not need this call, because it is a local table
            }
            //restart the loader and let it decide which table needs to be read
            //A loaderManager is needed to manage loaders
            LoaderManager loaderManager = getSupportLoaderManager();
            //The system automatically determines if the loader wuth the id exists. If yes it reuses the
            //old one elese creates a new one
            //Since this activity implements Loader callbacks we can pass this as the 3rd argument
            loaderManager.restartLoader(CONTENT_LOADER_ID, null, this);
        }
    }

    /*String sortOrderPrefValue = sharedPreferences.getString(getString(R.string.pref_sort_key),
                getString(R.string.pref_by_popularity_value));*/


    /*
    * A class to receive messages from the service so we know when to update the UI
    * https://stackoverflow.com/questions/20594936/communication-between-activity-and-service
    * */
    public static class MessageHandler extends Handler {
        //A weak reference so we can show toasts, use UI
        //A weak reference, simply put, is a reference that isn't strong enough to force an object to remain in memory.
        private final WeakReference<MainActivity> mWeakReference;


        public MessageHandler(MainActivity activity) {
            mWeakReference = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            int message = msg.arg1;
            MainActivity mActivity = mWeakReference.get();
            //TODO:???Should I check if the reference is null here?
            //hide the loading indicator after receiving message from the service
            mActivity.loadingIndicator.setVisibility(View.INVISIBLE);
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            switch (message) {
                case MESSAGE_SUCCESS:
                    //scroll to top
                    mActivity.movieGridView.smoothScrollToPosition(0);
                    //if the download from the API is successful
                    mActivity.showToast("Download successful");
                    //save the current page in shared preferences
                    int displayedPage = sharedPreferences.getInt(mActivity.getString(R.string.pref_page_to_open), 1);
                    editor.putInt(mActivity.getString(R.string.pref_current_page), displayedPage);
                    mActivity.currentPageText.setText(String.valueOf(displayedPage));
                    //Save that we have some values so even if there is no internet something is displayed
                    editor.putBoolean(mActivity.getString(R.string.pref_has_data_key), true);
                    //save the time value when the values were received
                    editor.putLong(mActivity.getString(R.string.pref_update_time_key), System.currentTimeMillis());
                    break;
                case MESSAGE_FAIL:
                    //if the movie download failed then set the page to be downloaded as the current one
                    //so when the user presses refresh, the same page is refreshed
                    int currentPage = sharedPreferences.getInt(mActivity.getString(R.string.pref_current_page), 1);
                    editor.putInt(mActivity.getString(R.string.pref_page_to_open), currentPage);
                    mActivity.showToast("Failed to download");
                    break;
                default:
                    Log.e(TAG, "Unknown message in handler");
                    break;
            }
            //must be called for the preferences to be saved
            editor.apply();
        }
    }
}
