package de.azapps.mirakel.model.semantic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.task.Task;

public class Semantic extends SemanticBase {
	private static final String[] allColumns = { "_id", "condition",
			"priority", "due", "default_list_id", "weekday" };
	private static SQLiteDatabase database;
	private static DatabaseHelper dbHelper;
	private static Map<String, Semantic> semantics = new HashMap<String, Semantic>();
	public static final String TABLE = "semantic_conditions";
	@SuppressWarnings("unused")
	private static final String TAG = "Semantic";

	public static List<Semantic> all() {
		Cursor c = database.query(TABLE, allColumns, null, null, null, null,
				null);
		c.moveToFirst();
		List<Semantic> all = new ArrayList<Semantic>();
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
			boolean useSemantic, Context context) {
		GregorianCalendar due = null;
		int prio = 0;
		if (currentList != null && currentList.isSpecialList()) {
			try {
				SpecialList slist = (SpecialList) currentList;
				currentList = slist.getDefaultList();

				if (slist.getDefaultDate() != null) {
					due = new GregorianCalendar();
					due.add(GregorianCalendar.DAY_OF_MONTH,
							slist.getDefaultDate());
				}
				if (slist.getWhereQuery(false).contains("priority")) {
					boolean[] mSelectedItems = new boolean[5];
					boolean not = false;
					String[] p = slist.getWhereQuery(false).split("and");
					for (String s : p) {
						if (s.contains("priority")) {
							not = s.contains("not");
							String[] r = s
									.replace(
											(!not ? "" : "not ")
													+ "priority in (", "")
									.replace(")", "").trim().split(",");
							for (String t : r) {
								try {
									switch (Integer.parseInt(t)) {
									case -2:
										mSelectedItems[0] = true;
										break;
									case -1:
										mSelectedItems[1] = true;
										break;
									case 0:
										mSelectedItems[2] = true;
										break;
									case 1:
										mSelectedItems[3] = true;
										break;
									case 2:
										mSelectedItems[4] = true;
										break;
										default:
											break;
									}
								} catch (NumberFormatException e) {
									//eat it
								}
								for (int i = 0; i < mSelectedItems.length; i++) {
									if (mSelectedItems[i] != not) {
										prio = i - 2;
										break;
									}
								}
							}
							break;
						}
					}
				}
			} catch (NullPointerException e) {
				currentList = ListMirakel.safeFirst(context);
			}
		}

		if (useSemantic) {
			GregorianCalendar tempdue = new GregorianCalendar();
			String lowername = taskName.toLowerCase(Locale.getDefault());
			List<String> words = new ArrayList<String>(Arrays.asList(lowername
					.split("\\s+")));
			while (words.size() > 1) {
				String word = words.get(0);

				Semantic s = semantics.get(word);
				if (s == null) {
					break;
				}
				// Set due
				if (s.getDue() != null) {
					tempdue.add(GregorianCalendar.DAY_OF_MONTH, s.getDue());
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
		}
		if (currentList == null) {
			currentList = ListMirakel.safeFirst(context);
		}
		return Task.newTask(taskName, currentList, due, prio);
	}

	private static Semantic cursorToSemantic(Cursor c) {
		int id = c.getInt(0);
		// BE CAREFUL!!!! – Don't forget to change the numbers
		String condition = c.getString(1);
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
			list = ListMirakel.getList(c.getInt(4));
		}
		Integer weekday = null;
		if (!c.isNull(5)) {
			weekday = c.getInt(5);
		}

		return new Semantic(id, condition, priority, due, list, weekday);
	}

	public static Semantic first() {
		Cursor cursor = database.query(TABLE, allColumns, null, null, null,
				null, null);
		cursor.moveToFirst();
		if (cursor.getCount() != 0) {
			Semantic s = cursorToSemantic(cursor);
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
	public static Semantic get(int id) {
		Cursor cursor = database.query(TABLE, allColumns, "_id=" + id, null,
				null, null, null);
		cursor.moveToFirst();
		if (cursor.getCount() != 0) {
			Semantic s = cursorToSemantic(cursor);
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
	public static void init(Context context) {
		dbHelper = new DatabaseHelper(context);
		database = dbHelper.getWritableDatabase();
		initAll();
	}

	private static void initAll() {
		Cursor c = database.query(TABLE, allColumns, null, null, null, null,
				null);
		c.moveToFirst();
		while (!c.isAfterLast()) {
			Semantic s = cursorToSemantic(c);
			semantics.put(s.getCondition(), s);
			c.moveToNext();
		}
		c.close();
	}

	public static Semantic newSemantic(String condition, Integer priority,
			Integer due, ListMirakel list, Integer weekday) {
		Semantic m = new Semantic(0, condition, priority, due, list, weekday);
		return m.create();
	}

	protected Semantic(int id, String condition, Integer priority, Integer due,
			ListMirakel list, Integer weekday) {
		super(id, condition, priority, due, list, weekday);
	}

	public Semantic create() {
		database.beginTransaction();
		ContentValues values = getContentValues();
		values.remove("_id");
		int insertId = (int) database.insertOrThrow(TABLE, null, values);
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
		ContentValues values = getContentValues();
		database.update(TABLE, values, "_id = " + getId(), null);
		database.setTransactionSuccessful();
		database.endTransaction();
		initAll();
	}
}
