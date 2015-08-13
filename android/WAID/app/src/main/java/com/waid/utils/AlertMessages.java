package com.waid.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.Toast;

import com.waid.R;

/**
 * Created by kodjobaah on 20/07/2015.
 */
public class AlertMessages {

    public static void displayGenericMessageDialog(
            Activity activity, String message) {
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
    }
}
