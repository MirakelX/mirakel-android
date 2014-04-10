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
package de.azapps.mirakel.model.task;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;

import android.accounts.Account;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Pair;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import de.azapps.mirakel.DefinitionsHelper.NoSuchListException;
import de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.UndoHistory;
import de.azapps.mirakel.helper.error.ErrorReporter;
import de.azapps.mirakel.helper.error.ErrorType;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.MirakelContentProvider;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.reminders.ReminderAlarm;
import de.azapps.mirakel.services.NotificationService;
import de.azapps.tools.Log;

public class Task extends TaskBase {

	public static final String[] allColumns = { DatabaseHelper.ID,
			TaskBase.UUID, TaskBase.LIST_ID, DatabaseHelper.NAME,
			TaskBase.CONTENT, TaskBase.DONE, TaskBase.DUE, TaskBase.REMINDER,
			TaskBase.PRIORITY, DatabaseHelper.CREATED_AT,
			DatabaseHelper.UPDATED_AT, DatabaseHelper.SYNC_STATE_FIELD,
			TaskBase.ADDITIONAL_ENTRIES, TaskBase.RECURRING,
			TaskBase.RECURRING_REMINDER, TaskBase.PROGRESS };
	private static Context context;
	private static SQLiteDatabase database;

	private static DatabaseHelper dbHelper;

	public static final String SUBTASK_TABLE = "subtasks";

	public static final String TABLE = "tasks";

	private static final String TAG = "TasksDataSource";

	/**
	 * Get all Tasks
	 * 
	 * @return
	 */
	public static List<Task> all() {
		final List<Task> tasks = new ArrayList<Task>();
		final Cursor c = Task.database.query(Task.TABLE, Task.allColumns,
				"not " + DatabaseHelper.SYNC_STATE_FIELD + "= "
						+ SYNC_STATE.DELETE, null, null, null, null);
		c.moveToFirst();
		while (!c.isAfterLast()) {
			tasks.add(cursorToTask(c));
			c.moveToNext();
		}
		c.close();
		return tasks;
	}

	/**
	 * Close the Database-Connection
	 */
	public static void close() {
		Task.dbHelper.close();
	}

	/**
	 * Create a task from a Cursor
	 * 
	 * @param cursor
	 * @return
	 */
	public static Task cursorToTask(final Cursor cursor) {
		int i = 0;
		final int offset = DateTimeHelper.getTimeZoneOffset(true);

		GregorianCalendar due = new GregorianCalendar();
		if (cursor.isNull(6)) {
			due = null;
		} else {
			due.setTimeInMillis(cursor.getLong(6) * 1000 + offset);
			if (due.get(Calendar.HOUR) != 0 || due.get(Calendar.MINUTE) != 0
					|| due.get(Calendar.SECOND) != 0) {
				due.add(Calendar.MILLISECOND, offset);
			}
		}

		GregorianCalendar reminder = new GregorianCalendar();
		if (cursor.isNull(7)) {
			reminder = null;
		} else {
			reminder.setTimeInMillis(cursor.getLong(7) * 1000 + offset);
		}

		GregorianCalendar created_at = new GregorianCalendar();
		if (cursor.isNull(9)) {
			created_at = null;
		} else {
			created_at.setTimeInMillis(cursor.getLong(9) * 1000 + offset);
		}
		GregorianCalendar updated_at = new GregorianCalendar();
		if (cursor.isNull(10)) {
			updated_at = null;
		} else {
			updated_at.setTimeInMillis(cursor.getLong(10) * 1000 + offset);
		}

		final Task task = new Task(cursor.getLong(i++), cursor.getString(i++),
				ListMirakel.getList((int) cursor.getLong(i++)),
				cursor.getString(i++), cursor.getString(i++),
				cursor.getInt(i++) == 1, due, reminder, cursor.getInt(8),
				created_at, updated_at, SYNC_STATE.parseInt(cursor.getInt(11)),
				cursor.getString(12), cursor.getInt(13), cursor.getInt(14),
				cursor.getInt(15));
		return task;
	}

	private static List<Task> cursorToTaskList(final Cursor cursor) {
		cursor.moveToFirst();
		final List<Task> tasks = new ArrayList<Task>();
		while (!cursor.isAfterLast()) {
			tasks.add(cursorToTask(cursor));
			cursor.moveToNext();
		}
		cursor.close();
		return tasks;
	}

