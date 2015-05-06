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

package de.azapps.mirakel.model.list;

import android.content.Context;
import android.os.Parcel;
import android.support.v4.content.Loader;

import java.util.List;

import de.azapps.mirakel.model.MirakelInternalContentProvider;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.task.Task;

/**
 * Created by az on 03.05.15.
 */
public class SearchListMirakel implements ListMirakelInterface, android.os.Parcelable {
    private String searchText;
    private Context context;

    public SearchListMirakel(Context context, String searchText) {
        this.context = context;
        this.searchText = searchText;
    }

    private MirakelQueryBuilder getMirakelQueryBuilder() {
        MirakelQueryBuilder mirakelQueryBuilder = new MirakelQueryBuilder(context);
        mirakelQueryBuilder.and(Task.NAME, MirakelQueryBuilder.Operation.LIKE, "%" + searchText + "%");
        Task.addBasicFiler(mirakelQueryBuilder);
        ListMirakel.addSortBy(mirakelQueryBuilder, ListMirakel.SORT_BY.OPT, true);
        return mirakelQueryBuilder;
    }

    @Override
    public Loader getTaskOverviewSupportCursorLoader() {
        return ListMirakel.addTaskOverviewSelection(getMirakelQueryBuilder()).toSupportCursorLoader(
                   MirakelInternalContentProvider.TASK_URI);
    }

    @Override
    public String getName() {
        return context.getString(R.string.search_title, searchText);
    }

    @Override
    public List<Task> tasks() {
        return getMirakelQueryBuilder().getList(Task.class);
    }

    @Override
    public long countTasks() {
        return getMirakelQueryBuilder().count(MirakelInternalContentProvider.TASK_URI);
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.searchText);
    }

    private SearchListMirakel(Parcel in) {
        this.searchText = in.readString();
    }

    public static final Creator<SearchListMirakel> CREATOR = new Creator<SearchListMirakel>() {
        public SearchListMirakel createFromParcel(Parcel source) {
            return new SearchListMirakel(source);
        }

        public SearchListMirakel[] newArray(int size) {
            return new SearchListMirakel[size];
        }
    };
}
