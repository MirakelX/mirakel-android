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

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import java.util.ArrayList;
import java.util.List;

import de.azapps.mirakel.model.ModelBase;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.OptionalUtils;

public class SpecialListsListProperty extends SpecialListsSetProperty {

    public SpecialListsListProperty(final boolean isNegated,
                                    final @NonNull List<Integer> content) {
        super(isNegated, content);
    }

    private SpecialListsListProperty(final @NonNull Parcel p) {
        super(p);
    }

    public SpecialListsListProperty(final @NonNull SpecialListsBaseProperty oldProperty) {
        super(oldProperty);
        if (oldProperty instanceof SpecialListsListProperty) {
            content = ((SpecialListsListProperty) oldProperty).getContent();
        } else {
            content = new ArrayList<>();
        }
    }

    @Override
    protected String getPropertyName() {
        return Task.LIST_ID;
    }

    @Override
    public MirakelQueryBuilder getWhereQueryBuilder(final Context ctx) {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(ctx);
        final List<Integer> special = new ArrayList<>();
        final List<Integer> normal = new ArrayList<>();
        for (final int c : this.content) {
            if (c > 0) {
                normal.add(c);
            } else if (c < 0) {
                special.add(c);
            }
        }
        qb.and(Task.LIST_ID, MirakelQueryBuilder.Operation.IN, normal);
        // TODO handle loops here
        for (final int p : special) {
            final Optional<SpecialList> s = SpecialList.getSpecial(p);
            OptionalUtils.withOptional(s, new OptionalUtils.Procedure<SpecialList>() {
                @Override
                public void apply(SpecialList input) {
                    if (input.getWhereQueryForTasks() != null) {
                        qb.or(input.getWhereQueryForTasks());
                    }
                }
            });
        }
        if (isSet) {
            return new MirakelQueryBuilder(ctx).not(qb);
        } else {
            return qb;
        }
    }

    @Override
    public String getSummary(final Context ctx) {
        List <ListMirakel> lists = new MirakelQueryBuilder(ctx).and(ModelBase.ID,
                MirakelQueryBuilder.Operation.IN, content).getList(ListMirakel.class);
        lists.addAll(new MirakelQueryBuilder(ctx).and(ModelBase.ID,
                     MirakelQueryBuilder.Operation.IN,
        new ArrayList<>(Collections2.transform(Collections2.filter(content, new Predicate<Integer>() {
            @Override
            public boolean apply(Integer input) {
                return input < 0;
            }
        }), new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer input) {
                return input * -1;
            }
        }))).getList(SpecialList.class));

        return (this.isSet ? ctx.getString(R.string.not_in) : "") + TextUtils.join(", ",
        Collections2.transform(lists, new Function<ListMirakel, String>() {
            @Override
            public String apply(ListMirakel input) {
                return input.getName();
            }
        }));
    }

    @Override
    public String getTitle(Context ctx) {
        return ctx.getString(R.string.special_lists_list_title);
    }



    public static final Creator<SpecialListsListProperty> CREATOR = new
    Creator<SpecialListsListProperty>() {
        public SpecialListsListProperty createFromParcel(Parcel source) {
            return new SpecialListsListProperty(source);
        }

        public SpecialListsListProperty[] newArray(int size) {
            return new SpecialListsListProperty[size];
        }
    };
}