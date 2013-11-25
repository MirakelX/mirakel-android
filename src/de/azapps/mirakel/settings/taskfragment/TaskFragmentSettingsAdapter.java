package de.azapps.mirakel.settings.taskfragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import de.azapps.mirakel.adapter.MirakelArrayAdapter;
import de.azapps.mirakel.main_activity.task_fragment.TaskFragmentAdapter.TYPE;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakelandroid.R;

public class TaskFragmentSettingsAdapter extends MirakelArrayAdapter<Integer> {
	private Map<Integer, View> viewsForLists = new HashMap<Integer, View>();

	public View getViewForList(ListMirakel list) {
		return viewsForLists.get(list.getId());
	}

	public TaskFragmentSettingsAdapter(Context c) {
		// do not call this, only for error-fixing there
		super(c, 0, (List<Integer>) new ArrayList<Integer>());
	}

	public TaskFragmentSettingsAdapter(Context context, int layoutResourceId,
			List<Integer> data) {
		super(context, layoutResourceId, data);
	}

	@Override
	public void changeData(List<Integer> lists) {
		viewsForLists.clear();
		super.changeData(lists);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		ListHolder holder = null;

		if (row == null) {
			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			row = inflater.inflate(layoutResourceId, parent, false);
			holder = new ListHolder();
			holder.rowName = (TextView) row.findViewById(R.id.list_row_name);
			holder.rowDrag = (ImageView) row.findViewById(R.id.list_row_drag);
			row.setTag(holder);
		} else {
			holder = (ListHolder) row.getTag();
		}
		Integer item = data.get(position);
		holder.rowDrag.setVisibility(View.VISIBLE);

		holder.rowName.setText(TYPE.getName(item));
		holder.rowName.setTag(item);
		viewsForLists.put(item, row);
		if (selected.get(position)) {
			row.setBackgroundColor(context.getResources().getColor(
					darkTheme ? R.color.highlighted_text_holo_dark
							: R.color.highlighted_text_holo_light));
		}

		return row;
	}

	public void onRemove(int which) {
		if (which < 0 || which > data.size())
			return;
		viewsForLists.remove(data.get(which));
		data.remove(which);
	}

	public void onDrop(final int from, final int to) {
		// TODO
	}

	static class ListHolder {
		TextView rowName;
		ImageView rowDrag;
	}

}
