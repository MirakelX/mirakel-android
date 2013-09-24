package de.azapps.mirakel.model.file;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import de.azapps.mirakel.Mirakel.NoSuchListException;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.task.Task;

public class FileMirakel extends FileBase {

	public static final String TABLE = "files";
	private static final String TAG = "TasksDataSource";
	private static SQLiteDatabase database;
	private static DatabaseHelper dbHelper;
	private static final String[] allColumns = { "_id", "task_id", "name",
			"path" };
	private static Context context;

	protected FileMirakel(int id, Task task, String name, String path) {
		super(id, task, name, path);
	}

	public void save(boolean log) throws NoSuchListException {
		ContentValues values = getContentValues();
		database.update(TABLE, values, "_id = " + getId(), null);
	}

	// Static Methods

	/**
	 * Initialize the Database and the preferences
	 * 
	 * @param context
	 *            The Application-Context
	 */
	public static void init(Context context) {
		FileMirakel.context = context;
		dbHelper = new DatabaseHelper(context);
		database = dbHelper.getWritableDatabase();
	}

	/**
	 * Close the Database-Connection
	 */
	public static void close() {
		dbHelper.close();
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
	public static FileMirakel newFile(Task task, String name, String path) {
		FileMirakel m = new FileMirakel(0, task, name, path);
		return m.create();
	}

	public FileMirakel create() {

		ContentValues values = getContentValues();
		values.remove("_id");
		int insertId = (int) database.insertOrThrow(TABLE, null, values);
		return FileMirakel.get(insertId);
	}

	/**
	 * Get all Tasks
	 * 
	 * @return
	 */
	public static List<FileMirakel> all() {
		List<FileMirakel> files = new ArrayList<FileMirakel>();
		Cursor c = database.query(TABLE, allColumns, null, null, null, null,
				null);
		c.moveToFirst();
		while (!c.isAfterLast()) {
			files.add(cursorToFile(c));
			c.moveToNext();
		}
		return files;
	}

	/**
	 * Get a Task by id
	 * 
	 * @param id
	 * @return
	 */
	public static FileMirakel get(int id) {
		Cursor cursor = database.query(TABLE, allColumns, "_id=" + id, null,
				null, null, null);
		cursor.moveToFirst();
		if (cursor.getCount() != 0) {
			FileMirakel t = cursorToFile(cursor);
			cursor.close();
			return t;
		}
		return null;
	}

	public static List<FileMirakel> getForTask(Task task) {
		List<FileMirakel> files = new ArrayList<FileMirakel>();
		Cursor c = database.query(TABLE, allColumns, "task_id=" + task.getId(),
				null, null, null, null);
		c.moveToFirst();
		while (!c.isAfterLast()) {
			files.add(cursorToFile(c));
			c.moveToNext();
		}
		return files;
	}

	private static FileMirakel cursorToFile(Cursor cursor) {
		int i = 0;
		return new FileMirakel(cursor.getInt(i++),
				Task.get(cursor.getInt(i++)), cursor.getString(i++),
				cursor.getString(i++));
	}

}
