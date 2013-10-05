package de.azapps.mirakel.model.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.util.Log;
import android.util.TypedValue;
import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.Mirakel.NoSuchListException;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.task.Task;

public class FileMirakel extends FileBase {

	public static final String cacheDirPath = Mirakel.MIRAKEL_DIR
			+ "image_cache";
	public static final File cacheDir = new File(cacheDirPath);
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

	public static FileMirakel newFile(Context ctx, Task task, String file_path) {
		if (file_path == null)
			return null;
		try {
			File osFile = new File(file_path);

			if (osFile.exists()) {
				String name = osFile.getName();
				FileMirakel newFile = FileMirakel
						.newFile(task, name, file_path);

				ExifInterface exif = new ExifInterface(osFile.getAbsolutePath());
				int orientation = exif.getAttributeInt(
						ExifInterface.TAG_ORIENTATION,
						ExifInterface.ORIENTATION_NORMAL);
				int rotate = 0;
				switch (orientation) {
				case ExifInterface.ORIENTATION_ROTATE_270:
					rotate -= 90;
				case ExifInterface.ORIENTATION_ROTATE_180:
					rotate -= 90;
				case ExifInterface.ORIENTATION_ROTATE_90:
					rotate -= 90;
				}
				// Cache the image

				Bitmap myBitmap = BitmapFactory.decodeFile(osFile
						.getAbsolutePath());
				if (myBitmap != null) {

					float size = TypedValue.applyDimension(
							TypedValue.COMPLEX_UNIT_DIP, 150, ctx
									.getResources().getDisplayMetrics());
					myBitmap = Helpers.getScaleImage(myBitmap, size, rotate);
					// create directory if not exists

					boolean success = true;
					if (!FileMirakel.cacheDir.exists()) {
						success = FileMirakel.cacheDir.mkdirs();
					}
					if (success) {
						try {
							File destFile = new File(FileMirakel.cacheDir,
									newFile.getId() + ".png");
							FileOutputStream out = new FileOutputStream(
									destFile);
							myBitmap.compress(Bitmap.CompressFormat.PNG, 90,
									out);
							out.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				return newFile;
			}
		} catch (IOException e) {
			Log.wtf(TAG, "This should never ever happen…");
		}
		return null;
	}

	public FileMirakel create() {

		ContentValues values = getContentValues();
		values.remove("_id");
		int insertId = (int) database.insertOrThrow(TABLE, null, values);
		return FileMirakel.get(insertId);
	}

	public void destroy() {
		database.delete(TABLE, "_id=" + getId(), null);
		new File(cacheDir, getId() + ".png").delete();
	}

	public static void destroyForTask(Task t) {
		List<FileMirakel> files = getForTask(t);
		for (FileMirakel file : files) {
			File destFile = new File(FileMirakel.cacheDir, file.getId()
					+ ".png");
			if (destFile.exists()) {
				destFile.delete();
			}
			file.destroy();
		}
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
		c.close();
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
		cursor.close();
		return null;
	}

	public static List<FileMirakel> getForTask(Task task) {
		List<FileMirakel> files = new ArrayList<FileMirakel>();
		Cursor cursor = database.query(TABLE, allColumns, "task_id=" + task.getId(),
				null, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			files.add(cursorToFile(cursor));
			cursor.moveToNext();
		}
		cursor.close();
		return files;
	}

	private static FileMirakel cursorToFile(Cursor cursor) {
		int i = 0;
		return new FileMirakel(cursor.getInt(i++),
				Task.get(cursor.getInt(i++)), cursor.getString(i++),
				cursor.getString(i++));
	}

	public static int getFileCount(Task t) {
		Cursor c = database.rawQuery("Select count(_id) from " + TABLE
				+ " where task_id=?", new String[] { "" + t.getId() });
		c.moveToFirst();
		int count = c.getInt(0);
		c.close();
		return count;
	}

}
