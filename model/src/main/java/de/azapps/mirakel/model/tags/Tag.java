/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 *   Copyright (c) 2013-2015 Anatolij Zelenin, Georg Semmler.
 *
 *       This program is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       any later version.
 *
 *       This program is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU General Public License for more details.
 *
 *       You should have received a copy of the GNU General Public License
 *       along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package de.azapps.mirakel.model.tags;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.google.common.base.Optional;

import java.util.List;

import de.azapps.mirakel.model.provider.MirakelInternalContentProvider;
import de.azapps.mirakel.model.generic.ModelBase;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.query_builder.Cursor2List;
import de.azapps.mirakel.model.query_builder.CursorGetter;
import de.azapps.mirakel.model.query_builder.CursorWrapper;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder.Operation;

public class Tag extends TagBase {

    private static final CursorWrapper.CursorConverter<List<Tag>> LIST_FROM_CURSOR = new
    Cursor2List<>(Tag.class);
    public static final String TABLE = "tag";
    public static final String TAG_CONNECTION_TABLE = "task_tag";
    public static final String[] allColumns = {ModelBase.ID, ModelBase.NAME, DARK_TEXT,
                                               BACKGROUND_COLOR
                                              };
    public static final Uri URI = MirakelInternalContentProvider.TAG_URI;

    public Tag(final long id, final String name, final int backColor, final boolean isDarkBackground) {
        super(id, name, backColor, isDarkBackground);
    }

    public Tag(final @NonNull CursorGetter c) {
        super(c.getLong(ID), c.getString(NAME),
              c.getInt(BACKGROUND_COLOR), c.getBoolean(DARK_TEXT) );
    }


    @Override
    protected Uri getUri() {
        return URI;
    }

    public static long count() {
        return new MirakelQueryBuilder(context).count(URI);
    }

    @NonNull
    public static Optional<Tag> get(final long id) {
        return new MirakelQueryBuilder(context).get(Tag.class, id);
    }

    @NonNull
    public static List<Tag> all() {
        return new MirakelQueryBuilder(context).getList(Tag.class);
    }

    @NonNull
    public static List<Tag> getTagsForTask(final long id) {
        return new MirakelQueryBuilder(context).select(addPrefix(allColumns,
                TABLE)).and(TAG_CONNECTION_TABLE + ".task_id", Operation.EQ, id)
               .query(MirakelInternalContentProvider.TASK_TAG_JOIN_URI).doWithCursor(LIST_FROM_CURSOR);
    }

    @NonNull
    public static String getTagsQuery(final String[] columns) {
        final StringBuilder stringBuilder = new StringBuilder();
        boolean first = true;
        for (final String column : columns) {
            if (!first) {
                stringBuilder.append(", ");
            } else {
                first = false;
            }
            stringBuilder.append(Tag.TABLE + '.').append(column);
        }
        return "SELECT " + stringBuilder + " FROM " + TAG_CONNECTION_TABLE
               + " INNER JOIN " + Tag.TABLE + " ON " + TAG_CONNECTION_TABLE
               + ".tag_id=" + Tag.TABLE + '.' + ModelBase.ID + " WHERE "
               + TAG_CONNECTION_TABLE + ".task_id=?";
    }

    @NonNull
    public static Tag newTag(final String name) {
        return newTag(name, true, getNextColor(count(), context));
    }

    @NonNull
    public static Tag newTag(final String name, final boolean dark,
                             final int color) {
        final Optional<Tag> t = getByName(name);
        if (t.isPresent()) {
            return t.get();
        }
        final ContentValues cv = new ContentValues();
        cv.put(ModelBase.NAME, name);
        cv.put(DARK_TEXT, dark);
        cv.put(BACKGROUND_COLOR, color);
        final long id = insert(URI, cv);
        return get(id).get();
    }

    public static int getNextColor(final long count, final Context ctx) {
        final TypedArray ta = ctx.getResources().obtainTypedArray(
                                  R.array.default_colors);
        final int color;
        if (ta.length() == 0) {
            //Robolectic does not load the typedarray in the right way, so bypass the calculation of the nec color for this case
            color = Color.RED;
        } else {
            final int transparency[] = ctx.getResources().getIntArray(
                                           R.array.default_transparency);
            final int alpha = ((int) count / ta.length()) % transparency.length;
            final int colorPos = (int) count % ta.length();
            color = android.graphics.Color.argb(transparency[alpha],
                                                Color.red(ta.getColor(colorPos, 0)),
                                                Color.green(ta.getColor(colorPos, 0)),
                                                Color.blue(ta.getColor(colorPos, 0)));
        }
        ta.recycle();
        return color;
    }


    @NonNull
    public static Optional<Tag> getByName(final String name) {
        return new MirakelQueryBuilder(context).and(NAME, Operation.EQ, name).get(Tag.class);
    }

    /**
     * Serialize Tags of a Task to a tw-compatible json-String
     *
     * @param id of the task, to which the tags should be serialized
     * @return All tags as json-string in tw-form
     */
    @NonNull
    public static String serialize(final long id) {
        final StringBuilder json = new StringBuilder();
        final List<Tag> tags = getTagsForTask(id);
        json.append("\"tags\":[");
        if (!tags.isEmpty()) {
            boolean first = true;
            for (final Tag t : tags) {
                if (!first) {
                    json.append(',');
                } else {
                    first = false;
                }
                /*
                 * The "tags" field is an array of string, where each string is
                 * a single word containing no spaces.
                 */
                json.append('"').append(t.getName().replace(" ", "_")).append('"');
            }
        }
        json.append(']');
        return json.toString();
    }

    // Parcelable stuff

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeByte(isDarkText ? (byte) 1 : (byte) 0);
        dest.writeInt(this.backgroundColor);
        dest.writeLong(this.getId());
        dest.writeString(this.getName());
    }

    private Tag(final Parcel in) {
        super();
        this.isDarkText = in.readByte() != 0;
        this.backgroundColor = in.readInt();
        this.setId(in.readLong());
        this.setName(in.readString());
    }

    public static final Parcelable.Creator<Tag> CREATOR = new Parcelable.Creator<Tag>() {
        @Override
        public Tag createFromParcel(final Parcel source) {
            return new Tag(source);
        }
        @Override
        public Tag[] newArray(final int size) {
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
