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
import android.os.Parcel;
import android.support.annotation.NonNull;

import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.Log;

public class SpecialListsDueProperty extends SpecialListsBaseProperty {

    public enum Unit {
        DAY, MONTH, YEAR
    }

    private static final String TAG = "SpecialListsDueProperty";

    @NonNull
    protected Unit unit = Unit.DAY;
    protected int length;

    public SpecialListsDueProperty(final @NonNull Unit unit, final int length) {
        this.length = length;
        this.unit = unit;
        if (this.unit == null) {
            this.unit = Unit.DAY;
        }
    }

    public SpecialListsDueProperty(final @NonNull SpecialListsBaseProperty oldProperty) {
        if (oldProperty instanceof SpecialListsDueProperty) {
            length = ((SpecialListsDueProperty) oldProperty).getLength();
            unit = ((SpecialListsDueProperty) oldProperty).getUnit();
        } else {
            length = 0;
            unit = Unit.DAY;
        }
    }

    private SpecialListsDueProperty(final @NonNull Parcel in) {
        int tmpUnit = in.readInt();
        this.unit = tmpUnit == -1 ? null : Unit.values()[tmpUnit];
        this.length = in.readInt();
    }

    @NonNull
    public Unit getUnit() {
        return this.unit;
    }

    public int getLength() {
        return this.length;
    }

    public void setLength(final int l) {
        this.length = l;
    }

    public void setUnit(final @NonNull Unit u) {
        this.unit = u;
    }

    @Override
    public MirakelQueryBuilder getWhereQueryBuilder(final Context ctx) {
        MirakelQueryBuilder qb = new MirakelQueryBuilder(ctx).and(Task.DUE,
                MirakelQueryBuilder.Operation.NOT_EQ, (String)null);
        String query = "date('now','";
        if (this.length == 0) {
            return qb.and("date(" + Task.DUE
                          + ",'unixepoch','localtime') <= date('now','localtime')");
        }
        if (this.length > 0) {
            query += "+";
        }
        query += this.length + " ";
        switch (this.unit) {
        case DAY:
            query += "day";
            break;
        case MONTH:
            query += "month";
            break;
        case YEAR:
            query += "year";
            break;
        default:
            return new MirakelQueryBuilder(ctx);
        }
        return qb.and("date(" + Task.DUE + ",'unixepoch','localtime') <= " + query + "')");
    }

    @Override
    public String serialize() {
        if (this.unit == null) {
            Log.wtf(TAG, "unit is null");
        }
        String ret = "{\"" + Task.DUE + "\":{";
        ret += "\"unit\":" + this.unit.ordinal();
        ret += ",\"length\":" + this.length;
        return ret + "} }";
    }

    @Override
    public String getSummary(final Context mContext) {
        return this.length
               + " "
               + mContext.getResources().getStringArray(
                   this.length == 1 ? R.array.due_day_year_values
                   : R.array.due_day_year_values_plural)[this.unit
                           .ordinal()];
        // TODO use plurals here
    }

    @Override
    public String getTitle(Context ctx) {
        return ctx.getString(R.string.special_lists_due_title);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.unit == null ? -1 : this.unit.ordinal());
        dest.writeInt(this.length);
    }

    public static final Creator<SpecialListsDueProperty> CREATOR = new
    Creator<SpecialListsDueProperty>() {
        public SpecialListsDueProperty createFromParcel(Parcel source) {
            return new SpecialListsDueProperty(source);
        }

        public SpecialListsDueProperty[] newArray(int size) {
            return new SpecialListsDueProperty[size];
        }
    };
}
