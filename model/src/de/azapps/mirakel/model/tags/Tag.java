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

import android.content.ContentValues;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

import de.azapps.mirakel.model.MirakelInternalContentProvider;
import de.azapps.mirakel.model.ModelBase;
import de.azapps.mirakel.model.R;

public class Tag extends TagBase {

    public static final String TABLE = "tag";
    public static final String TAG_CONNECTION_TABLE = "task_tag";
    public static final String[] allColumns = {ModelBase.ID, ModelBase.NAME, DARK_TEXT,
                                               BACKGROUND_COLOR
                                              };
    private static final Uri URI = MirakelInternalContentProvider.TAG_URI;

    public Tag(final int id, final String name, final int backColor, final boolean isDarkBackground) {
        super(id, name, backColor, isDarkBackground);
    }

    protected Uri getUri() {
        return URI;
    }

    public static int count() {
        int count = 0;
        final Cursor c = query(URI, new String[] {"count(*)"}, null, null, null);
        c.moveToFirst();
        if (c.moveToFirst()) {
            count = c.getInt(0);
        }
        c.close();
        return count;
    }

    public static Tag get(final long id) {
        final Cursor c = query(URI, allColumns, ModelBase.ID
                               + "=?", new String[] {id + ""}, null);
        Tag t = null;
        if (c.moveToFirst()) {
            t = cursorToTag(c);
        }
        c.close();
        return t;
    }

    public static List<Tag> all() {
        final Cursor c = query(URI, allColumns, null, null, null);
        return cursorToTagList(c);
    }

    public static List<Tag> getTagsForTask(final long id) {
        final Cursor c = query(MirakelInternalContentProvider.TASK_TAG_URI, addPrefix(allColumns, TABLE),
                               TAG_CONNECTION_TABLE + ".task_id=?", new String[] {id + ""}, null);
        return Tag.cursorToTagList(c);
    }

    public static String getTagsQuery(final String[] columns) {
        String s = "";
        boolean first = true;
        for (final String c : columns) {
            if (!first) {
                s += ", ";
            } else {
                first = false;
            }
            s += Tag.TABLE + "." + c;
        }
        final String query = "SELECT " + s + " FROM " + TAG_CONNECTION_TABLE
                             + " INNER JOIN " + Tag.TABLE + " ON " + TAG_CONNECTION_TABLE
                             + ".tag_id=" + Tag.TABLE + "." + ModelBase.ID + " WHERE "
                             + TAG_CONNECTION_TABLE + ".task_id=?";
        return query;
    }

    public static Tag newTag(final String name) {
        return newTag(name, true, getNextColor(count(), context));
    }

    public static Tag newTag(final String name, final boolean dark,
                             final int color) {
        final Tag t = getByName(name);
        if (t != null) {
            return t;
        }
        final ContentValues cv = new ContentValues();
        cv.put(ModelBase.NAME, name);
        cv.put(DARK_TEXT, dark);
        cv.put(BACKGROUND_COLOR, color);
        final long id = insert(URI, cv);
        return get(id);
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

    public static Tag cursorToTag(final Cursor c) {
        return new Tag(c.getInt(c.getColumnIndex(ID)), c.getString(c.getColumnIndex(NAME)),
                       c.getInt(c.getColumnIndex(BACKGROUND_COLOR)), c.getShort(c.getColumnIndex(DARK_TEXT)) == 1
                      );
    }

    private static List<Tag> cursorToTagList(final Cursor c) {
        final List<Tag> tags = new ArrayList<>();
        if (c.moveToFirst()) {
            do {
                tags.add(cursorToTag(c));
            } while (c.moveToNext());
        }
        c.close();
        return tags;
    }

    private static Tag getByName(final String name) {
        final Cursor c = query(URI, allColumns, ModelBase.NAME
                               + "=?", new String[] {name}, null);
        Tag t = null;
        if (c.moveToFirst()) {
            t = cursorToTag(c);
        }
        c.close();
        return t;
    }

    /**
     * Serialize Tags of a Task to a tw-compatible json-String
     *
     * @param id of the task, to which the tags should be serialized
     * @return All tags as json-string in tw-form
     */
    public static String serialize(final long id) {
        String json = "";
        final List<Tag> tags = getTagsForTask(id);
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
