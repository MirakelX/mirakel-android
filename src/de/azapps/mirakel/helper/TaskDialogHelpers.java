package de.azapps.mirakel.helper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.widget.DatePicker;
import android.widget.NumberPicker;
import de.azapps.mirakel.R;
import de.azapps.mirakel.helper.Helpers.ExecInterface;
import de.azapps.mirakel.model.task.Task;

public class TaskDialogHelpers {
	/**
	 * Ugly helper variable
	 */
	private static AlertDialog alert;
	private static NumberPicker numberPicker;

	public static void handlePriority(Context ctx, final Task task,
			final Helpers.ExecInterface onSuccess) {

		numberPicker = new NumberPicker(ctx);
		numberPicker.setMaxValue(4);
		numberPicker.setMinValue(0);
		String[] t = { "-2", "-1", "0", "1", "2" };
		numberPicker.setDisplayedValues(t);
		numberPicker.setWrapSelectorWheel(false);
		numberPicker.setValue(task.getPriority() + 2);
		new AlertDialog.Builder(ctx)
				.setTitle(ctx.getString(R.string.task_change_prio_title))
				.setMessage(ctx.getString(R.string.task_change_prio_cont))
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

	private static boolean mIgnoreTimeSet = false;

	public static void handleReminder(Context ctx, final Task task,final ExecInterface onSuccess) {
		mIgnoreTimeSet = false;
		GregorianCalendar reminder = (task.getReminder() == null ? new GregorianCalendar()
				: task.getReminder());
		DatePickerDialog dialog = new DatePickerDialog(
				ctx,
				new OnDateSetListener() {

					@Override
					public void onDateSet(DatePicker view, int year,
							int monthOfYear, int dayOfMonth) {
						if (mIgnoreTimeSet)
							return;

						task.setReminder(new GregorianCalendar(year,
								monthOfYear, dayOfMonth));
						task.save();
						onSuccess.exec();

					}
				}, reminder.get(Calendar.YEAR), reminder.get(Calendar.MONTH),
				reminder.get(Calendar.DAY_OF_MONTH));
		dialog.setButton(DialogInterface.BUTTON_NEGATIVE,
				ctx.getString(R.string.no_date),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						if (which == DialogInterface.BUTTON_NEGATIVE) {
							mIgnoreTimeSet = true;
							task.setReminder(null);
							task.save();
							onSuccess.exec();
						}
					}
				});
		dialog.show();

	}

}
