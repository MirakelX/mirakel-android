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

import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.new_ui.R;

public class ListAdapter extends CursorAdapter {
    private LayoutInflater mInflater;

    public ListAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = mInflater.inflate(R.layout.row_list, null);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        ListMirakel listMirakel = new ListMirakel(cursor);
        viewHolder.list = listMirakel;
        viewHolder.name.setText(listMirakel.getName());
        viewHolder.count.setText(listMirakel.countTasks() + "");
    }

    public static class ViewHolder {
        private final TextView name;
        private final TextView count;
        private ListMirakel list;

        private ViewHolder(View view) {
            name = (TextView) view.findViewById(R.id.list_name);
            count = (TextView) view.findViewById(R.id.list_count);
        }

        public ListMirakel getList() {
            return list;
        }
    }
}
