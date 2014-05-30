package de.azapps.mirakel.model.semantic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.list.meta.SpecialListsPriorityProperty;
import de.azapps.mirakel.model.task.Task;

public class Semantic extends SemanticBase {
	private static final String[] allColumns = { "_id", "condition",
			"priority", "due", "default_list_id", "weekday" };
	private static SQLiteDatabase database;
	private static DatabaseHelper dbHelper;
	private static Map<String, Semantic> semantics = new HashMap<String, Semantic>();
	public static final String TABLE = "semantic_conditions";

	public static List<Semantic> all() {
		final Cursor c = database.query(TABLE, allColumns, null, null, null,
				null, null);
		c.moveToFirst();
		final List<Semantic> all = new ArrayList<Semantic>();
		while (!c.isAfterLast()) {
			all.add(cursorToSemantic(c));
			c.moveToNext();
		}
		c.close();
		return all;
	}

	/**
	 * Close the Database-Connection
	 */
	public static void close() {
		dbHelper.close();
	}

	// Static

	public static Task createTask(String taskName, ListMirakel currentList,
			final boolean useSemantic, final Context context) {
		GregorianCalendar due = null;
		int prio = 0;
		if (currentList != null && currentList.isSpecial()) {
			try {
				final SpecialList slist = (SpecialList) currentList;
				currentList = slist.getDefaultList();

				if (slist.getDefaultDate() != null) {
					due = new GregorianCalendar();
					due.add(Calendar.DAY_OF_MONTH, slist.getDefaultDate());
				}
				if (slist.getWhere().containsKey(Task.PRIORITY)) {
					final SpecialListsPriorityProperty prop = (SpecialListsPriorityProperty) slist
							.getWhere().get(Task.PRIORITY);
					final boolean not = prop.isNegated();
					prio = not ? -2 : 2;
					final List<Integer> content = prop.getContent();
					Collections.sort(content);
					final int length = prop.getContent().size();
					for (int i = not ? 0 : length - 1; not ? i < length
							: i >= 0; i += not ? 1 : -1) {
						if (not && prio == content.get(i)) {
							--prio;
						} else if (!not && prio == content.get(i)) {
							prio = content.get(i);
						}
					}
				}
			} catch (final NullPointerException e) {
				currentList = ListMirakel.safeFirst(context);
			}
		}

		if (useSemantic) {
			GregorianCalendar tempdue = new GregorianCalendar();
			final String lowername = taskName.toLowerCase(Locale.getDefault());
			final List<String> words = new ArrayList<String>(
					Arrays.asList(lowername.split("\\s+")));
			while (words.size() > 1) {
				final String word = words.get(0);

				final Semantic s = semantics.get(word);
				if (s == null) {
					break;
				}
				// Set due
				if (s.getDue() != null) {
					tempdue.add(Calendar.DAY_OF_MONTH, s.getDue());
					due = tempdue;
				}
				// Set priority
				if (s.getPriority() != null) {
					prio = s.getPriority();
				}
				// Set list
				if (s.getList() != null) {
					currentList = s.getList();
				}
				// Weekday?
				if (s.getWeekday() != null) {
					tempdue = new GregorianCalendar();
					int nextWeekday = s.getWeekday() + 1;
					// Because there are some dudes which means, sunday is the
					// first day of the week… That's obviously wrong!
					if (nextWeekday == 8) {
						nextWeekday = 1;
					}
					do {
						tempdue.add(Calendar.DAY_OF_YEAR, 1);
					} while (tempdue.get(Calendar.DAY_OF_WEEK) != nextWeekday);
					due = tempdue;

				}
				taskName = taskName.substring(word.length()).trim();
				words.remove(0);
			}
			if (due != null) {
				due.set(Calendar.HOUR_OF_DAY, 0);
				due.set(Calendar.MINUTE, 0);
				due.set(Calendar.SECOND, 0);
				due.add(Calendar.SECOND,
						DateTimeHelper.getTimeZoneOffset(false, due));
			}
		}
		if (currentList == null) {
			currentList = ListMirakel.safeFirst(context);
		}
		return Task.newTask(taskName, currentList, due, prio);
	}

	private static Semantic cursorToSemantic(final Cursor c) {
		final int id = c.getInt(0);
		// BE CAREFUL!!!! – Don't forget to change the numbers
		final String condition = c.getString(1);
		Integer priority = null;
		if (!c.isNull(2)) {
			priority = c.getInt(2);
		}
		Integer due = null;
		if (!c.isNull(3)) {
			due = c.getInt(3);
		}
		ListMirakel list = null;
		if (!c.isNull(4)) {
			list = ListMirakel.get(c.getInt(4));
		}
		Integer weekday = null;
		if (!c.isNull(5)) {
			weekday = c.getInt(5);
		}

		return new Semantic(id, condition, priority, due, list, weekday);
	}

	public static Semantic first() {
		final Cursor cursor = database.query(TABLE, allColumns, null, null,
				null, null, null);
		cursor.moveToFirst();
		if (cursor.getCount() != 0) {
			final Semantic s = cursorToSemantic(cursor);
			cursor.close();
			return s;
		}
		cursor.close();
		return null;
	}

	/**
	 * Get a Semantic by id
	 * 
	 * @param id
	 * @return
	 */
	public static Semantic get(final int id) {
		final Cursor cursor = database.query(TABLE, allColumns, "_id=" + id,
				null, null, null, null);
		cursor.moveToFirst();
		if (cursor.getCount() != 0) {
			final Semantic s = cursorToSemantic(cursor);
			cursor.close();
			return s;
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
	public static void init(final Context context) {
		dbHelper = new DatabaseHelper(context);
		database = dbHelper.getWritableDatabase();
		initAll();
	}

	/**
	 * CALL THIS ONLY FROM DBHelper
	 * 
	 * @param db
	 */
	public static void setDB(final SQLiteDatabase db) {
		database = db;
		initAll();
	}

	private static void initAll() {
		final Cursor c = database.query(TABLE, allColumns, null, null, null,
				null, null);
		c.moveToFirst();
		while (!c.isAfterLast()) {
			final Semantic s = cursorToSemantic(c);
			semantics.put(s.getCondition(), s);
			c.moveToNext();
		}
		c.close();
	}

	public static Semantic newSemantic(final String condition,
			final Integer priority, final Integer due, final ListMirakel list,
			final Integer weekday) {
		final Semantic m = new Semantic(0, condition, priority, due, list,
				weekday);
		return m.create();
	}

	protected Semantic(final int id, final String condition,
			final Integer priority, final Integer due, final ListMirakel list,
			final Integer weekday) {
		super(id, condition, priority, due, list, weekday);
	}

	public Semantic create() {
		database.beginTransaction();
		final ContentValues values = getContentValues();
		values.remove("_id");
		final int insertId = (int) database.insertOrThrow(TABLE, null, values);
		database.setTransactionSuccessful();
		database.endTransaction();
		initAll();
		return Semantic.get(insertId);
	}

	public void destroy() {
		database.beginTransaction();
		database.delete(TABLE, "_id=" + getId(), null);
		database.setTransactionSuccessful();
		database.endTransaction();
		initAll();
	}

	public void save() {
		database.beginTransaction();
		final ContentValues values = getContentValues();
		database.update(TABLE, values, "_id = " + getId(), null);
		database.setTransactionSuccessful();
		database.endTransaction();
		initAll();
	}
}
