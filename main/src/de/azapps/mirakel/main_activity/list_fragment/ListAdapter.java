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
package de.azapps.mirakel.main_activity.list_fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import de.azapps.mirakel.adapter.MirakelArrayAdapter;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.ViewHelper;
import de.azapps.mirakel.model.MirakelContentProvider;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakelandroid.R;

@SuppressLint("UseSparseArrays")
public class ListAdapter extends MirakelArrayAdapter<ListMirakel> {
	static class ListHolder {
		TextView listAccount;
		ImageView listRowDrag;
		TextView listRowName;
		TextView listRowTaskNumber;
	}

	private boolean enableDrop;

	private final Map<Integer, View> viewsForLists = new HashMap<Integer, View>();

	public ListAdapter(final Context c) {
		// do not call this, only for error-fixing there
		super(c, 0, new ArrayList<ListMirakel>());
	}

	public ListAdapter(final Context context, final int layoutResourceId,
			final List<ListMirakel> data, final boolean enable) {
		super(context, layoutResourceId, data);
		this.enableDrop = enable;
	}

	@Override
	public void changeData(final List<ListMirakel> lists) {
		this.viewsForLists.clear();
		super.changeData(lists);
	}

	@Override
	public View getView(final int position, final View convertView,
			final ViewGroup parent) {
		View row = convertView;
		ListHolder holder = null;

		if (row == null) {
			final LayoutInflater inflater = ((Activity) this.context)
					.getLayoutInflater();
			row = inflater.inflate(this.layoutResourceId, parent, false);
			holder = new ListHolder();
			holder.listRowName = (TextView) row
					.findViewById(R.id.list_row_name);
			holder.listRowTaskNumber = (TextView) row
					.findViewById(R.id.list_row_task_number);
			holder.listRowDrag = (ImageView) row
					.findViewById(R.id.list_row_drag);
			holder.listAccount = (TextView) row
					.findViewById(R.id.list_row_account_name);
			row.setTag(holder);
		} else {
			holder = (ListHolder) row.getTag();
		}
		final ListMirakel list = this.getDataAt(position);
		if (!this.enableDrop) {
			holder.listRowDrag.setVisibility(View.GONE);
		} else {
			holder.listRowDrag.setVisibility(View.VISIBLE);
		}

		holder.listRowName.setText(list.getName());
		holder.listRowName.setTag(list);
		holder.listRowTaskNumber.setText("" + list.countTasks());
		if (list.isSpecialList()
				|| !MirakelCommonPreferences.isShowAccountName()) {
			holder.listAccount.setVisibility(View.GONE);
		} else {
			holder.listAccount.setVisibility(View.VISIBLE);
			AccountMirakel a = list.getAccount();
			if (a == null) {
				a = AccountMirakel.getLocal();
				list.setAccount(a);
				list.save(false);
			}
			holder.listAccount.setText(a.getName());
			holder.listAccount.setTextColor(this.context.getResources()
					.getColor(android.R.color.darker_gray));
		}
		this.viewsForLists.put(list.getId(), row);
		final int w = row.getWidth() == 0 ? parent.getWidth() : row.getWidth();
		ViewHelper.setListColorBackground(list, row, w);
		if (this.isSelectedAt(position)) {
			row.setBackgroundColor(this.context.getResources().getColor(
					this.darkTheme ? R.color.highlighted_text_holo_dark
							: R.color.highlighted_text_holo_light));
		}

		return row;
	}

	public View getViewForList(final ListMirakel list) {
		return this.viewsForLists.get(list.getId());
	}

	public boolean isDropEnabled() {
		return this.enableDrop;
	}

	public void onDrop(final int from, final int to) {
		final ListMirakel t = this.getDataAt(from);
		String TABLE;
		if (t.getId() < 0) {
			TABLE = SpecialList.TABLE;
		} else {
			TABLE = ListMirakel.TABLE;
		}
		if (to < from) {// move list up
			MirakelContentProvider.getWritableDatabase().execSQL(
					"UPDATE " + TABLE + " SET lft=lft+2 where lft>="
							+ this.getDataAt(to).getLft() + " and lft<"
							+ this.getDataAt(from).getLft());
		} else if (to > from) {// move list down
			MirakelContentProvider.getWritableDatabase().execSQL(
					"UPDATE " + TABLE + " SET lft=lft-2 where lft>"
							+ this.getDataAt(from).getLft() + " and lft<="
							+ this.getDataAt(to).getLft());
		} else {
			return;
		}
		t.setLft(this.getDataAt(to).getLft());
		t.save();
		MirakelContentProvider.getWritableDatabase().execSQL(
				"UPDATE " + TABLE + " SET rgt=lft+1;");// Fix rgt
		this.remove(from);
		this.addToData(to, t);
		notifyDataSetChanged();
		final Thread load = new Thread(new Runnable() {
			@Override
			public void run() {
				changeData(ListMirakel.all());
			}
		});
		load.start();
	}

	public void onRemove(final int which) {
		if (which < 0 || which > this.getCount()) {
			return;
		}
		this.viewsForLists.remove(this.getDataAt(which).getId());
		this.remove(which);
	}

	public void setEnableDrop(final boolean enableDrop) {
		this.enableDrop = enableDrop;
	}

}
