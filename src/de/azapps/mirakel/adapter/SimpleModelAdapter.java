package de.azapps.mirakel.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.azapps.mirakel.model.IGenericElementInterface;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;

public class SimpleModelAdapter<T extends IGenericElementInterface> extends CursorAdapter {
    private final LayoutInflater mInflater;
    private final Class<T> tClass;

    public SimpleModelAdapter(final Context context, final Cursor c, final int flags,
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

    public T getItem(final int position) {
        return MirakelQueryBuilder.cursorToObject((Cursor)super.getItem(position), tClass);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void bindView(final View view, final Context context, final Cursor cursor) {
        final ViewHolder<T> viewHolder = (ViewHolder<T>) view.getTag();
        viewHolder.model = MirakelQueryBuilder.cursorToObject(cursor, tClass);
        viewHolder.name.setText(viewHolder.model.getName());
    }

    public static class ViewHolder<T extends IGenericElementInterface> {
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
