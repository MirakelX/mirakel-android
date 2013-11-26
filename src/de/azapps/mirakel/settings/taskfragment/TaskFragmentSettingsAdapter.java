package de.azapps.mirakel.settings.taskfragment;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import de.azapps.mirakel.adapter.MirakelArrayAdapter;
import de.azapps.mirakel.main_activity.task_fragment.TaskFragmentAdapter;
import de.azapps.mirakel.main_activity.task_fragment.TaskFragmentAdapter.TYPE;
import de.azapps.mirakel.main_activity.task_fragment.TaskFragmentAdapter.TYPE.NoSuchItemException;
import de.azapps.mirakelandroid.R;

public class TaskFragmentSettingsAdapter extends MirakelArrayAdapter<Integer> {
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
			holder.rowName = (TextView) row
					.findViewById(R.id.row_taskfragment_settings_name);
			holder.rowDrag = (ImageView) row
					.findViewById(R.id.row_taskfragment_settings_drag);
			row.setTag(holder);
		} else {
			holder = (ListHolder) row.getTag();
		}
		Integer item = data.get(position);
		holder.rowDrag.setVisibility(View.VISIBLE);

		try {
			holder.rowName.setText(TYPE.getTranslatedName(context, item));
		} catch (NoSuchItemException e) {
			holder.rowName.setText("");
		}
		holder.rowName.setTag(item);
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
		data.remove(which);
	}

	public void onDrop(final int from, final int to) {
		Integer item = data.get(from);
		data.remove(from);
		data.add(to, item);
		TaskFragmentAdapter.setValues(context, data);
		notifyDataSetChanged();
	}

	static class ListHolder {
		TextView rowName;
		ImageView rowDrag;
	}

}
