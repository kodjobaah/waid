package com.waid.contentproviders;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class StreamTokenProvider extends ContentProvider {

	 // All URIs share these parts
    public static final String AUTHORITY = "com.whatamidoing.provider";
    public static final String SCHEME = "content://";

    // URIs
    // Used for all persons
    public static final String STREAM_TOKEN = SCHEME + AUTHORITY + "/streamToken";
    public static final Uri URI_STREAM_TOKEN = Uri.parse(STREAM_TOKEN);
    // Used for a single person, just add the id to the end
    public static final String STREAM_TOKEN_BASE = STREAM_TOKEN + "/";

    public StreamTokenProvider() {
    }

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Implement this to handle requests to delete one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public String getType(Uri uri) {
		 // TODO: Implement this to handle requests for the MIME type of the data
        // at the given URI.
        throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO: Implement this to handle requests to insert a new row.
        throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public boolean onCreate() {

		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		Cursor result = null;
        if (URI_STREAM_TOKEN.equals(uri)) {
            result = DatabaseHandler
                    .getInstance(getContext())
                    .getReadableDatabase()
                    .query(StreamToken.TABLE_NAME, StreamToken.FIELDS, null, null, null,
                            null, null, null);
            result.setNotificationUri(getContext().getContentResolver(), URI_STREAM_TOKEN);
        } else if (uri.toString().startsWith(STREAM_TOKEN_BASE)) {
            final String id = uri.getLastPathSegment();
            result = DatabaseHandler
                    .getInstance(getContext())
                    .getReadableDatabase()
                    .query(StreamToken.TABLE_NAME, StateAttribute.FIELDS,
                            StreamToken.COL_ID + " IS ?",
                            new String[] {id }, null, null,
                            null, null);
             result.setNotificationUri(getContext().getContentResolver(), URI_STREAM_TOKEN);
        } else {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        return result;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO: Implement this to handle requests to update one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
	}

}
