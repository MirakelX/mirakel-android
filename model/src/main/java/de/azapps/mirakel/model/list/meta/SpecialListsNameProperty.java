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

import de.azapps.mirakel.model.generic.ModelBase;
import de.azapps.mirakel.model.R;

public class SpecialListsNameProperty extends SpecialListsStringProperty {

    public SpecialListsNameProperty(final boolean isNegated,
                                    final @NonNull String searchString, final @NonNull Type type) {
        super(isNegated, searchString, type);
    }


    private SpecialListsNameProperty(final @NonNull Parcel in) {
        super(in);
    }

    public SpecialListsNameProperty(final @NonNull SpecialListsBaseProperty oldProperty) {
        super(oldProperty);
    }

    @Override
    protected String getPropertyName() {
        return ModelBase.NAME;
    }

    @NonNull
    @Override
    public String getTitle(@NonNull Context ctx) {
        return ctx.getString(R.string.special_lists_name_title);
    }


    public static final Creator<SpecialListsNameProperty> CREATOR = new
    Creator<SpecialListsNameProperty>() {
        public SpecialListsNameProperty createFromParcel(Parcel source) {
            return new SpecialListsNameProperty(source);
        }

        public SpecialListsNameProperty[] newArray(int size) {
            return new SpecialListsNameProperty[size];
        }
    };
}
