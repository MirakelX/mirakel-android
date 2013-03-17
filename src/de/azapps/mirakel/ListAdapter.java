package de.azapps.mirakel;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ListAdapter extends ArrayAdapter<List_mirakle> {
	Context context;
	int layoutResourceId;
	List<List_mirakle> data = null;

	public ListAdapter(Context context, int layoutResourceId,
			List<List_mirakle> data) {
		super(context, layoutResourceId, data);
		this.layoutResourceId = layoutResourceId;
		this.data = data;
		this.context = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		ListHolder holder = null;

		if (row == null) {
			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			row = inflater.inflate(layoutResourceId, parent, false);
			holder = new ListHolder();
			holder.listRowName = (TextView) row
					.findViewById(R.id.list_row_name);
			holder.listRowTaskNumber = (TextView) row
					.findViewById(R.id.list_row_task_number);

			row.setTag(holder);
		} else {
			holder = (ListHolder) row.getTag();
		}
		List_mirakle list = data.get(position);
		holder.listRowName.setText(list.getName());
		holder.listRowName.setTag(list);
		holder.listRowTaskNumber.setText(list.getTask_count() + "");
		return row;
	}

	static class ListHolder {
		TextView listRowName;
		TextView listRowTaskNumber;
	}
}
