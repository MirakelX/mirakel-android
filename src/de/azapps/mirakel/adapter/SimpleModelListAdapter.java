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

package de.azapps.mirakel.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.azapps.mirakel.model.ModelBase;

import static de.azapps.mirakel.model.query_builder.MirakelQueryBuilder.cursorToObject;

/**
 * Created by az on 12/10/14.
 */
public class SimpleModelListAdapter<T extends ModelBase> extends CursorAdapter {
    private final LayoutInflater mInflater;
    private final Class<T> tClass;

    public SimpleModelListAdapter(final Context context, final Cursor c, final int flags,
                                  final Class<T> tClass) {
        super(context, c, flags);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.tClass = tClass;
    }

    @Override
    public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
        final View view = mInflater.inflate(android.R.layout.simple_list_item_1, null);
        final ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

    @Override
    public T getItem(final int position) {
        return cursorToObject((Cursor) super.getItem(position), tClass);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void bindView(final View view, final Context context, final Cursor cursor) {
        final ViewHolder<T> viewHolder = (ViewHolder<T>) view.getTag();
        viewHolder.model = cursorToObject(cursor, tClass);
        viewHolder.name.setText(viewHolder.model.getName());
    }

    public static class ViewHolder<T extends ModelBase> {
        private final TextView name;
        private T model;

        private ViewHolder(final View view) {
            name = (TextView) view.findViewById(android.R.id.text1);
        }

        public T getModel() {
            return model;
        }
    }
}
