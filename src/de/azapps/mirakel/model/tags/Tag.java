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
package de.azapps.mirakel.model.tags;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.task.Task;

public class Tag extends TagBase {

	public static final String TABLE = "tag";
	public static final String[] allColumns = { DatabaseHelper.ID, DARK_TEXT,
			DatabaseHelper.NAME, BACKGROUND_COLOR_A, BACKGROUND_COLOR_R,
			BACKGROUND_COLOR_G, BACKGROUND_COLOR_B };
	private static Context context;
	private static DatabaseHelper dbHelper;
	private static SQLiteDatabase database;

	public Tag(final int id, final boolean isDarkBackground, final String name,
			final int backColor) {
		super(id, isDarkBackground, backColor, name);
	}

	public static int count() {
		int count = 0;
		final Cursor c = database.rawQuery("SELECT count(*) FROM " + TABLE,
				null);
		c.moveToFirst();
		if (c.getCount() > 0) {
			count = c.getInt(0);
		}
		c.close();
		return count;
	}

	public static Tag newTag(final String name) {
		return newTag(name, true);
	}

	public static Tag newTag(final String name, final boolean dark) {
		return newTag(name, dark, getNextColor(count(), context));

	}

	public static int getNextColor(final int count, final Context ctx) {
		final TypedArray ta = ctx.getResources().obtainTypedArray(
				R.array.default_colors);
		final int transparency[] = ctx.getResources().getIntArray(
				R.array.default_transparency);
		final int alpha = count / ta.length() % transparency.length;
		final int colorPos = count % ta.length();
		final int color = android.graphics.Color.argb(transparency[alpha],
				Color.red(ta.getColor(colorPos, 0)),
				Color.green(ta.getColor(colorPos, 0)),
				Color.blue(ta.getColor(colorPos, 0)));
		ta.recycle();
		return color;
	}

	public static Tag newTag(final String name, final boolean dark,
			final int color) {

		final ContentValues cv = new ContentValues();
		cv.put(DatabaseHelper.NAME, name);
		cv.put(DARK_TEXT, dark);
		cv.put(BACKGROUND_COLOR_R, Color.red(color));
		cv.put(BACKGROUND_COLOR_G, Color.green(color));
		cv.put(BACKGROUND_COLOR_B, Color.blue(color));
		cv.put(BACKGROUND_COLOR_A, Color.alpha(color));
		final int id = (int) database.insert(TABLE, null, cv);
		return getTag(id);
	}

	public static Tag getTag(final int id) {
		final Cursor c = database.query(TABLE, allColumns, DatabaseHelper.ID
				+ "=?", new String[] { "" + id }, null, null, null);
		c.moveToFirst();
		final Tag t = cursorToTag(c);
		c.close();
		return t;
	}

	public static Tag cursorToTag(final Cursor c) {
		if (c.getCount() > 0) {
			return new Tag(c.getInt(0), c.getShort(1) == 1, c.getString(2),
					Color.argb(c.getShort(3), c.getShort(4), c.getShort(5),
							c.getShort(6)));
		}
		return null;
	}

	public static List<Tag> all() {
		final Cursor c = database.query(TABLE, allColumns, null, null, null,
				null, null);
		return cursorToTagList(c);

	}

	public static List<Tag> cursorToTagList(final Cursor c) {
		final List<Tag> tags = new ArrayList<Tag>();
		if (c.getCount() > 0) {
			c.moveToFirst();
			do {
				tags.add(cursorToTag(c));
			} while (c.moveToNext());
		}
		c.close();
		return tags;
	}

	/**
	 * Close the Database-Connection
	 */
	public static void close() {
		dbHelper.close();
	}

	public static Tag getByName(final String name) {
		final Cursor c = database.query(TABLE, allColumns, DatabaseHelper.NAME
				+ "=?", new String[] { name }, null, null, null);
		c.moveToFirst();
		final Tag t = cursorToTag(c);
		c.close();
		return t;
	}

	/**
	 * Initialize the Database and the preferences
	 * 
	 * @param context
	 *            The Application-Context
	 */
	public static void init(final Context ctx) {
		context = ctx;
		dbHelper = new DatabaseHelper(context);
		database = dbHelper.getWritableDatabase();
	}

	@Override
	public String toString() {
		return getName();
	}

	public void destroy() {
		database.delete(TABLE, DatabaseHelper.ID + "=?", new String[] { getId()
				+ "" });
	}

	public void save() {
		database.update(TABLE, getContentValues(), DatabaseHelper.ID + "=?",
				new String[] { getId() + "" });
	}

	@Override
	public boolean equals(final Object o) {
		if (o instanceof Tag) {
			final Tag t = (Tag) o;
			return t.getId() == getId();
		}
		return false;
	}

	/**
	 * Serialize Tags of a Task to a tw-compatible json-String
	 * 
	 * @param The
	 *            task, to which the tags should be serialized
	 * 
	 * @return All tags as json-string in tw-form
	 */
	public static String serialize(final Task task) {
		String json = "";
		final List<Tag> tags = task.getTags();
		json += "\"tags\":[";
		if (tags.size() > 0) {

			boolean first = true;
			for (final Tag t : tags) {
				if (!first) {
					json += ",";
				} else {
					first = false;
				}
				/*
				 * The "tags" field is an array of string, where each string is
				 * a single word containing no spaces.
				 */
				json += "\"" + t.getName().replace(" ", "_") + "\"";
			}

		}
		json += "]";
		return json;
	}
}
