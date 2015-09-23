package com.waid.contentproviders;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class StateAttributeProvider extends ContentProvider {

	 // All URIs share these parts
    public static final String AUTHORITY = "com.whatamidoing.provider";
    public static final String SCHEME = "content://";

    // URIs
    // Used for all persons
    public static final String STATE_ATTRIBUTE= SCHEME + AUTHORITY + "/stateAttribute";
    public static final Uri URI_STATE_ATTRIBUTE = Uri.parse(STATE_ATTRIBUTE);
    // Used for a single person, just add the id to the end
    public static final String STATE_ATTRIBUTE_BASE = STATE_ATTRIBUTE + "/";

    public StateAttributeProvider() {
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
        if (URI_STATE_ATTRIBUTE.equals(uri)) {
            result = DatabaseHandler
                    .getInstance(getContext())
                    .getReadableDatabase()
                    .query(StateAttribute.TABLE_NAME, StateAttribute.FIELDS, null, null, null,
                            null, null, null);
            result.setNotificationUri(getContext().getContentResolver(), URI_STATE_ATTRIBUTE);
        } else if (uri.toString().startsWith(STATE_ATTRIBUTE_BASE)) {
            final String id = uri.getLastPathSegment();
            result = DatabaseHandler
                    .getInstance(getContext())
                    .getReadableDatabase()
                    .query(StateAttribute.TABLE_NAME, StateAttribute.FIELDS,
                            StateAttribute.COL_ID + " IS ?",
                            new String[] {id }, null, null,
                            null, null);
             result.setNotificationUri(getContext().getContentResolver(), URI_STATE_ATTRIBUTE);
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
