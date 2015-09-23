package com.waid.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.ImageButton;
import android.widget.Toast;

import com.waids.R;

/**
 * Created by kodjobaah on 20/07/2015.
 */
public class AlertMessages {

    public static void displayGenericMessageDialog(
            final Activity activity, final String message) {

        activity.runOnUiThread(new Thread(new Runnable() {
            public void run() {

                Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
            }
        }));

    }
}
