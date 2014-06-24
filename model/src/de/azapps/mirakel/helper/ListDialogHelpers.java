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
package de.azapps.mirakel.helper;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.Preference;
import android.widget.TextView;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;

public class ListDialogHelpers {
	protected static final String TAG = "ListDialogHelpers";
	/**
	 * Ugly helper variable
	 */
	private static AlertDialog alert;

	/**
	 * Handle the SortBy dialog
	 * 
	 * @param ctx
	 * @param list
	 * @return
	 */
	public static ListMirakel handleSortBy(final Context ctx,
			final ListMirakel list, final TextView res) {
		return handleSortBy(ctx, list, null, null);
	}

	public static ListMirakel handleSortBy(final Context ctx,
			final ListMirakel list, final Preference res) {
		return handleSortBy(ctx, list, null, res);
	}

	public static ListMirakel handleSortBy(final Context ctx,
			final ListMirakel list) {
		return handleSortBy(ctx, list, null, null);
	}

	/**
	 * Handle the SortBy dialog
	 * 
	 * @param ctx
	 * @param list
	 * @param cls
	 * @return
	 */
	public static ListMirakel handleSortBy(final Context ctx,
			final ListMirakel list, final Helpers.ExecInterface cls,
			final Preference res) {
		final CharSequence[] SortingItems = ctx.getResources().getStringArray(
				R.array.task_sorting_items);

		final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setTitle(ctx.getString(R.string.task_sorting_title));
		builder.setSingleChoiceItems(SortingItems, list.getSortBy(),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int item) {
						list.setSortBy(item);
						list.save();
						if (res != null) {
							res.setSummary(SortingItems[item]);
						}
						if (cls != null) {
							cls.exec();
						}
						alert.dismiss(); // Ugly
					}
				});
		alert = builder.create();
		alert.show();
		return list;
	}

	/**
	 * Handle the actions after clicking on a move task button
	 * 
	 * @param task
	 */
	public static SpecialList handleDefaultList(final Context ctx,
			final SpecialList specialList, final List<ListMirakel> lists,
			final Preference res) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setTitle(R.string.special_list_def_list);
		final List<CharSequence> items = new ArrayList<CharSequence>();
		final List<Integer> list_ids = new ArrayList<Integer>();
		int currentItem = 0, i = 1;
		items.add(ctx.getString(R.string.special_list_first));
		list_ids.add(null);
		for (final ListMirakel list : lists) {
			if (list.getId() > 0) {
				items.add(list.getName());
				if (specialList.getDefaultList() == null) {
					currentItem = 0;
				} else {
					if (specialList.getDefaultList().getId() == list.getId()) {
						currentItem = i;
					}
				}
				list_ids.add(list.getId());
				++i;
			}
		}

		builder.setSingleChoiceItems(
				items.toArray(new CharSequence[items.size()]), currentItem,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int item) {
						final Integer lid = list_ids.get(item);
						if (lid == null) {
							specialList.setDefaultList(null);
						} else {
							specialList.setDefaultList(ListMirakel.get(lid));
						}
						specialList.save();
						alert.dismiss();
						if (res != null) {
							res.setSummary(specialList.getDefaultList()
									.getName());
						}
					}
				});

		alert = builder.create();
		alert.show();
		return specialList;
	}

	/**
	 * Handle the Default Date Dialog for a SpecialList
	 * 
	 * @param ctx
	 * @param specialList
	 * @return
	 */
	public static SpecialList handleDefaultDate(final Context ctx,
			final SpecialList specialList, final Preference res) {

		final String[] items = ctx.getResources().getStringArray(
				R.array.special_list_def_date_picker);
		final int[] values = ctx.getResources().getIntArray(
				R.array.special_list_def_date_picker_val);
		int currentItem = 0;
		if (specialList.getDefaultDate() != null) {
			final int ddate = specialList.getDefaultDate();
			for (int i = 0; i < values.length; i++) {
				if (values[i] == ddate) {
					currentItem = i;
				}
			}
		}

		final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setTitle(R.string.special_list_def_date);
		builder.setSingleChoiceItems(items, currentItem,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int item) {
						Integer date = values[item];
						if (date == -1337) {
							date = null;
						}
						specialList.setDefaultDate(date);
						specialList.save();
						alert.dismiss();
						if (res != null) {
							res.setSummary(items[item]);
						}
					}
				});

		alert = builder.create();
		alert.show();
		return specialList;
	}

}
