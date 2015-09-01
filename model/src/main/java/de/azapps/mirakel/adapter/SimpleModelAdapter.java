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
import android.support.annotation.NonNull;
import android.support.v7.widget.CursorAdapter;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.generic.IGenericElementInterface;
import de.azapps.mirakel.model.generic.ModelFactory;
import de.azapps.mirakel.model.query_builder.CursorGetter;

public class SimpleModelAdapter<T extends IGenericElementInterface> extends
    CursorAdapter<SimpleModelAdapter.ModelViewHolder> {
    @NonNull
    private final Class<T> tClass;
    @NonNull
    private final OnItemClickedListener<T> onItemClickedListener;


    public SimpleModelAdapter(final Context context, final Cursor c,
                              final Class<T> tClass, final @NonNull OnItemClickedListener<T> onClick) {
        super(context, c);
        this.tClass = tClass;
        this.onItemClickedListener = onClick;
    }


    @Override
    public void onBindViewHolder(final SimpleModelAdapter.ModelViewHolder holder, final Cursor cursor,
                                 final int position) {
        holder.model = ModelFactory.createModel(CursorGetter.unsafeGetter(cursor), tClass);
        holder.name.setText(holder.model.getName());
    }


    @Override
    public ModelViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        final View view = LayoutInflater.from(viewGroup.getContext()).inflate(
                              R.layout.simple_list_item_1, null);
        view.setMinimumWidth(viewGroup.getWidth());
        return new ModelViewHolder(view);
    }

    public class ModelViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private T model;

        ModelViewHolder(final View view) {
            super(view);
            name = (TextView) view.findViewById(android.R.id.text1);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickedListener.onItemSelected(model);
                }
            });
        }

        public T getModel() {
            return model;
        }
    }
}
