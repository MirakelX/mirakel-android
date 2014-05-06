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
package de.azapps.mirakel.adapter;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import de.azapps.mirakel.model.task.Task;

public class SubtaskAdapter extends MirakelArrayAdapter<Task> {

	private final Task task;
	private final boolean asSubtask;

	public SubtaskAdapter(final Context context, final int textViewResourceId,
			final List<Task> objects, final Task task, final boolean asSubtask) {
		super(context, textViewResourceId, objects);
		this.task = task;
		this.asSubtask = asSubtask;
		determineSelected();
	}

	@Override
	public View getView(final int position, final View convertView,
			final ViewGroup parent) {
		if (position >= getCount()) {
			return new View(this.context);

		}
		final CheckBox c = new CheckBox(this.context);
		c.setChecked(this.isSelectedAt(position));
		c.setText(this.getDataAt(position).getName());
		c.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(final CompoundButton buttonView,
					final boolean isChecked) {
				SubtaskAdapter.this.setSelected(position, isChecked);
			}
		});
		return c;
	}

	@Override
	public void changeData(final List<Task> newData) {
		super.changeData(newData);
		determineSelected();
	}

	private void determineSelected() {
		for (int i = 0; i < getCount(); i++) {
			final Task t = this.getDataAt(i);
			if (!this.asSubtask) {
				setSelected(i, t.isSubtaskOf(this.task));
			} else {
				setSelected(i, this.task.isSubtaskOf(t));
			}
			notifyDataSetChanged();
		}
	}

}
