package de.azapps.mirakel.model.semantic;

import java.util.ArrayList;
import java.util.Arrays;
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
import de.azapps.mirakel.model.list.SearchList;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.task.Task;

public class Semantic extends SemanticBase {
	public static final String TABLE = "semantic_conditions";
	private static SQLiteDatabase database;
	@SuppressWarnings("unused")
	private static final String TAG = "Semantic";
	private static DatabaseHelper dbHelper;
	private static final String[] allColumns = { "_id", "condition",
			"priority", "due", "default_list_id" };
	private static Map<String, Semantic> semantics = new HashMap<String, Semantic>();

	protected Semantic(int id, String condition, Integer priority, Integer due,
			ListMirakel list) {
		super(id, condition, priority, due, list);
	}

	public void save() {
		ContentValues values = getContentValues();
		database.update(TABLE, values, "_id = " + getId(), null);
		initAll();
	}

	// Static

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

	/**
	 * Close the Database-Connection
	 */
	public static void close() {
		dbHelper.close();
	}

	public static Semantic newSemantic(String condition, Integer priority,
			Integer due, ListMirakel list) {
		Semantic m = new Semantic(0, condition, priority, due, list);
		return m.create();
	}

	public Semantic create() {
		ContentValues values = getContentValues();
		values.remove("_id");
		int insertId = (int) database.insertOrThrow(TABLE, null, values);
		initAll();
		return Semantic.get(insertId);
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

	public void destroy() {
		database.delete(TABLE, "_id=" + getId(), null);
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

	private static Semantic cursorToSemantic(Cursor c) {
		int id = c.getInt(0);
		// BE CAREFUL!!!! – Don't forget to change the numbers
		String condition = c.getString(1);
		Integer priority = null;
		if (!c.isNull(2))
			priority = c.getInt(2);
		Integer due = null;
		if (!c.isNull(3))
			due = c.getInt(3);
		ListMirakel list = null;
		if (!c.isNull(4))
			list = ListMirakel.getList(c.getInt(4));

		return new Semantic(id, condition, priority, due, list);
	}

	public static Task createTask(String taskName, ListMirakel currentList,
			boolean useSemantic, Context context) throws NoListsException {
		GregorianCalendar due = null;
		int prio = 0;
		if (currentList instanceof SearchList) {
			currentList = SpecialList.firstSpecial();
		}
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
									}
								} catch (NumberFormatException e) {
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
				throw new NoListsException();
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
				if (s == null)
					break;
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
				taskName = taskName.substring(word.length()).trim();
				words.remove(0);
			}
		}
		if (currentList == null) {
			currentList = ListMirakel.safeFirst(context);
		}
		if (currentList == null) {
			throw new NoListsException();
		}
		return Task.newTask(taskName, currentList, due, prio);
	}

	public static class NoListsException extends Exception {
		private static final long serialVersionUID = 1380190481;

	}
}
