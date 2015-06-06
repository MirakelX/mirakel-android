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
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder.Operation;
import de.azapps.mirakel.model.task.Task;

public class SpecialListsDueExistsProperty extends SpecialListsBooleanProperty {
    public SpecialListsDueExistsProperty(final boolean exists) {
        super(exists);
    }

    private SpecialListsDueExistsProperty(final @NonNull Parcel p) {
        super(p);
    }

    public SpecialListsDueExistsProperty(final @NonNull SpecialListsBaseProperty oldProperty) {
        super(oldProperty);
    }

    @Override
    protected String getPropertyName() {
        return Task.DUE + "_exists";
    }

    @NonNull
    @Override
    public MirakelQueryBuilder getWhereQueryBuilder(@NonNull final Context ctx) {
        return new MirakelQueryBuilder(ctx).and(Task.DUE, isSet ? Operation.EQ : Operation.NOT_EQ,
                                                (String)null);
    }

    @NonNull
    @Override
    public String getSummary(@NonNull Context ctx) {
        return isSet ? ctx.getString(R.string.due_dont_exist) : ctx.getString(R.string.due_exists);
    }

    @NonNull
    @Override
    public String getTitle(@NonNull Context ctx) {
        return ctx.getString(R.string.special_lists_due_exist_title);
    }

    public static final Creator<SpecialListsDueExistsProperty> CREATOR = new
    Creator<SpecialListsDueExistsProperty>() {
        public SpecialListsDueExistsProperty createFromParcel(Parcel source) {
            return new SpecialListsDueExistsProperty(source);
        }

        public SpecialListsDueExistsProperty[] newArray(int size) {
            return new SpecialListsDueExistsProperty[size];
        }
    };
}
