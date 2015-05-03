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

package de.azapps.mirakel.model.search;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;

import de.azapps.mirakel.model.MirakelInternalContentProvider;
import de.azapps.mirakel.model.ModelBase;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;

public class Autocomplete {
    public static final String TABLE = "autocomplete_helper";
    public static final String OBJ_ID = "obj_id";
    public static final String TYPE = "type";

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
    @NonNull
    private final AUTOCOMPLETE_TYPE autocompleteType;

    public Autocomplete(@NonNull final Cursor cursor) {
        this.objId = cursor.getInt(1);
        this.name = cursor.getString(2);
        this.autocompleteType = AUTOCOMPLETE_TYPE.fromString(cursor.getString(3));
        this.score = cursor.getInt(4);
    }

    @NonNull
    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }

    public int getObjId() {
        return objId;
    }

    @NonNull
    public AUTOCOMPLETE_TYPE getAutocompleteType() {
        return autocompleteType;
    }

    public static Cursor autocomplete(@NonNull final Context context, @NonNull final String input) {
        MirakelQueryBuilder mirakelQueryBuilder = new MirakelQueryBuilder(context);
        mirakelQueryBuilder.and("name", MirakelQueryBuilder.Operation.LIKE, "%" + input + "%")
        .select(ModelBase.ID, OBJ_ID, ModelBase.NAME, TYPE,
                "score + length(" + input.length() + ") - length(name) as score")
        .sort("score", MirakelQueryBuilder.Sorting.DESC);
        return mirakelQueryBuilder.query(MirakelInternalContentProvider.AUTOCOMPLETE_URI);
    }
}
