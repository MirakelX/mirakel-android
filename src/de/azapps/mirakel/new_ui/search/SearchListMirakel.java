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

package de.azapps.mirakel.new_ui.search;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.text.SpannableStringBuilder;

import java.util.List;

import de.azapps.mirakel.model.MirakelInternalContentProvider;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.ListMirakelInterface;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.mirakel.model.task.Task;

public class SearchListMirakel implements ListMirakelInterface {
    private SearchObject search;
    private Context context;

    public SearchListMirakel(final Context context, final SearchObject search) {
        this.context = context;
        this.search = search;
    }

    private Uri getUri() {
        switch (search.getAutocompleteType()) {
        case TASK:
            return Task.URI;
        case TAG:
            return MirakelInternalContentProvider.TASK_VIEW_TAG_JOIN_URI;
        }
        throw new IllegalArgumentException("The autocomplete type is not set");
    }

    @Override
    public MirakelQueryBuilder getTasksQueryBuilder() {
        final MirakelQueryBuilder mirakelQueryBuilder = new MirakelQueryBuilder(context);
        switch (search.getAutocompleteType()) {
        case TASK:
            mirakelQueryBuilder.and(Task.VIEW_TABLE + '.' + Task.NAME, MirakelQueryBuilder.Operation.LIKE,
                                    '%' + search.getName() + '%');
            break;
        case TAG:
            mirakelQueryBuilder.and(Tag.TABLE + '.' + Tag.ID, MirakelQueryBuilder.Operation.EQ,
                                    search.getObjId());
            break;
        }
        Task.addBasicFiler(mirakelQueryBuilder);
        ListMirakel.addSortBy(mirakelQueryBuilder, ListMirakel.SORT_BY.OPT, true);
        return mirakelQueryBuilder;
    }

    @Override
    public CharSequence getName() {
        return new SpannableStringBuilder(context.getString(R.string.search_title))
               .append(search.getText(context));
    }

    public SearchObject getSearch() {
        return search;
    }

    @Override
    public List<Task> tasks() {
        return getTasksQueryBuilder().getList(Task.class);
    }

    @Override
    public long countTasks() {
        return getTasksQueryBuilder().count(getUri());
    }

    @Override
    public boolean shouldShowDoneToggle() {
        // yes a search list is kind of special ;)
        return true;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeParcelable(this.search, 0);
    }

    private SearchListMirakel(final Parcel in) {
        this.search = in.readParcelable(SearchObject.class.getClassLoader());
    }

    public static final Creator<SearchListMirakel> CREATOR = new Creator<SearchListMirakel>() {
        public SearchListMirakel createFromParcel(final Parcel source) {
            return new SearchListMirakel(source);
        }

        public SearchListMirakel[] newArray(final int size) {
            return new SearchListMirakel[size];
        }
    };
}
