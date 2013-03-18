/**
 * 
 */
package de.azapps.mirakel;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * @author weiznich
 * 
 */
public class ListsDataSource {
	private static final String TAG = "ListsDataSource";
	private SQLiteDatabase database;
	private DatabaseHelper dbHelper;
	private String[] allColumns = { "_id", "name", "sort_by", "created_at",
			"updated_at" };

	public ListsDataSource(Context context) {
		dbHelper = new DatabaseHelper(context);
	}

	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}

	public void close() {
		dbHelper.close();
	}

	public List_mirakle createList(String name) {
		return createList(name, 0);
	}

	public List_mirakle createList(String name, int sort_by) {
		ContentValues values = new ContentValues();
		values.put("name", name);
		values.put("sort_by", sort_by);
		long insertId = database.insert("lists", null, values);
		Cursor cursor = database.query("lists", allColumns,
				"_id = " + insertId, null, null, null, null);
		cursor.moveToFirst();
		List_mirakle newList = cursorToList(cursor);
		cursor.close();
		return newList;
	}

	public void saveList(List_mirakle list) {
		Log.v(TAG, "saveTask");
		ContentValues values = list.getContentValues();
		database.update("lists", values, "_id = " + list.getId(), null);
	}

	public void deleteList(List_mirakle list) {
		long id = list.getId();
		database.delete("tasks", "list_id = " + id, null);
		database.delete("lists", "_id = " + id, null);
	}

	public List<List_mirakle> getAllLists() {
		List<List_mirakle> lists = new ArrayList<List_mirakle>();
		// TODO Get from strings.xml
		lists.add(new List_mirakle(Mirakel.LIST_ALL, "All Lists",
				task_count(Mirakel.LIST_ALL)));
		lists.add(new List_mirakle(Mirakel.LIST_DAILY, "Today",
				task_count(Mirakel.LIST_DAILY)));
		lists.add(new List_mirakle(Mirakel.LIST_WEEKLY, "This Week",
				task_count(Mirakel.LIST_WEEKLY)));
		Cursor cursor = database.query("lists", allColumns, null, null, null,
				null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			List_mirakle list = cursorToList(cursor);
			lists.add(list);
			cursor.moveToNext();
		}
		cursor.close();
		return lists;
	}

	public List_mirakle getList(long id) {
		Cursor cursor = database.query("lists", allColumns, "_id='" + id + "'",
				null, null, null, null);
		cursor.moveToFirst();
		if (cursor.getCount() != 0) {
			List_mirakle t = cursorToList(cursor);
			cursor.close();
			return t;
		}
		return null;
	}

	private int task_count(int list_id) {
		String count = "Select count(_id) from tasks ";
		switch (list_id) {
		case Mirakel.LIST_ALL:
			break;
		case Mirakel.LIST_DAILY:
			count += "where due<=date('now')";
			break;
		case Mirakel.LIST_WEEKLY:
			count += "where due<=date('now','+7 days')";
			break;
		default:
			count += "where list_id=" + list_id;

		}
		Cursor c = database.rawQuery(count, null);
		c.moveToFirst();
		int task_count = c.getInt(0);
		c.close();
		return task_count;
	}

	private List_mirakle cursorToList(Cursor cursor) {
		int i = 0;
		int id = cursor.getInt(i++);
		List_mirakle list = new List_mirakle(id, cursor.getString(i++),
				cursor.getShort(i++), cursor.getString(i++),
				cursor.getString(i), task_count(id));
		return list;
	}

}
