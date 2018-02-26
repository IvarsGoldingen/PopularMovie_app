package com.example.android.popularmoviesstage2;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.support.v4.content.AsyncTaskLoader;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.popularmoviesstage2.ExtraDataObjects.Review;
import com.example.android.popularmoviesstage2.ExtraDataObjects.Video;
import com.example.android.popularmoviesstage2.database.MovieContract;
import com.example.android.popularmoviesstage2.database.MovieContract.FavoriteMovieEntry;
import com.example.android.popularmoviesstage2.utilities.NetworkUtilities;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity that displays a single movie with all fields used
 */

public class DetailMovieActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks{

    private static final String TAG = "Detail activity: ";
    //Loader ids
    private static final int GET_MOVIE_DETAILS_LOADER_ID = 3;
    private static final int CHECK_IF_ADDED_TO_FAVORITES_LOADER_ID = 4;
    private static final int GET_REVIEWS_LOADER = 5;
    private static final int GET_TRAILERS_LOADER = 6;

    private static final String MOVIE_ID_EXTRA_KEY = "movie_id_extra";

    //these constants together with the Uri  matcher will be used to show which data needs to be accessed
    //constants for the temporary DB
    private static final int POPULAR_MOVIES_WITH_ID = 101;
    //constants for favorite movie DB
    private static final int FAVORITE_MOVIES_WITH_ID = 201;

    //xml views
    private TextView movieTitleText;
    private TextView movieScoreText;
    private TextView movieRealeaseDateText;
    private TextView moviePlotText;
    private ImageView moviePosterImage;
    private ImageButton addToFavoritesButton;
    private TextView reviewTitleTextView;
    private TextView videoTitleTextView;
    private ProgressBar reviewProgressBar;
    private ProgressBar videoProgressBar;

    //Uri of the single movie that is opened. Global so it can easy be accessed in onCreateLoader()
    private Uri singleMovieUri;

    //The movie info values are global, so that if necessary the movie can be added to favorites
    private String imageResourceString;
    private String titleString;
    private String overviewString;
    private String realeaseDateString;
    private String voteAvarageString;
    private int movieId;
    //boolean that indicates if the movie is in the favorites list. For button clicks
    private boolean isInFavorites;
    private Toast mToast;
    //A list of reviews
    List<Review> reviewList;
    //A list of videos
    List<Video> videoList;

