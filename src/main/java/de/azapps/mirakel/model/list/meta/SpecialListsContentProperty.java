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
import android.os.Parcelable;
import android.support.annotation.NonNull;

import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.task.Task;

public class SpecialListsContentProperty extends SpecialListsStringProperty {

    public SpecialListsContentProperty(final boolean isNegated,
                                       final @NonNull String searchString, final @NonNull Type type) {
        super(isNegated, searchString, type);
    }

    // needed for class.newInstance()
    public SpecialListsContentProperty() {
        super();
    }

    public SpecialListsContentProperty(final @NonNull SpecialListsBaseProperty oldProperty) {
        super(oldProperty);
    }

    @Override
    protected String getPropertyName() {
        return Task.CONTENT;
    }

    @NonNull
    @Override
    public String getTitle(@NonNull Context ctx) {
        return ctx.getString(R.string.special_lists_content_title);
    }

    private SpecialListsContentProperty(final @NonNull Parcel p) {
        super(p);
    }

    public static final Creator<SpecialListsContentProperty> CREATOR = new
    Creator<SpecialListsContentProperty>() {
        public SpecialListsContentProperty createFromParcel(Parcel source) {
            return new SpecialListsContentProperty(source);
        }

        public SpecialListsContentProperty[] newArray(int size) {
            return new SpecialListsContentProperty[size];
        }
    };
}
