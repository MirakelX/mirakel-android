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
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.task.Task;

public class SpecialListsProgressProperty extends SpecialListsBaseProperty {

    public enum OPERATION {
        GREATER_THAN, EQUAL, LESS_THAN
    }

    private int value;
    @NonNull
    private OPERATION op = OPERATION.LESS_THAN;

    public SpecialListsProgressProperty(final int value, final @NonNull OPERATION op) {
        this.value = value;
        this.op = op;
    }

    private SpecialListsProgressProperty(final @NonNull Parcel in) {
        this.value = in.readInt();
        final int tmpOp = in.readInt();
        this.op = OPERATION.values()[tmpOp];
    }

    public SpecialListsProgressProperty(final @NonNull SpecialListsBaseProperty oldProperty) {
        if (oldProperty instanceof SpecialListsProgressProperty) {
            value = ((SpecialListsProgressProperty) oldProperty).getValue();
            op = ((SpecialListsProgressProperty) oldProperty).getOperation();
        } else {
            value = 50;
            op = OPERATION.GREATER_THAN;
        }
    }

    @NonNull
    @Override
    public MirakelQueryBuilder getWhereQueryBuilder(@NonNull final Context ctx) {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(ctx);
        switch (op) {
        case EQUAL:
            return qb.and(Task.PROGRESS, MirakelQueryBuilder.Operation.EQ, value);
        case GREATER_THAN:
            return qb.and(Task.PROGRESS, MirakelQueryBuilder.Operation.GT, value);
        case LESS_THAN:
            return qb.and(Task.PROGRESS, MirakelQueryBuilder.Operation.LT, value);
        }
        return qb;
    }

    @NonNull
    @Override
    public String serialize() {
        String ret = "{\"" + Task.PROGRESS + "\":{";
        ret += "\"value\":" + value;
        ret += ",\"op\":" + op.ordinal();
        return ret + "} }";
    }

    @NonNull
    @Override
    public String getSummary(@NonNull final Context ctx) {
        switch (op) {
        case EQUAL:
            return ctx.getString(R.string.special_list_progress_equal, value);
        case GREATER_THAN:
            return ctx.getString(R.string.special_list_progress_greater_than,
                                 value);
        case LESS_THAN:
            return ctx.getString(R.string.special_list_progress_less_than,
                                 value);
        }
        return "";
    }

    @NonNull
    @Override
    public String getTitle(@NonNull final Context ctx) {
        return ctx.getString(R.string.special_lists_progress_title);
    }

    @NonNull
    public OPERATION getOperation() {
        return op;
    }

    public int getValue() {
        return value;
    }

    public void setValue(final int value) {
        this.value = value;
    }

    public void setOperation(final @NonNull OPERATION op) {
        this.op = op;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeInt(this.value);
        dest.writeInt(this.op.ordinal());
    }


    public static final Creator<SpecialListsProgressProperty> CREATOR = new
    Creator<SpecialListsProgressProperty>() {
        @Override
        public SpecialListsProgressProperty createFromParcel(final Parcel source) {
            return new SpecialListsProgressProperty(source);
        }

        @Override
        public SpecialListsProgressProperty[] newArray(final int size) {
            return new SpecialListsProgressProperty[size];
        }
    };
}