    //key values to save the movie on device rotation, this is needed because if the movie is removed
    //from the favorites list, the cursor does not hold it's values any more
    private static final String KEY_IMAGE_RESOURCE_STRING = "image_resource";
    private static final String KEY_TITLE_STRING = "title";
    private static final String KEY_OVERVIEW_STRING = "overview";
    private static final String KEY_RELEASE_DATE_STRING = "realease_date";
    private static final String KEY_VOTE_AVARAGE_STRING = "vote_avarage";
    private static final String KEY_IS_FAVORITE_BOOL = "is_favorite";
    //Keys for saving reviews
    private static final String KEY_REVIEW_AUTHOR_ARRAY_STRING  = "review_author";
    private static final String KEY_REVIEW_CONTENT_ARRAY_STRING  = "review_content";
    private static final String KEY_REVIEW_URL_ARRAY_STRING  = "review_url";
    private static final String KEY_REVIEW_TITLE_TEXT  = "review_title";
    //keys for saving videos
    private static final String KEY_VIDEO_NAME_ARRAY_STRING  = "video_name";
    private static final String KEY_VIDEO_KEY_ARRAY_STRING  = "video_key";
    private static final String KEY_VIDEO_TITLE_TEXT  = "video_title";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail_activity);
        singleMovieUri = getIntent().getData();

        movieTitleText = findViewById(R.id.movie_title_text);
        movieScoreText = findViewById(R.id.movie_rating_text);
        movieRealeaseDateText = findViewById(R.id.movie_release_date_text);
        moviePlotText = findViewById(R.id.movie_plot_overview_text);
        moviePosterImage = findViewById(R.id.movie_poster_image);
        addToFavoritesButton = findViewById(R.id.favorites_star_button);
        reviewTitleTextView = findViewById(R.id.review_title_textview);
        reviewProgressBar = findViewById(R.id.review_loading_indicator);
        videoTitleTextView = findViewById(R.id.video_title_textview);
        videoProgressBar = findViewById(R.id.video_loading_indicator);

        addToFavoritesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isInFavorites){
                    /* delete the loader, because if we got the singleMovieUri from favorites list
                    * the cursor will be lost after we delete tha data from db
                    * all data is saved in onInstanceSave*/
                    getSupportLoaderManager().destroyLoader(GET_MOVIE_DETAILS_LOADER_ID);
                    getSupportLoaderManager().destroyLoader(CHECK_IF_ADDED_TO_FAVORITES_LOADER_ID);
                    String deleteSelection = FavoriteMovieEntry.COLUMN_MOVIE_ID + "=?";
                    String[] deleteSelectionArgs = new String[]{String.valueOf(movieId)};
                    int moviesDeleted = getContentResolver().delete(FavoriteMovieEntry.CONTENT_URI, deleteSelection, deleteSelectionArgs);
                    if (moviesDeleted > 0){
                        isInFavorites = false;
                        addToFavoritesButton.setBackgroundResource(R.drawable.ic_star_border_black_48dp);
                        showToast("Movie removed from favorites list");
                    }
                } else {
                    //Create a content values object that can be inserted in the db
                    ContentValues singleMovieContentValues = new ContentValues();
                    singleMovieContentValues.put(FavoriteMovieEntry.COLUMN_TITLE, titleString);
                    singleMovieContentValues.put(FavoriteMovieEntry.COLUMN_POSTER_PATH, imageResourceString);
                    singleMovieContentValues.put(FavoriteMovieEntry.COLUMN_OVERVIEW, overviewString);
                    singleMovieContentValues.put(FavoriteMovieEntry.COLUMN_VOTE_AVARAGE, voteAvarageString);
                    singleMovieContentValues.put(FavoriteMovieEntry.COLUMN_REALEASE_DATE, realeaseDateString);
                    singleMovieContentValues.put(FavoriteMovieEntry.COLUMN_MOVIE_ID, movieId);

                    //getContentResolver().bulkInsert(MovieContract.MovieEntry.CONTENT_URI, movieValues);
                    //insert this movie into the favorites db table
                    Uri insertedUri = getContentResolver().insert(FavoriteMovieEntry.CONTENT_URI, singleMovieContentValues);
                    String uri = insertedUri.toString();
                    if (uri != null){
                        showToast("Movie added to favorites list");
                        isInFavorites = true;
                        addToFavoritesButton.setBackgroundResource(R.drawable.ic_star_black_48dp);
                    }
                }


            }
        });

        if (singleMovieUri == null) {
            Toast.makeText(this, "Invalid movie", Toast.LENGTH_SHORT).show();
            //exit activity if the uri is invalid
            finish();
        }
        if (savedInstanceState == null){
            //get the data from the loader only if there is no data in in savedInstanceState
            getSupportLoaderManager().initLoader(GET_MOVIE_DETAILS_LOADER_ID, null, this);
        } else {
            if (savedInstanceState.containsKey(KEY_IMAGE_RESOURCE_STRING)){
                imageResourceString = savedInstanceState.getString(KEY_IMAGE_RESOURCE_STRING);
            }
            if (savedInstanceState.containsKey(KEY_TITLE_STRING)){
                titleString = savedInstanceState.getString(KEY_TITLE_STRING);
            }
            if (savedInstanceState.containsKey(KEY_OVERVIEW_STRING)){
                overviewString = savedInstanceState.getString(KEY_OVERVIEW_STRING);
            }
            if (savedInstanceState.containsKey(KEY_RELEASE_DATE_STRING)){
                realeaseDateString = savedInstanceState.getString(KEY_RELEASE_DATE_STRING);
            }
            if (savedInstanceState.containsKey(KEY_VOTE_AVARAGE_STRING)){
                voteAvarageString = savedInstanceState.getString(KEY_VOTE_AVARAGE_STRING);
            }
            if (savedInstanceState.containsKey(KEY_IS_FAVORITE_BOOL)){
                isInFavorites = savedInstanceState.getBoolean(KEY_IS_FAVORITE_BOOL);
            }
            movieTitleText.setText(titleString);
            String votePlaceHolder = voteAvarageString + "/10";
            movieScoreText.setText(votePlaceHolder);
            moviePlotText.setText(overviewString);
            movieRealeaseDateText.setText(realeaseDateString);
            if (isInFavorites){
                addToFavoritesButton.setBackgroundResource(R.drawable.ic_star_black_48dp);
            }
            String url = NetworkUtilities.createImageUrlString(imageResourceString);
            Picasso.with(this)
                    .load(url)
                    .error(R.drawable.no_image_svg)
                    .into(moviePosterImage);
            //reviews restoring
            if (savedInstanceState.containsKey(KEY_REVIEW_CONTENT_ARRAY_STRING) &&
                    savedInstanceState.containsKey(KEY_REVIEW_AUTHOR_ARRAY_STRING)){
                restoreReviews(
                        savedInstanceState.getStringArray(KEY_REVIEW_AUTHOR_ARRAY_STRING),
                        savedInstanceState.getStringArray(KEY_REVIEW_CONTENT_ARRAY_STRING),
                        savedInstanceState.getStringArray(KEY_REVIEW_URL_ARRAY_STRING)
                );
            } else {
                //The title differes if there is no reviews or if there is no internet
                String reviewTitle = savedInstanceState.getString(KEY_REVIEW_TITLE_TEXT);
                reviewTitleTextView.setText(reviewTitle);
                reviewList = null;
            }
            if (savedInstanceState.containsKey(KEY_VIDEO_KEY_ARRAY_STRING) &&
                    savedInstanceState.containsKey(KEY_VIDEO_NAME_ARRAY_STRING)){
                restoreVideos(
                        savedInstanceState.getStringArray(KEY_VIDEO_NAME_ARRAY_STRING),
                        savedInstanceState.getStringArray(KEY_VIDEO_KEY_ARRAY_STRING)
                );
            } else {
                //The title differes if there is no videos or if there is no internet
                String videoTitle = savedInstanceState.getString(KEY_VIDEO_TITLE_TEXT);
                videoTitleTextView.setText(videoTitle);
                videoList = null;
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //For base data savings
        //This is not handled with the adapter, because if the movie is opened from favorites list
        //and deleted, the cursor is no longer available
        outState.putString(KEY_IMAGE_RESOURCE_STRING, imageResourceString);
        outState.putString(KEY_TITLE_STRING, titleString);
        outState.putString(KEY_OVERVIEW_STRING, overviewString);
        outState.putString(KEY_RELEASE_DATE_STRING, realeaseDateString);
        outState.putString(KEY_VOTE_AVARAGE_STRING, voteAvarageString);
        outState.putBoolean(KEY_IS_FAVORITE_BOOL, isInFavorites);
        outState.putString(KEY_REVIEW_TITLE_TEXT, reviewTitleTextView.getText().toString());
        if (reviewList != null){
            //For review saving
            String [] authorArray = createAuthorsArrayFromList();
            String [] reviewsArray = createReviewsArrayFromList();
            String [] reviewUrlArray = createReviewsUrlArrayFromList();
            outState.putStringArray(KEY_REVIEW_AUTHOR_ARRAY_STRING, authorArray);
            outState.putStringArray(KEY_REVIEW_CONTENT_ARRAY_STRING, reviewsArray);
            outState.putStringArray(KEY_REVIEW_URL_ARRAY_STRING, reviewUrlArray);
        }
        outState.putString(KEY_VIDEO_TITLE_TEXT, videoTitleTextView.getText().toString());
        if (videoList != null){
            //for video saving
            String [] videoNameArray = createVideosArrayFromList();
            String [] videoKeyArray = createVideosKeyArrayFromList();
            outState.putStringArray(KEY_VIDEO_NAME_ARRAY_STRING, videoNameArray);
            outState.putStringArray(KEY_VIDEO_KEY_ARRAY_STRING, videoKeyArray);
        }
    }

    private String[] createAuthorsArrayFromList (){
        int numberOfReviews = reviewList.size();
        String [] reviews = new String[numberOfReviews];
        for (int i =0; i < numberOfReviews; i++){
            reviews[i] = reviewList.get(i).getAuthor();
        }
        return reviews;
    }

    private String[] createReviewsArrayFromList (){
        int numberOfReviews = reviewList.size();
        String [] reviews = new String[numberOfReviews];
        for (int i =0; i < numberOfReviews; i++){
            reviews[i] = reviewList.get(i).getReview();
        }
        return reviews;
    }

    private String[] createReviewsUrlArrayFromList (){
        int numberOfReviews = reviewList.size();
        String [] reviews = new String[numberOfReviews];
        for (int i =0; i < numberOfReviews; i++){
            reviews[i] = reviewList.get(i).getReviewUrl().toString();
        }
        return reviews;
    }

    private String[] createVideosArrayFromList (){
        int numberOfVideos = videoList.size();
        String [] videos = new String[numberOfVideos];
        for (int i =0; i < numberOfVideos; i++){
            videos[i] = videoList.get(i).getName();
        }
        return videos;
    }

    private String[] createVideosKeyArrayFromList (){
        int numberOfVideos = videoList.size();
        String [] videos = new String[numberOfVideos];
        for (int i =0; i < numberOfVideos; i++){
            videos[i] = videoList.get(i).getKey();
        }
        return videos;
    }

    private void restoreReviews (String[] authors, String[] reviews, String[] urls){
        reviewList = new ArrayList<>();
        int reviewCount = authors.length;
        for (int i =0; i < reviewCount; i++){
            URL reviewUrl = null;
            try {
                reviewUrl = new URL(urls[i]);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            reviewList.add(new Review(
                    authors[i],
                    reviews[i],
                    reviewUrl
            ));
        }
        addReviewsToUI(reviewList);
    }

    private void restoreVideos (String [] names, String [] keys){
        videoList = new ArrayList<>();
        int videoCount = names.length;
        for (int i = 0; i < videoCount; i++){
            videoList.add(new Video(
                    names[i],
                    keys[i]
            ));
        }
        addVideosToUI(videoList);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    /**
     * Creates and returns a CursorLoader that loads the data for our URI and stores it in a Cursor.
     *
     * @param id   The loader ID for which we need to create a loader
     * @param args Any arguments supplied by the caller
     * @return A new Loader instance that is ready to start loading.
     */
    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        switch (id){
            case GET_MOVIE_DETAILS_LOADER_ID:
                //get the movie passed in by the intent
                String[] projection = {
                        MovieContract.MovieEntry._ID,
                        MovieContract.MovieEntry.COLUMN_TITLE,
                        MovieContract.MovieEntry.COLUMN_VOTE_AVARAGE,
                        MovieContract.MovieEntry.COLUMN_REALEASE_DATE,
                        MovieContract.MovieEntry.COLUMN_OVERVIEW,
                        MovieContract.MovieEntry.COLUMN_POSTER_PATH,
                        MovieContract.MovieEntry.COLUMN_MOVIE_ID
                };
                return new CursorLoader(this,
                        singleMovieUri,
                        projection,//All columns necessary so this could be not implemented
                        null,//row selection will be done from the uri since it is specific to one movie
                        null,
                        null
                );
            case CHECK_IF_ADDED_TO_FAVORITES_LOADER_ID:
                //check if the movie is already in the movies list
                //It is not neceesary to retrieve the movie from the favorites list, just to check if it there
                //So it is enough with just getting the ID
                String[] chekcProjection = {
                        FavoriteMovieEntry._ID
                };
                //create the selection so that the the movies is searched for by title
                String checkSelection = FavoriteMovieEntry.COLUMN_TITLE + "=?";
                String[] checkSelectionArgs = new String[]{titleString};
                return new CursorLoader (
                        this,
                        FavoriteMovieEntry.CONTENT_URI,
                        chekcProjection,
                        checkSelection,
                        checkSelectionArgs,
                        null
                );
            case GET_REVIEWS_LOADER:
                return new GetReviewsTask(this, args);
            case GET_TRAILERS_LOADER:
                return new GetVideosTask(this, args);
            default:
                Log.e(TAG, "Unknown loader create");
                return null;
        }
    }

    /**
     * Runs on the main thread when a load is complete. If initLoader is called (we call it from
     * onCreate in DetailActivity) and the LoaderManager already has completed a previous load
     * for this Loader, onLoadFinished will be called immediately. Within onLoadFinished, we bind
     * the data to our views so the user can see the details of the movie
     *
     * @param loader The cursor loader that finished.
     * @param data   The cursor that is being returned.
     */
    @Override
    public void onLoadFinished(Loader loader, Object data) {
        int id = loader.getId();
        switch (id){
            case GET_MOVIE_DETAILS_LOADER_ID:
                Cursor movieCursor = (Cursor)data;
                //check if the received cursor has data
                boolean cursorHasValidData = false;
                if (movieCursor != null && movieCursor.moveToFirst()) {
                    cursorHasValidData = true;
                }
                if (!cursorHasValidData) {
                    Toast.makeText(this, "Database error", Toast.LENGTH_SHORT).show();
                /* No data to display, simply return and do nothing */
                    return;
                }

                int imageResourceIndex = movieCursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_POSTER_PATH);
                int titleIndex = movieCursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_TITLE);
                int overviewIndex = movieCursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_OVERVIEW);
                int realeaseDateIndex = movieCursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_REALEASE_DATE);
                int voteAvarageIndex = movieCursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_VOTE_AVARAGE);
                int movieIdIndex = movieCursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_MOVIE_ID);

                imageResourceString = movieCursor.getString(imageResourceIndex);
                titleString = movieCursor.getString(titleIndex);
                overviewString = movieCursor.getString(overviewIndex);
                realeaseDateString = movieCursor.getString(realeaseDateIndex);
                voteAvarageString = movieCursor.getString(voteAvarageIndex);
                movieId = movieCursor.getInt(movieIdIndex);

                //when we get the movieId, start the loader which downloads the reviews and videos
                if(NetworkUtilities.checkInternetConnection(DetailMovieActivity.this)){
                    reviewProgressBar.setVisibility(View.VISIBLE);
                    videoProgressBar.setVisibility(View.VISIBLE);
                    Bundle movieIdBundle = new Bundle();
                    movieIdBundle.putInt(MOVIE_ID_EXTRA_KEY, movieId);
                    getSupportLoaderManager().initLoader(GET_REVIEWS_LOADER, movieIdBundle, DetailMovieActivity.this);
                    getSupportLoaderManager().initLoader(GET_TRAILERS_LOADER, movieIdBundle, DetailMovieActivity.this);
                } else {
                    reviewTitleTextView.setText("Internet required to get reviews");
                    videoTitleTextView.setText("Internet required to get videos");
                }

                movieTitleText.setText(titleString);
                String votePlaceHolder = voteAvarageString + "/10";
                movieScoreText.setText(votePlaceHolder);
                moviePlotText.setText(overviewString);
                movieRealeaseDateText.setText(realeaseDateString);

                //Use a uri matcher to check if the movie was opened from the favorites list
                //If not check if if it is at the favorites list with another loader
                UriMatcher uriMatcher = buildUriMatcher();
                int match = uriMatcher.match(singleMovieUri);
                switch (match){
                    case POPULAR_MOVIES_WITH_ID:
                        //Start a new loader to chech if the movie is in the favorites list
                        getSupportLoaderManager().initLoader(CHECK_IF_ADDED_TO_FAVORITES_LOADER_ID, null, this);
                        break;
                    case FAVORITE_MOVIES_WITH_ID:
                        //if the movie is in the favorites list show in UI
                        addToFavoritesButton.setBackgroundResource(R.drawable.ic_star_black_48dp);
                        isInFavorites = true;
                        break;
                    default:
                        Log.e(TAG, "Unknown uri in matcher");
                        break;
                }

                String url = NetworkUtilities.createImageUrlString(imageResourceString);
                Picasso.with(this)
                        .load(url)
                        .error(R.drawable.no_image_svg)
                        .into(moviePosterImage);
                break;
            case CHECK_IF_ADDED_TO_FAVORITES_LOADER_ID:
                Cursor cursor = (Cursor)data;
                if (data != null && cursor.moveToFirst()) {
                    //if the movie is in the favorites list show in UI
                    addToFavoritesButton.setBackgroundResource(R.drawable.ic_star_black_48dp);
                    isInFavorites = true;
                } else {
                    //if the movie is NOT in the favorites list show in UI
                    addToFavoritesButton.setBackgroundResource(R.drawable.ic_star_border_black_48dp);
                    isInFavorites = false;
                }
                break;
            case GET_REVIEWS_LOADER:
                reviewProgressBar.setVisibility(View.GONE);
                if (data == null){
                    reviewTitleTextView.setText("No reviews found");
                    reviewList = null;
                } else {
                    reviewList = (List<Review>) data;
                    addReviewsToUI(reviewList);
                }
                break;
            case GET_TRAILERS_LOADER:
                videoProgressBar.setVisibility(View.GONE);
                if (data == null){
                    videoTitleTextView.setText("No videos found");
                    videoList = null;
                } else {
                    videoList = (List<Video>) data;
                    addVideosToUI(videoList);
                }
                break;
            default:
                Log.e(TAG, "UnknownLoader");
                break;
        }
    }


    /**
     * Called when a previously created loader is being reset, thus making its data unavailable.
     * The application should at this point remove any references it has to the Loader's data.
     * Since the cursor does not reload with new data nothing is done here
     *
     * @param loader The Loader that is being reset.
     */
    @Override
    public void onLoaderReset(Loader loader) {
        Log.d(TAG, "Loader reset");
    }

    /*
    * A loader task that will download the reviews  from the API
    * */
    private static class GetReviewsTask extends AsyncTaskLoader<List<Review>>{
        private Bundle args;

        public GetReviewsTask(DetailMovieActivity context, Bundle bundle) {
            super(context);
            args = bundle;
        }

        //handles the request to start the loader
        @Override
        protected void onStartLoading() {
            //if the movie ID was not passed return
            if (args == null){
                return;
            }
            forceLoad();
        }

        /*
        *Does work in a different threadm and returns the result of its work
        */
        @Override
        public List<Review> loadInBackground() {
            int movieId = args.getInt(MOVIE_ID_EXTRA_KEY);
            URL dataUrl = NetworkUtilities.buildReviewVideoUrl(NetworkUtilities.QUERY_TYPE_REVIEWS, movieId);
            try {
                String jsonDataString = NetworkUtilities.getResponseFromHttpUrl(dataUrl);
                return NetworkUtilities.getReviewsFromJson(jsonDataString);
            } catch (IOException e){
                Log.e(TAG, e.toString());
            }
            return null;
        }

        //called when new data is available
        @Override
        public void deliverResult(List<Review> data) {
            super.deliverResult(data);
        }
    }

    //A loader task that will download videos of the movie
    private static class GetVideosTask extends AsyncTaskLoader<List<Video>>{
        private Bundle args;
        private List<Video> mList;

        public GetVideosTask(Context context, Bundle bundle) {
            super(context);
            args = bundle;
        }

        @Override
        public List<Video> loadInBackground() {
            int movieId = args.getInt(MOVIE_ID_EXTRA_KEY);
            URL dataUrl = NetworkUtilities.buildReviewVideoUrl(NetworkUtilities.QUERY_TYPE_VIDEOS, movieId);
            try{
                String jsonDataString = NetworkUtilities.getResponseFromHttpUrl(dataUrl);
                return NetworkUtilities.getVideosFromJson(jsonDataString);
            } catch (IOException e){
                Log.e(TAG, e.toString());
            }
            return null;
        }

        @Override
        protected void onStartLoading() {
            //if the movie ID was not passed return
            if (args == null){
                return;
            }
            if(mList == null){
                forceLoad();
            } else {
                deliverResult(mList);
            }
        }

        @Override
        public void deliverResult(List<Video> data) {
            mList = data;
            super.deliverResult(data);
        }
    }

    /*
    * used to check from which list the movie was opened
    * if it was favorites, the the database does not need to be searched to check whether the movie
    * is in the favorites list
    * */
    private static UriMatcher buildUriMatcher() {
        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        //add all the Uris we want to recognise to the uriMatcher
        //Uri for a single popular movie
        //# is a wildcard symbol which can be any numerical value. It will be used to specify which movie from the db we want
        uriMatcher.addURI(MovieContract.AUTHORITY, MovieContract.PATH_POPULAR_MOVIES + "/#", POPULAR_MOVIES_WITH_ID);
        //Uri for a single favorite movie
        //# is a wildcard symbol which can be any numerical value. It will be used to specify which movie from the db we want
        uriMatcher.addURI(MovieContract.AUTHORITY, MovieContract.PATH_FAVORITE_MOVIES + "/#", FAVORITE_MOVIES_WITH_ID);
        return uriMatcher;
    }

    //Method to show toast and not let them be accumulated
    private void showToast(String message) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        mToast.show();
    }

    private void addVideosToUI (final List<Video> videos){
        int videoCount = videos.size();
        LinearLayout linearLayout = findViewById(R.id.video_linear_layout);
        int childCount = linearLayout.getChildCount();
        //Do not allow adding the same cideos again by checking if any reviews have already been
        //added to the layout(happens when returning from opening the video in youtube app)
        // . Child 1 is the title, child 2 is the loading indicator
        if (childCount <= 2){
            //A layout inflater instantiates a layout XML file into its corresponding View objects
            LayoutInflater inflater = LayoutInflater.from(this);
            for (int i = 0; i < videoCount; i++){
                //A final variable is necessary to access the list
                final int j = i;
                LinearLayout layout = (LinearLayout)inflater.inflate(R.layout.video_item_layout, null, false);
                ImageButton playButton = layout.findViewById(R.id.video_play_button);
                TextView videoNameText = layout.findViewById(R.id.video_name_textview);
                playButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Uri youtubeUri = NetworkUtilities.createYoutubeUri(videos.get(j).getKey());
                        Intent youtTubeIntent = new Intent(Intent.ACTION_VIEW, youtubeUri);
                        startActivity(youtTubeIntent);
                    }
                });
                videoNameText.setText(videos.get(j).getName());
                linearLayout.addView(layout);
            }
        }
    }

    /*
    * Adds received review to UI
    * */
    private void addReviewsToUI(final List<Review> reviews){
        int reviewCount = reviews.size();
        LinearLayout linearLayout = findViewById(R.id.review_linear_layout);
        int childCount = linearLayout.getChildCount();
        //Do not allow adding the same reviews again by checking if any reviews have already been
        //added to the layout(happens when returning from opening the review in a browser)
        // . Child 1 is the title, child 2 is the loading indicator
        if (childCount <= 2){
            linearLayout.addView(createDivider());
            for (int i = 0; i < reviewCount; i++){
                //needed to acces the items in the list
                final int j = i;
                //AUTHORS textview
                TextView authorTextViewToAdd = getTextViewForReviews(reviews.get(i).getAuthor());
                //display author in 1 line
                authorTextViewToAdd.setLines(1);
                authorTextViewToAdd.setTypeface(null, Typeface.ITALIC);
                //Reviews textview
                //Remove new lines from review since a very small part of the review is visible in the textview
                String reviewText = reviews.get(i).getReview().replace("\n", "");
                TextView reviewTextViewToAdd = getTextViewForReviews(reviewText);
                //display review in maximum of 2 lines
                reviewTextViewToAdd.setLines(2);
                //open web for full review on click
                reviewTextViewToAdd.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String urlString = reviews.get(j).getReviewUrl().toString();
                        Intent webIntent = new Intent(Intent.ACTION_VIEW);
                        webIntent.setData(Uri.parse(urlString));
                        startActivity(webIntent);
                    }
                });
                linearLayout.addView(authorTextViewToAdd);
                linearLayout.addView(reviewTextViewToAdd);
                linearLayout.addView(createDivider());
            }
        }
    }

    private View createDivider(){
        View view = new View(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,//width
                TypedValue.COMPLEX_UNIT_DIP, getResources().getDimension(R.dimen.divider_height)
        );
        view.setLayoutParams(layoutParams);
        view.setBackgroundColor(getResources().getColor(R.color.divider_color));
        return view;
    }

    private TextView getTextViewForReviews(String text){
        TextView textView = new TextView(this);
        textView.setText(text);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,//width
                LinearLayout.LayoutParams.WRAP_CONTENT//height
        );
        textView.setLayoutParams(layoutParams);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setTextColor(getResources().getColor(R.color.text_color));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.detail_info_text_size));
        return textView;
    }

}
