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
import android.widget.ImageView;
import android.widget.TextView;
import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.R;
import de.azapps.mirakel.model.list.ListMirakel;

public class ListAdapter extends ArrayAdapter<ListMirakel> {
	@SuppressWarnings("unused")
	private static final String TAG = "ListAdapter";
	private boolean enableDrop;
	private Context context;
	private int layoutResourceId;
	private List<ListMirakel> data = null;

	public ListAdapter(Context context, int layoutResourceId,
			List<ListMirakel> data, boolean enable) {
		super(context, layoutResourceId, layoutResourceId, data);
		this.layoutResourceId = layoutResourceId;
		this.data = data;
		this.context = context;
		this.enableDrop = enable;
	}

	void changeData(List<ListMirakel> lists) {
		data.clear();
		data.addAll(lists);
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
			holder.listRowDrag = (ImageView) row
					.findViewById(R.id.list_row_drag);

			row.setTag(holder);
		} else {
			holder = (ListHolder) row.getTag();
		}
		ListMirakel list = data.get(position);
		if (!enableDrop || list.getId() < 0)
			holder.listRowDrag.setVisibility(View.GONE);
		else
			holder.listRowDrag.setVisibility(View.VISIBLE);
		holder.listRowName.setText(list.getName());
		holder.listRowName.setTag(list);
		holder.listRowTaskNumber.setText("" + list.countTasks());
		return row;
	}

	public void onRemove(int which) {
		if (which < 0 || which > data.size())
			return;
		data.remove(which);
	}

	public void onDrop(final int from, final int to) {
		ListMirakel t = data.get(from);
		if (to < from) {// move list up
			Mirakel.getWritableDatabase().execSQL(
					"UPDATE " + ListMirakel.TABLE
							+ " SET lft=lft+2 where lft>="
							+ data.get(to).getLft() + " and lft<"
							+ data.get(from).getLft());
		} else if (to > from) {// move list down
			Mirakel.getWritableDatabase().execSQL(
					"UPDATE " + ListMirakel.TABLE + " SET lft=lft-2 where lft>"
							+ data.get(from).getLft() + " and lft<="
							+ data.get(to).getLft());
		} else {// Nothing
			return;
		}
		t.setLft(data.get(to).getLft());
		t.save();
		Mirakel.getWritableDatabase().execSQL(
				"UPDATE " + ListMirakel.TABLE + " SET rgt=lft+1;");// Fix rgt
		data.remove(from);
		data.add(to, t);
		notifyDataSetChanged();
		Thread load = new Thread(new Runnable() {
			@Override
			public void run() {
				data = ListMirakel.all();
			}
		});
		load.start();
	}

	public void setEnableDrop(boolean enableDrop) {
		this.enableDrop = enableDrop;
	}

	public boolean isDropEnabled() {
		return enableDrop;
	}

	static class ListHolder {
		TextView listRowName;
		TextView listRowTaskNumber;
		ImageView listRowDrag;
	}
}
