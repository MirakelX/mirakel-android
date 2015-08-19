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

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;

import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.generic.ModelBase;
import de.azapps.mirakel.model.generic.ModelFactory;
import de.azapps.mirakel.model.query_builder.CursorGetter;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class MultiSelectModelAdapter<T extends ModelBase> extends CursorAdapter {
    private LayoutInflater mInflater;
    private Class<T> tClass;
    private Set<T> selectedItems = new HashSet<>();
    private OnSelectionChangedListener<T> selectionChangedListener;

    public interface OnSelectionChangedListener<T extends ModelBase> {
        public void onSelectionChanged(Set<T> selectedItems);
    }

    public MultiSelectModelAdapter(Context context, Cursor c, int flags, Class<T> tClass) {
        super(context, c, flags);
        mInflater = LayoutInflater.from(context);
        this.tClass = tClass;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = mInflater.inflate(R.layout.row_selectable_item, null);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void bindView(final View view, final Context context, final Cursor cursor) {
        final ViewHolder<T> viewHolder = (ViewHolder<T>) view.getTag();
        viewHolder.model = ModelFactory.createModel(CursorGetter.unsafeGetter(cursor), tClass);
        viewHolder.name.setText(cursor.getString(cursor.getColumnIndex(ModelBase.NAME)));
        viewHolder.checkBox.setChecked(selectedItems.contains(viewHolder.model));
        viewHolder.checkBox.setTag(viewHolder.model);
        viewHolder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    selectedItems.add((T) compoundButton.getTag());
                } else {
                    selectedItems.remove((T) compoundButton.getTag());
                }
                if (selectionChangedListener != null) {
                    selectionChangedListener.onSelectionChanged(selectedItems);
                }
            }
        });
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewHolder.checkBox.setChecked(!viewHolder.checkBox.isChecked());
            }
        });
    }

    public static class ViewHolder<T extends ModelBase> {
        private final TextView name;
        private final CheckBox checkBox;
        private T model;

        private ViewHolder(View view) {
            name = (TextView) view.findViewById(R.id.selectable_item_text_view);
            checkBox = (CheckBox) view.findViewById(R.id.selectable_item_checkbox);
        }

        public T getModel() {
            return model;
        }
    }

    public void setSelectionChangedListener(OnSelectionChangedListener<T> selectionChangedListener) {
        this.selectionChangedListener = selectionChangedListener;
    }

    public Set<T> getSelectedItems() {
        return selectedItems;
    }
}

