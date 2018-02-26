package com.example.android.popularmoviesstage2.utilities;

import android.content.ContentValues;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.example.android.popularmoviesstage2.ExtraDataObjects.Review;
import com.example.android.popularmoviesstage2.ExtraDataObjects.Video;
import com.example.android.popularmoviesstage2.database.MovieContract.MovieEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static android.content.Context.CONNECTIVITY_SERVICE;

/**
 * Used to connect to the movies api
 */
//The final modifier makes the class not expandable
public final class NetworkUtilities {
    //Strings to change the sort order with API
    public static final String QUERY_TYPE_POPULAR = "popular";
    public static final String QUERY_TYPE_TOP_RATED = "top_rated";
    //Strings to change the query type for video and review download
    public static final String QUERY_TYPE_REVIEWS = "reviews";
    public static final String QUERY_TYPE_VIDEOS = "videos";
    private static final String TAG = "Network utils: ";
    private final static String POPULAR_MOVIE_BASE_URL =
            "https://api.themoviedb.org/3/movie";
    private static final String YOUTUBE_BASE_URL = "http://www.youtube.com/watch";
    private final static String PARAM_KEY = "api_key";
    //TODO: add your key here for the app to work
    private static final String API_KEY = "";
    //Strings to change page number
    private static final String PARAM_PAGE_NUMBER = "page";
    //Youtube video open parameter
    private static final String PARAM_VIDEO = "v";

    private static final String MOVIE_IMAGE_BASE_URL =
            "https://image.tmdb.org/t/p/";
    private static final String MOVIE_IMAGE_SIZE = "w185";
    //maximum nuber of reviews read from the API, if more than 5 then the code must be changed
    private final static int MAX_NUMBER_OF_REVIEWS = 5;
    //maximum nuber of videos read from the API
    private final static int MAX_NUMBER_OF_VIDEOS = 3;

    public static String createImageUrlString(String posterPath) {
        return MOVIE_IMAGE_BASE_URL + MOVIE_IMAGE_SIZE + posterPath;
    }


