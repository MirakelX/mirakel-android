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
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;

public abstract class SpecialListsSetProperty extends SpecialListsBooleanProperty {

    @NonNull
    protected List<Integer> content = new ArrayList<>();

    public SpecialListsSetProperty(final boolean isNegated,
                                   final @NonNull List<Integer> content) {
        super(isNegated);
        this.content = content;
    }


    protected SpecialListsSetProperty(Parcel in) {
        super(in);
        this.content = new ArrayList<>();
        in.readList(this.content, Integer.class.getClassLoader());
    }

    protected SpecialListsSetProperty(SpecialListsBaseProperty oldProperty) {
        super(oldProperty);
        //converting between different setproperties like list or tag is evil
    }


    @Override
    public MirakelQueryBuilder getWhereQueryBuilder(final Context ctx) {
        return new MirakelQueryBuilder(ctx).and(getPropertyName(),
                                                isSet ? MirakelQueryBuilder.Operation.NOT_IN : MirakelQueryBuilder.Operation.IN, content);
    }

    @Override
    public String serialize() {
        String ret = "{\"" + getPropertyName() + "\":{";
        ret += "\"isSet\":" + (this.isSet ? "true" : "false");
        ret += ",\"content\":[";
        ret += TextUtils.join(",", content);
        return ret + "]} }";
    }

    @NonNull
    public List<Integer> getContent() {
        return this.content;
    }

    public void setContent(final @NonNull List<Integer> content) {
        this.content = content;
    }


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(isSet ? (byte) 1 : (byte) 0);
        dest.writeList(this.content);
    }


}
