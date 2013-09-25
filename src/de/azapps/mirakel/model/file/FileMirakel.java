package de.azapps.mirakel.model.file;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.Mirakel.NoSuchListException;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.task.Task;

public class FileMirakel extends FileBase {

	public static final File cacheDir = new File(Environment.getDataDirectory()
			+ "/data/" + Mirakel.APK_NAME + "/image_cache");
	public static final String TABLE = "files";
	private static final String TAG = "FileMirakel";
	private static SQLiteDatabase database;
	private static DatabaseHelper dbHelper;
	private static final String[] allColumns = { "_id", "task_id", "name",
			"path" };

	protected FileMirakel(int id, Task task, String name, String path) {
		super(id, task, name, path);
	}

	public void save(boolean log) throws NoSuchListException {
		ContentValues values = getContentValues();
		database.update(TABLE, values, "_id = " + getId(), null);
	}

	public Bitmap getPreview() {
		File osFile = new File(cacheDir, getId() + ".png");
		if (osFile.exists()) {
			return BitmapFactory.decodeFile(osFile.getAbsolutePath());
		}
		return null;
	}

	// Static Methods

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

	public static FileMirakel newFile(Task task, String file_path) {
		File osFile = new File(file_path);
		if (osFile.exists()) {
			String name = osFile.getName();
			FileMirakel newFile = FileMirakel.newFile(task, name, file_path);
			// Cache the image

			Bitmap myBitmap = BitmapFactory
					.decodeFile(osFile.getAbsolutePath());
			if (myBitmap != null) {
				myBitmap = Bitmap.createScaledBitmap(myBitmap, 150, 150, true);
				// create directory if not exists

				boolean success = true;
				if (!FileMirakel.cacheDir.exists()) {
					success = FileMirakel.cacheDir.mkdir();
				}
				if (success) {
					try {
						File destFile = new File(FileMirakel.cacheDir,
								newFile.getId() + ".png");
						FileOutputStream out = new FileOutputStream(destFile);
						myBitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
						out.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			return newFile;
		}
		return null;
	}

	public FileMirakel create() {

		ContentValues values = getContentValues();
		values.remove("_id");
		int insertId = (int) database.insertOrThrow(TABLE, null, values);
		return FileMirakel.get(insertId);
	}
	
	public void destroy(){
		database.delete(TABLE, "_id="+getId(),null);
		new File(cacheDir,getId() + ".png").delete();
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
	
	public static int getFileCount(Task t){
		Cursor c=database.rawQuery("Select count(_id) from "+TABLE+" where task_id=?", new String[]{""+t.getId()});
		c.moveToFirst();
		int count=c.getInt(0);
		c.close();
		return count;
	}

}
