package de.azapps.mirakel.model.semantic;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.list.ListMirakel;

public class Semantic extends SemanticBase {
	public static final String TABLE = "semantic_conditions";
	private static SQLiteDatabase database;
	@SuppressWarnings("unused")
	private static final String TAG = "Semantic";
	private static DatabaseHelper dbHelper;
	private static final String[] allColumns = { "_id", "condition",
			"priority", "default_list_id" };

	protected Semantic(int id, String condition, int priority, ListMirakel list) {
		super(id, condition, priority, list);
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
	}

	/**
	 * Close the Database-Connection
	 */
	public static void close() {
		dbHelper.close();
	}

	public static Semantic newFile(String condition, int priority,
			ListMirakel list) {
		Semantic m = new Semantic(0, condition, priority, list);
		return m.create();
	}

	public Semantic create() {
		ContentValues values = getContentValues();
		values.remove("_id");
		int insertId = (int) database.insertOrThrow(TABLE, null, values);
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
		return null;
	}

	public void destroy() {
		database.delete(TABLE, "_id=" + getId(), null);
	}

	public static List<Semantic> all() {
		List<Semantic> semantics = new ArrayList<Semantic>();
		Cursor c = database.query(TABLE, allColumns, null, null, null, null,
				null);
		c.moveToFirst();
		while (!c.isAfterLast()) {
			semantics.add(cursorToSemantic(c));
			c.moveToNext();
		}
		return semantics;
	}

	private static Semantic cursorToSemantic(Cursor c) {
		int i = 0;
		return new Semantic(c.getInt(i++), c.getString(i++), c.getInt(i++),
				ListMirakel.getList(c.getInt(i++)));
	}

}
