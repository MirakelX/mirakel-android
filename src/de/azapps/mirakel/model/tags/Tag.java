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
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

import de.azapps.mirakel.model.MirakelInternalContentProvider;
import de.azapps.mirakel.model.ModelBase;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder.Operation;

public class Tag extends TagBase {

    public static final String TABLE = "tag";
    public static final String TAG_CONNECTION_TABLE = "task_tag";
    public static final String[] allColumns = {ModelBase.ID, ModelBase.NAME, DARK_TEXT,
                                               BACKGROUND_COLOR
                                              };
    public static final Uri URI = MirakelInternalContentProvider.TAG_URI;

    public Tag(final int id, final String name, final int backColor, final boolean isDarkBackground) {
        super(id, name, backColor, isDarkBackground);
    }

    protected Uri getUri() {
        return URI;
    }

    public static long count() {
        return new MirakelQueryBuilder(context).count(URI);
    }

    public static Tag get(final long id) {
        return new MirakelQueryBuilder(context).get(Tag.class, id);
    }

    public static List<Tag> all() {
        return new MirakelQueryBuilder(context).getList(Tag.class);
    }

    public static List<Tag> getTagsForTask(final long id) {
        return Tag.cursorToTagList(new MirakelQueryBuilder(context).select(addPrefix(allColumns,
                                   TABLE)).and(TAG_CONNECTION_TABLE + ".task_id", Operation.EQ, id)
                                   .query(MirakelInternalContentProvider.TASK_TAG_JOIN_URI));
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


    public static int getNextColor(final long count, final Context ctx) {
        final TypedArray ta = ctx.getResources().obtainTypedArray(
                                  R.array.default_colors);
        final int transparency[] = ctx.getResources().getIntArray(
                                       R.array.default_transparency);
        final int alpha = (int)count / ta.length() % transparency.length;
        final int colorPos = (int)count % ta.length();
        final int color = android.graphics.Color.argb(transparency[alpha],
                          Color.red(ta.getColor(colorPos, 0)),
                          Color.green(ta.getColor(colorPos, 0)),
                          Color.blue(ta.getColor(colorPos, 0)));
        ta.recycle();
        return color;
    }


    public Tag(final Cursor c) {
        super(c.getInt(c.getColumnIndex(ID)), c.getString(c.getColumnIndex(NAME)),
              c.getInt(c.getColumnIndex(BACKGROUND_COLOR)), c.getShort(c.getColumnIndex(DARK_TEXT)) == 1);
    }

    private static List<Tag> cursorToTagList(final Cursor c) {
        final List<Tag> tags = new ArrayList<>();
        if (c.moveToFirst()) {
            do {
                tags.add(new Tag(c));
            } while (c.moveToNext());
        }
        c.close();
        return tags;
    }

    private static Tag getByName(final String name) {
        return new MirakelQueryBuilder(context).and(NAME, Operation.EQ, name).get(Tag.class);
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

    // Parcelable stuff

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(isDarkText ? (byte) 1 : (byte) 0);
        dest.writeInt(this.backgroundColor);
        dest.writeLong(this.getId());
        dest.writeString(this.getName());
    }

    private Tag(Parcel in) {
        super();
        this.isDarkText = in.readByte() != 0;
        this.backgroundColor = in.readInt();
        this.setId(in.readLong());
        this.setName(in.readString());
    }

    public static final Parcelable.Creator<Tag> CREATOR = new Parcelable.Creator<Tag>() {
        public Tag createFromParcel(Parcel source) {
            return new Tag(source);
        }
        public Tag[] newArray(int size) {
            return new Tag[size];
        }
    };

    public static Tag getSafeFirst() {
        List<Tag> all = all();
        if (all.isEmpty()) {
            return newTag(context.getString(R.string.new_tag));
        } else {
            return all.get(0);
        }
    }
}