	public static void deleteDoneTasks() {
		Task.database.beginTransaction();
		final ContentValues values = new ContentValues();
		values.put("sync_state", SYNC_STATE.DELETE.toInt());
		final String where = "sync_state!=" + SYNC_STATE.ADD + " AND done=1";
		Task.database.update(Task.TABLE, values, where, null);
		Task.database.delete(Task.TABLE, "sync_state=" + SYNC_STATE.ADD
				+ " AND done=1", null);
		Task.database.setTransactionSuccessful();
		Task.database.endTransaction();
	}

	/**
	 * Get a Task by id
	 * 
	 * @param id
	 * @return
	 */
	public static Task get(final long id) {
		final Cursor cursor = Task.database.query(Task.TABLE, Task.allColumns,
				DatabaseHelper.ID + "='" + id + "' and not "
						+ DatabaseHelper.SYNC_STATE_FIELD + "="
						+ SYNC_STATE.DELETE, null, null, null, null);
		cursor.moveToFirst();
		if (cursor.getCount() != 0) {
			final Task t = cursorToTask(cursor);
			cursor.close();
			return t;
		}
		cursor.close();
		return null;
	}

	/**
	 * Get tasks by Sync State
	 * 
	 * @param state
	 * @return
	 */
	public static List<Task> getBySyncState(final SYNC_STATE state) {
		final Cursor c = Task.database.query(Task.TABLE, Task.allColumns,
				DatabaseHelper.SYNC_STATE_FIELD + "=" + state.toInt() + " and "
						+ TaskBase.LIST_ID + ">0", null, null, null, null);
		return cursorToTaskList(c);
	}

	public static Task getByUUID(final String uuid) {
		final Cursor cursor = Task.database.query(Task.TABLE, Task.allColumns,
				TaskBase.UUID + "='" + uuid + "' and not "
						+ DatabaseHelper.SYNC_STATE_FIELD + "="
						+ SYNC_STATE.DELETE, null, null, null, null);
		cursor.moveToFirst();
		if (cursor.getCount() != 0) {
			final Task t = cursorToTask(cursor);
			cursor.close();
			return t;
		}
		cursor.close();
		return null;

	}

	public static Task getDummy(final Context ctx) {
		return new Task(ctx.getString(R.string.task_empty));
	}

	public static Task getDummy(final Context ctx, final ListMirakel list) {
		final Task task = new Task(ctx.getString(R.string.task_empty));
		task.setList(list, false);
		return task;
	}

	public static String getSorting(final int sorting) {
		String order = "";
		switch (sorting) {
		case ListMirakel.SORT_BY_PRIO:
			order = TaskBase.PRIORITY + " desc";
			break;
		case ListMirakel.SORT_BY_OPT:
			order = ", " + TaskBase.PRIORITY + " DESC";
			//$FALL-THROUGH$
		case ListMirakel.SORT_BY_DUE:
			order = "done ASC, CASE WHEN (" + TaskBase.DUE
					+ " IS NULL) THEN strftime('%s','now','+10 years') ELSE "
					+ TaskBase.DUE + " END ASC" + order;
			break;
		case ListMirakel.SORT_BY_REVERT_DEFAULT:
			order = TaskBase.PRIORITY + " DESC,  CASE WHEN (" + TaskBase.DUE
					+ " IS NULL) THEN strftime('%s','now','+10 years') ELSE "
					+ TaskBase.DUE + " END ASC" + order;
			//$FALL-THROUGH$
		default:
			order = DatabaseHelper.ID + " ASC";
		}
		return order;
	}

	public static List<Pair<Long, String>> getTaskNames() {
		final Cursor c = Task.database.query(Task.TABLE, new String[] {
				DatabaseHelper.ID, DatabaseHelper.NAME }, "not "
				+ DatabaseHelper.SYNC_STATE_FIELD + "=" + SYNC_STATE.DELETE
				+ " and done = 0", null, null, null, null);
		c.moveToFirst();
		final List<Pair<Long, String>> names = new ArrayList<Pair<Long, String>>();
		while (!c.isAfterLast()) {
			names.add(new Pair<Long, String>(c.getLong(0), c.getString(1)));
			c.moveToNext();
		}
		c.close();
		return names;
	}

	/**
	 * Get Tasks from a List Use it only if really necessary!
	 * 
	 * @param listId
	 * @param sorting
	 *            The Sorting (@see Mirakel.SORT_*)
	 * @param showDone
	 * @return
	 */
	public static List<Task> getTasks(final int listId, final int sorting,
			final boolean showDone) {
		final Cursor cursor = getTasksCursor(listId, sorting, showDone);
		return cursorToTaskList(cursor);
	}

