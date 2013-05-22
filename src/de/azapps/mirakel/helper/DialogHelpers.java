package de.azapps.mirakel.helper;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import de.azapps.mirakel.R;
import de.azapps.mirakel.model.list.ListMirakel;

public class DialogHelpers {
	private static AlertDialog alert;

	public static ListMirakel handleSortBy(Context ctx, final ListMirakel list) {
		return handleSortBy(ctx, list, new onSuccess() {
			@Override
			public void exec() {
			}
		});
	}

	public static ListMirakel handleSortBy(Context ctx, final ListMirakel list,
			final onSuccess cls) {
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

	public interface onSuccess {
		public void exec();
	}
}
