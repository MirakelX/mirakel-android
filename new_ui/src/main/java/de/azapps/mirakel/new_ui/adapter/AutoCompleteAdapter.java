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
import android.graphics.Paint;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.mirakel.new_ui.search.SearchObject;
import de.azapps.mirakel.new_ui.views.TagSpan;
import de.azapps.mirakelandroid.R;

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
        final SearchObject searchObject = new SearchObject(cursor);
        // This is ok for tasks and a fallback variant for tags
        viewHolder.taskName.setText(searchObject.getName());
        switch (searchObject.getAutocompleteType()) {
        case TASK:
            if (searchObject.isDone()) {
                viewHolder.taskName.setPaintFlags(viewHolder.taskName.getPaintFlags() |
                                                  Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                viewHolder.taskName.setPaintFlags(viewHolder.taskName.getPaintFlags() &
                                                  ~Paint.STRIKE_THRU_TEXT_FLAG);
            }
            break;
        case TAG:
            final Tag tag = new Tag(searchObject.getObjId(), searchObject.getName(),
                                    searchObject.getBackgroundColor(), false);
            TagSpan tagSpan = new TagSpan(tag, context);
            SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
            stringBuilder.append(new SpannableString(tag.getName()));
            stringBuilder.setSpan(tagSpan, 0, tag.getName().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            viewHolder.taskName.setText(stringBuilder, TextView.BufferType.SPANNABLE);
            break;
        }
    }


    @Override
    public CharSequence convertToString(Cursor cursor) {
        final SearchObject searchObject = new SearchObject(cursor);
        return searchObject.getName();
    }


    @Override
    public Cursor runQueryOnBackgroundThread(final CharSequence constraint) {
        if (constraint == null) {
            return super.runQueryOnBackgroundThread(null);
        }
        return SearchObject.autocomplete(mContext, constraint.toString());
    }


    public class AutocompleteViewHolder extends RecyclerView.ViewHolder {
        @InjectView(R.id.task_name)
        TextView taskName;

        public AutocompleteViewHolder(final View view) {
            super(view);
            ButterKnife.inject(this, view);
        }
    }
}
