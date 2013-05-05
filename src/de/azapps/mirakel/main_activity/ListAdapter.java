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

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import de.azapps.mirakel.R;
import de.azapps.mirakel.model.list.ListMirakel;

public class ListAdapter extends ArrayAdapter<ListMirakel> {
	Context context;
	int layoutResourceId;
	List<ListMirakel> data = null;

	public ListAdapter(Context context, int layoutResourceId,
			List<ListMirakel> data) {
		super(context, layoutResourceId, layoutResourceId, data);
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
		ListMirakel list = data.get(position);
		holder.listRowName.setText(list.getName());
		holder.listRowName.setTag(list);
		holder.listRowTaskNumber.setText(""+list.countTasks());
		return row;
	}
	
	public void onRemove(int which) {
		if (which < 0 || which >data.size()) return;		
		data.remove(which);
	}

	public void onDrop(int from, int to) {
		ListMirakel t = data.get(from);
		t.setLft(data.get(to).getLft());
		t.setRgt(data.get(to).getRgt());
		t.save();
		int add=to<from?2:-2;
		for(int i=(to<from?to:from+1);i<(from>to?from:to+1);i++){
			data.get(i).setLft(data.get(i).getLft()+add);
			data.get(i).setRgt(data.get(i).getRgt()+add);
			data.get(i).save();
		}
		data.remove(from);
		data.add(to,t);
	}
	
	static class ListHolder {
		TextView listRowName;
		TextView listRowTaskNumber;
	}
}
