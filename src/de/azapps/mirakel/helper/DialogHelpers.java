package de.azapps.mirakel.helper;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import de.azapps.mirakel.R;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;

public class DialogHelpers {
	/**
	 * Ugly helper variable
	 */
	private static AlertDialog alert;

	/**
	 * Handle the SortBy dialog
	 * @param ctx
	 * @param list
	 * @return
	 */
	public static ListMirakel handleSortBy(Context ctx, final ListMirakel list) {
		return handleSortBy(ctx, list, new ExecInterface() {
			@Override
			public void exec() {
			}
		});
	}

	/**
	 * Handle the SortBy dialog
	 * @param ctx
	 * @param list
	 * @param cls
	 * @return
	 */
	public static ListMirakel handleSortBy(Context ctx, final ListMirakel list,
			final ExecInterface cls) {
		final CharSequence[] SortingItems = ctx.getResources().getStringArray(
				R.array.task_sorting_items);

		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setTitle(ctx.getString(R.string.task_sorting_title));

		builder.setSingleChoiceItems(SortingItems, list.getSortBy(),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						switch (item) {
						case 0:
							list.setSortBy(ListMirakel.SORT_BY_OPT);
							break;
						case 1:
							list.setSortBy(ListMirakel.SORT_BY_DUE);
							break;
						case 2:
							list.setSortBy(ListMirakel.SORT_BY_PRIO);
							break;
						default:
							list.setSortBy(ListMirakel.SORT_BY_ID);
							break;
						}
						list.save();
						cls.exec();
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
	public static SpecialList handleDefaultList(Context ctx, final SpecialList specialList, List<ListMirakel> lists) {
		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setTitle(R.string.special_list_def_list);
		List<CharSequence> items = new ArrayList<CharSequence>();
		final List<Integer> list_ids = new ArrayList<Integer>();
		int currentItem = 0, i = 1;
		items.add(ctx.getString(R.string.special_list_first));
		list_ids.add(null);
		for (ListMirakel list : lists) {
			if (list.getId() > 0) {
				items.add(list.getName());
				if (specialList.getDefaultList() == null) {
					currentItem = 0;
				} else {
					if (specialList.getDefaultList().getId() == list.getId())
						currentItem = i;
				}
				list_ids.add(list.getId());
				++i;
			}
		}

		builder.setSingleChoiceItems(
				items.toArray(new CharSequence[items.size()]), currentItem,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						Integer lid = list_ids.get(item);
						if (lid == null) {
							specialList.setDefaultList(null);
						} else {
							specialList.setDefaultList(ListMirakel.getList(lid));
						}
						specialList.save();
						alert.dismiss();
					}
				});

		alert = builder.create();
		alert.show();
		return specialList;
	}
	/**
	 * Wrapper-Class
	 * @author az
	 *
	 */
	public interface ExecInterface {
		public void exec();
	}
}
