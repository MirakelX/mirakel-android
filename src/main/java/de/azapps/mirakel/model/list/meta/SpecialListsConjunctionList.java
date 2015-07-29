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
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;

public class SpecialListsConjunctionList extends SpecialListsBaseProperty {

    public enum CONJUNCTION {
        AND, OR
    }

    @NonNull
    private CONJUNCTION type = CONJUNCTION.AND;
    @NonNull
    private List<SpecialListsBaseProperty> childs = new ArrayList<>(3);

    public SpecialListsConjunctionList(final @NonNull SpecialListsBaseProperty property,
                                       final @NonNull CONJUNCTION conjunction) {
        if (property instanceof SpecialListsConjunctionList) {
            type = ((SpecialListsConjunctionList) property).type;
            childs = ((SpecialListsConjunctionList) property).childs;
        } else {
            type = conjunction;
            childs = new ArrayList<>(3);
            childs.add(property);
        }
    }

    public SpecialListsConjunctionList(final @NonNull CONJUNCTION type,
                                       final @NonNull List<SpecialListsBaseProperty> childs) {
        this.type = type;
        this.childs = childs;
    }


    @NonNull
    public CONJUNCTION getConjunction() {
        return type;
    }

    @NonNull
    public List<SpecialListsBaseProperty> getChilds() {
        return childs;
    }

    public void setChilds(final @NonNull List<SpecialListsBaseProperty> childs) {
        this.childs = childs;
    }

    public void addChild(final @NonNull SpecialListsBaseProperty child) {
        this.childs.add(child);
    }

    public CONJUNCTION getOperation() {
        return type;
    }


    @NonNull
    @Override
    public MirakelQueryBuilder getWhereQueryBuilder(@NonNull final Context ctx) {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(ctx);
        for (final SpecialListsBaseProperty c : childs) {
            final MirakelQueryBuilder childQB = c.getWhereQueryBuilder(ctx);
            if (type == CONJUNCTION.AND) {
                qb.and(childQB);
            } else {
                qb.or(childQB);
            }
        }
        return qb;
    }

    @NonNull
    @Override
    public String serialize() {
        if ((childs.size() > 1) || (!childs.isEmpty() &&
                                    (childs.get(0) instanceof SpecialListsConjunctionList))) {
            final String childList = TextUtils.join(",", Collections2.transform(childs,
            new Function<SpecialListsBaseProperty, String>() {
                @Override
                public String apply(SpecialListsBaseProperty input) {
                    return input.serialize();
                }
            }));
            return '[' + childList + ']';
        } else if (childs.size() == 1) {
            return childs.get(0).serialize() ;
        } else {
            return "";
        }
    }

    @NonNull
    @Override
    public String getSummary(@NonNull final Context ctx) {
        return TextUtils.join(' ' + getTitle(ctx) + ' ', Collections2.transform(childs,
        new Function<SpecialListsBaseProperty, String>() {
            @Override
            public String apply(SpecialListsBaseProperty input) {
                if (input instanceof SpecialListsConjunctionList) {
                    return '(' + input.getSummary(ctx) + ')';
                }
                return input.getSummaryForConjunction(ctx);
            }
        }));

    }

    @NonNull
    @Override
    public String getTitle(@NonNull Context ctx) throws NoSuchElementException {
        switch (type) {
        case AND:
            return ctx.getString(R.string.and);
        case OR:
            return ctx.getString(R.string.or);
        }
        //how ever this could be reached
        throw new NoSuchElementException("Unknown conjunction type: " + type);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeInt(this.type.ordinal());
        dest.writeParcelableArray(childs.toArray(new Parcelable[childs.size()]), 1);
    }

    private SpecialListsConjunctionList(final Parcel in) {
        this.type = CONJUNCTION.values()[in.readInt()];
        final SpecialListsBaseProperty[] childs = (SpecialListsBaseProperty[])in.readParcelableArray(
                    ClassLoader.getSystemClassLoader());
        this.childs = new ArrayList<>(3);
        for (SpecialListsBaseProperty c : childs) {
            this.childs.add(c);
        }
    }

    public static final Creator<SpecialListsConjunctionList> CREATOR = new
    Creator<SpecialListsConjunctionList>() {
        @Override
        public SpecialListsConjunctionList createFromParcel(final Parcel source) {
            return new SpecialListsConjunctionList(source);
        }

        @Override
        public SpecialListsConjunctionList[] newArray(final int size) {
            return new SpecialListsConjunctionList[size];
        }
    };
}
