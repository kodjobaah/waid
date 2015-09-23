package com.waid.contentproviders;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.UUID;

public class StreamToken {

	public static final String TABLE_NAME = "StreamToken";

	public static final String COL_ID = "_id";

	public static final String COL_TOKEN = "token";


	//For database projects so order is consistent
	public static final String[] FIELDS = {COL_ID, COL_TOKEN};

	/*
	 * The sql code that creates  Table for storing Authentications.
	 *
	 */
	public static final String CREATE_TABLE =
			"CREATE TABLE " + TABLE_NAME + "("
			+ COL_ID + " TEXT PRIMARY KEY,"
			+ COL_TOKEN + " TEXT NOT NULL"
			+ ")";

	private String id;

	private String token;

	public StreamToken(String token) {
		this.id = UUID.randomUUID().toString();
		this.token = token;
	}

	public StreamToken(final Cursor cursor) {
		this.id = cursor.getString(0);
		this.token = cursor.getString(1);
	}

	public ContentValues getContent() {
		final ContentValues values = new ContentValues();
		values.put(COL_ID,id);
		values.put(COL_TOKEN,token);
		return values;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getToken() {
		return token;
	}

    @Override
    public String toString() {
        return "StateAttribute{" +
                "id='" + id + '\'' +
                ", token='" + token + '\'' +
                '}';
    }

    public void setToken(String token) {
		this.token = token;
	}
}
