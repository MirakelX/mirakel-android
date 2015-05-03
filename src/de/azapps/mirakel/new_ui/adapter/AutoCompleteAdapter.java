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
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.azapps.mirakel.model.search.Autocomplete;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.new_ui.views.AddTagView;
import de.azapps.mirakel.new_ui.views.TaskNameView;
import de.azapps.mirakelandroid.R;
import de.azapps.tools.OptionalUtils;

public class AutoCompleteAdapter extends CursorAdapter {


    private final LayoutInflater mInflater;

    public AutoCompleteAdapter(final Context context, final Cursor cursor) {
        super(context, cursor, true);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        final View view = mInflater.inflate(R.layout.row_search, viewGroup, false);
        view.setTag(new AutocompleteViewHolder(view));
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final AutocompleteViewHolder viewHolder = (AutocompleteViewHolder) view.getTag();
        final Autocomplete autocomplete = new Autocomplete(cursor);
        // This is ok for tasks and a fallback variant for tags
        viewHolder.taskName.setText(autocomplete.getName());
        viewHolder.taskName.setStrikeThrough(false);
        switch (autocomplete.getAutocompleteType()) {
        case TASK:
            viewHolder.viewSwitcher.setDisplayedChild(0);
            OptionalUtils.withOptional(Task.get(autocomplete.getObjId()), new OptionalUtils.Procedure<Task>() {
                @Override
                public void apply(Task task) {
                    viewHolder.taskName.setStrikeThrough(task.isDone());
                }
            });
            break;
        case TAG:
            final Tag tag = Tag.get(autocomplete.getObjId()).orNull();
            if (tag != null) {
                ArrayList<Tag> tags = new ArrayList<>(1);
                tags.add(tag);
                viewHolder.tagView.setTags(tags);
                viewHolder.viewSwitcher.setDisplayedChild(1);
            } else {
                viewHolder.viewSwitcher.setDisplayedChild(0);
            }
            break;
        }
    }


    @Override
    public CharSequence convertToString(Cursor cursor) {
        final Autocomplete autocomplete = new Autocomplete(cursor);
        return autocomplete.getName();
    }


    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
        return Autocomplete.autocomplete(mContext, constraint.toString());
    }


    public class AutocompleteViewHolder extends RecyclerView.ViewHolder {
        @InjectView(R.id.tag_name)
        AddTagView tagView;
        @InjectView(R.id.task_name)
        TaskNameView taskName;
        @InjectView(R.id.view_switcher)
        ViewSwitcher viewSwitcher;

        public AutocompleteViewHolder(final View view) {
            super(view);
            ButterKnife.inject(this, view);
            tagView.setClickEnabled(false);
        }
    }
}
