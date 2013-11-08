/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013 Anatolij Zelenin, Georg Semmler.
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.azapps.mirakel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import org.dmfs.provider.tasks.TaskContract;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MatrixCursor.RowBuilder;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.net.rtp.RtpStream;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.sync.SyncAdapter.SYNC_STATE;

public class MirakelContentProvider extends ContentProvider {
	// public static final String PROVIDER_NAME = Mirakel.AUTHORITY_TYP;
	// public static final Uri CONTENT_URI = Uri.parse("content://" +
	// PROVIDER_NAME);
	private SQLiteDatabase database;
	private static final UriMatcher uriMatcher;
	private static final int LISTS = 0;
	private static final int LIST_ID = 1;
	private static final int TASKS = 2;
	private static final int TASK_ID = 3;

	private static final String TAG = "MirakelContentProvider";
	// TODO for what we will need this?
	private static final int INSTANCE_ID = 4;
	private static final int INSTANCES = 5;
	private static final int CATEGORIES = 6;
	private static final int CATEGORY_ID = 7;

	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(TaskContract.AUTHORITY,
				TaskContract.TaskLists.CONTENT_URI_PATH, LISTS);

		uriMatcher.addURI(TaskContract.AUTHORITY,
				TaskContract.TaskLists.CONTENT_URI_PATH + "/#", LIST_ID);

		uriMatcher.addURI(TaskContract.AUTHORITY,
				TaskContract.Tasks.CONTENT_URI_PATH, TASKS);
		uriMatcher.addURI(TaskContract.AUTHORITY,
				TaskContract.Tasks.CONTENT_URI_PATH + "/#", TASK_ID);

		uriMatcher.addURI(TaskContract.AUTHORITY,
				TaskContract.Instances.CONTENT_URI_PATH, INSTANCES);
		uriMatcher.addURI(TaskContract.AUTHORITY,
				TaskContract.Instances.CONTENT_URI_PATH + "/#", INSTANCE_ID);

