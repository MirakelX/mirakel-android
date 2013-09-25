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
package de.azapps.mirakel.main_activity;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.preference.PreferenceManager;
import android.widget.ArrayAdapter;

public class MirakelArrayAdapter<T> extends ArrayAdapter<T> {
	protected List<T> data;
	protected int layoutResourceId;
	protected Context context;
	protected List<Boolean> selected;
	protected int selecdetCount;
	protected boolean darkTheme;
	

	public MirakelArrayAdapter(Context context, int textViewResourceId,
			List<T> data) {
		super(context, textViewResourceId,textViewResourceId, data);
		this.layoutResourceId=textViewResourceId;
		this.data=data;
		this.context=context;
		this.darkTheme=PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean("DarkTheme", false);
		this.selected = new ArrayList<Boolean>();
		for (int i = 0; i < data.size(); i++) {
			this.selected.add(false);
		}
		this.selecdetCount = 0;
		
	}
	
	public void setSelected(int position, boolean selected) {
		this.selected.set(position, selected);
		notifyDataSetChanged();
		selecdetCount += (selected ? 1 : -1);
	}

	public int getSelectedCount() {
		return selecdetCount;
	}

	public void resetSelected() {
		for (int i = 0; i < selected.size(); i++) {
			selected.set(i, false);
		}
		notifyDataSetChanged();
		selecdetCount = 0;
	}
	
	public void changeData(List<T> newData) {
		data.clear();
		data.addAll(newData);
		while (data.size() > selected.size()) {
			selected.add(false);
		}
	}
	
	public List<T> getSelected() {
		List<T> selected=new ArrayList<T>();
		for(int i=0;i<data.size();i++){
			if(this.selected.get(i))
			{
				selected.add(data.get(i));
			}
		}
		return selected;
	}
	

}
