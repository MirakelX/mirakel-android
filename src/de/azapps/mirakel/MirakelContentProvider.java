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

import java.sql.SQLSyntaxErrorException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.dmfs.provider.tasks.TaskContract;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.task.Task;

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
		boolean isTask = false;
		switch (uriMatcher.match(uri)) {
		case LIST_ID:
			sqlBuilder.appendWhere(DatabaseHelper.ID + "=" + getId(uri));
		case LISTS:
			sqlBuilder.setTables(ListMirakel.TABLE);
			break;
		case TASK_ID:
			sqlBuilder.appendWhere(DatabaseHelper.ID + "=" + getId(uri));
		case TASKS:
			String taskQuery = getTaskQuery();
			if (selection.contains(TaskContract.Tasks.LIST_ID)) {
				String[] t = selection.split(TaskContract.Tasks.LIST_ID);
//				android.os.Debug.waitForDebugger();
				if (t[1].trim().charAt(0) == '=') {
					t[1] = t[1].trim().substring(1);
					int list_id = 0;
					if (t[1].trim().charAt(0) == '?') {
						int count = StringUtils.countMatches(t[0], "?");
						list_id = Integer.parseInt(selectionArgs[count]);
					} else {
						try {
							boolean negative = t[1].trim().charAt(0) == '-';
							Matcher matcher = Pattern.compile("\\d+").matcher(
									t[1]);
							matcher.find();
							list_id = (negative ? -1 : 1)
									* Integer.valueOf(matcher.group());
						} catch (Exception e) {
							Log.e(TAG, "cannot parse list_id");
							return new MatrixCursor(projection);
						}
					}
					if (list_id < 0) {// is special list...
						SpecialList s = SpecialList
								.getSpecialList(-1 * list_id);
						if (s != null) {
							taskQuery = getTaskQuery(true, list_id) + " WHERE "
									+ s.getWhereQuery(true);
						} else {
							Log.e(TAG, "no matching list found");
							return new MatrixCursor(projection);
						}
					}

				}
			}
			selection=insertSelectionArgs(selection,selectionArgs);
			sqlBuilder.setTables("(" + taskQuery + ")");
			String query= sqlBuilder.buildQuery(projection, selection, null, null,
					sortOrder, null);
			Log.d(TAG,query);
			return database.rawQuery(query, null);

			// sqlBuilder.setTables(Task.TABLE);
			// isTask=true;
			// break;
		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
		return null;
	}

	private String insertSelectionArgs(String selection, String[] selectionArgs) {
		for(int i=0;i<selectionArgs.length;i++){
			selection=selection.replace("?", selectionArgs[i]);
		}
		return selection;
	}

	private String getTaskQuery() {
		return getTaskQuery(false, 0);
	}

	private String getTaskQuery(boolean isSpecial, int list_id) {
		String query = "Select ";
		query += addSegment(DatabaseHelper.NAME, TaskContract.Tasks.TITLE,
				false);
		query += addSegment(Task.CONTENT, TaskContract.Tasks.DESCRIPTION, true);
		query += addSegment(Task.PRIORITY, TaskContract.Tasks.PRIORITY, true);
		query += addSegment("strftime('%s'," + Task.DUE + ")",
				TaskContract.Tasks.DUE, true);
		query += addSegment(Task.DONE, TaskContract.Tasks.STATUS, true);
		if (isSpecial) {
			query += addSegment("CASE " + Task.LIST_ID + " WHEN 1 THEN "
					+ list_id + " ELSE " + list_id + " END",
					TaskContract.Tasks.LIST_ID, true);
		} else {
			query += addSegment(Task.LIST_ID, TaskContract.Tasks.LIST_ID, true);
		}
		query += " FROM " + Task.TABLE;
		Log.d(TAG, query);
		return query;
	}

	private String addSegment(String ownName, String remoteName, boolean comma) {
		return (comma ? " , " : " ") + ownName + " as " + remoteName;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO implement this
		return 0;
	}

}
