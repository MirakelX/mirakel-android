/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013-2014 Anatolij Zelenin, Georg Semmler.
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
package de.azapps.mirakel.model;

import java.sql.SQLWarning;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dmfs.provider.tasks.TaskContract;
import org.dmfs.provider.tasks.TaskContract.TaskLists;
import org.dmfs.provider.tasks.TaskContract.Tasks;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.annotation.SuppressLint;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Build;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.sync.SyncAdapter;
import de.azapps.mirakel.sync.SyncAdapter.SYNC_STATE;
import de.azapps.mirakelandroid.R;
import de.azapps.tools.Log;

public class MirakelContentProvider extends ContentProvider implements OnAccountsUpdateListener {
	private static final int		CATEGORIES	= 6;
	private static final int		CATEGORY_ID	= 7;
	// TODO for what we will need this?
	private static final int		INSTANCE_ID	= 0;
	private static final int		INSTANCES	= 1;
	private static final int		LIST_ID		= 6;
	private static final int		LISTS		= 5;

	private static final String		TAG			= "MirakelContentProvider";
	private static final int		TASK_ID		= 3;
	private static final int		TASKS		= 2;
	private static final UriMatcher	uriMatcher;
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

	private static String addSegment(String ownName, String remoteName, boolean comma) {
		return (comma ? " , " : " ") + ownName + " as " + remoteName;
	}

	private static ContentValues convertValues(ContentValues values, boolean isSyncadapter) {
		ContentValues newValues = new ContentValues();
		if (values.containsKey(Tasks.TITLE)) {
			newValues.put(DatabaseHelper.NAME, values.getAsString(Tasks.TITLE));
		}
		if (values.containsKey(TaskLists.LIST_NAME)) {
			newValues.put(DatabaseHelper.NAME, values.getAsString(Tasks.TITLE));
		}
		if (values.containsKey(Tasks.DESCRIPTION)) {
			newValues.put(Task.CONTENT, values.getAsString(Tasks.DESCRIPTION));
		}
		if (values.containsKey(Tasks.DUE)) {
			Date d = new Date(values.getAsLong(Tasks.DUE));
			GregorianCalendar due = new GregorianCalendar();
			d.setTime(values.getAsLong(Tasks.DUE));
			String db = DateTimeHelper.formatDate(due);
			newValues.put(Task.DUE, db);
		}
		if (values.containsKey(Tasks.PRIORITY)) {
			int prio = values.getAsInteger(Tasks.PRIORITY);
			if (isSyncadapter) {
				switch (prio) {
					case 1:
					case 2:
					case 3:
						prio = 2;
						break;
					case 4:
					case 5:
					case 6:
						prio = 1;
						break;
					case 7:
					case 8:
					case 9:
						prio = -1;
						break;
					default:
						prio = 0;
						break;
				}
			} else {
				if (prio > 2) {
					prio = 2;
				} else if (prio < -2) {
					prio = -2;
				}
			}
			newValues.put(Task.PRIORITY, prio);
		}
		if (values.containsKey(Tasks.PERCENT_COMPLETE)) {
			newValues.put(Task.PROGRESS,
					values.getAsInteger(Tasks.PERCENT_COMPLETE));
		}
		if (values.containsKey(Tasks.STATUS)) {
			int status = values.getAsInteger(Tasks.STATUS);
			boolean done = status == Tasks.STATUS_COMPLETED;
			newValues.put(Task.DONE, done);
		}
		if (values.containsKey(Tasks.LIST_ID)) {
			// newValues.put(Task.LIST_ID, values.getAsInteger(Tasks.LIST_ID));
		}

		if (isSyncadapter) {
			if (values.containsKey(Tasks._ID)) {
				newValues
				.put(DatabaseHelper.ID, values.getAsInteger(Tasks._ID));
			}
			if (values.containsKey(TaskLists._ID)) {
				newValues.put(DatabaseHelper.ID,
						values.getAsInteger(TaskLists._ID));
			}
			if (values.containsKey(TaskLists.LIST_COLOR)) {
				newValues.put(ListMirakel.COLOR,
						values.getAsInteger(TaskLists.LIST_COLOR));
			}
			if (values.containsKey(Tasks.CREATED)) {
				Date d = new Date(values.getAsLong(Tasks.CREATED));
				GregorianCalendar due = new GregorianCalendar();
				d.setTime(values.getAsLong(Tasks.CREATED));
				String db = DateTimeHelper.formatDate(due);
				newValues.put(DatabaseHelper.CREATED_AT, db);
			}
			if (values.containsKey(Tasks.LAST_MODIFIED)) {
				Date d = new Date(values.getAsLong(Tasks.LAST_MODIFIED));
				GregorianCalendar due = new GregorianCalendar();
				d.setTime(values.getAsLong(Tasks.LAST_MODIFIED));
				String db = DateTimeHelper.formatDate(due);
				newValues.put(DatabaseHelper.UPDATED_AT, db);
			}
			// if (values.containsKey(Tasks._SYNC_ID)) {
			// newValues.put("caldav_extra.SYNC_ID",
			// values.getAsString(Tasks._SYNC_ID));
			// }
			if (values.containsKey(Tasks._DIRTY)) {
				boolean val = values.getAsBoolean(Tasks._DIRTY);
				if (!values.containsKey(Tasks._DELETED)) {
					newValues.put(SyncAdapter.SYNC_STATE,
							val ? SYNC_STATE.NEED_SYNC.toInt()
									: SYNC_STATE.NOTHING.toInt());
				} else {
					boolean del = values.getAsBoolean(Tasks._DELETED);
					if (del) {
						newValues.put(SyncAdapter.SYNC_STATE,
								SYNC_STATE.DELETE.toInt());
					} else if (val) {
						newValues.put(SyncAdapter.SYNC_STATE,
								SYNC_STATE.NEED_SYNC.toInt());
					} else {
						newValues.put(SyncAdapter.SYNC_STATE,
								SYNC_STATE.NOTHING.toInt());
					}
				}
			}
			if (values.containsKey(Tasks._DELETED)
					&& !values.containsKey(Tasks._DIRTY)) {
				newValues.put(
						SyncAdapter.SYNC_STATE,
						values.getAsBoolean(Tasks._DELETED) ? SYNC_STATE.DELETE
								.toInt() : SYNC_STATE.NOTHING.toInt());
			}
		}
		// newValues.put(SyncAdapter.SYNC_STATE, SYNC_STATE.ADD.toInt());
		return newValues;
	}