    //this method builds the url which will be used to connect to the movies api
    public static URL buildPopularMovieUrl(String queryType, int pageNumber) {
        Uri builtUri = Uri.parse(POPULAR_MOVIE_BASE_URL).buildUpon()
                .appendPath(queryType)
                .appendQueryParameter(PARAM_PAGE_NUMBER, String.valueOf(pageNumber))
                .appendQueryParameter(PARAM_KEY, API_KEY)
                .build();
        //To get the response the uri must be changed in to an url
        URL url = null;
        try {
            url = new URL(builtUri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return url;
    }

    //this method builds the url which will be used to connect to the movies api
    public static URL buildReviewVideoUrl(String dataType, int movieId) {
        Uri builtUri = Uri.parse(POPULAR_MOVIE_BASE_URL).buildUpon()
                .appendPath(String.valueOf(movieId))
                .appendPath(dataType)
                .appendQueryParameter(PARAM_KEY, API_KEY)
                .build();
        //To get the response the uri must be changed in to an url
        URL url = null;
        try {
            url = new URL(builtUri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return url;
    }

    public static String getResponseFromHttpUrl(URL url) throws IOException {
        //this does not connect to the network yet, it just creates the object
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            //The connection returns an inputStream
            InputStream inputStream = urlConnection.getInputStream();

            //A scanner will be used to retriew data from the stream.
            Scanner scanner = new Scanner(inputStream);
            //This makes the scanner read all data from the scanner.
            scanner.useDelimiter("\\A");

            boolean hasInput = scanner.hasNext();
            if (hasInput) {
                //if the scanner has data retun it
                return scanner.next();
            } else {
                return null;
            }
        } finally {
            urlConnection.disconnect();
        }
    }

    /*
    * this method creates an array of content values from json data so movies can be saved in a db
    * */
    public static ContentValues[] getMovieContentValuesFromJson(String JSONdata) {
        if (TextUtils.isEmpty(JSONdata)) {
            return null;
        }
        //content values are a set of key-value pairs, they are used to insert data into databases
        ContentValues[] movieContentValues = null;
        try {
            JSONObject baseJson = new JSONObject(JSONdata);
            JSONArray movieJsonArray = baseJson.getJSONArray("results");
            movieContentValues = new ContentValues[movieJsonArray.length()];

            for (int i = 0; i < movieJsonArray.length(); i++) {
                String movieTitle;
                String imageLocation;
                String plotSynopsis;
                double userRating;
                String releaseDate;
                int movieId;

                JSONObject movieObject = movieJsonArray.getJSONObject(i);
                movieTitle = movieObject.getString("title");
                imageLocation = movieObject.getString("poster_path");
                plotSynopsis = movieObject.getString("overview");
                userRating = movieObject.getDouble("vote_average");
                releaseDate = movieObject.getString("release_date");
                movieId = movieObject.getInt("id");

                ContentValues singleMovieContentValues = new ContentValues();
                singleMovieContentValues.put(MovieEntry.COLUMN_TITLE, movieTitle);
                singleMovieContentValues.put(MovieEntry.COLUMN_POSTER_PATH, imageLocation);
                singleMovieContentValues.put(MovieEntry.COLUMN_OVERVIEW, plotSynopsis);
                singleMovieContentValues.put(MovieEntry.COLUMN_VOTE_AVARAGE, userRating);
                singleMovieContentValues.put(MovieEntry.COLUMN_REALEASE_DATE, releaseDate);
                singleMovieContentValues.put(MovieEntry.COLUMN_MOVIE_ID, movieId);

                movieContentValues[i] = singleMovieContentValues;
            }
        } catch (JSONException e) {
            Log.e(TAG, "Problem parsing the movie JSON results", e);
        }
        return movieContentValues;
    }

    /*
    * Retrievs reviews about the movie
    * used in details view
    * */
    public static List<Review> getReviewsFromJson (String jsonData){
        if (TextUtils.isEmpty(jsonData)){
            //no data received
            return null;
        }
        try{
            JSONObject baseJson = new JSONObject(jsonData);
            JSONArray reviewsArray = baseJson.getJSONArray("results");
            int numberOfreviewsToRead = reviewsArray.length();
            if (numberOfreviewsToRead < 1){
                return null;
            } else if (numberOfreviewsToRead > MAX_NUMBER_OF_REVIEWS) {
                //do not read more than 5 reviews
                numberOfreviewsToRead = MAX_NUMBER_OF_REVIEWS;
            }
            List<Review> reviewList = new ArrayList<>();
            for (int i = 0; i < numberOfreviewsToRead; i++){
                String author;
                String review;
                URL reviewUrl;

                JSONObject reviewObject = reviewsArray.getJSONObject(i);
                author = reviewObject.getString("author");
                review = reviewObject.getString("content");
                String urlString = reviewObject.getString("url");
                try {
                    reviewUrl = new URL(urlString);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    reviewUrl = null;
                }

                reviewList.add(new Review(author, review, reviewUrl));
            }
            return reviewList;
        }catch (JSONException e) {
            Log.e(TAG, "Problem parsing the reviews JSON results", e);
            return null;
        }
    }

    /*
    * Retrievs videos about the movie
    * used in details view
    * */
    public static List<Video> getVideosFromJson (String jsonData){
        if (TextUtils.isEmpty(jsonData)){
            //no data received
            return null;
        }
        try{
            JSONObject baseJson = new JSONObject(jsonData);
            JSONArray videosArray = baseJson.getJSONArray("results");
            int numberOfvideosToRead = videosArray.length();
            if (numberOfvideosToRead < 1){
                return null;
            } else if (numberOfvideosToRead > MAX_NUMBER_OF_VIDEOS) {
                //do not read more than 3 reviews
                numberOfvideosToRead = MAX_NUMBER_OF_VIDEOS;
            }
            List<Video> videoList = new ArrayList<>();
            for (int i = 0; i < numberOfvideosToRead; i++){
                String name;
                String site;
                String key;

                JSONObject reviewObject = videosArray.getJSONObject(i);
                site = reviewObject.getString("site");
                //add videos only from youtube
                if (site.equals("YouTube")){
                    name = reviewObject.getString("name");
                    key = reviewObject.getString("key");
                    videoList.add(new Video(name, key));
                }
            }
            return videoList;
        }catch (JSONException e) {
            Log.e(TAG, "Problem parsing videos JSON results", e);
            return null;
        }
    }

    //method to check if network connection is available
    public static boolean checkInternetConnection(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        boolean isConnected = networkInfo != null &&
                networkInfo.isConnectedOrConnecting();
        return isConnected;
    }

    //Method to create youtube Uri
    public static Uri createYoutubeUri(String id){
        Uri builtUri = Uri.parse(YOUTUBE_BASE_URL).buildUpon()
                .appendQueryParameter(PARAM_VIDEO, id)
                .build();
        return builtUri;
    }

}