	/**
	 * Get Tasks from a List Use it only if really necessary!
	 * 
	 * @param listId
	 * @param sorting
	 *            The Sorting (@see Mirakel.SORT_*)
	 * @param showDone
	 * @return
	 */
	public static List<Task> getTasks(final ListMirakel list,
			final int sorting, final boolean showDone) {
		return getTasks(list.getId(), sorting, showDone);
	}

	/**
	 * Get Tasks from a List Use it only if really necessary!
	 * 
	 * @param listId
	 * @param sorting
	 *            The Sorting (@see Mirakel.SORT_*)
	 * @param showDone
	 * @return
	 */
	public static List<Task> getTasks(final ListMirakel list,
			final int sorting, final boolean showDone, final String where) {
		final Cursor cursor = getTasksCursor(list.getId(), sorting, where);
		return cursorToTaskList(cursor);
	}

	/**
	 * Get a Cursor with all Tasks of a list
	 * 
	 * @param listId
	 * @param sorting
	 * @return
	 */
	private static Cursor getTasksCursor(final int listId, final int sorting,
			final boolean showDone) {
		final ListMirakel l = ListMirakel.getList(listId);
		if (l == null) {
			Log.wtf(TAG, "list not found");
			return new MatrixCursor(allColumns);
		}
		String where = l.getWhereQueryForTasks();
		if (!showDone) {
			where += (where.trim().equals("") ? "" : " AND ") + " "
					+ TaskBase.DONE + "=0";
		}
		return getTasksCursor(listId, sorting, where);
	}

	/**
	 * Get a Cursor with all Tasks of a list
	 * 
	 * @param listId
	 * @param sorting
	 * @return
	 */
	private static Cursor getTasksCursor(final int listId, final int sorting,
			String where) {
		if (where == null) {
			where = "";
		} else if (!where.equals("")) {
			where += " and ";
		}
		where += " not " + DatabaseHelper.SYNC_STATE_FIELD + "="
				+ SYNC_STATE.DELETE;
		String order = getSorting(sorting);

		if (listId < 0) {
			order += ", " + TaskBase.LIST_ID + " ASC";
		}
		return Task.database.query(Task.TABLE, Task.allColumns, where, null,
				null, null, TaskBase.DONE + ", " + order);
	}

	private static Cursor getTasksCursor(final Task subtask) {
		String columns = "t." + Task.allColumns[0];
		for (int i = 1; i < Task.allColumns.length; i++) {
			columns += ", t." + Task.allColumns[i];
		}
		return Task.database.rawQuery("SELECT " + columns + " FROM "
				+ Task.TABLE + " t INNER JOIN " + Task.SUBTASK_TABLE
				+ " s on t._id=s.child_id WHERE s.parent_id=? ORDER BY "
				+ getSorting(ListMirakel.SORT_BY_OPT), new String[] { ""
				+ subtask.getId() });
	}

	// Static Methods

	public static List<Task> getTasksToSync(final Account account) {
		final AccountMirakel a = AccountMirakel.get(account);
		final List<ListMirakel> lists = ListMirakel.getListsForAccount(a);
		String listIDs = "";
		boolean first = true;
		for (final ListMirakel l : lists) {
			listIDs += (first ? "" : ",") + l.getId();
			first = false;
		}
		final String where = "NOT " + DatabaseHelper.SYNC_STATE_FIELD + "='"
				+ SYNC_STATE.NOTHING + "' and " + TaskBase.LIST_ID + " IN ("
				+ listIDs + ")";
		final Cursor cursor = MirakelContentProvider.getReadableDatabase()
				.query(Task.TABLE, Task.allColumns, where, null, null, null,
						null);
		return cursorToTaskList(cursor);

	}

	public static List<Task> getTasksWithReminders() {
		final String where = TaskBase.REMINDER + " NOT NULL and "
				+ TaskBase.DONE + "=0";
		final Cursor cursor = MirakelContentProvider.getReadableDatabase()
				.query(Task.TABLE, Task.allColumns, where, null, null, null,
						null);
		return cursorToTaskList(cursor);
	}

	/**
	 * Get a Task to sync it
	 * 
	 * @param id
	 * @return
	 */
	public static Task getToSync(final long id) {
		final Cursor cursor = Task.database.query(Task.TABLE, Task.allColumns,
				DatabaseHelper.ID + "='" + id + "'", null, null, null, null);
		cursor.moveToFirst();
		if (cursor.getCount() != 0) {
			final Task t = cursorToTask(cursor);
			cursor.close();
			return t;
		}
		cursor.close();
		return null;
	}

