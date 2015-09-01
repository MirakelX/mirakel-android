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
import android.text.TextUtils;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

import java.util.ArrayList;
import java.util.List;

import de.azapps.mirakel.model.provider.MirakelInternalContentProvider;
import de.azapps.mirakel.model.generic.ModelBase;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.mirakel.model.task.Task;

public class SpecialListsTagProperty extends SpecialListsSetProperty {

    public SpecialListsTagProperty(final boolean isNegated,
                                   final List<Integer> content) {
        super(isNegated, content);
    }

    private SpecialListsTagProperty(final @NonNull Parcel in) {
        super(in);
    }

    public SpecialListsTagProperty(final @NonNull SpecialListsBaseProperty oldProperty) {
        super(oldProperty);
        if (oldProperty instanceof SpecialListsTagProperty) {
            content = ((SpecialListsTagProperty) oldProperty).getContent();
            isSet = ((SpecialListsTagProperty) oldProperty).isSet();
        } else {
            content = new ArrayList<>(5);
            isSet = false;
        }
    }

    @Override
    protected String getPropertyName() {
        return Tag.TABLE;
    }

    @NonNull
    @Override
    public MirakelQueryBuilder getWhereQueryBuilder(@NonNull final Context ctx) {
        if (content.isEmpty()) {
            return new MirakelQueryBuilder(ctx);
        }
        return new MirakelQueryBuilder(ctx).and(Task.ID,
                                                isSet ? MirakelQueryBuilder.Operation.NOT_IN : MirakelQueryBuilder.Operation.IN,
                                                new MirakelQueryBuilder(ctx).distinct().select("task_id").and("tag_id",
                                                        MirakelQueryBuilder.Operation.IN, content), MirakelInternalContentProvider.TAG_CONNECTION_URI);
    }

    @NonNull
    @Override
    public String getSummary(@NonNull final Context ctx) {
        if (content.isEmpty()) {
            return "";
        }
        List<Tag> tags = new MirakelQueryBuilder(ctx).and(ModelBase.ID, MirakelQueryBuilder.Operation.IN,
                content).getList(Tag.class);
        return (this.isSet ? ctx.getString(R.string.not_in) : "") + ' ' + TextUtils.join(", ",
        Collections2.transform(tags, new Function<Tag, String>() {
            @Override
            public String apply(Tag input) {
                return input.getName();
            }
        }));
    }

    @NonNull
    @Override
    public String getTitle(@NonNull Context ctx) {
        return ctx.getString(R.string.special_lists_tag_title);
    }




    public static final Creator<SpecialListsTagProperty> CREATOR = new
    Creator<SpecialListsTagProperty>() {
        @Override
        public SpecialListsTagProperty createFromParcel(final Parcel source) {
            return new SpecialListsTagProperty(source);
        }

        @Override
        public SpecialListsTagProperty[] newArray(final int size) {
            return new SpecialListsTagProperty[size];
        }
    };
}
