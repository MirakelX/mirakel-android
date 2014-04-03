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
import android.util.TypedValue;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.error.ErrorReporter;
import de.azapps.mirakel.helper.error.ErrorType;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.FileUtils;

public class FileMirakel extends FileBase {

	private static final String[] allColumns = { "_id", "task_id", "name",
			"path" };
	public static final String cacheDirPath = FileUtils.getMirakelDir()
			+ "image_cache";
	private static SQLiteDatabase database;

	private static DatabaseHelper dbHelper;
	public static final File fileCacheDir = new File(cacheDirPath);
	public static final String TABLE = "files";

	// private static final String TAG = "FileMirakel";

	/**
	 * Get all Tasks
	 * 
	 * @return
	 */
	public static List<FileMirakel> all() {
		final List<FileMirakel> files = new ArrayList<FileMirakel>();
		final Cursor c = database.query(TABLE, allColumns, null, null, null,
				null, null);
		c.moveToFirst();
		while (!c.isAfterLast()) {
			files.add(cursorToFile(c));
			c.moveToNext();
		}
		c.close();
		return files;
	}

	/**
	 * Close the Database-Connection
	 */
	public static void close() {
		dbHelper.close();
	}

	private static FileMirakel cursorToFile(final Cursor cursor) {
		int i = 0;
		return new FileMirakel(cursor.getInt(i++),
				Task.get(cursor.getInt(i++)), cursor.getString(i++),
				cursor.getString(i++));
	}

	// Static Methods

	public static void destroyForTask(final Task t) {
		final List<FileMirakel> files = getForTask(t);
		database.beginTransaction();
		for (final FileMirakel file : files) {
			final File destFile = new File(FileMirakel.fileCacheDir,
					file.getId() + ".png");
			if (destFile.exists()) {
				destFile.delete();
			}
			file.destroy(false);
		}
		database.setTransactionSuccessful();
		database.endTransaction();
	}

	/**
	 * Get a Task by id
	 * 
	 * @param id
	 * @return
	 */
	public static FileMirakel get(final int id) {
		final Cursor cursor = database.query(TABLE, allColumns, "_id=" + id,
				null, null, null, null);
		cursor.moveToFirst();
		if (cursor.getCount() != 0) {
			final FileMirakel t = cursorToFile(cursor);
			cursor.close();
			return t;
		}
		cursor.close();
		return null;
	}

	public static int getFileCount(final Task t) {
		if (t == null) {
			return 0;
		}
		final Cursor c = database.rawQuery("Select count(_id) from " + TABLE
				+ " where task_id=?", new String[] { "" + t.getId() });
		c.moveToFirst();
		final int count = c.getInt(0);
		c.close();
		return count;
	}

	public static List<FileMirakel> getForTask(final Task task) {
		final List<FileMirakel> files = new ArrayList<FileMirakel>();
		final Cursor cursor = database.query(TABLE, allColumns, "task_id="
				+ task.getId(), null, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			files.add(cursorToFile(cursor));
			cursor.moveToNext();
		}
		cursor.close();
		return files;
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
	}

	public static FileMirakel newFile(final Context ctx, final Task task,
			final String file_path) {
		if (file_path == null) {
			return null;
		}
		final File osFile = new File(file_path);

		if (osFile.exists()) {
			final String name = osFile.getName();
			final FileMirakel newFile = FileMirakel.newFile(task, name,
					file_path);

			// new ExifInterface(osFile.getAbsolutePath());
			// int orientation = exif.getAttributeInt(
			// ExifInterface.TAG_ORIENTATION,
			// ExifInterface.ORIENTATION_NORMAL);
			// int rotate = 0;
			// switch (orientation) {
			// case ExifInterface.ORIENTATION_ROTATE_270:
			// rotate -= 90;
			// case ExifInterface.ORIENTATION_ROTATE_180:
			// rotate -= 90;
			// case ExifInterface.ORIENTATION_ROTATE_90:
			// rotate -= 90;
			// default:
			// break;
			// }
			// Cache the image
			Bitmap myBitmap = null;
			try {
				myBitmap = BitmapFactory.decodeFile(osFile.getAbsolutePath());
			} catch (final OutOfMemoryError e) {
				ErrorReporter.report(ErrorType.FILE_TO_LARGE_FOR_THUMBNAIL);
			}
			if (myBitmap != null) {

				final float size = TypedValue.applyDimension(
						TypedValue.COMPLEX_UNIT_DIP, 150, ctx.getResources()
								.getDisplayMetrics());
				myBitmap = Helpers.getScaleImage(myBitmap, size);
				// create directory if not exists

				boolean success = true;
				if (!FileMirakel.fileCacheDir.exists()) {
					success = FileMirakel.fileCacheDir.mkdirs();
				}
				if (success) {
					try {
						final File destFile = new File(
								FileMirakel.fileCacheDir, newFile.getId()
										+ ".png");
						final FileOutputStream out = new FileOutputStream(
								destFile);
						myBitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
						out.close();
					} catch (final Exception e) {
						e.printStackTrace();
					}
				}
			}
			return newFile;
		}
		return null;
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
	public static FileMirakel newFile(final Task task, final String name,
			final String path) {
		final FileMirakel m = new FileMirakel(0, task, name, path);
		return m.create();
	}

	protected FileMirakel(final int id, final Task task, final String name,
			final String path) {
		super(id, task, name, path);
	}

	public FileMirakel create() {
		final ContentValues values = getContentValues();
		values.remove("_id");
		database.beginTransaction();
		final int insertId = (int) database.insertOrThrow(TABLE, null, values);
		database.setTransactionSuccessful();
		database.endTransaction();
		return FileMirakel.get(insertId);
	}

	public void destroy() {
		destroy(true);
	}

	public void destroy(final boolean oneTransaction) {
		if (oneTransaction) {
			database.beginTransaction();
		}
		database.delete(TABLE, "_id=" + getId(), null);
		if (oneTransaction) {
			database.setTransactionSuccessful();
			database.endTransaction();
		}
		new File(fileCacheDir, getId() + ".png").delete();
	}

	@Override
	public boolean equals(final Object o) {
		if (o instanceof FileMirakel) {
			final FileMirakel f = (FileMirakel) o;
			return f.getId() == getId() && f.getPath().equals(getPath());
		}
		return false;
	}

	public Bitmap getPreview() {
		final File osFile = new File(fileCacheDir, getId() + ".png");
		if (osFile.exists()) {
			return BitmapFactory.decodeFile(osFile.getAbsolutePath());
		}
		return null;
	}

	public void save() {
		database.beginTransaction();
		final ContentValues values = getContentValues();
		database.update(TABLE, values, "_id = " + getId(), null);
		database.setTransactionSuccessful();
		database.endTransaction();
	}

}
