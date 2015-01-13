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

public class SpecialListsDoneProperty extends SpecialListsBooleanProperty {

    public SpecialListsDoneProperty(final boolean done) {
        super(done);
    }

    private SpecialListsDoneProperty(final @NonNull Parcel p) {
        super(p);
    }

    public SpecialListsDoneProperty(final @NonNull SpecialListsBaseProperty oldProperty) {
        super(oldProperty);
    }

    @NonNull
    @Override
    public MirakelQueryBuilder getWhereQueryBuilder(@NonNull final Context ctx) {
        return new MirakelQueryBuilder(ctx).and(Task.DONE, MirakelQueryBuilder.Operation.EQ, isSet);
    }

    @NonNull
    @Override
    public String getSummary(@NonNull final Context mContext) {
        return this.isSet ? mContext.getString(R.string.done) : mContext
               .getString(R.string.undone);
    }

    @Override
    protected String getPropertyName() {
        return Task.DONE;
    }

    @NonNull
    @Override
    public String getTitle(@NonNull Context ctx) {
        return ctx.getString(R.string.special_lists_done_title);
    }


    public static final Creator<SpecialListsDoneProperty> CREATOR = new
    Creator<SpecialListsDoneProperty>() {
        public SpecialListsDoneProperty createFromParcel(Parcel source) {
            return new SpecialListsDoneProperty(source);
        }

        public SpecialListsDoneProperty[] newArray(int size) {
            return new SpecialListsDoneProperty[size];
        }
    };
}
