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

package de.azapps.mirakel.model.list.meta;

import android.content.Context;
import android.os.Parcel;
import android.support.annotation.NonNull;

import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder.Operation;

public abstract class SpecialListsStringProperty extends
    SpecialListsBooleanProperty {
    public enum Type {
        BEGIN, END, CONTAINS;
    }

    @NonNull
    protected String searchString = "";
    @NonNull
    protected Type type = Type.CONTAINS;

    public SpecialListsStringProperty(final boolean isNegated,
                                      final @NonNull String searchString, final @NonNull Type type) {
        super(isNegated);
        this.searchString = searchString;
        this.type = type;
    }

    protected SpecialListsStringProperty() {
        super(true);
        this.searchString = "";
        this.type = Type.BEGIN;
    }

    protected SpecialListsStringProperty(final @NonNull Parcel in) {
        super(in);
        this.searchString = in.readString();
        this.type = Type.values()[in.readInt()];
    }

    protected SpecialListsStringProperty(final @NonNull SpecialListsBaseProperty oldProperty) {
        super(oldProperty);
        if (oldProperty instanceof SpecialListsStringProperty) {
            searchString = ((SpecialListsStringProperty) oldProperty).getSearchString();
            type = ((SpecialListsStringProperty) oldProperty).getType();
        } else {
            searchString = "";
            type = Type.CONTAINS;
        }
    }

    public void setSearchString(final @NonNull String s) {
        this.searchString = s;
    }

    @NonNull
    public String getSearchString() {
        return this.searchString;
    }

    @NonNull
    public Type getType() {
        return this.type;
    }

    public void setType(final @NonNull Type t) {
        this.type = t;
    }


    @NonNull
    @Override
    public String getSummary(@NonNull final Context mContext) {
        switch (this.type) {
        case END:
            return mContext.getString(R.string.where_like_end_text,
                                      "\"" + this.searchString + "\"");
        case BEGIN:
            return mContext.getString(R.string.where_like_begin_text,
                                      "\"" + this.searchString + "\"");
        case CONTAINS:
            return mContext.getString(R.string.where_like_contain_text,
                                      "\"" + this.searchString + "\"");
        default:
            return "";
        }
    }

    @NonNull
    @Override
    public MirakelQueryBuilder getWhereQueryBuilder(@NonNull final Context ctx) {
        MirakelQueryBuilder qb = new MirakelQueryBuilder(ctx);
        MirakelQueryBuilder.Operation op = isSet ? Operation.NOT_LIKE :
                                           Operation.LIKE;
        if (this.type == null) {
            return qb.and(getPropertyName(), op, "%");
        }
        switch (this.type) {
        case BEGIN:
            return qb.and(getPropertyName(), op, searchString + "%");
        case CONTAINS:
            return qb.and(getPropertyName(), op, "%" + searchString + "%");
        case END:
            return qb.and(getPropertyName(), op, "%" + searchString );
        default:
            return qb;
        }
    }

    @NonNull
    @Override
    public String serialize() {
        String ret = "{\"" + getPropertyName() + "\":{";
        ret += "\"isSet\":" + (this.isSet ? "true" : "false");
        ret += ",\"type\":" + this.type.ordinal();
        ret += ",\"searchString\":\"" + this.searchString + "\"";
        return ret + "} }";
    }


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(isSet ? (byte) 1 : (byte) 0);
        dest.writeString(this.searchString);
        dest.writeInt(this.type == null ? -1 : this.type.ordinal());
    }

}
