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
import de.azapps.mirakel.model.task.Task;

public class SpecialListsReminderProperty extends SpecialListsBaseProperty {
    private boolean isSet;

    public SpecialListsReminderProperty(final boolean isSet) {
        super();
        this.isSet = isSet;
    }

    public boolean isSet() {
        return this.isSet;
    }

    public void setIsSet(final boolean s) {
        this.isSet = s;
    }

    @Override
    public String getWhereQuery() {
        return Task.REMINDER + " IS " + (this.isSet ? " NOT " : "") + " NULL ";
    }

    @Override
    public String serialize() {
        String ret = "\"" + Task.REMINDER + "\":{";
        ret += "\"isset\":" + (this.isSet ? "true" : "false");
        return ret + "}";
    }

    @Override
    public String getSummary(final Context mContext) {
        return this.isSet ? mContext.getString(R.string.reminder_set)
               : mContext.getString(R.string.reminder_unset);
    }

}
