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

package de.azapps.mirakel.new_ui.search;

import android.content.Context;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;

import de.azapps.mirakel.model.provider.MirakelInternalContentProvider;
import de.azapps.mirakel.model.generic.ModelBase;
import de.azapps.mirakel.model.query_builder.CursorGetter;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.new_ui.views.TagSpan;

public class SearchObject implements Parcelable {
    public static final String OBJ_ID = "obj_id";
    public static final String TYPE = "type";
    public static final String SCORE = "score";


    public enum AUTOCOMPLETE_TYPE {
        TASK, TAG;

        public static AUTOCOMPLETE_TYPE fromString(String text) {
            switch (text) {
            case "task":
                return TASK;
            case "tag":
                return TAG;
            default:
                throw new IllegalArgumentException("No such type");
            }
        }
    }

    @NonNull
    private final String name;
    private final int objId;
    private final int score;
    // For performance
    private final int backgroundColor;
    // For performance
    private final boolean done;
    @NonNull
    private final AUTOCOMPLETE_TYPE autocompleteType;

    public SearchObject(@NonNull String text) {
        this.objId = -1;
        this.score = -1;
        this.name = text;
        autocompleteType = AUTOCOMPLETE_TYPE.TASK;
        backgroundColor = 0;
        done = false;
    }

    public SearchObject(@NonNull final Cursor cursor) {
        final CursorGetter cursorGetter = CursorGetter.unsafeGetter(cursor);
        this.objId = cursorGetter.getInt(OBJ_ID);
        this.name = cursorGetter.getString(ModelBase.NAME);
        this.autocompleteType = AUTOCOMPLETE_TYPE.fromString(cursorGetter.getString(TYPE));
        this.score = cursorGetter.getInt(SCORE);
        this.backgroundColor = cursorGetter.getInt(Tag.BACKGROUND_COLOR);
        this.done = cursorGetter.getBoolean(Task.DONE);
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public CharSequence getText(Context context) {
        switch (autocompleteType) {
        case TASK:
            return name;
        case TAG:
            Tag tag = Tag.get(objId).get();
            if (tag == null) {
                return name;
            } else {
                final TagSpan tagSpan = new TagSpan(tag, context);
                SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
                stringBuilder.append(new SpannableString(tag.getName()));
                stringBuilder.setSpan(tagSpan, 0, tag.getName().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                return stringBuilder;
            }
        }
        // this can not happen
        return "";
    }

    public int getScore() {
        return score;
    }

    public int getObjId() {
        return objId;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public boolean isDone() {
        return done;
    }

    @NonNull
    public AUTOCOMPLETE_TYPE getAutocompleteType() {
        return autocompleteType;
    }

    public static Cursor autocomplete(@NonNull final Context context, @NonNull final String input) {
        MirakelQueryBuilder mirakelQueryBuilder = new MirakelQueryBuilder(context);
        mirakelQueryBuilder.and("name", MirakelQueryBuilder.Operation.LIKE, '%' + input + '%')
        .select(ModelBase.ID, OBJ_ID, ModelBase.NAME, TYPE, Tag.BACKGROUND_COLOR, Task.DONE,
                "score + length(" + input.length() + ") - length(name) as score")
        .sort("score", MirakelQueryBuilder.Sorting.DESC);
        return mirakelQueryBuilder.query(MirakelInternalContentProvider.AUTOCOMPLETE_URI).getRawCursor();
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeInt(this.objId);
        dest.writeInt(this.score);
        dest.writeInt(this.backgroundColor);
        dest.writeByte(done ? (byte) 1 : (byte) 0);
        dest.writeInt(this.autocompleteType == null ? -1 : this.autocompleteType.ordinal());
    }

    private SearchObject(Parcel in) {
        this.name = in.readString();
        this.objId = in.readInt();
        this.score = in.readInt();
        this.backgroundColor = in.readInt();
        this.done = in.readByte() != 0;
        int tmpAutocompleteType = in.readInt();
        this.autocompleteType = tmpAutocompleteType == -1 ? null :
                                AUTOCOMPLETE_TYPE.values()[tmpAutocompleteType];
    }

    public static final Creator<SearchObject> CREATOR = new Creator<SearchObject>() {
        public SearchObject createFromParcel(Parcel source) {
            return new SearchObject(source);
        }

        public SearchObject[] newArray(int size) {
            return new SearchObject[size];
        }
    };
}