	private static String getId(Uri uri) {
		return uri.getPathSegments().get(1);
	}

	private static String getListQuery(boolean isSyncAdapter) {
		String query = getListQueryBase(isSyncAdapter);
		query += addSegment(DatabaseHelper.ID, TaskLists._ID, true);
		query += " FROM " + ListMirakel.TABLE;
		Log.d(TAG, query);
		return query;
	}

	private static String getListQueryBase(boolean isSyncAdapter) {
		String query = "SELECT ";
		query += addSegment(DatabaseHelper.NAME, TaskLists.LIST_NAME, false);
		query += addSegment(ListMirakel.COLOR, TaskLists.LIST_COLOR, true);
		if (isSyncAdapter) {
			query += addSegment("CASE " + SyncAdapter.SYNC_STATE + " WHEN "
					+ SYNC_STATE.NEED_SYNC + " THEN 1 ELSE 0 END",
					TaskLists._DIRTY, true);
			query += addSegment(DatabaseHelper.ID, Tasks._ID, true);
			// query += addSegment("CASE " + SyncAdapter.SYNC_STATE + " WHEN "
			// + SYNC_STATE.DELETE + " THEN 1 ELSE 0 END",
			// TaskLists._DELETED, true);
			// query += addSegment("CASE " + SyncAdapter.SYNC_STATE + " WHEN "
			// + SYNC_STATE.ADD + " THEN 1 ELSE 0 END",
			// TaskLists.IS_NEW, true);
			query += addSegment(DatabaseHelper.ID, TaskLists._SYNC_ID, true);
		}
		return query;
	}

	private static String getListQuerySpecial() {
		String query = getListQuery(false);
		query += " UNION ";
		query += getListQueryBase(false);
		query += addSegment(DatabaseHelper.ID + "*-1", TaskLists._ID, true);
		query += " FROM " + SpecialList.TABLE;
		return query;
	}

	private static String getTaskQuery(boolean isSyncAdapter) {
		return getTaskQuery(false, 0, isSyncAdapter);
	}

