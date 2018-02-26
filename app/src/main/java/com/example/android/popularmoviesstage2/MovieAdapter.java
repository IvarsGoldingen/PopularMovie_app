package com.example.android.popularmoviesstage2;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.example.android.popularmoviesstage2.database.MovieContract;
import com.example.android.popularmoviesstage2.utilities.NetworkUtilities;
import com.squareup.picasso.Picasso;

/**
 * The adapter will display movie posters in a gridview
 */

public class MovieAdapter extends CursorAdapter {

    public MovieAdapter(@NonNull Context context, Cursor data) {
        super(context, data, 0);
    }

    //this is called when a new needs to be created
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        ImageView imageView;
        imageView = new ImageView(context);
        imageView.setLayoutParams(new GridView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 800));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return imageView;
    }

    //this is called when data is binded to a view
    //the cursor is already at the correct position
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        int imageResourceIndex = cursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_POSTER_PATH);
        String currentImageResource = cursor.getString(imageResourceIndex);
        String url = NetworkUtilities.createImageUrlString(currentImageResource);
        Picasso.with(context)
                .load(url)
                .error(R.drawable.no_image_svg)
                .into((ImageView) view);
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        return super.swapCursor(newCursor);
    }
}
