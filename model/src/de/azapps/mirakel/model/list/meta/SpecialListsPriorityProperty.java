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
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.task.Task;

public class SpecialListsPriorityProperty extends SpecialListsSetProperty {

    public SpecialListsPriorityProperty(final boolean isNegated,
                                        final @NonNull List<Integer> content) {
        super(isNegated, content);
    }

    private SpecialListsPriorityProperty(final @NonNull Parcel in) {
        super(in);
    }

    public SpecialListsPriorityProperty(final @NonNull SpecialListsBaseProperty oldProperty) {
        super(oldProperty);
        if (oldProperty instanceof SpecialListsPriorityProperty) {
            content = ((SpecialListsPriorityProperty) oldProperty).getContent();
        } else {
            content = new ArrayList<>(5);
        }
    }

    @Override
    protected String getPropertyName() {
        return Task.PRIORITY;
    }

    @NonNull
    @Override
    public String getSummary(@NonNull final Context ctx) {
        return (this.isSet ? ctx.getString(R.string.not_in)
                : "") + ' ' + TextUtils.join(", ", content);
    }

    @NonNull
    @Override
    public String getTitle(@NonNull Context ctx) {
        return ctx.getString(R.string.special_lists_priority_title);
    }

    public static final Creator<SpecialListsPriorityProperty> CREATOR = new
    Creator<SpecialListsPriorityProperty>() {
        @Override
        public SpecialListsPriorityProperty createFromParcel(final Parcel source) {
            return new SpecialListsPriorityProperty(source);
        }

        @Override
        public SpecialListsPriorityProperty[] newArray(final int size) {
            return new SpecialListsPriorityProperty[size];
        }
    };
}