	/**
	 * Initialize the Database and the preferences
	 * 
	 * @param context
	 *            The Application-Context
	 */
	public static void init(final Context ctx) {
		Task.context = ctx;
		Task.dbHelper = new DatabaseHelper(Task.context);
		Task.database = Task.dbHelper.getWritableDatabase();
	}

	public static Task newTask(final String name, final ListMirakel list) {
		return newTask(name, list, "", false, null, 0);
	}

	public static Task newTask(final String name, final ListMirakel list,
			final GregorianCalendar due, final int prio) {
		return newTask(name, list, "", false, due, prio);
	}

	/**
	 * Create a new Task
	 * 
	 * @param name
	 * @param list_id
	 * @param content
	 * @param done
	 * @param due
	 * @param priority
	 * @return
	 */

	public static Task newTask(final String name, final ListMirakel list,
			final String content, final boolean done,
			final GregorianCalendar due, final int priority) {
		final Calendar now = new GregorianCalendar();
		final Task t = new Task(0, java.util.UUID.randomUUID().toString(),
				list, name, content, done, due, null, priority, now, now,
				SYNC_STATE.ADD, "", -1, -1, 0);

		try {
			final Task task = t.create();
			NotificationService.updateNotificationAndWidget(Task.context);
			return task;
		} catch (final NoSuchListException e) {
			ErrorReporter.report(ErrorType.TASKS_NO_LIST);
			Log.e(Task.TAG, Log.getStackTraceString(e));
			return null;
		}
	}

