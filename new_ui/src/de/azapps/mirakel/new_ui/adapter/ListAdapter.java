package de.azapps.mirakel.new_ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.ResourceCursorAdapter;
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
		viewHolder.list= listMirakel;

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