	private static String getTaskQuery(boolean isSpecial, int list_id, boolean isSyncadapter) {
		String query = "SELECT ";
		query += addSegment(Task.TABLE + "." + DatabaseHelper.NAME,
				TaskContract.Tasks.TITLE, false);
		query += addSegment(Task.TABLE + "." + Task.CONTENT,
				TaskContract.Tasks.DESCRIPTION, true);
		query += addSegment(" NULL ", Tasks.LOCATION, true);
		query += addSegment("(CASE WHEN (" + Task.TABLE + "." + Task.DUE
				+ " IS NULL) " + "THEN NULL ELSE strftime('%s'," + Task.TABLE
				+ "." + Task.DUE + ")*1000 END)", TaskContract.Tasks.DUE, true);
		query += addSegment("(CASE " + Task.TABLE + "." + Task.DONE
				+ " WHEN 1 THEN 2 ELSE 0 END)", TaskContract.Tasks.STATUS, true);
		query += addSegment(Task.TABLE + "." + Task.PROGRESS,
				Tasks.PERCENT_COMPLETE, true);
		if (isSyncadapter) {
			query += addSegment("CASE " + Task.TABLE + "."
					+ SyncAdapter.SYNC_STATE + " WHEN " + SYNC_STATE.NEED_SYNC
					+ " THEN 1 WHEN " + SYNC_STATE.ADD + " THEN 1 ELSE 0 END",
					TaskContract.Tasks._DIRTY, true);
			query += addSegment(Task.TABLE + "." + DatabaseHelper.ID,
					Tasks._ID, true);
			query += addSegment("CASE " + Task.TABLE + "."
					+ SyncAdapter.SYNC_STATE + " WHEN " + SYNC_STATE.DELETE
					+ " THEN 1 ELSE 0 END", TaskContract.Tasks._DELETED, true);
			query += addSegment(
					"CASE "
							+ Task.TABLE
							+ "."
							+ Task.PRIORITY
							+ " WHEN 2 THEN 1 WHEN 1 THEN 5 WHEN -1 THEN 9"
							+ " WHEN -2 THEN 9 ELSE 0 END",
							Tasks.PRIORITY, true);
			// query += addSegment("CASE " +
			// Task.TABLE+"."+SyncAdapter.SYNC_STATE + " WHEN "
			// + SYNC_STATE.ADD + " THEN 1 ELSE 0 END",
			// TaskContract.Tasks.IS_NEW, true);
			query += addSegment("caldav_extra.SYNC_ID", Tasks._SYNC_ID, true);
			query += addSegment("caldav_extra.ETAG", Tasks.SYNC1, true);
			query += addSegment(AccountMirakel.TABLE + "."
					+ DatabaseHelper.NAME, TaskContract.ACCOUNT_NAME, true);
			query += addSegment("caldav_extra.REMOTE_NAME", Tasks.LIST_ID, true);
		} else {
			query += addSegment(Task.TABLE + "." + Task.PRIORITY,
					TaskContract.Tasks.PRIORITY, true);
			if (isSpecial) {
				query += addSegment("CASE " + Task.TABLE + "." + Task.LIST_ID
						+ " WHEN 1 THEN " + list_id + " ELSE " + list_id
						+ " END", TaskContract.Tasks.LIST_ID, true);
			} else {
				query += addSegment(Task.TABLE + "." + Task.LIST_ID,
						TaskContract.Tasks.LIST_ID, true);
			}
		}
		query += addSegment("strftime('%s'," + Task.TABLE + "."
				+ DatabaseHelper.UPDATED_AT + ")*1000", Tasks.LAST_MODIFIED,
				true);
		query += addSegment("strftime('%s'," + Task.TABLE + "."
				+ DatabaseHelper.CREATED_AT + ")*1000", Tasks.CREATED, true);
		// query += " FROM " + Task.TABLE;
		if (isSyncadapter) {
			query += " FROM (" + Task.TABLE + " inner join "
					+ ListMirakel.TABLE;
			query += " on " + Task.TABLE + "." + Task.LIST_ID + "="
					+ ListMirakel.TABLE + "." + DatabaseHelper.ID + ")";
			query += " inner join " + AccountMirakel.TABLE + " on "
					+ ListMirakel.TABLE + "." + ListMirakel.ACCOUNT_ID;
			query += "=" + AccountMirakel.TABLE + "." + DatabaseHelper.ID;
			query += " LEFT JOIN caldav_extra ON " + Task.TABLE + "."
					+ DatabaseHelper.ID + "=caldav_extra." + DatabaseHelper.ID;
		} else {
			query += " FROM " + Task.TABLE;
		}
		Log.d(TAG, query);
		return query;
	}

