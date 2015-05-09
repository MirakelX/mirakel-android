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

package de.azapps.mirakel.new_ui.adapter;

import android.content.Context;
import android.os.Parcel;
import android.support.v4.content.Loader;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;

import java.util.List;

import de.azapps.mirakel.model.MirakelInternalContentProvider;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.ListMirakelInterface;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.new_ui.views.TagSpan;


public class SearchTagList implements ListMirakelInterface {
    private Tag tag;
    private Context context;


    public SearchTagList(final Context context, final Tag tag) {
        this.tag = tag;
        this.context = context;
    }

    private SearchTagList(final Parcel in) {
        this.tag = in.readParcelable(Tag.class.getClassLoader());
    }


    @Override
    public MirakelQueryBuilder getTasksQueryBuilder() {
        final MirakelQueryBuilder mirakelQueryBuilder = new MirakelQueryBuilder(context);
        mirakelQueryBuilder.and(Tag.TABLE + '.' + Tag.ID, MirakelQueryBuilder.Operation.EQ, tag.getId());
        Task.addBasicFiler(mirakelQueryBuilder);
        ListMirakel.addSortBy(mirakelQueryBuilder, ListMirakel.SORT_BY.OPT, true);
        return mirakelQueryBuilder;
    }

    @Override
    public Loader getTaskOverviewSupportCursorLoader() {
        return ListMirakel.addTaskOverviewSelection(getTasksQueryBuilder()).toSupportCursorLoader(
                MirakelInternalContentProvider.TASK_VIEW_TAG_JOIN_URI);
    }

    @Override
    public List<Task> tasks() {
        return getTasksQueryBuilder().getList(Task.class);
    }

    @Override
    public long countTasks() {
        return getTasksQueryBuilder().count(MirakelInternalContentProvider.TASK_VIEW_TAG_JOIN_URI);
    }


    @Override
    public CharSequence getName() {
        final TagSpan tagSpan = new TagSpan(tag, context);
        final SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
        stringBuilder.append(new SpannableString(tag.getName()));
        stringBuilder.setSpan(tagSpan, 0, tag.getName().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return context.getString(de.azapps.mirakel.model.R.string.search_title, stringBuilder);
    }



    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeParcelable(this.tag, flags);
    }

    public static final Creator<SearchTagList> CREATOR = new Creator<SearchTagList>() {
        public SearchTagList createFromParcel(final Parcel source) {
            return new SearchTagList(source);
        }

        public SearchTagList[] newArray(final int size) {
            return new SearchTagList[size];
        }
    };
}
