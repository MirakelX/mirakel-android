package de.azapps.mirakel.settings.taskfragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.app.Activity;
import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import de.azapps.mirakel.adapter.MirakelArrayAdapter;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.main_activity.task_fragment.TaskFragmentAdapter.TYPE;
import de.azapps.mirakel.main_activity.task_fragment.TaskFragmentAdapter.TYPE.NoSuchItemException;
import de.azapps.mirakelandroid.R;

public class TaskFragmentSettingsAdapter extends MirakelArrayAdapter<Integer> {
	private static final String TAG = "TaskFragmentSettingsAdapter";

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
	public View getView(final int position, View convertView, ViewGroup parent) {
		if (data.size() - 1 == position
				&& data.get(position) == TaskFragmentSettings.ADD_KEY) {
			return setupAddButton();
		}
		View row = convertView;
		ListHolder holder = null;
		if (row == null || row.getId() != R.id.wrapper_taskfragmentsettings_row) {
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
		final Integer item = data.get(position);
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

	private View setupAddButton() {
		Spinner b = new Spinner(context);
		final SparseArray<String> allItems = new SparseArray<String>();
		try {
			allItems.put(TYPE.HEADER,
					TYPE.getTranslatedName(context, TYPE.HEADER));
			allItems.put(TYPE.CONTENT,
					TYPE.getTranslatedName(context, TYPE.CONTENT));
			allItems.put(TYPE.DUE, TYPE.getTranslatedName(context, TYPE.DUE));
			allItems.put(TYPE.FILE, TYPE.getTranslatedName(context, TYPE.FILE));
			allItems.put(TYPE.PROGRESS,
					TYPE.getTranslatedName(context, TYPE.PROGRESS));
			allItems.put(TYPE.SUBTASK,
					TYPE.getTranslatedName(context, TYPE.SUBTASK));
			allItems.put(TYPE.SUBTITLE,
					TYPE.getTranslatedName(context, TYPE.SUBTITLE));
			allItems.put(TYPE.REMINDER,
					TYPE.getTranslatedName(context, TYPE.REMINDER));
		} catch (NoSuchItemException e) {
			Log.wtf(TAG, "go sleeping, its to late");
		}
		for (int d : data) {
			if (d != TaskFragmentSettings.ADD_KEY) {
				allItems.remove(d);
			}
		}
		CharSequence[] items = new String[allItems.size() + 1];
		items[0] = "+";
		for (int i = 0; i < allItems.size(); i++) {
			items[i + 1] = allItems.valueAt(i);
		}
		ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
				context, android.R.layout.simple_spinner_item, items);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		b.setAdapter(adapter);
		b.setSelection(0);
		b.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int pos,
					long arg3) {
				if (pos != 0) {
					data.add(data.size()-1, allItems.keyAt(pos - 1));
					notifyDataSetChanged();
				}

			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// Nothing

			}
		});
		if(items.length==1)
			b.setEnabled(false);//Nothing to add
		return b;
	}

	public void onRemove(int which) {
		Log.d(TAG, "which" + which);
		if (which < 0 || which > data.size())
			return;
		data.remove(which);
		notifyDataSetChanged();
	}

	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
		data.remove(data.size()-1);
		MirakelPreferences.setTaskFragmentLayout(data);
		data.add(TaskFragmentSettings.ADD_KEY);
	}

	public void onDrop(final int from, final int to) {
		Integer item = data.get(from);
		data.remove(from);
		data.add(to, item);
		notifyDataSetChanged();
	}

	static class ListHolder {
		TextView rowName;
		ImageView rowDrag;
	}

}
