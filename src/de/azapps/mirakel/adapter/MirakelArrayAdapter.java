/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013 Anatolij Zelenin, Georg Semmler.
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

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.widget.ArrayAdapter;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.tools.Log;

public class MirakelArrayAdapter<T> extends ArrayAdapter<T> {
	private static final String TAG = "MirakelArrayAdapter";
	protected Context context;
	protected boolean darkTheme;
	protected List<T> data;
	protected int layoutResourceId;
	protected List<Boolean> selected;
	protected int selectedCount;

	public MirakelArrayAdapter(Context context, int textViewResourceId,
			List<T> data) {
		super(context, textViewResourceId, textViewResourceId, data);
		this.layoutResourceId = textViewResourceId;
		this.data = data;
		this.context = context;
		this.darkTheme = MirakelPreferences.isDark();
		this.selected = new ArrayList<Boolean>();
		for (int i = 0; i < data.size(); i++) {
			this.selected.add(false);
		}
		this.selectedCount = 0;

	}

	public void addToEnd(T el) {
		this.data.add(el);
		this.selected.add(false);
	}

	public void addToHead(T el) {
		this.data.add(0, el);
		this.selected.add(false);
	}

	public void changeData(List<T> newData) {
		this.data.clear();
		this.data.addAll(newData);
		while (this.data.size() > this.selected.size()) {
			this.selected.add(false);
		}
	}

	@Override
	public int getCount() {
		return this.data.size();
	}

	public List<T> getSelected() {
		List<T> selected = new ArrayList<T>();
		for (int i = 0; i < this.data.size(); i++) {
			if (this.selected.get(i)) {
				selected.add(this.data.get(i));
			}
		}
		return selected;
	}

	public int getSelectedCount() {
		return this.selectedCount;
	}

	public void resetSelected() {
		Log.d(TAG, "reset selected");
		this.selected = new ArrayList<Boolean>();
		for (int i = 0; i < this.data.size(); i++) {
			this.selected.add(false);
		}
		notifyDataSetChanged();
		this.selectedCount = 0;
	}

	public void setSelected(int position, boolean selected) {
		this.selected.set(position, selected);
		this.selectedCount += selected ? 1 : -1;
		notifyDataSetChanged();
	}

}
