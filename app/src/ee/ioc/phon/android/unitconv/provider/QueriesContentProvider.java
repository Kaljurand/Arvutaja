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

public class QueriesContentProvider extends ContentProvider {

	public static final String QUERIES_TABLE_NAME = "queries";

	private static final String TAG = "QueriesContentProvider";

	private static final String DATABASE_NAME = "unitconv.db";

	private static final int DATABASE_VERSION = 3;
	
	private static final String UNKNOWN_URI = "Unknown URI: ";

	public static final String AUTHORITY = "ee.ioc.phon.android.unitconv.provider.QueriesContentProvider";

	private static final UriMatcher sUriMatcher;

	private static final int QUERIES = 1;
	private static final int QUERY_ID = 2;

	private static HashMap<String, String> grammarsProjectionMap;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		private final Context mContext;

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			mContext = context;
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + QUERIES_TABLE_NAME + " ("
					+ Query.Columns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ Query.Columns.TIMESTAMP + " TEXT,"
					+ Query.Columns.UTTERANCE + " TEXT,"
					+ Query.Columns.TRANSLATION + " TEXT,"
					+ Query.Columns.EVALUATION + " TEXT"
					+ ");");
			
			db.execSQL("INSERT INTO " + QUERIES_TABLE_NAME + " VALUES (" +
					"'1', " +
					"'20111014:1904', " +
					"'kaks minutit sekundites', " +
					"'2 min IN sec', " +
					"'120'" +
			");");
			
			db.execSQL("INSERT INTO " + QUERIES_TABLE_NAME + " VALUES (" +
					"'2', " +
					"'20111014:1904', " +
					"'kaks minutit sekundites', " +
					"'2 angmin IN angsec', " +
					"'ang120'" +
			");");
		}


		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database v" + oldVersion + " -> v" + newVersion + ", which will destroy all old data.");
			db.execSQL("DROP TABLE IF EXISTS " + QUERIES_TABLE_NAME);
			onCreate(db);
		}
	}

	private DatabaseHelper dbHelper;

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)) {
		case QUERIES:
			count = db.delete(QUERIES_TABLE_NAME, where, whereArgs);
			break;

		case QUERY_ID:
			String grammarId = uri.getPathSegments().get(1);
			count = db.delete(
					QUERIES_TABLE_NAME,
					Query.Columns._ID + "=" + grammarId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
					whereArgs);
			break;

		default:
			throw new IllegalArgumentException(UNKNOWN_URI + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}


	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
		case QUERIES:
			return Query.Columns.CONTENT_TYPE;

		default:
			throw new IllegalArgumentException(UNKNOWN_URI + uri);
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
		case QUERIES:
			rowId = db.insert(QUERIES_TABLE_NAME, Query.Columns.EVALUATION, values);
			if (rowId <= 0) {
				throw new SQLException("Failed to insert row into " + uri);
			}
			returnUri = ContentUris.withAppendedId(Query.Columns.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(returnUri, null);
			return returnUri;

		default:
			throw new IllegalArgumentException(UNKNOWN_URI + uri);
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
		case QUERIES:
			qb.setTables(QUERIES_TABLE_NAME);
			qb.setProjectionMap(grammarsProjectionMap);
			break;

		default:
			throw new IllegalArgumentException(UNKNOWN_URI + uri);
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
		case QUERIES:
			count = db.update(QUERIES_TABLE_NAME, values, where, whereArgs);
			break;

		case QUERY_ID:
			String grammarId = uri.getPathSegments().get(1);
			count = db.update(
					QUERIES_TABLE_NAME,
					values,
					Query.Columns._ID + "=" + grammarId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
					whereArgs);
			break;

		default:
			throw new IllegalArgumentException(UNKNOWN_URI + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(AUTHORITY, QUERIES_TABLE_NAME, QUERIES);
		sUriMatcher.addURI(AUTHORITY, QUERIES_TABLE_NAME + "/#", QUERY_ID);

		grammarsProjectionMap = new HashMap<String, String>();
		grammarsProjectionMap.put(Query.Columns._ID, Query.Columns._ID);
		grammarsProjectionMap.put(Query.Columns.TIMESTAMP, Query.Columns.TIMESTAMP);
		grammarsProjectionMap.put(Query.Columns.UTTERANCE, Query.Columns.UTTERANCE);
		grammarsProjectionMap.put(Query.Columns.TRANSLATION, Query.Columns.TRANSLATION);
		grammarsProjectionMap.put(Query.Columns.EVALUATION, Query.Columns.EVALUATION);

	}
}