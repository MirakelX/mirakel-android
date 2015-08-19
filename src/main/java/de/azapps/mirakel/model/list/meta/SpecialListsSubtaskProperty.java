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

import de.azapps.mirakel.model.provider.MirakelInternalContentProvider;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.task.Task;

public class SpecialListsSubtaskProperty extends SpecialListsBooleanProperty {

    boolean isParent;

    public SpecialListsSubtaskProperty(boolean isNegated, boolean isParent) {
        super(isNegated);
        this.isParent = isParent;
    }


    private SpecialListsSubtaskProperty(final @NonNull Parcel in) {
        super(in);
        this.isParent = in.readByte() != 0;
    }

    public SpecialListsSubtaskProperty(final @NonNull SpecialListsBaseProperty oldProperty) {
        super(oldProperty);
        if (oldProperty instanceof SpecialListsSubtaskProperty) {
            isParent = ((SpecialListsSubtaskProperty) oldProperty).isParent();
        } else {
            isParent = false;
        }
    }

    @Override
    protected String getPropertyName() {
        return Task.SUBTASK_TABLE;
    }

    @NonNull
    @Override
    public String getSummary(@NonNull Context ctx) {
        if (isParent && isSet) {
            return ctx.getString(R.string.special_list_subtask_parent_not);
        } else if (isParent && !isSet) {
            return ctx.getString(R.string.special_list_subtask_parent);
        } else if (!isParent && isSet) {
            return ctx.getString(R.string.special_list_subtask_child_not);
        } else {
            return ctx.getString(R.string.special_list_subtask_child);
        }
    }

    @NonNull
    @Override
    public String getTitle(@NonNull Context ctx) {
        return ctx.getString(R.string.special_lists_subtask_title);
    }

    @NonNull
    @Override
    public MirakelQueryBuilder getWhereQueryBuilder(@NonNull final Context ctx) {
        return new MirakelQueryBuilder(ctx).and(Task.ID,
                                                isSet ? MirakelQueryBuilder.Operation.NOT_IN : MirakelQueryBuilder.Operation.IN,
                                                new MirakelQueryBuilder(ctx).distinct().select((isParent ? "parent_id" : "child_id")),
                                                MirakelInternalContentProvider.SUBTASK_URI);
    }

    @NonNull
    @Override
    public String serialize() {
        String ret = "{\"" + Task.SUBTASK_TABLE + "\":{";
        ret += "\"isSet\":" + isSet;
        return ret + ",\"isParent\":" + isParent + "} }";
    }


    public boolean isParent() {
        return isParent;
    }

    public void setParent(boolean parent) {
        isParent = parent;
    }


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(isParent ? (byte) 1 : (byte) 0);
        dest.writeByte(isSet ? (byte) 1 : (byte) 0);
    }

    public static final Creator<SpecialListsSubtaskProperty> CREATOR = new
    Creator<SpecialListsSubtaskProperty>() {
        public SpecialListsSubtaskProperty createFromParcel(Parcel source) {
            return new SpecialListsSubtaskProperty(source);
        }

        public SpecialListsSubtaskProperty[] newArray(int size) {
            return new SpecialListsSubtaskProperty[size];
        }
    };
}
