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

import java.util.List;

import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.task.Task;

public class SpecialListsPriorityProperty extends SpecialListsSetProperty {

    public SpecialListsPriorityProperty(final boolean isNegated,
                                        final List<Integer> content) {
        super(isNegated, content);
    }

    @Override
    public String propertyName() {
        return Task.PRIORITY;
    }

    @Override
    public String getSummary(final Context mContext) {
        String summary = this.isNegated ? mContext.getString(R.string.not_in)
                         : "";
        boolean first = true;
        for (final int p : this.content) {
            summary += (first ? "" : ",") + p;
            if (first) {
                first = false;
            }
        }
        return summary;
    }
}
