package de.azapps.mirakel.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.azapps.mirakel.model.ModelBase;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;

public class SimpleModelAdapter<T extends ModelBase> extends CursorAdapter {
    private LayoutInflater mInflater;
    private Class<T> tClass;

    public SimpleModelAdapter(Context context, Cursor c, int flags, Class<T> tClass) {
        super(context, c, flags);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.tClass = tClass;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = mInflater.inflate(android.R.layout.simple_list_item_1, null);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        viewHolder.model = MirakelQueryBuilder.cursorToObject(cursor, tClass);
        viewHolder.name.setText(viewHolder.model.getName());
    }

    public static class ViewHolder<T extends ModelBase> {
        private final TextView name;
        private T model;

        private ViewHolder(View view) {
            name = (TextView) view.findViewById(android.R.id.text1);
        }

        public T getModel() {
            return model;
        }
    }
}