		uriMatcher.addURI(TaskContract.AUTHORITY,
				TaskContract.Categories.CONTENT_URI_PATH, CATEGORIES);
		uriMatcher.addURI(TaskContract.AUTHORITY,
				TaskContract.Categories.CONTENT_URI_PATH + "/#", CATEGORY_ID);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO implement
		return 0;
	}

	protected boolean getIsCallerSyncAdapter(Uri uri) {
		String param = uri
				.getQueryParameter(TaskContract.CALLER_IS_SYNCADAPTER);
		return param != null && !"false".equals(param);
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case LISTS:
			return TaskContract.TaskLists.CONTENT_TYPE;
		case LIST_ID:
			return TaskContract.TaskLists.CONTENT_ITEM_TYPE;
		case TASKS:
			return TaskContract.Tasks.CONTENT_TYPE;
		case TASK_ID:
			return TaskContract.Tasks.CONTENT_ITEM_TYPE;
		case INSTANCES:
			return TaskContract.Instances.CONTENT_TYPE;
		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO implement
		return null;
	}

	@Override
	public boolean onCreate() {
		database = new DatabaseHelper(getContext()).getWritableDatabase();
		return database == null;
	}

	private String getId(Uri uri) {
		return uri.getPathSegments().get(1);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
		boolean isTask=false;
		switch (uriMatcher.match(uri)) {
		case LIST_ID:
			sqlBuilder.appendWhere(DatabaseHelper.ID+"=" + getId(uri));
		case LISTS:
			sqlBuilder.setTables(ListMirakel.TABLE);
			break;
		case TASK_ID:
			sqlBuilder.appendWhere(DatabaseHelper.ID+"=" + getId(uri));
		case TASKS:
			sqlBuilder.setTables(Task.TABLE);
			isTask=true;
			break;
		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
		boolean isSyncAdapter = getIsCallerSyncAdapter(uri);
		String[] newProjection = transformProjection(projection, sqlBuilder,
				isSyncAdapter);
		String where = transformWhere(selection, selectionArgs, isTask);
		String newSortOrder=transformSortOrder(sortOrder, isTask);
		Cursor c;
		try {
			String s = sqlBuilder.buildQuery( newProjection, where, null, null,
					newSortOrder, null);
			Log.i(TAG,s);
			c=database.rawQuery(s,null);
		} catch (Exception e) {
			Log.d(TAG, "cannot execute query");
			Log.v(TAG, where);
			Log.v(TAG, newSortOrder);
			e.printStackTrace();
			return new MatrixCursor(projection);
		}
		return transfromCursor(c, newProjection, projection, isTask);
	}

	private Cursor transfromCursor(Cursor fromDB, String[] newProjection,
			String[] projection, boolean isTask) {
		fromDB.moveToFirst();
		MatrixCursor c = new MatrixCursor(projection);
		while (!fromDB.isAfterLast()) {
			RowBuilder builder = c.newRow();
			builder = buildRow(builder, fromDB, projection, newProjection, isTask);
			fromDB.moveToNext();
		}
		fromDB.close();
		return c;
	}

	private RowBuilder buildRow(RowBuilder builder, Cursor c,
			String[] projection, String[] newProjection, boolean isTask) {
		for (int i = 0; i < newProjection.length; i++) {
			builder=builder.add(getColoumnIndex(newProjection[i], isTask),
					getValue(c, newProjection[i], i,isTask));
		}
		return builder;
	}

	private Object getValue(Cursor c, String newProjection, int i,boolean isTask) {
		if (isTask) {
			if (newProjection.equals(Task.PRIORITY)||newProjection.equals(Task.LIST_ID)) {
				return c.getInt(i);
			} else if (newProjection.equals(DatabaseHelper.NAME)||newProjection.equals(Task.CONTENT)) {
				return c.getString(i);
			} else if (newProjection.equals(Task.DONE)) {
				return c.getInt(i)==0?TaskContract.Tasks.STATUS_NEEDS_ACTION:TaskContract.Tasks.STATUS_COMPLETED;
			} else if (newProjection.equals(Task.DUE)) {
				GregorianCalendar due = new GregorianCalendar();
				SimpleDateFormat dateTimeFormat = new SimpleDateFormat(
						"yyyy-MM-dd'T'kkmmss'Z'", Locale.getDefault());
				try {
					due.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
							.parse(c.getString(i)));
				} catch (ParseException e) {
					due = null;
				} catch (NullPointerException e) {
					due = null;
				}
				return due.getTimeInMillis();
			}
		}
		throw new RuntimeException();
	}

	private String getColoumnIndex(String newProjection, boolean isTask) {
		if (isTask) {
			if (newProjection.equals(Task.PRIORITY)) {
				return TaskContract.Tasks.PRIORITY;
			} else if (newProjection.equals(DatabaseHelper.NAME)) {
				return TaskContract.Tasks.TITLE;
			} else if (newProjection.equals(Task.CONTENT)) {
				return TaskContract.Tasks.DESCRIPTION;
			} else if (newProjection.equals(Task.DONE)) {
				return TaskContract.Tasks.STATUS;
			} else if (newProjection.equals(Task.LIST_ID)) {
				return TaskContract.Tasks.LIST_ID;
			} else if(newProjection.equals(Task.DUE)){
				return TaskContract.Tasks.DUE;
			}
		}
		throw new RuntimeException();
	}

	private String transformWhere(String selection, String[] selectionArgs,
			boolean isTask) {
		if(isTask){
			Log.i(TAG, selection);
			selection=selection.replaceAll(TaskContract.Tasks.PRIORITY, Task.PRIORITY);
			selection=selection.replaceAll(TaskContract.Tasks.TITLE, DatabaseHelper.NAME);
			selection=selection.replaceAll(TaskContract.Tasks.DESCRIPTION, Task.CONTENT);
			selection=selection.replaceAll(TaskContract.Tasks.STATUS, Task.DONE);
			selection=selection.replaceAll(TaskContract.Tasks.LIST_ID, Task.LIST_ID);
			//TODO due,status
		}
		return selection;
	}
	
	private String transformSortOrder(String selection,
			boolean isTask) {
		if(isTask){
			Log.i(TAG, selection);
			selection=selection.replaceAll(TaskContract.Tasks.PRIORITY, Task.PRIORITY);
			selection=selection.replaceAll(TaskContract.Tasks.TITLE, DatabaseHelper.NAME);
			selection=selection.replaceAll(TaskContract.Tasks.DESCRIPTION, Task.CONTENT);
			selection=selection.replaceAll(TaskContract.Tasks.LIST_ID, Task.LIST_ID);
			selection=selection.replaceAll(TaskContract.Tasks.DUE, Task.DUE);
		}
		return selection;
	}

	private String[] transformProjection(String[] projection,
			SQLiteQueryBuilder sqlBuilder, boolean isSyncAdapter) {
		List<String> newProjection = new ArrayList<String>();
		for (String p : projection) {
			if (sqlBuilder.getTables().equals(Task.TABLE)) {
				if (p.equals(TaskContract.Tasks.PRIORITY)) {
					newProjection.add(Task.PRIORITY);
				} else if (p.equals(TaskContract.Tasks.TITLE)) {
					newProjection.add(DatabaseHelper.NAME);
				} else if (p.equals(TaskContract.Tasks.DESCRIPTION)) {
					newProjection.add(Task.CONTENT);
				} else if (p.equals(TaskContract.Tasks.STATUS)) {
					newProjection.add(Task.DONE);
				} else if (p.equals(TaskContract.Tasks.DUE)) {
					newProjection.add(Task.DUE);
				} else if (p.equals(TaskContract.Tasks.LIST_NAME)
						|| p.equals(TaskContract.Tasks.LIST_ID)) {
					newProjection.add(Task.LIST_ID);
				} else if (isSyncAdapter) {

				}
			}
		}
		String [] t={};
		return newProjection.toArray(t);
	}

	private boolean isEqual(String p, String s, boolean task) {
		return p.equals(s) && task;
	}

	private CharSequence getAnd(boolean andNeeded) {
		return andNeeded ? " and " : "";
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO implement this
		return 0;
	}

}
