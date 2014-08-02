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
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder.Operation;
import de.azapps.mirakel.model.task.Task;

public class SpecialListsFileProperty extends SpecialListsNegatedProperty {

    public SpecialListsFileProperty(boolean done) {
        super(done);
    }

    @Override
    protected String propertyName() {
        return FileMirakel.TABLE;
    }

    @Override
    public MirakelQueryBuilder getWhereQuery(final Context ctx) {
        final MirakelQueryBuilder.Operation op = done ? Operation.IN :
                Operation.NOT_IN;
        return new MirakelQueryBuilder(ctx).and(Task.ID, op,
                                                new MirakelQueryBuilder(ctx).select(FileMirakel.TASK), FileMirakel.URI);
    }

    @Override
    public String getSummary(Context ctx) {
        return ctx.getString(done ? R.string.has_file : R.string.no_file);
    }

}
