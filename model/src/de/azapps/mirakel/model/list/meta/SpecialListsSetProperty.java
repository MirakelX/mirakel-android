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
import android.text.TextUtils;

import java.util.List;

import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;

public abstract class SpecialListsSetProperty extends SpecialListsBaseProperty {
    protected boolean isNegated;
    protected List<Integer> content;

    public SpecialListsSetProperty(final boolean isNegated,
                                   final List<Integer> content) {
        this.isNegated = isNegated;
        this.content = content;
    }

    @Override
    public MirakelQueryBuilder getWhereQuery(final Context ctx) {
        return new MirakelQueryBuilder(ctx).and(propertyName(),
                                                isNegated ? MirakelQueryBuilder.Operation.NOT_IN : MirakelQueryBuilder.Operation.IN, content);
    }

    @Override
    public String serialize() {
        String ret = "\"" + propertyName() + "\":{";
        ret += "\"isNegated\":" + (this.isNegated ? "true" : "false");
        ret += ",\"content\":[";
        ret += TextUtils.join(",", content);
        return ret + "]}";
    }


    public boolean isNegated() {
        return this.isNegated;
    }

    public List<Integer> getContent() {
        return this.content;
    }

    public void setNegated(final boolean negated) {
        this.isNegated = negated;
    }

    public void setContent(final List<Integer> content) {
        this.content = content;
    }

    abstract protected String propertyName();

}
