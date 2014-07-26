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

import de.azapps.mirakel.model.MirakelInternalContentProvider;
import de.azapps.mirakel.model.ModelBase;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.mirakel.model.task.Task;

public class SpecialListsTagProperty extends SpecialListsSetProperty {

    public SpecialListsTagProperty(final boolean isNegated,
                                   final List<Integer> content) {
        super(isNegated, content);
    }

    @Override
    protected String propertyName() {
        return Tag.TABLE;
    }

    @Override
    public MirakelQueryBuilder getWhereQuery(final Context ctx) {
        return new MirakelQueryBuilder(ctx).and(Task.ID,
                                                isNegated ? MirakelQueryBuilder.Operation.NOT_IN : MirakelQueryBuilder.Operation.IN,
                                                new MirakelQueryBuilder(ctx).distinct().select("task_id").and("tag_id",
                                                        MirakelQueryBuilder.Operation.IN, content), MirakelInternalContentProvider.TAG_CONNECTION_URI);
    }

    @Override
    public String getSummary(final Context ctx) {
        String summary = this.isNegated ? ctx.getString(R.string.not_in) : "";
        boolean first = true;
        for (final int p : this.content) {
            final Tag t = Tag.get(p);
            if (t == null) {
                continue;
            }
            summary += (first ? "" : ",") + t;
            if (first) {
                first = false;
            }
        }
        return summary;
    }

}
