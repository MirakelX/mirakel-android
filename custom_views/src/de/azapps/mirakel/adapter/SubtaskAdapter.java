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
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import de.azapps.mirakel.model.task.Task;

public class SubtaskAdapter extends ArrayAdapter<Task> {

	private List<Task> data;
	private final Context context;
	private boolean[] checked;
	private final Task task;
	private final boolean asSubtask;

	public SubtaskAdapter(final Context context, final int textViewResourceId,
			final List<Task> objects, final Task task, final boolean asSubtask) {
		super(context, textViewResourceId, objects);
		this.data = objects;
		this.context = context;
		this.task = task;
		this.checked = new boolean[this.data.size()];
		this.asSubtask = asSubtask;
	}

	@Override
	public int getCount() {
		return this.data.size();
	}

	@Override
	public View getView(final int position, final View convertView,
			final ViewGroup parent) {
		if (position >= this.data.size()) {
			return new View(this.context);
		}
		final CheckBox c = new CheckBox(this.context);
		if (!this.asSubtask) {
			this.checked[position] = this.data.get(position).isSubtaskOf(
					this.task);
		} else {
			this.checked[position] = this.task.isSubtaskOf(this.data
					.get(position));
		}
		c.setChecked(this.checked[position]);
		c.setText(this.data.get(position).getName());
		c.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(final CompoundButton buttonView,
					final boolean isChecked) {
				SubtaskAdapter.this.checked[position] = isChecked;
			}
		});
		return c;
	}

	public boolean[] getChecked() {
		return this.checked;
	}

	public List<Task> getData() {
		return this.data;
	}

	final Handler mHandler = new Handler();

	public void setData(final List<Task> newData) {
		this.data = newData;
		this.checked = new boolean[this.data.size()];
		this.mHandler.post(new Runnable() {
			@Override
			public void run() {
				notifyDataSetChanged();
			}
		});

	}

}