	private static String handleListID(String selection, boolean isSyncAdapter, String taskQuery) throws SQLWarning {
		String[] t = selection.split(TaskContract.Tasks.LIST_ID);
		if (t.length < 2) return taskQuery;
		boolean not;
		try {
			not = t[0].trim().substring(t[0].trim().length() - 3)
					.equalsIgnoreCase("not");
		} catch (Exception e) {
			not = false;
		}
		if (t[1].trim().charAt(0) == '=') {
			taskQuery = handleListIDEqual(isSyncAdapter, taskQuery, t, not);
		} else {
			taskQuery = handleListIDIn(isSyncAdapter, taskQuery, t, not);
		}
		return taskQuery;
	}

	private static String handleListIDEqual(boolean isSyncAdapter, String taskQuery, String[] t, boolean not) throws SQLWarning {
		t[1] = t[1].trim().substring(1);
		int list_id = 0;
		try {
			boolean negative = t[1].trim().charAt(0) == '-';
			Matcher matcher = Pattern.compile("\\d+").matcher(t[1]);
			matcher.find();
			list_id = (negative ? -1 : 1) * Integer.valueOf(matcher.group());
		} catch (Exception e) {
			Log.e(TAG, "cannot parse list_id");
			throw new SQLWarning();
		}
		if (list_id < 0) {// is special list...
			SpecialList s = SpecialList.getSpecialList(-1 * list_id);
			if (s != null) {
				taskQuery = getTaskQuery(true, not ? 0 : list_id, isSyncAdapter);
				if (s.getWhereQuery(true) != null
						&& !s.getWhereQuery(true).trim().equals("")) {
					taskQuery += " WHERE " + (not ? "NOT ( " : "")
							+ s.getWhereQuery(true) + (not ? " )" : "");
				}
			} else {
				Log.e(TAG, "no matching list found");
				throw new SQLWarning();
			}
		}
		return taskQuery;
	}

	private static String handleListIDIn(boolean isSyncAdapter, String taskQuery, String[] t, boolean not) throws SQLWarning {
		if (t[1].trim().substring(0, 2).equalsIgnoreCase("in")) {
			t[1] = t[1].trim().substring(3).trim();
			int counter = 1;
			String buffer = "";
			List<Integer> idList = new ArrayList<Integer>();
			while (t[1].charAt(counter) >= '0' && t[1].charAt(counter) <= '9'
					|| t[1].charAt(counter) == ','
					|| t[1].charAt(counter) == ' '
					|| t[1].charAt(counter) == '-') {
				if (t[1].charAt(counter) == ',') {
					try {
						idList.add(Integer.parseInt(buffer));
						buffer = "";
					} catch (NumberFormatException e) {
						Log.e(TAG, "cannot parse list id");
						throw new SQLWarning();
					}
				} else if (t[1].charAt(counter) >= '0' && t[1].charAt(counter) <= '9'
						|| t[1].charAt(counter) == '-') {
					buffer += t[1].charAt(counter);
				}
				++counter;
			}
			try {
				idList.add(Integer.parseInt(buffer));
			} catch (NumberFormatException e) {
				Log.e(TAG, "cannot parse list id");
				throw new SQLWarning();
			}
			if (idList.size() == 0) {
				Log.e(TAG, "inavlid SQL");
				throw new SQLWarning();
			}
			List<String> wheres = new ArrayList<String>();
			List<Integer> ordonaryIds = new ArrayList<Integer>();
			for (int id : idList) {
				if (id < 0) {
					SpecialList s = SpecialList.getSpecialList(-1 * id);
					if (s != null) {
						wheres.add(s.getWhereQuery(true));
					} else {
						Log.e(TAG, "no matching list found");
						throw new SQLWarning();

					}
				} else {
					ordonaryIds.add(id);
				}
			}
			taskQuery = getTaskQuery(true, not ? 0 : idList.get(0),
					isSyncAdapter) + " WHERE " + (not ? " NOT (" : "");
			for (int i = 0; i < wheres.size(); i++) {
				taskQuery += (i != 0 ? " AND " : " ") + wheres.get(i);
			}
			if (ordonaryIds.size() > 0) {
				if (wheres.size() > 0) {
					taskQuery += " OR ";
				}
				taskQuery += Task.LIST_ID + " IN (";
				for (int i = 0; i < ordonaryIds.size(); i++) {
					taskQuery += (i != 0 ? "," : "") + ordonaryIds.get(i);
				}
				taskQuery += ")";
			}
			taskQuery += not ? ")" : "";

		}
		return taskQuery;
	}

