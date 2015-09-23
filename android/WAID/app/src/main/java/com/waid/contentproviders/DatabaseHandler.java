package com.waid.contentproviders;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.NetworkInfo;
import android.util.Log;

public class DatabaseHandler extends SQLiteOpenHelper {

    private static DatabaseHandler singleton;

    public static DatabaseHandler getInstance(final Context context) {
        if (singleton == null) {
            singleton = new DatabaseHandler(context);
        }
        return singleton;
    }

    private static final String TAG = "DatabaseHandler";
    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "whatamidoing";

    private final Context context;

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        // Good idea to use process context here
        this.context = context.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        Log.i(TAG, "---------------------should be creating table");
        db.execSQL(Authentication.CREATE_TABLE);
        db.execSQL(TwitterAuthenticationToken.CREATE_TABLE);
        db.execSQL(LinkedInAuthenticationToken.CREATE_TABLE);
        db.execSQL(StateAttribute.CREATE_TABLE);
        db.execSQL(StreamToken.CREATE_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
        // TODO Auto-generated method stub

    }

    public synchronized StateAttribute getStateAttribute(String attribute) {

        final SQLiteDatabase db = this.getReadableDatabase();
        StateAttribute sa = null;

        try {
            final Cursor cursor = db.query(StateAttribute.TABLE_NAME,
                    StateAttribute.FIELDS,
                    StateAttribute.COL_STATE_ATTRIBUTE + "=?",
                    new String[]{attribute}, null, null, null, null);

            if ((cursor != null) && (cursor.moveToFirst()) && !cursor.isBeforeFirst() && !cursor.isAfterLast()) {
                sa = new StateAttribute(cursor);
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG,"APPINIT_DATABASE_HANDLER_getStateAttribute["+e.getMessage()+"]");

            if (e.getMessage().contains("no such table")) {
              final SQLiteDatabase wdb = this.getWritableDatabase();
                wdb.execSQL(StateAttribute.CREATE_TABLE);
            }
        }

        return sa;

    }


    public synchronized StreamToken getDefaultStreamToken() {

        final SQLiteDatabase db = this.getReadableDatabase();
        StreamToken item = null;

        Cursor cursor = db.query(StreamToken.TABLE_NAME,
                StreamToken.FIELDS, null, null, null, null, null);
        if (cursor == null || cursor.isAfterLast()) {
            return null;
        }

        if (cursor.moveToFirst()) {
            item = new StreamToken(cursor);
        }
        cursor.close();

        return item;
    }


    public synchronized TwitterAuthenticationToken getDefaultTwitterAuthentication() {

        final SQLiteDatabase db = this.getReadableDatabase();
        TwitterAuthenticationToken item = null;

        Cursor cursor = db.query(TwitterAuthenticationToken.TABLE_NAME,
                TwitterAuthenticationToken.FIELDS, null, null, null, null, null);
        if (cursor == null || cursor.isAfterLast()) {
            return null;
        }

        if (cursor.moveToFirst()) {
            item = new TwitterAuthenticationToken(cursor);
        }
        cursor.close();

        return item;
    }

    public synchronized LinkedInAuthenticationToken getDefaultLinkedinAuthentication() {

        final SQLiteDatabase db = this.getReadableDatabase();
        LinkedInAuthenticationToken item = null;

        Cursor cursor = db.query(LinkedInAuthenticationToken.TABLE_NAME,
                LinkedInAuthenticationToken.FIELDS, null, null, null, null, null);
        if (cursor == null || cursor.isAfterLast()) {
            return null;
        }

        if (cursor.moveToFirst()) {
            item = new LinkedInAuthenticationToken(cursor);
        }
        cursor.close();

        return item;
    }

    public synchronized Authentication getDefaultAuthentication() {

        final SQLiteDatabase db = this.getReadableDatabase();
        Authentication item = null;

        Cursor cursor = db.query(Authentication.TABLE_NAME,
                Authentication.FIELDS, null, null, null, null, null);
        if (cursor == null || cursor.isAfterLast()) {
            return null;
        }

        if (cursor.moveToFirst()) {
            item = new Authentication(cursor);
        }
        cursor.close();

        return item;
    }


    public synchronized LinkedInAuthenticationToken getAuthenticationLinkedin(final String id) {

        final SQLiteDatabase db = this.getReadableDatabase();
        final Cursor cursor = db.query(LinkedInAuthenticationToken.TABLE_NAME,
                LinkedInAuthenticationToken.FIELDS, TwitterAuthenticationToken.COL_ID + " IS ?",
                new String[]{String.valueOf(id)}, null, null, null, null);

        LinkedInAuthenticationToken item = null;
        if ((cursor != null) && !cursor.isBeforeFirst() && !cursor.isAfterLast()) {
            item = new LinkedInAuthenticationToken(cursor);
            cursor.close();
        }


        return item;

    }

    public synchronized TwitterAuthenticationToken getAuthenticationTwitter(final String id) {

        final SQLiteDatabase db = this.getReadableDatabase();
        final Cursor cursor = db.query(TwitterAuthenticationToken.TABLE_NAME,
                TwitterAuthenticationToken.FIELDS, TwitterAuthenticationToken.COL_ID + " IS ?",
                new String[]{String.valueOf(id)}, null, null, null, null);

        TwitterAuthenticationToken item = null;
        if ((cursor != null) && !cursor.isBeforeFirst() && !cursor.isAfterLast()) {
            item = new TwitterAuthenticationToken(cursor);
            cursor.close();
        }


        return item;

    }

