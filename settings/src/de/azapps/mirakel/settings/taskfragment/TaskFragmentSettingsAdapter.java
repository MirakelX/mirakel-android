/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013-2014 Anatolij Zelenin, Georg Semmler.
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.azapps.mirakel.settings.taskfragment;

import java.util.ArrayList;
import java.util.List;

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
import de.azapps.mirakel.custom_views.TaskDetailView.TYPE;
import de.azapps.mirakel.custom_views.TaskDetailView.TYPE.NoSuchItemException;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.settings.R;
import de.azapps.tools.Log;

public class TaskFragmentSettingsAdapter extends MirakelArrayAdapter<Integer> {
	static class ListHolder {
		ImageView rowDrag;
		TextView rowName;
	}

	private static final String TAG = "TaskFragmentSettingsAdapter";

	public TaskFragmentSettingsAdapter(final Context c) {
		// do not call this, only for error-fixing there
		super(c, 0, new ArrayList<Integer>());
	}

	public TaskFragmentSettingsAdapter(final Context context,
			final int layoutResourceId, final List<Integer> data) {
		super(context, layoutResourceId, data);
	}

	@Override
	public void changeData(final List<Integer> lists) {
		super.changeData(lists);
	}

	@Override
	public View getView(final int position, final View convertView,
			final ViewGroup parent) {
		if (this.data.size() - 1 == position
				&& this.data.get(position) == TaskFragmentSettingsFragment.ADD_KEY) {
			return setupAddButton();
		}
		View row = convertView;
		ListHolder holder = null;
		if (row == null || row.getId() != R.id.wrapper_taskfragmentsettings_row) {
			final LayoutInflater inflater = ((Activity) this.context)
					.getLayoutInflater();
			row = inflater.inflate(this.layoutResourceId, parent, false);
			holder = new ListHolder();
			holder.rowName = (TextView) row
					.findViewById(R.id.row_taskfragment_settings_name);
			holder.rowDrag = (ImageView) row
					.findViewById(R.id.row_taskfragment_settings_drag);
			row.setTag(holder);
		} else {
			holder = (ListHolder) row.getTag();
		}
		final Integer item = this.data.get(position);
		holder.rowDrag.setVisibility(View.VISIBLE);

		try {
			holder.rowName.setText(TYPE.getTranslatedName(this.context, item));
		} catch (final NoSuchItemException e) {
			holder.rowName.setText("");
		}
		holder.rowName.setTag(item);
		if (this.selected.get(position)) {
			row.setBackgroundColor(this.context.getResources().getColor(
					this.darkTheme ? R.color.highlighted_text_holo_dark
							: R.color.highlighted_text_holo_light));
		}

		return row;
	}

	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
		this.data.remove(this.data.size() - 1);
		MirakelCommonPreferences.setTaskFragmentLayout(this.data);
		this.data.add(TaskFragmentSettingsFragment.ADD_KEY);
	}

	public void onDrop(final int from, final int to) {
		final Integer item = this.data.get(from);
		this.data.remove(from);
		this.data.add(to, item);
		notifyDataSetChanged();
	}

	public void onRemove(final int which) {
		Log.d(TAG, "which" + which);
		if (which < 0 || which > this.data.size()) {
			return;
		}
		this.data.remove(which);
		notifyDataSetChanged();
	}

	private View setupAddButton() {
		final Spinner b = new Spinner(this.context);
		final SparseArray<String> allItems = new SparseArray<String>();
		try {
			allItems.put(TYPE.HEADER,
					TYPE.getTranslatedName(this.context, TYPE.HEADER));
			allItems.put(TYPE.CONTENT,
					TYPE.getTranslatedName(this.context, TYPE.CONTENT));
			allItems.put(TYPE.DUE,
					TYPE.getTranslatedName(this.context, TYPE.DUE));
			allItems.put(TYPE.FILE,
					TYPE.getTranslatedName(this.context, TYPE.FILE));
			allItems.put(TYPE.PROGRESS,
					TYPE.getTranslatedName(this.context, TYPE.PROGRESS));
			allItems.put(TYPE.SUBTASK,
					TYPE.getTranslatedName(this.context, TYPE.SUBTASK));
			// allItems.put(TYPE.SUBTITLE,
			// TYPE.getTranslatedName(context, TYPE.SUBTITLE));
			allItems.put(TYPE.REMINDER,
					TYPE.getTranslatedName(this.context, TYPE.REMINDER));
		} catch (final NoSuchItemException e) {
			Log.wtf(TAG, "go sleeping, its to late");
		}
		for (final int d : this.data) {
			if (d != TaskFragmentSettingsFragment.ADD_KEY) {
				allItems.remove(d);
			}
		}
		final CharSequence[] items = new String[allItems.size() + 1];
		items[0] = "+";
		for (int i = 0; i < allItems.size(); i++) {
			items[i + 1] = allItems.valueAt(i);
		}
		final ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
				this.context, android.R.layout.simple_spinner_item, items);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		b.setAdapter(adapter);
		b.setSelection(0);
		b.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(final AdapterView<?> arg0,
					final View arg1, final int pos, final long arg3) {
				if (pos != 0) {
					TaskFragmentSettingsAdapter.this.data.add(
							TaskFragmentSettingsAdapter.this.data.size() - 1,
							allItems.keyAt(pos - 1));
					notifyDataSetChanged();
				}

			}

			@Override
			public void onNothingSelected(final AdapterView<?> arg0) {
				// Nothing

			}
		});
		if (items.length == 1) {
			b.setEnabled(false);// Nothing to add
		}
		return b;
	}

}