	/**
	 * Parses a JSON-Object to a task
	 * 
	 * @param el
	 * @return
	 */
	public static Task parse_json(final JsonObject el,
			final AccountMirakel account, final boolean isTW) {
		Task t = null;
		JsonElement id = el.get("id");
		if (id != null) {
			t = Task.get(id.getAsLong());
		} else {
			id = el.get("uuid");
			if (id != null) {
				t = Task.getByUUID(id.getAsString());
			}
		}
		if (t == null) {
			t = new Task();
		}
		if (isTW) {
			t.setDue(null);
			t.setDone(false);
			t.setContent(null);
			t.setPriority(0);
			t.setProgress(0);
			t.setList(null, false);
		}

		// Name
		final Set<Entry<String, JsonElement>> entries = el.entrySet();
		for (final Entry<String, JsonElement> entry : entries) {
			final String key = entry.getKey();
			final JsonElement val = entry.getValue();
			if (key == null || key.equalsIgnoreCase("id")) {
				continue;
			}
			if (key.equals("uuid")) {
				t.setUUID(val.getAsString());
			} else if (key.equalsIgnoreCase("name")
					|| key.equalsIgnoreCase("description")) {
				t.setName(val.getAsString());
			} else if (key.equalsIgnoreCase("content")) {
				String content = val.getAsString();
				if (content == null) {
					content = "";
				}
				t.setContent(content);
			} else if (key.equalsIgnoreCase("priority")) {
				final String prioString = val.getAsString().trim();
				if (prioString.equalsIgnoreCase("L") && t.getPriority() != -1) {
					t.setPriority(-2);
				} else if (prioString.equalsIgnoreCase("M")) {
					t.setPriority(1);
				} else if (prioString.equalsIgnoreCase("H")) {
					t.setPriority(2);
				} else if (!prioString.equalsIgnoreCase("L")) {
					t.setPriority(val.getAsInt());
				}
			} else if (key.equalsIgnoreCase("progress")) {
				final int progress = (int) val.getAsDouble();
				t.setProgress(progress);
			} else if (key.equalsIgnoreCase("list_id")) {
				ListMirakel list = ListMirakel.getList(val.getAsInt());
				if (list == null) {
					list = SpecialList.firstSpecial().getDefaultList();
				}
				t.setList(list, true);
			} else if (key.equalsIgnoreCase("project")) {
				ListMirakel list = ListMirakel.findByName(val.getAsString(),
						account);
				if (list == null
						|| list.getAccount().getId() != account.getId()) {
					list = ListMirakel.newList(val.getAsString(),
							ListMirakel.SORT_BY_OPT, account);
				}
				t.setList(list, true);
			} else if (key.equalsIgnoreCase("created_at")) {
				t.setCreatedAt(val.getAsString().replace(":", ""));
			} else if (key.equalsIgnoreCase("updated_at")) {
				t.setUpdatedAt(val.getAsString().replace(":", ""));
			} else if (key.equalsIgnoreCase("entry")) {
				t.setCreatedAt(parseDate(val.getAsString(),
						Task.context.getString(R.string.TWDateFormat)));
			} else if (key.equalsIgnoreCase("modification")
					|| key.equalsIgnoreCase("modified")) {
				t.setUpdatedAt(parseDate(val.getAsString(),
						Task.context.getString(R.string.TWDateFormat)));
			} else if (key.equals("done")) {
				t.setDone(val.getAsBoolean());
			} else if (key.equalsIgnoreCase("status")) {
				final String status = val.getAsString();
				if (status.equalsIgnoreCase("completed")) {
					t.setDone(true);
				} else if (status.equalsIgnoreCase("deleted")) {
					t.setSyncState(SYNC_STATE.DELETE);
				} else {
					t.setDone(false);
				}
				t.addAdditionalEntry(key, "\"" + val.getAsString() + "\"");
				// TODO don't ignore waiting and recurring!!!
			} else if (key.equalsIgnoreCase("due")) {
				Calendar due = parseDate(val.getAsString(), "yyyy-MM-dd");
				if (due == null) {
					due = parseDate(val.getAsString(),
							Task.context.getString(R.string.TWDateFormat));
					// try to workaround timezone-bug
					if (due != null) {
						due.setTimeInMillis(due.getTimeInMillis()
								+ TimeZone.getDefault().getRawOffset());
					}
				}
				t.setDue(due);
			} else if (key.equalsIgnoreCase("reminder")) {
				Calendar reminder = parseDate(val.getAsString(), "yyyy-MM-dd");
				if (reminder == null) {
					reminder = parseDate(val.getAsString(),
							Task.context.getString(R.string.TWDateFormat));
				}
				t.setReminder(reminder);
			} else if (key.equalsIgnoreCase("annotations")) {
				String content = "";
				try {
					final JsonArray annotations = val.getAsJsonArray();
					boolean first = true;
					for (final JsonElement a : annotations) {
						if (first) {
							first = false;
						} else {
							content += "\n";
						}
						content += a.getAsJsonObject().get("description")
								.getAsString();
					}
				} catch (final Exception e) {
					Log.e(Task.TAG, "cannot parse json");
				}
				t.setContent(content);
			} else if (key.equalsIgnoreCase("content")) {
				t.setContent(val.getAsString());
			} else if (key.equalsIgnoreCase("sync_state")) {
				t.setSyncState(SYNC_STATE.parseInt(val.getAsInt()));
			} else if (key.equalsIgnoreCase("depends")) {
				t.setDependencies(val.getAsString().split(","));
			} else {
				if (val.isJsonPrimitive()) {
					final JsonPrimitive p = (JsonPrimitive) val;
					if (p.isBoolean()) {
						t.addAdditionalEntry(key, val.getAsBoolean() + "");
					} else if (p.isNumber()) {
						t.addAdditionalEntry(key, val.getAsInt() + "");
					} else if (p.isJsonNull()) {
						t.addAdditionalEntry(key, "null");
					} else if (p.isString()) {
						t.addAdditionalEntry(key, "\"" + val.getAsString()
								+ "\"");
					} else {
						Log.w(Task.TAG, "unkown json-type");
					}
				} else if (val.isJsonArray()) {
					final JsonArray a = (JsonArray) val;
					String s = "[";
					boolean first = true;
					for (final JsonElement e : a) {
						if (e.isJsonPrimitive()) {
							final JsonPrimitive p = (JsonPrimitive) e;
							String add;
							if (p.isBoolean()) {
								add = p.getAsBoolean() + "";
							} else if (p.isNumber()) {
								add = p.getAsInt() + "";
							} else if (p.isString()) {
								add = "\"" + p.getAsString() + "\"";
							} else if (p.isJsonNull()) {
								add = "null";
							} else {
								Log.w(Task.TAG, "unkown json-type");
								break;
							}
							s += (first ? "" : ",") + add;
							first = false;
						} else {
							Log.w(Task.TAG, "unkown json-type");
						}
					}
					t.addAdditionalEntry(key, s + "]");
				} else {
					Log.w(Task.TAG, "unkown json-type");
				}
			}
		}
		return t;
	}

