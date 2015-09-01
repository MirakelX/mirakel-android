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

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder.Operation;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.Log;

public class SpecialListsDueProperty extends SpecialListsBooleanProperty {

    public enum Unit {
        DAY, MONTH, YEAR
    }

    private static final String TAG = "SpecialListsDueProperty";

    @NonNull
    protected Unit unit = Unit.DAY;
    protected int length;

    public SpecialListsDueProperty(final @NonNull Unit unit, final int length, final  boolean negated) {
        super(negated);
        this.length = length;
        this.unit = unit;
    }

    public SpecialListsDueProperty(final @NonNull SpecialListsBaseProperty oldProperty) {
        super(oldProperty);
        if (oldProperty instanceof SpecialListsDueProperty) {
            length = ((SpecialListsDueProperty) oldProperty).getLength();
            unit = ((SpecialListsDueProperty) oldProperty).getUnit();
        } else {
            length = 0;
            unit = Unit.DAY;
        }
    }

    @NonNull
    @Override
    protected String getPropertyName() {
        //not really used
        return Task.DUE;
    }

    private SpecialListsDueProperty(final @NonNull Parcel in) {
        super(in);
        final int tmpUnit = in.readInt();
        this.unit = Unit.values()[tmpUnit];
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

    @NonNull
    @Override
    public MirakelQueryBuilder getWhereQueryBuilder(@NonNull final Context ctx) {
        MirakelQueryBuilder qb = new MirakelQueryBuilder(ctx).and(Task.DUE,
                MirakelQueryBuilder.Operation.NOT_EQ, (String)null);
        DateTime date = new LocalDate().toDateTimeAtStartOfDay().plusDays(1).minusSeconds(10);
        Log.w(TAG, String.valueOf(date.getMillis()));
        switch (unit) {
        case DAY:
            date = date.plusDays(length);
            break;
        case MONTH:
            date = date.plusMonths(length);
            break;
        case YEAR:
            date = date.plusYears(length);
            break;
        }
        qb = qb.and(Task.DUE, isSet ? Operation.GT : Operation.LT, date.getMillis());
        Log.w(TAG, qb.toString(Task.URI));
        return qb;
    }

    @NonNull
    @Override
    public String serialize() {
        String ret = "{\"" + Task.DUE + "\":{";
        ret += "\"negated\": " + (isSet ? "true" : "false");
        ret += ",\"unit\":" + this.unit.ordinal();
        ret += ",\"length\":" + this.length;
        return ret + "} }";
    }

    @NonNull
    @Override
    public String getSummary(@NonNull final Context ctx) {
        String unit = "";
        switch (this.unit) {
        case DAY:
            unit = ctx.getResources().getQuantityString(R.plurals.day, length);
            break;
        case MONTH:
            unit = ctx.getResources().getQuantityString(R.plurals.month, length);
            break;
        case YEAR:
            unit = ctx.getResources().getQuantityString(R.plurals.year, length);
            break;
        }
        final String value = ((length > 0) ? "+" : "") + length + ' ' + unit;
        if (isSet) {
            return ctx.getString(R.string.special_lists_due_negated, value);
        } else {
            return ctx.getString(R.string.special_lists_due, value);
        }
    }

    @NonNull
    @Override
    public String getTitle(@NonNull final Context ctx) {
        return ctx.getString(R.string.special_lists_due_title);
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(this.unit.ordinal());
        dest.writeInt(this.length);
    }

    public static final Creator<SpecialListsDueProperty> CREATOR = new
    Creator<SpecialListsDueProperty>() {
        @Override
        public SpecialListsDueProperty createFromParcel(final Parcel source) {
            return new SpecialListsDueProperty(source);
        }

        @Override
        public SpecialListsDueProperty[] newArray(final int size) {
            return new SpecialListsDueProperty[size];
        }
    };
}
