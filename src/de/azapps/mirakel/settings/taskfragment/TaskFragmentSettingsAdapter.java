package de.azapps.mirakel.settings.taskfragment;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;
import de.azapps.mirakel.adapter.MirakelArrayAdapter;
import de.azapps.mirakel.main_activity.task_fragment.TaskFragmentAdapter;
import de.azapps.mirakel.main_activity.task_fragment.TaskFragmentAdapter.TYPE;
import de.azapps.mirakel.main_activity.task_fragment.TaskFragmentAdapter.TYPE.NoSuchItemException;
import de.azapps.mirakelandroid.R;

public class TaskFragmentSettingsAdapter extends
		MirakelArrayAdapter<Pair<Integer, Boolean>> {
	public TaskFragmentSettingsAdapter(Context c) {
		// do not call this, only for error-fixing there
		super(
				c,
				0,
				(List<Pair<Integer, Boolean>>) new ArrayList<Pair<Integer, Boolean>>());
	}

	public TaskFragmentSettingsAdapter(Context context, int layoutResourceId,
			List<Pair<Integer, Boolean>> data) {
		super(context, layoutResourceId, data);
	}

	@Override
	public void changeData(List<Pair<Integer, Boolean>> lists) {
		super.changeData(lists);
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
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
			holder.rowShow = (CheckBox) row
					.findViewById(R.id.row_taskfragment_settings_show);
			row.setTag(holder);
		} else {
			holder = (ListHolder) row.getTag();
		}
		final Pair<Integer, Boolean> item = data.get(position);
		holder.rowDrag.setVisibility(View.VISIBLE);

		try {
			holder.rowName.setText(TYPE.getTranslatedName(context, item.first));
		} catch (NoSuchItemException e) {
			holder.rowName.setText("");
		}
		if (item.second)
			holder.rowShow.setChecked(true);
		else
			holder.rowShow.setChecked(false);
		holder.rowShow
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						data.set(position, new Pair<Integer, Boolean>(
								item.first, isChecked));
						TaskFragmentAdapter.setValues(context, data);
					}
				});
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
		Pair<Integer, Boolean> item = data.get(from);
		data.remove(from);
		data.add(to, item);
		TaskFragmentAdapter.setValues(context, data);
		notifyDataSetChanged();
	}

	static class ListHolder {
		TextView rowName;
		ImageView rowDrag;
		CheckBox rowShow;
	}

}
