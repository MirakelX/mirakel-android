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
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.task.Task;

public class SpecialListsListNameProperty extends SpecialListsStringProperty {

    public SpecialListsListNameProperty(final boolean isNegated,
                                        final @NonNull String searchString, final @NonNull Type type) {
        super(isNegated, searchString, type);
    }


    private SpecialListsListNameProperty(final @NonNull Parcel in) {
        super(in);
    }

    public SpecialListsListNameProperty(final @NonNull SpecialListsBaseProperty oldProperty) {
        super(oldProperty);
    }

    @NonNull
    @Override
    protected String getPropertyName() {
        //use full name here to prevent collision in json
        return ListMirakel.TABLE + "." + ListMirakel.NAME;
    }

    @NonNull
    @Override
    public MirakelQueryBuilder getWhereQueryBuilder(@NonNull Context ctx) {
        MirakelQueryBuilder qb = super.getWhereQueryBuilder(ctx).select(ListMirakel.ID);
        return new MirakelQueryBuilder(ctx).and(Task.LIST_ID, MirakelQueryBuilder.Operation.IN, qb,
                                                ListMirakel.URI);
    }

    @NonNull
    @Override
    public String getTitle(@NonNull Context ctx) {
        return ctx.getString(R.string.special_list_list_name);
    }

    public static final Creator<SpecialListsListNameProperty> CREATOR = new
    Creator<SpecialListsListNameProperty>() {
        public SpecialListsListNameProperty createFromParcel(Parcel source) {
            return new SpecialListsListNameProperty(source);
        }

        public SpecialListsListNameProperty[] newArray(int size) {
            return new SpecialListsListNameProperty[size];
        }
    };
}
