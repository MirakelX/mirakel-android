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

import java.util.List;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
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
	private static final UriMatcher uriMatcher;
	private static final int LISTS = 0;
	private static final int LISTS_ITEM = 1;
	private static final int TASKS = 2;
	private static final int TASKS_ITEM = 3;
	private static final int SPECIAL_LIST = 4;
	private static final int SPECIAL_LIST_ITEM = 5;
	private static final String TAG = "MirakelContentProvider";
	//
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(Mirakel.AUTHORITY_TYP, ListMirakel.TABLE, LISTS);
		uriMatcher.addURI(Mirakel.AUTHORITY_TYP, ListMirakel.TABLE + "/#",
				LISTS_ITEM);
		uriMatcher.addURI(Mirakel.AUTHORITY_TYP, Task.TABLE, TASKS);
		uriMatcher.addURI(Mirakel.AUTHORITY_TYP, Task.TABLE + "/#", TASKS_ITEM);
		uriMatcher.addURI(Mirakel.AUTHORITY_TYP, SpecialList.TABLE,
				SPECIAL_LIST);
		uriMatcher.addURI(Mirakel.AUTHORITY_TYP, SpecialList.TABLE + "/#",
				SPECIAL_LIST_ITEM);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		switch (uriMatcher.match(uri)) {
		case SPECIAL_LIST:
		case LISTS:
			Log.d(TAG, "DELETE ALL LISTS?!!");
			List<ListMirakel> lists = ListMirakel.all();
			for (ListMirakel list : lists) {
				if (list.getId() > 0) {
					list.destroy();
				}
			}
			return lists.size();
		case SPECIAL_LIST_ITEM:
		case LISTS_ITEM:
			int list_id = 0;
			Log.d(TAG, "DELETE LIST " + list_id);
			ListMirakel list = ListMirakel.getList(Integer.parseInt(uri
					.getLastPathSegment()));
			if (list.getId() > 0) {
				list.destroy();
				return 1;
			}
			return 0;
		case TASKS:
			Log.d(TAG, "DELETE ALL TASKS?!!");
			List<Task> tasks = Task.all();
			for (Task t : tasks) {
				t.destroy();
			}
			return tasks.size();
		case TASKS_ITEM:
			int task_id = 0;
			Log.d(TAG, "DELETE TASK " + task_id);
			Task task = Task.get(Long.parseLong(uri.getLastPathSegment()));
			if (task != null) {
				task.destroy();
				return 1;
			}
			return 0;
		default:
			Log.wtf(TAG, "Unsupportet uri");
			break;
		}
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case LISTS:
		case SPECIAL_LIST:
			return "LISTS";
		case LISTS_ITEM:
		case SPECIAL_LIST_ITEM:
			return "LIST";
		case TASKS:
			return "TASKS";
		case TASKS_ITEM:
			return "TASK";
		default:
			Log.wtf(TAG, "Unsupportet uri");
			return null;
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		values.put("sync_state", SYNC_STATE.ADD.toInt());
		switch (uriMatcher.match(uri)) {
		case LISTS:
			long id = Mirakel.getWritableDatabase().insert(ListMirakel.TABLE,
					null, values);
			return Uri.parse("content://" + Mirakel.AUTHORITY_TYP + "/"
					+ ListMirakel.TABLE + "/" + id);
		case LISTS_ITEM:
		case SPECIAL_LIST_ITEM:
		case TASKS_ITEM:
			return null;
		case TASKS:
			id = Mirakel.getWritableDatabase().insert(Task.TABLE, null, values);
			return Uri.parse("content://" + Mirakel.AUTHORITY_TYP + "/"
					+ Task.TABLE + "/" + id);
		case SPECIAL_LIST:
			id = Mirakel.getWritableDatabase().insert(SpecialList.TABLE, null,
					values);
			return Uri.parse("content://" + Mirakel.AUTHORITY_TYP + "/"
					+ SpecialList.TABLE + "/" + id);
		default:
			Log.wtf(TAG, "Unsupportet uri");
		}
		return null;
	}

	@Override
	public boolean onCreate() {
		@SuppressWarnings("unused")
		SQLiteOpenHelper openhelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		switch (uriMatcher.match(uri)) {
		case LISTS:
			return Mirakel.getReadableDatabase().query(
					ListMirakel.TABLE,
					projection,
					"(" + selection + " ) and not sync_state= "
							+ SYNC_STATE.DELETE, selectionArgs, null,
					null, sortOrder);
		case LISTS_ITEM:
			return Mirakel.getReadableDatabase().query(
					ListMirakel.TABLE,
					projection,
					"(" + selection + " ) and _id=" + uri.getLastPathSegment()
							+ " and not sync_state= "
							+ SYNC_STATE.DELETE, selectionArgs, null,
					null, sortOrder);
		case TASKS:
			return Mirakel.getReadableDatabase().query(
					Task.TABLE,
					projection,
					"(" + selection + " ) and not sync_state= "
							+ SYNC_STATE.DELETE, selectionArgs, null,
					null, sortOrder);
		case TASKS_ITEM:
			return Mirakel.getReadableDatabase().query(
					Task.TABLE,
					projection,
					"(" + selection + " ) and _id=" + uri.getLastPathSegment()
							+ " and not sync_state= "
							+ SYNC_STATE.DELETE, selectionArgs, null,
					null, sortOrder);
		case SPECIAL_LIST:
			return Mirakel.getReadableDatabase().query(
					SpecialList.TABLE,
					projection,
					"(" + selection + " ) and not sync_state= "
							+ SYNC_STATE.DELETE, selectionArgs, null,
					null, sortOrder);
		case SPECIAL_LIST_ITEM:
			return Mirakel.getReadableDatabase().query(
					SpecialList.TABLE,
					projection,
					"(" + selection + " ) and _id=" + uri.getLastPathSegment()
							+ " and not sync_state= "
							+ SYNC_STATE.DELETE, selectionArgs, null,
					null, sortOrder);
		default:
			Log.wtf(TAG, "Unsupportet uri");
			break;
		}
		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		values.put("sync_state", SYNC_STATE.NEED_SYNC.toInt());
		switch (uriMatcher.match(uri)) {
		case LISTS:
			return Mirakel.getWritableDatabase().update(ListMirakel.TABLE,
					values, selection, selectionArgs);
		case LISTS_ITEM:
			return Mirakel.getWritableDatabase().update(ListMirakel.TABLE,
					values,
					"(" + selection + ") and _id=" + uri.getLastPathSegment(),
					selectionArgs);
		case TASKS:
			return Mirakel.getWritableDatabase().update(Task.TABLE, values,
					selection, selectionArgs);
		case TASKS_ITEM:
			return Mirakel.getWritableDatabase().update(Task.TABLE, values,
					"(" + selection + ") and _id=" + uri.getLastPathSegment(),
					selectionArgs);
		case SPECIAL_LIST:
			return Mirakel.getWritableDatabase().update(SpecialList.TABLE,
					values, selection, selectionArgs);
		case SPECIAL_LIST_ITEM:
			return Mirakel.getWritableDatabase().update(SpecialList.TABLE,
					values,
					"(" + selection + ") and _id=" + uri.getLastPathSegment(),
					selectionArgs);
		default:
			Log.wtf(TAG, "Unsupportet uri");
			break;
		}
		return 0;
	}

}