	/**
	 * Parse a JSON–String to a List of Tasks
	 * 
	 * @param result
	 * @return
	 */
	public static List<Task> parse_json(final String result,
			final AccountMirakel account) {
		try {
			final List<Task> tasks = new ArrayList<Task>();
			final Iterator<JsonElement> i = new JsonParser().parse(result)
					.getAsJsonArray().iterator();
			while (i.hasNext()) {
				final JsonObject el = (JsonObject) i.next();
				final Task t = parse_json(el, account, false);
				tasks.add(t);
			}
			return tasks;
		} catch (final Exception e) {
			Log.e(Task.TAG, "Cannot parse response");
			Log.e(Task.TAG, result);
			Log.d(Task.TAG, Log.getStackTraceString(e));
		}
		return new ArrayList<Task>();
	}

	private static Calendar parseDate(final String date, final String format) {
		final GregorianCalendar temp = new GregorianCalendar();
		try {
			temp.setTime(new SimpleDateFormat(format, Locale.getDefault())
					.parse(date));
			return temp;
		} catch (final ParseException e) {
			return null;
		}
	}

	public static List<Task> rawQuery(final String generateQuery) {
		final Cursor c = Task.database.rawQuery(generateQuery, null);
		final List<Task> ret = cursorToTaskList(c);
		c.close();
		return ret;
	}

	public static void resetSyncState(final List<Task> tasks) {
		if (tasks.size() == 0) {
			return;
		}
		for (final Task t : tasks) {
			if (t.getSyncState() != SYNC_STATE.DELETE) {
				t.setSyncState(SYNC_STATE.NOTHING);
				try {
					Task.database.update(Task.TABLE, t.getContentValues(),
							DatabaseHelper.ID + " = " + t.getId(), null);
				} catch (final NoSuchListException e) {
					Log.d(Task.TAG, "List did vanish");
				} catch (final Exception e) {
					t.destroy(false);
					Log.d(Task.TAG, "destroy: " + t.getName());
					Log.w(Task.TAG, Log.getStackTraceString(e));
				}
			} else {
				Log.d(Task.TAG, "destroy: " + t.getName());
				t.destroy(true);
			}
		}
	}

	public static List<Task> search(final String query) {
		final Cursor cursor = Task.database.query(Task.TABLE, Task.allColumns,
				query, null, null, null, null);
		return cursorToTaskList(cursor);

	}

	/**
	 * Search Tasks
	 * 
	 * @param id
	 * @return
	 */
	public static List<Task> searchName(final String query) {
		final String[] args = { "%" + query + "%" };
		final Cursor cursor = Task.database.query(Task.TABLE, Task.allColumns,
				DatabaseHelper.NAME + " LIKE ?", args, null, null, null);
		return cursorToTaskList(cursor);
	}

	private String dependencies[];

	Task() {
		super();
	}

	public Task(final long id, final String uuid, final ListMirakel list,
			final String name, final String content, final boolean done,
			final Calendar due, final Calendar reminder, final int priority,
			final Calendar created_at, final Calendar updated_at,
			final SYNC_STATE sync_state, final String additionalEntriesString,
			final int recurring, final int recurring_reminder,
			final int progress) {
		super(id, uuid, list, name, content, done, due, reminder, priority,
				created_at, updated_at, sync_state, additionalEntriesString,
				recurring, recurring_reminder, progress);
	}

	public Task(final String name) {
		super(name);
	}

	public FileMirakel addFile(final Context ctx, final String path) {
		return FileMirakel.newFile(ctx, this, path);
	}

	public void addSubtask(final Task t) {
		if (checkIfParent(t)) {
			return;
		}
		final ContentValues cv = new ContentValues();
		cv.put("parent_id", getId());
		cv.put("child_id", t.getId());
		Task.database.beginTransaction();
		Task.database.insert(Task.SUBTASK_TABLE, null, cv);
		Task.database.setTransactionSuccessful();
		Task.database.endTransaction();
	}

	public boolean checkIfParent(final Task t) {
		return isChildRec(t);
	}

	public Task create() throws NoSuchListException {
		return create(true);
	}

