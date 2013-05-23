package de.azapps.mirakel.helper;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.NumberPicker;
import de.azapps.mirakel.R;
import de.azapps.mirakel.model.task.Task;

public class TaskDialogHelpers {
	/**
	 * Ugly helper variable
	 */
	private static AlertDialog alert;
	private static NumberPicker numberPicker;
	
	public static void handlePriority(Context ctx, final Task task,final Helpers.ExecInterface onSuccess){

		numberPicker = new NumberPicker(ctx);
		numberPicker.setMaxValue(4);
		numberPicker.setMinValue(0);
		String[] t = { "-2", "-1", "0", "1", "2" };
		numberPicker.setDisplayedValues(t);
		numberPicker.setWrapSelectorWheel(false);
		numberPicker.setValue(task.getPriority() + 2);
		new AlertDialog.Builder(ctx)
				.setTitle(
						ctx.getString(R.string.task_change_prio_title))
				.setMessage(
						ctx.getString(R.string.task_change_prio_cont))
				.setView(numberPicker)
				.setPositiveButton(ctx.getString(R.string.OK),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								task.setPriority((numberPicker.getValue() - 2));
								task.save();
								onSuccess.exec();
							}

						})
				.setNegativeButton(ctx.getString(R.string.Cancel),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								// Do nothing.
							}
						}).show();
	}

}
