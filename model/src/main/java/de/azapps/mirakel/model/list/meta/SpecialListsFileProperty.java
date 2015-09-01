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
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder.Operation;
import de.azapps.mirakel.model.task.Task;

public class SpecialListsFileProperty extends SpecialListsBooleanProperty {

    public SpecialListsFileProperty(boolean hasFile) {
        super(hasFile);
    }

    private SpecialListsFileProperty(final @NonNull Parcel in) {
        super(in);
    }

    public SpecialListsFileProperty(final @NonNull SpecialListsBaseProperty oldProperty) {
        super(oldProperty);
    }

    @Override
    //not used here
    protected String getPropertyName() {
        return FileMirakel.TABLE;
    }

    @NonNull
    @Override
    public MirakelQueryBuilder getWhereQueryBuilder(@NonNull final Context ctx) {
        final MirakelQueryBuilder.Operation op = isSet ? Operation.IN :
                Operation.NOT_IN;
        return new MirakelQueryBuilder(ctx).and(Task.ID, op,
                                                new MirakelQueryBuilder(ctx).select(FileMirakel.TASK), FileMirakel.URI);
    }

    @NonNull
    @Override
    public String getSummary(@NonNull Context ctx) {
        return ctx.getString(isSet ? R.string.has_file : R.string.no_file);
    }

    @NonNull
    @Override
    public String getTitle(@NonNull Context ctx) {
        return ctx.getString(R.string.special_lists_file_title);
    }

    @Override
    public String getSummaryForConjunction(@NonNull Context ctx) {
        return getSummary(ctx);
    }

    public static final Creator<SpecialListsFileProperty> CREATOR = new
    Creator<SpecialListsFileProperty>() {
        public SpecialListsFileProperty createFromParcel(Parcel source) {
            return new SpecialListsFileProperty(source);
        }

        public SpecialListsFileProperty[] newArray(int size) {
            return new SpecialListsFileProperty[size];
        }
    };
}
