/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 *  Copyright (c) 2013-2014 Anatolij Zelenin, Georg Semmler.
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package de.azapps.mirakel.new_ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakelandroid.R;

public class ListAdapter extends CursorAdapter {
    final LayoutInflater mInflater;

    public ListAdapter(final Context context, final Cursor c, final int flags) {
        super(context, c, flags);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
        final View view = mInflater.inflate(R.layout.row_list, null);
        final ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

    @Override
    public void bindView(final View view, final Context context, final Cursor cursor) {
        final ViewHolder viewHolder = (ViewHolder) view.getTag();
        final ListMirakel listMirakel = new ListMirakel(cursor);
        viewHolder.list = listMirakel;
        viewHolder.name.setText(listMirakel.getName());
        viewHolder.count.setText(String.valueOf(listMirakel.countTasks()));
    }

    public static class ViewHolder {
        @InjectView(R.id.list_name)
        TextView name;
        @InjectView(R.id.list_count)
        TextView count;
        ListMirakel list;

        private ViewHolder(final View view) {
            ButterKnife.inject(this, view);
        }

        public ListMirakel getList() {
            return list;
        }
    }
}