    public synchronized Authentication getAuthentication(final String id) {

        final SQLiteDatabase db = this.getReadableDatabase();
        final Cursor cursor = db.query(Authentication.TABLE_NAME,
                Authentication.FIELDS, Authentication.COL_ID + " IS ?",
                new String[]{String.valueOf(id)}, null, null, null, null);

        Authentication item = null;
        if ((cursor != null) && !cursor.isBeforeFirst() && !cursor.isAfterLast()) {
            item = new Authentication(cursor);
            cursor.close();
        }


        return item;

    }

    public synchronized boolean putStreamToken(final StreamToken streamToken) {
        boolean success = false;
        int result = 0;
        final SQLiteDatabase db = this.getWritableDatabase();

        if (streamToken.getId() != null) {
            result += db.update(StreamToken.TABLE_NAME, streamToken.getContent(),
                    StreamToken.COL_ID + "  =?",
                    new String[]{streamToken.getId()});

        }

        if (result > 0) {

            success = true;
        } else {

            // Update failed or wasn't possible, insert instead

            final long id = db.insert(StreamToken.TABLE_NAME, null,
                    streamToken.getContent());

            if (id > -1) {

                db.close();
                success = true;
            }
        }

        return success;
    }


    public synchronized boolean putStateAttribute(final StateAttribute sa) {
        boolean success = false;
        int result = 0;
        final SQLiteDatabase db = this.getWritableDatabase();

        if (sa.getId() != null) {
            result += db.update(StateAttribute.TABLE_NAME, sa.getContent(),
                    StateAttribute.COL_ID + "  =?",
                    new String[]{sa.getId()});

        }

        if (result > 0) {

            success = true;
        } else {

            // Update failed or wasn't possible, insert instead

            final long id = db.insert(StateAttribute.TABLE_NAME, null,
                    sa.getContent());

            if (id > -1) {

                db.close();
                success = true;
            }
        }

        return success;
    }

    public synchronized boolean putAuthentication(final Authentication auth) {
        boolean success = false;
        int result = 0;
        final SQLiteDatabase db = this.getWritableDatabase();


        if (auth.getId() != null) {
            result += db.update(Authentication.TABLE_NAME, auth.getContent(),
                    Authentication.COL_ID + " IS ?",
                    new String[]{auth.getId()});

        }

        if (result > 0) {

            success = true;
        } else {

            // Update failed or wasn't possible, insert instead

            final long id = db.insert(Authentication.TABLE_NAME, null,
                    auth.getContent());

            if (id > -1) {

                db.close();
                success = true;
            }
        }

        return success;
    }

    public synchronized boolean putAuthentication(final TwitterAuthenticationToken auth) {
        boolean success = false;
        int result = 0;
        final SQLiteDatabase db = this.getWritableDatabase();


        if (auth.getId() != null) {
            result += db.update(TwitterAuthenticationToken.TABLE_NAME, auth.getContent(),
                    TwitterAuthenticationToken.COL_ID + " IS ?",
                    new String[]{auth.getId()});

        }

        if (result > 0) {

            success = true;
        } else {

            // Update failed or wasn't possible, insert instead

            final long id = db.insert(TwitterAuthenticationToken.TABLE_NAME, null,
                    auth.getContent());

            if (id > -1) {

                db.close();
                success = true;
            }
        }

        return success;
    }

    public synchronized boolean putAuthentication(final LinkedInAuthenticationToken auth) {
        boolean success = false;
        int result = 0;
        final SQLiteDatabase db = this.getWritableDatabase();


        if (auth.getId() != null) {
            result += db.update(LinkedInAuthenticationToken.TABLE_NAME, auth.getContent(),
                    LinkedInAuthenticationToken.COL_ID + " IS ?",
                    new String[]{auth.getId()});

        }

        if (result > 0) {

            success = true;
        } else {

            // Update failed or wasn't possible, insert instead

            final long id = db.insert(LinkedInAuthenticationToken.TABLE_NAME, null,
                    auth.getContent());

            if (id > -1) {

                db.close();
                success = true;
            }
        }

        return success;
    }


    public synchronized int removeStateAttribute(final StateAttribute sa) {

        final SQLiteDatabase db = this.getWritableDatabase();
        final int result = db.delete(StateAttribute.TABLE_NAME,
                StateAttribute.COL_ID + " IS ?", new String[]{sa.getId()});

        return result;
    }

    public synchronized int removeAuthentication(final Authentication auth) {

        final SQLiteDatabase db = this.getWritableDatabase();
        final int result = db.delete(Authentication.TABLE_NAME,
                Authentication.COL_ID + " IS ?", new String[]{auth.getId()});

        return result;
    }


    public synchronized int removeAuthentication(final TwitterAuthenticationToken auth) {

        final SQLiteDatabase db = this.getWritableDatabase();
        final int result = db.delete(TwitterAuthenticationToken.TABLE_NAME,
                TwitterAuthenticationToken.COL_ID + " IS ?", new String[]{auth.getId()});

        return result;
    }


    public synchronized int removeAuthentication(final LinkedInAuthenticationToken auth) {

        final SQLiteDatabase db = this.getWritableDatabase();
        final int result = db.delete(LinkedInAuthenticationToken.TABLE_NAME,
                LinkedInAuthenticationToken.COL_ID + " IS ?", new String[]{auth.getId()});

        return result;
    }

}
