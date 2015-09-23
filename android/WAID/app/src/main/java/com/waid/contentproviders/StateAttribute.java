package com.waid.contentproviders;

import android.content.ContentValues;
import android.database.Cursor;

public class StateAttribute {

	public static final String OPEN_BROWSER = "open_browser";

    public static final String ON_PAUSE = "onPause";

	public static final String TABLE_NAME = "StateAttribute";

	public static final String COL_ID = "_id";

	public static final String COL_STATE_ATTRIBUTE = "attribute";

	public static final String COL_STATE_ATTRIBUTE_VALUE = "value";


	//For database projects so order is consistent
	public static final String[] FIELDS = {COL_ID, COL_STATE_ATTRIBUTE,COL_STATE_ATTRIBUTE_VALUE};

	/*
	 * The sql code that creates  Table for storing Authentications.
	 *
	 */
	public static final String CREATE_TABLE =
			"CREATE TABLE " + TABLE_NAME + "("
			+ COL_ID + " TEXT PRIMARY KEY,"
			+ COL_STATE_ATTRIBUTE + " TEXT NOT NULL,"
			+ COL_STATE_ATTRIBUTE_VALUE + " TEXT NOT NULL"
			+ ")";

	private String id;

	private String attribute;

	private String value;



	public StateAttribute(String id, String attribute, String value) {
		this.id = id;
		this.attribute = attribute;
		this.value = value;
	}

	public StateAttribute(final Cursor cursor) {
		this.id = cursor.getString(0);
		this.attribute = cursor.getString(1);
		this.value = cursor.getString(2);
	}


	public ContentValues getContent() {
		final ContentValues values = new ContentValues();
		values.put(COL_ID,id);
		values.put(COL_STATE_ATTRIBUTE, attribute);
		values.put(COL_STATE_ATTRIBUTE_VALUE, value);
		return values;
	}

	public String getId() {
		return id;
	}



	public void setId(String id) {
		this.id = id;
	}



	public String getAttribute() {
		return attribute;
	}



	public void setValue(String value) {
		this.value = value;
	}



	public String getValue() {
		return value;
	}


    @Override
    public String toString() {
        return "StateAttribute{" +
                "id='" + id + '\'' +
                ", attribute='" + attribute + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

    public void setAttribute(String attribute) {
		this.attribute = attribute;
	}
}
