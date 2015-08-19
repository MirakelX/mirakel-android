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

package de.azapps.mirakel.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.azapps.mirakel.model.generic.ModelBase;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.generic.ModelFactory;
import de.azapps.mirakel.model.query_builder.CursorGetter;


public class SimpleModelListAdapter<T extends ModelBase> extends CursorAdapter {
    private final LayoutInflater mInflater;
    private final Class<T> tClass;
    private final int layout;

    public SimpleModelListAdapter(final Context context, final Cursor cursor, final int flags,
                                  final Class<T> tClass) {
        this(context, cursor, flags, tClass,  R.layout.simple_list_row);
    }

    public SimpleModelListAdapter(final Context context, final Cursor cursor, final int flags,
                                  final Class<T> tClass, final int layout) {
        super(context, cursor, flags);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.tClass = tClass;
        this.layout = layout;
    }

    @Override
    public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
        final View view = mInflater.inflate(layout, null);
        final ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

    @Override
    public T getItem(final int position) {
        return ModelFactory.createModel(CursorGetter.unsafeGetter((Cursor) super.getItem(position)),
                                        tClass);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void bindView(final View view, final Context context, final Cursor cursor) {
        final ViewHolder<T> viewHolder = (ViewHolder<T>) view.getTag();
        viewHolder.model = ModelFactory.createModel(CursorGetter.unsafeGetter(cursor), tClass);
        viewHolder.setText(viewHolder.model.getName());
    }

    @SuppressWarnings("unchecked")
    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        final View v = super.getView(position, convertView, parent);
        final ViewHolder<T> viewHolder = (ViewHolder<T>) v.getTag();
        viewHolder.header_text.setVisibility(View.VISIBLE);
        viewHolder.normal_text.setVisibility(View.GONE);
        return v;
    }

    @SuppressWarnings("unchecked")
    @Override
    public View getDropDownView(final int position, final View convertView, final ViewGroup parent) {
        final View v = super.getView(position, convertView, parent);
        final ViewHolder<T> viewHolder = (ViewHolder<T>) v.getTag();
        viewHolder.header_text.setVisibility(View.GONE);
        viewHolder.normal_text.setVisibility(View.VISIBLE);
        return v;
    }

    public static class ViewHolder<T extends ModelBase> {
        private final TextView header_text;
        private final TextView normal_text;
        private T model;

        private ViewHolder(final View view) {
            header_text = (TextView) view.findViewById(R.id.header_text);
            normal_text = (TextView) view.findViewById(android.R.id.text1);
        }

        public void setText(final String text) {
            header_text.setText(text);
            normal_text.setText(text);
        }

        public T getModel() {
            return model;
        }
    }
}
