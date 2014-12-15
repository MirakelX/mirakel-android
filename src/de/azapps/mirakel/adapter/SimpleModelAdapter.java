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

import de.azapps.mirakel.model.IGenericElementInterface;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;

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
    public void onBindViewHolder(SimpleModelAdapter.ModelViewHolder holder, Cursor cursor) {
        holder.model = MirakelQueryBuilder.cursorToObject(cursor, tClass);
        holder.name.setText(holder.model.getName());
    }


    @Override
    public ModelViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        final View view = LayoutInflater.from(viewGroup.getContext()).inflate(
                              android.R.layout.simple_list_item_1, null);
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
