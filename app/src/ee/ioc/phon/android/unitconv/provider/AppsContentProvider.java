/*
 * Copyright 2011, Institute of Cybernetics at Tallinn University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ee.ioc.phon.android.unitconv.provider;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class AppsContentProvider extends ContentProvider {

	private static final String TAG = "AppsContentProvider";

	private static final String DATABASE_NAME = "unitconv.db";

	private static final int DATABASE_VERSION = 1;

	private static final String GRAMMARS_TABLE_NAME = "grammars";

	public static final String AUTHORITY = "ee.ioc.phon.android.unitconv.provider.AppsContentProvider";

	private static final UriMatcher sUriMatcher;

	private static final int GRAMMARS = 1;
	private static final int GRAMMAR_ID = 2;

	private static HashMap<String, String> grammarsProjectionMap;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		private final Context mContext;

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			mContext = context;
		}

		/**
		 * GRAMMAR and SERVER should be a foreign keys
		 * Grammar should have language ID
		 */
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + GRAMMARS_TABLE_NAME + " ("
					+ Grammar.Columns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ Grammar.Columns.NAME + " VARCHAR(255),"
					+ Grammar.Columns.DESC + " TEXT,"
					+ Grammar.Columns.LANG + " VARCHAR(255),"
					+ Grammar.Columns.URL + " TEXT NOT NULL,"
					+ "UNIQUE(" + Grammar.Columns.URL + ") ON CONFLICT REPLACE"
					+ ");");

		}


		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database v" + oldVersion + " -> v" + newVersion + ", which will destroy all old data.");
			db.execSQL("DROP TABLE IF EXISTS " + GRAMMARS_TABLE_NAME);
			onCreate(db);
		}
	}

	private DatabaseHelper dbHelper;

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)) {
		case GRAMMARS:
			count = db.delete(GRAMMARS_TABLE_NAME, where, whereArgs);
			break;

		case GRAMMAR_ID:
			String grammarId = uri.getPathSegments().get(1);
			count = db.delete(
					GRAMMARS_TABLE_NAME,
					Grammar.Columns._ID + "=" + grammarId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
					whereArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}


	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
		case GRAMMARS:
			return Grammar.Columns.CONTENT_TYPE;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}


	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}

		SQLiteDatabase db = dbHelper.getWritableDatabase();
		long rowId = 0;
		Uri returnUri = null;

		switch (sUriMatcher.match(uri)) {
		case GRAMMARS:
			rowId = db.insert(GRAMMARS_TABLE_NAME, Grammar.Columns.DESC, values);
			if (rowId <= 0) {
				throw new SQLException("Failed to insert row into " + uri);
			}
			returnUri = ContentUris.withAppendedId(Grammar.Columns.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(returnUri, null);
			return returnUri;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}


	@Override
	public boolean onCreate() {
		dbHelper = new DatabaseHelper(getContext());
		return true;
	}


	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		switch (sUriMatcher.match(uri)) {
		case GRAMMARS:
			qb.setTables(GRAMMARS_TABLE_NAME);
			qb.setProjectionMap(grammarsProjectionMap);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);

		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}


	@Override
	public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)) {
		case GRAMMARS:
			count = db.update(GRAMMARS_TABLE_NAME, values, where, whereArgs);
			break;

		case GRAMMAR_ID:
			String grammarId = uri.getPathSegments().get(1);
			count = db.update(
					GRAMMARS_TABLE_NAME,
					values,
					Grammar.Columns._ID + "=" + grammarId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
					whereArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(AUTHORITY, GRAMMARS_TABLE_NAME, GRAMMARS);
		sUriMatcher.addURI(AUTHORITY, GRAMMARS_TABLE_NAME + "/#", GRAMMAR_ID);

		grammarsProjectionMap = new HashMap<String, String>();
		grammarsProjectionMap.put(Grammar.Columns._ID, Grammar.Columns._ID);
		grammarsProjectionMap.put(Grammar.Columns.NAME, Grammar.Columns.NAME);
		grammarsProjectionMap.put(Grammar.Columns.LANG, Grammar.Columns.LANG);
		grammarsProjectionMap.put(Grammar.Columns.DESC, Grammar.Columns.DESC);
		grammarsProjectionMap.put(Grammar.Columns.URL, Grammar.Columns.URL);

	}
}