	public Task create(final boolean addFlag) throws NoSuchListException {
		final ContentValues values = new ContentValues();
		values.put(TaskBase.UUID, getUUID());
		values.put(DatabaseHelper.NAME, getName());
		if (getList() == null) {
			throw new NoSuchListException();
		}
		values.put(TaskBase.LIST_ID, getList().getId());
		values.put(TaskBase.CONTENT, getContent());
		values.put(TaskBase.DONE, isDone());
		values.put(
				TaskBase.DUE,
				getDue() == null ? null : DateTimeHelper
						.formatDBDateTime(getDue()));
		values.put(TaskBase.PRIORITY, getPriority());
		values.put(DatabaseHelper.SYNC_STATE_FIELD,
				addFlag ? SYNC_STATE.ADD.toInt() : SYNC_STATE.NOTHING.toInt());
		values.put(DatabaseHelper.CREATED_AT,
				DateTimeHelper.formatDateTime(getCreatedAt()));
		if (getUpdatedAt() == null) {
			setUpdatedAt(new GregorianCalendar());
		}
		values.put(DatabaseHelper.UPDATED_AT,
				DateTimeHelper.formatDateTime(getUpdatedAt()));
		values.put(TaskBase.PROGRESS, getProgress());

		values.put("additional_entries", getAdditionalEntriesString());
		Task.database.beginTransaction();
		final long insertId = Task.database.insertOrThrow(Task.TABLE, null,
				values);
		Task.database.setTransactionSuccessful();
		Task.database.endTransaction();
		final Cursor cursor = Task.database.query(Task.TABLE, Task.allColumns,
				DatabaseHelper.ID + " = " + insertId, null, null, null, null);
		cursor.moveToFirst();
		final Task newTask = cursorToTask(cursor);
		cursor.close();
		UndoHistory.logCreate(newTask, Task.context);
		return newTask;
	}

	public void deleteSubtask(final Task s) {
		Task.database.beginTransaction();
		Task.database.delete(Task.SUBTASK_TABLE, "parent_id=" + getId()
				+ " and child_id=" + s.getId(), null);
		Task.database.setTransactionSuccessful();
		Task.database.endTransaction();

	}

	/**
	 * Delete a task
	 * 
	 * @param task
	 */
	public void destroy() {
		destroy(false);
	}

	public void destroy(final boolean force) {
		if (!force) {
			UndoHistory.updateLog(this, Task.context);
		}
		final long id = getId();
		if (getSyncState() == SYNC_STATE.ADD || force) {
			Task.database.delete(Task.TABLE, DatabaseHelper.ID + " = " + id,
					null);
			FileMirakel.destroyForTask(this);
			Task.database.delete(Task.SUBTASK_TABLE, "parent_id=" + id
					+ " or child_id=" + id, null);
		} else {
			final ContentValues values = new ContentValues();
			values.put(DatabaseHelper.SYNC_STATE_FIELD,
					SYNC_STATE.DELETE.toInt());
			Task.database.update(Task.TABLE, values, DatabaseHelper.ID + "="
					+ id, null);
		}

	}

	@Override
	public boolean equals(final Object o) {

		if (!(o instanceof Task)) {
			return false;
		}
		final Task t = (Task) o;
		// Id
		if (getId() != t.getId()) {
			return false;
		}

		// List
		if (t.getList() != null && getList() != null) {
			if (t.getList().getId() != getList().getId()) {
				return false;
			}
		} else if (t.getList() != null || getList() != null) {
			return false;
		}

		// Name
		if (t.getName() != null && getName() != null) {
			if (!t.getName().equals(getName())) {
				return false;
			}
		} else if (getName() != null || t.getName() != null) {
			return false;
		}

		// Content
		if (t.getContent() != null && getContent() != null) {
			if (!t.getContent().equals(getContent())) {
				return false;
			}
		} else if (getContent() != null || t.getContent() != null) {
			return false;
		}

		// Done
		if (t.isDone() != isDone()) {
			return false;
		}

		// Due
		if (t.getDue() != null && getDue() != null) {
			if (t.getDue().compareTo(getDue()) != 0) {
				return false;
			}
		} else if (t.getDue() != null || getDue() != null) {
			return false;
		}

		// Priority
		if (t.getPriority() != getPriority()) {
			return false;
		}
		// Additional Entries
		if (t.getAdditionalEntries() != null && getAdditionalEntries() != null) {
			if (!t.getAdditionalEntries().equals(getAdditionalEntries())) {
				return false;
			}
		} else if (t.getAdditionalEntries() != null
				|| getAdditionalEntries() != null) {
			return false;
		}
		// Reminder
		if (t.getReminder() != null && getReminder() != null) {
			if (t.getReminder().compareTo(getReminder()) != 0) {
				return false;
			}
		} else if (getReminder() != null || t.getReminder() != null) {
			return false;
		}

		// progress
		if (t.getProgress() != getProgress()) {
			return false;
		}

		return true;
	}

