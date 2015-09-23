package com.waid.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.Display;

/**
 * Created by kodjobaah on 20/07/2015.
 */
public class WaidUtils {


    public static Point getScreenSize(Activity activity) {
        Point size = null;
        if (activity != null) {
            Display display = activity.getWindowManager().getDefaultDisplay();
            size = new Point();
            display.getSize(size);
        }
        return size;
    }

    public static Drawable resizeImage(int resId, int w, int h, Resources res) {
        // load the origial Bitmap
        Bitmap BitmapOrg = BitmapFactory.decodeResource(res, resId);
        int width = BitmapOrg.getWidth();
        int height = BitmapOrg.getHeight();
        int newWidth = w;
        int newHeight = h;
        // calculate the scale
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // create a matrix for the manipulation
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(BitmapOrg, 0, 0, width,
                height, matrix, true);
        return new BitmapDrawable(resizedBitmap);
    }



    public static boolean isNetworkConnectionAvailable(Activity activity) {
        ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info == null) return false;
        NetworkInfo.State network = info.getState();
        return (network == NetworkInfo.State.CONNECTED || network == NetworkInfo.State.CONNECTING);
    }
}
