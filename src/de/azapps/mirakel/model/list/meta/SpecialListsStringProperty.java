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
package de.azapps.mirakel.model.list.meta;

import android.content.Context;
import de.azapps.mirakel.model.R;

public abstract class SpecialListsStringProperty extends
    SpecialListsBaseProperty {
    public enum Type {
        BEGIN, END, CONTAINS;
    }

    protected boolean isNegated;
    protected String searchString;
    protected Type type;

    public SpecialListsStringProperty(final boolean isNegated,
                                      final String serachString, final int type) {
        this.isNegated = isNegated;
        this.searchString = serachString;
        this.type = Type.values()[type];
    }

    public boolean isNegated() {
        return this.isNegated;
    }

    public void setNegated(final boolean negated) {
        this.isNegated = negated;
    }

    public void setSearchString(final String s) {
        this.searchString = s;
    }

    public String getSearchString() {
        return this.searchString;
    }

    public Type getType() {
        return this.type;
    }

    public void setType(final Type t) {
        this.type = t;
    }

    abstract protected String propertyName();

    @Override
    public String getSummary(final Context mContext) {
        switch (this.type) {
        case END:
            return mContext.getString(R.string.where_like_end_text,
                                      this.searchString);
        case BEGIN:
            return mContext.getString(R.string.where_like_begin_text,
                                      this.searchString);
        case CONTAINS:
            return mContext.getString(R.string.where_like_contain_text,
                                      this.searchString);
        default:
            return "";
        }
    }

    @Override
    public String getWhereQuery() {
        String query = this.isNegated ? " NOT " : "";
        query += propertyName() + " LIKE '";
        if (this.type == null) {
            return query + "%'";
        }
        final String keyword = this.searchString.replace("'", "''");
        switch (this.type) {
        case BEGIN:
            query += "%" + keyword;
            break;
        case CONTAINS:
            query += "%" + keyword + "%";
            break;
        case END:
            query += keyword + "%";
            break;
        default:
            break;
        }
        return query + "'";
    }

    @Override
    public String serialize() {
        String ret = "\"" + propertyName() + "\":{";
        ret += "\"isNegated\":" + (this.isNegated ? "true" : "false");
        ret += ",\"type\":" + this.type.ordinal();
        ret += ",\"serachString\":\"" + this.searchString + "\"";
        return ret + "}";
    }

}