	public String[] getDependencies() {
		return this.dependencies;
	}

	public List<FileMirakel> getFiles() {
		return FileMirakel.getForTask(this);
	}

	public int getSubtaskCount() {
		final Cursor c = Task.database.rawQuery("Select count(_id) from "
				+ Task.SUBTASK_TABLE + " where parent_id=" + getId(), null);
		c.moveToFirst();
		final int count = c.getInt(0);
		c.close();
		return count;
	}

	public List<Task> getSubtasks() {
		final Cursor c = Task.getTasksCursor(this);
		final List<Task> subTasks = new ArrayList<Task>();
		c.moveToFirst();
		while (!c.isAfterLast()) {
			subTasks.add(cursorToTask(c));
			c.moveToNext();
		}
		c.close();
		return subTasks;

	}

	private boolean isChildRec(final Task t) {
		final List<Task> subtasks = getSubtasks();
		for (final Task s : subtasks) {
			if (s.getId() == t.getId() || s.isChildRec(t)) {
				return true;
			}
		}
		return false;
	}

	public boolean isSubtaskOf(final Task otherTask) {
		if (otherTask == null) {
			return false;
		}
		final Cursor c = Task.database.rawQuery("Select count(_id) from "
				+ Task.SUBTASK_TABLE + " where parent_id=" + otherTask.getId()
				+ " AND child_id=" + getId(), null);
		c.moveToFirst();
		final int count = c.getInt(0);
		c.close();
		return count > 0;
	}

	public void safeSave() {
		safeSave(true);
	}

	/**
	 * Save a Task
	 * 
	 * @param task
	 */
	public void safeSave(final boolean log) {
		try {
			save(log);
		} catch (final NoSuchListException e) {
			Log.w(Task.TAG, "List did vanish");
		}
	}

	private void save(final boolean log) throws NoSuchListException {
		if (!isEdited()) {
			Log.d(Task.TAG, "new Task equals old, didnt need to save it");
			return;
		}
		if (isEdited(TaskBase.DONE) && isDone()) {
			setSubTasksDone();
		}
		setSyncState(getSyncState() == SYNC_STATE.ADD
				|| getSyncState() == SYNC_STATE.IS_SYNCED ? getSyncState()
				: SYNC_STATE.NEED_SYNC);
		if (Task.context != null) {
			setUpdatedAt(new GregorianCalendar());
		}
		final ContentValues values = getContentValues();
		if (log) {
			final Task old = Task.get(getId());
			UndoHistory.updateLog(old, Task.context);
		}
		Task.database.beginTransaction();
		Task.database.update(Task.TABLE, values, DatabaseHelper.ID + " = "
				+ getId(), null);
		Task.database.setTransactionSuccessful();
		Task.database.endTransaction();
		if (isEdited(TaskBase.REMINDER)
				|| isEdited(TaskBase.RECURRING_REMINDER)) {
			ReminderAlarm.updateAlarms(Task.context);
		}
		clearEdited();
		NotificationService.updateNotificationAndWidget(Task.context);
	}

	private void setDependencies(final String[] dep) {
		this.dependencies = dep;
	}

	private void setSubTasksDone() {
		final List<Task> subTasks = getSubtasks();
		for (final Task t : subTasks) {
			t.setDone(true);
			try {
				t.save(true);
			} catch (final NoSuchListException e) {
				Log.d(Task.TAG, "List did vanish");
			}
		}
	}

	public String toJson() {
		String json = "{";
		json += "\"id\":" + getId() + ",";
		json += "\"name\":\"" + getName() + "\",";
		json += "\"content\":\"" + getContent() + "\",";
		json += "\"done\":" + (isDone() ? "true" : "false") + ",";
		json += "\"priority\":" + getPriority() + ",";
		json += "\"list_id\":" + getList().getId() + ",";
		String s = "";
		if (getDue() != null) {
			s = DateTimeHelper.formatDate(getDue());
		}
		json += "\"due\":\"" + s + "\",";
		s = "";
		if (getReminder() != null) {
			s = DateTimeHelper.formatDateTime(getReminder());
		}
		json += "\"reminder\":\"" + s + "\",";
		json += "\"sync_state\":" + getSyncState() + ",";
		json += "\"created_at\":\""
				+ DateTimeHelper.formatDateTime(getCreatedAt()) + "\",";
		json += "\"updated_at\":\""
				+ DateTimeHelper.formatDateTime(getUpdatedAt()) + "\"}";
		return json;
	}
}
