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
import de.azapps.mirakel.model.task.Task;

public class SpecialListsReminderProperty extends SpecialListsBooleanProperty {


    public SpecialListsReminderProperty(final boolean isSet) {
        super(isSet);
    }

    private SpecialListsReminderProperty(final @NonNull Parcel in) {
        super(in);
    }

    public SpecialListsReminderProperty(final @NonNull SpecialListsBaseProperty oldProperty) {
        super(oldProperty);
    }

    @Override
    //not used here
    protected String getPropertyName() {
        return Task.REMINDER;
    }

    @NonNull
    @Override
    public MirakelQueryBuilder getWhereQueryBuilder(@NonNull final Context ctx) {
        return new MirakelQueryBuilder(ctx).and(Task.REMINDER,
                                                isSet ? MirakelQueryBuilder.Operation.NOT_EQ : MirakelQueryBuilder.Operation.EQ, (String)null);
    }

    @NonNull
    @Override
    public String serialize() {
        String ret = "{\"" + Task.REMINDER + "\":{";
        ret += "\"isset\":" + (this.isSet ? "true" : "false");
        return ret + "} }";
    }

    @NonNull
    @Override
    public String getSummary(@NonNull final Context mContext) {
        return this.isSet ? mContext.getString(R.string.reminder_set)
               : mContext.getString(R.string.reminder_unset);
    }

    @NonNull
    @Override
    public String getTitle(@NonNull Context ctx) {
        return ctx.getString(R.string.special_lists_reminder_title);
    }

    public static final Creator<SpecialListsReminderProperty> CREATOR = new
    Creator<SpecialListsReminderProperty>() {
        public SpecialListsReminderProperty createFromParcel(Parcel source) {
            return new SpecialListsReminderProperty(source);
        }

        public SpecialListsReminderProperty[] newArray(int size) {
            return new SpecialListsReminderProperty[size];
        }
    };
}
