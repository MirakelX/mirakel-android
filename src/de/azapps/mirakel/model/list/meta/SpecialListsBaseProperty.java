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
import android.os.Parcelable;
import android.support.annotation.NonNull;

import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;

public abstract class SpecialListsBaseProperty implements Parcelable {

    public SpecialListsBaseProperty() {
        // nothing
    }

    public SpecialListsBaseProperty(final @NonNull SpecialListsBaseProperty oldProperty) {
        // nothing
    }

    // for wherequery
    @NonNull
    abstract public MirakelQueryBuilder getWhereQueryBuilder(final @NonNull Context ctx);

    // for db
    @NonNull
    abstract public String serialize();

    // for meta-lists-settings
    @NonNull
    abstract public String getSummary(final @NonNull Context ctx);
    @NonNull
    abstract public String getTitle(final @NonNull Context ctx);

    public String getSummaryForConjunction(final @NonNull Context ctx) {
        return getTitle(ctx) + " : " + getSummary(ctx);
    }

}
