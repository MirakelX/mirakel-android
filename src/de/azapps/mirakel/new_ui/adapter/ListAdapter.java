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
import android.support.v7.widget.CursorAdapter;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.azapps.mirakel.adapter.OnItemClickedListener;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakelandroid.R;

public class ListAdapter extends CursorAdapter<ListAdapter.ListViewHolder> {
    final LayoutInflater mInflater;
    private final OnItemClickedListener<ListMirakel> itemClickListener;

    public ListAdapter(final Context context, final Cursor cursor, final int flags,
                       final OnItemClickedListener<ListMirakel> itemClickListener) {
        super(context, cursor, flags);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.itemClickListener = itemClickListener;
        setHasStableIds(true);
    }

    @Override
    public ListViewHolder onCreateViewHolder(final ViewGroup viewGroup, final int i) {
        final View view = mInflater.inflate(R.layout.row_list, viewGroup, false);
        return new ListViewHolder(view);
    }


    @Override
    protected void onContentChanged() {
        //nothing for now
    }

    @Override
    public void onBindViewHolder(final ListViewHolder holder, final Cursor cursor) {
        final ListMirakel listMirakel = new ListMirakel(cursor);
        holder.list = listMirakel;
        holder.name.setText(listMirakel.getName());
        holder.count.setText(String.valueOf(listMirakel.countTasks()));
    }

    public class ListViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @InjectView(R.id.list_name)
        TextView name;
        @InjectView(R.id.list_count)
        TextView count;
        ListMirakel list;

        public ListViewHolder(final View view) {
            super(view);
            view.setOnClickListener(this);
            ButterKnife.inject(this, view);
        }

        public ListMirakel getList() {
            return list;
        }

        @Override
        public void onClick(final View v) {
            itemClickListener.onItemSelected(list);
        }
    }
}