	private static String insertSelectionArgs(String selection, String[] selectionArgs) {
		if (selectionArgs != null) {
			for (String selectionArg : selectionArgs) {
				selection = selection.replace("?", selectionArg);
			}
		}
		return selection;
	}

	// public static final String PROVIDER_NAME = Mirakel.AUTHORITY_TYP;
	// public static final Uri CONTENT_URI = Uri.parse("content://" +
	// PROVIDER_NAME);
	private SQLiteDatabase			database;

	private int createNewList(Uri uri) {
		String name = getContext().getString(R.string.inbox);
		AccountMirakel a = AccountMirakel.getByName(getAccountName(uri));
		if (a == null) throw new IllegalArgumentException("Unkown account");
		Cursor c = this.database.query(ListMirakel.TABLE,
				new String[] { DatabaseHelper.ID },
				DatabaseHelper.NAME + "='" + name + "' and "
						+ ListMirakel.ACCOUNT_ID + "=" + a.getId(), null, null,
						null, null);
		ListMirakel l;
		if (c.getCount() < 1) {
			c.close();
			l = ListMirakel.newList(name);
			l.setAccount(a);
			l.save(false);
			return l.getId();
		}
		c.moveToFirst();
		int id = c.getInt(0);
		c.close();
		return id;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		if (!isCallerSyncAdapter(uri)) {
			ContentValues cv = new ContentValues();
			cv.put(SyncAdapter.SYNC_STATE, SYNC_STATE.DELETE.toInt());
			switch (uriMatcher.match(uri)) {
				case LISTS:
				case TASKS:
					return update(uri, cv, selection, selectionArgs);
				case LIST_ID:
					this.database.update(ListMirakel.TABLE, cv, DatabaseHelper.ID
							+ "=" + getId(uri), null);
					return 1;
				case TASK_ID:
					this.database.update(Task.TABLE, cv, DatabaseHelper.ID + "="
							+ getId(uri), null);
					return 1;
				default:
					throw new IllegalArgumentException("Unsupported URI: "
							+ uri);
			}
		}
		AccountMirakel a = AccountMirakel.getByName(getAccountName(uri));
		if (a != null && !a.isEnabeld()) return 0;
		boolean isList = true;
		switch (uriMatcher.match(uri)) {
			case LIST_ID:
				return this.database.delete(ListMirakel.TABLE, DatabaseHelper.ID
						+ "=" + getId(uri), null);
			case LISTS:
				isList = true;
				break;
			case TASKS:
				isList = false;
				break;
			case TASK_ID:
				return this.database.delete(Task.TABLE, DatabaseHelper.ID + "="
						+ getId(uri), null);
			default:
				throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
		String s;
		try {
			s = getIdsFromSelection(uri, selection, selectionArgs, isList);
		} catch (RuntimeException e) {
			if (e.getMessage().equals("id not found")) return 0;
			throw e;
		}
		if (!s.equals("")) return this.database.delete(isList ? ListMirakel.TABLE : Task.TABLE,
				DatabaseHelper.ID + " IN (" + s + ")", null);
		throw new RuntimeException("id not found");
	}

	protected String getAccountName(Uri uri) {
		return uri.getQueryParameter(TaskContract.ACCOUNT_NAME);
	}

	protected String getAccountType(Uri uri) {
		return uri.getQueryParameter(TaskContract.ACCOUNT_TYPE);
	}

	private String getIdsFromSelection(Uri uri, String selection, String[] selectionArgs, boolean isList) {
		Cursor c = query(uri,
				new String[] { isList ? TaskLists._ID : Tasks._ID }, selection,
				selectionArgs, null);
		String s = "";
		if (c.getCount() > 0 && c.moveToFirst()) {
			while (!c.isAfterLast()) {
				s += (s.equals("") ? "" : ",") + c.getInt(0);
				c.moveToNext();
			}
		} else throw new RuntimeException("id not found");
		return s;
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
		// android.os.Debug.waitForDebugger();
		AccountMirakel a = AccountMirakel.getByName(getAccountName(uri));
		if (a != null && !a.isEnabeld()) return null;
		ContentValues newValues = convertValues(values,
				isCallerSyncAdapter(uri));
		newValues.put(SyncAdapter.SYNC_STATE, SYNC_STATE.NOTHING.toInt());
		String table;
		switch (uriMatcher.match(uri)) {
			case LISTS:
				table = ListMirakel.TABLE;
				break;
			case TASKS:
				table = Task.TABLE;
				int lID;
				// if (newValues.containsKey(Task.LIST_ID)) {
				// ListMirakel l = ListMirakel.getList(newValues
				// .getAsInteger(Task.LIST_ID));
				// if (l == null) {
				// lID = createNewList(uri);
				// } else {
				// if (a == null) {
				// throw new IllegalArgumentException("Unkown account");
				// }
				// if (l.getAccount().getId() != a.getId()) {
				// lID = createNewList(uri);
				// } else {
				// lID = l.getId();
				// }
				// }
				// } else {
				lID = createNewList(uri);
				// }
				newValues.put(Task.LIST_ID, lID);
				break;
			default:
				throw new IllegalArgumentException("Unsupported URI: " + uri);

		}
		long id;
		this.database.beginTransaction();
		try {
			boolean hasExtras = false;
			ContentValues extras = new ContentValues();
			if (table.equals(Task.TABLE)) {
				if (values.containsKey(Tasks.SYNC1)) {
					extras.put("ETAG", values.getAsString(Tasks.SYNC1));
					hasExtras = true;
				}
				if (values.containsKey(Tasks._SYNC_ID)) {
					extras.put("SYNC_ID", values.getAsString(Tasks._SYNC_ID));
					hasExtras = true;
				}
				if (values.containsKey(Tasks.LIST_ID)) {
					extras.put("REMOTE_NAME", values.getAsString(Tasks.LIST_ID));
					hasExtras = true;
				}
			}
			id = this.database.insert(table, null, newValues);
			if (hasExtras) {
				extras.put(DatabaseHelper.ID, id);
				this.database.insert("caldav_extra", null, extras);
			}
			this.database.setTransactionSuccessful();
		} catch (Exception e) {
			Log.d(TAG, "cannot insert new object");
			throw new RuntimeException();
		} finally {
			this.database.endTransaction();
		}
		Log.d(TAG, "insert...");
		return Uri.parse(uri.toString() + "/" + id);

	}

	protected boolean isCallerSyncAdapter(Uri uri) {
		String param = uri
				.getQueryParameter(TaskContract.CALLER_IS_SYNCADAPTER);
		return param != null && !"false".equals(param);
	}

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	private Cursor listQuery(String[] projection, String selection, String sortOrder, SQLiteQueryBuilder sqlBuilder, boolean isSyncAdapter, boolean hasId, String _id) {
		String listQuery;
		if (selection.equals("1=1")) {
			listQuery = getListQuerySpecial();
		} else {
			listQuery = getListQuery(isSyncAdapter);
		}
		if (hasId) {
			listQuery += "WHERE " + TaskLists._ID + "=" + _id;
		}
		sqlBuilder.setTables("(" + listQuery + ")");
		String query;
		if (Build.VERSION.SDK_INT >= 11) {
			query = sqlBuilder.buildQuery(projection, selection, null, null,
					sortOrder, null);
		} else {
			query = sqlBuilder.buildQuery(projection, selection, null, null,
					sortOrder, null, null);

		}
		Log.d(TAG, query);
		Cursor c = this.database.rawQuery(query, null);
		return c;
	}

	@Override
	public void onAccountsUpdated(Account[] accounts) {
		AccountMirakel.update(accounts);

	}

	@Override
	public boolean onCreate() {
		this.database = new DatabaseHelper(getContext()).getWritableDatabase();
		// register for account updates and check immediately
		AccountManager.get(getContext()).addOnAccountsUpdatedListener(this,
				null, true);
		return this.database == null;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
		boolean isSyncAdapter = isCallerSyncAdapter(uri);
		selection = insertSelectionArgs(selection, selectionArgs);
		int matcher = uriMatcher.match(uri);
		switch (matcher) {
			case LIST_ID:
				return listQuery(projection, selection, sortOrder, sqlBuilder,
						isSyncAdapter, true, getId(uri));
			case LISTS:
				return listQuery(projection, selection, sortOrder, sqlBuilder,
						isSyncAdapter, false, "");
			case TASK_ID:
				return taskQuery(projection, selection, sortOrder, sqlBuilder,
						isSyncAdapter, uri, getId(uri), true);
			case TASKS:
				return taskQuery(projection, selection, sortOrder, sqlBuilder,
						isSyncAdapter, uri, "", false);

			default:
				throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	private Cursor taskQuery(String[] projection, String selection, String sortOrder, SQLiteQueryBuilder sqlBuilder, boolean isSyncAdapter, Uri uri, String _id, boolean hasID) {
		String taskQuery = getTaskQuery(isSyncAdapter);
		if (isSyncAdapter) {
			taskQuery += " WHERE " + AccountMirakel.TABLE + "."
					+ DatabaseHelper.NAME + "='" + getAccountName(uri) + "' ";
		}
		if (hasID) {
			taskQuery += (isSyncAdapter ? " AND " : " WHERE ") + Task.TABLE
					+ "." + DatabaseHelper.ID + "=" + _id;
		}
		if (selection != null && selection.contains(TaskContract.Tasks.LIST_ID)) {
			if (!isSyncAdapter) {
				try {
					taskQuery = handleListID(selection, isSyncAdapter,
							taskQuery);
				} catch (SQLWarning s) {
					return new MatrixCursor(projection);
				}
			}
		}
		sqlBuilder.setTables("(" + taskQuery + ")");
		String query;
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			query = sqlBuilder.buildQuery(projection, selection, null, null,
					null, sortOrder, null);
		} else {
			query = sqlBuilder.buildQuery(projection, selection, null, null,
					sortOrder, null);
		}
		Log.d(TAG, query);
		return this.database.rawQuery(query, null);
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		AccountMirakel a = AccountMirakel.getByName(getAccountName(uri));
		if (a != null && !a.isEnabeld()) return 0;
		ContentValues newValues = convertValues(values,
				isCallerSyncAdapter(uri));
		boolean isList;
		boolean hasExtras = false;
		ContentValues extras = new ContentValues();
		if (values.containsKey(Tasks.SYNC1)) {
			extras.put("ETAG", values.getAsString(Tasks.SYNC1));
			hasExtras = true;
		}
		if (values.containsKey(Tasks._SYNC_ID)) {
			extras.put("SYNC_ID", values.getAsString(Tasks._SYNC_ID));
			hasExtras = true;
		}
		switch (uriMatcher.match(uri)) {
			case TASKS:
				isList = false;
				break;
			case TASK_ID:
				int count = 0;
				if (newValues.size() > 0) {
					count = this.database.update(Task.TABLE, newValues,
							DatabaseHelper.ID + "=" + getId(uri), null);
				}
				if (hasExtras && extras.size() > 0) {
					count = this.database.update("caldav_extra", extras,
							DatabaseHelper.ID + "=" + getId(uri), null);
					if (count != 1) {
						extras.put(DatabaseHelper.ID,
								Integer.parseInt(getId(uri)));
						this.database.insert("caldav_extra", null, extras);
					}
				}
				return count;
			case LIST_ID:
				return this.database.update(ListMirakel.TABLE, newValues,
						DatabaseHelper.ID + "=" + getId(uri), null);
			case LISTS:
				isList = true;
				break;
			default:
				throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
		String s;
		try {
			s = getIdsFromSelection(uri, selection, selectionArgs, isList);
		} catch (RuntimeException e) {
			if (e.getMessage().equals("id not found")) return 0;
			throw e;
		}
		if (!s.equals("")) {
			int count = this.database.update(
					isList ? ListMirakel.TABLE : Task.TABLE, newValues,
							DatabaseHelper.ID + " IN(" + s + ")", null);
			if (hasExtras) {
				this.database.update("caldav_extra", extras, DatabaseHelper.ID
						+ " IN(" + s + ")", null);
			}
			return count;
		}
		throw new RuntimeException("id not found");
	}

}
