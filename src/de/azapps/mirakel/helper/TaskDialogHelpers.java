package de.azapps.mirakel.helper;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import de.azapps.mirakel.MirakelHelper;
import de.azapps.mirakel.R;
import de.azapps.mirakel.helper.Helpers.ExecInterface;
import de.azapps.mirakel.model.task.Task;

public class TaskDialogHelpers {
	/**
	 * Ugly helper variable
	 */
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

	public static void handleReminder(final Activity act, final Task task,
			final ExecInterface onSuccess) {
		GregorianCalendar reminder = (task.getReminder() == null ? new GregorianCalendar()
				: task.getReminder());

		// Create the dialog
		final Dialog mDateTimeDialog = new Dialog(act);
		// Inflate the root layout
		final RelativeLayout mDateTimeDialogView = (RelativeLayout) act
				.getLayoutInflater().inflate(R.layout.date_time_dialog, null);
		// Grab widget instance
		final DateTimePicker mDateTimePicker = (DateTimePicker) mDateTimeDialogView
				.findViewById(R.id.DateTimePicker);
		mDateTimePicker.setIs24HourView(true);
		mDateTimePicker.updateDate(reminder.get(Calendar.YEAR),
				reminder.get(Calendar.MONTH),
				reminder.get(Calendar.DAY_OF_MONTH));
		mDateTimePicker.updateTime(reminder.get(Calendar.HOUR_OF_DAY),
				reminder.get(Calendar.MINUTE));

		// Update when the "OK" button is clicked
		((Button) mDateTimeDialogView.findViewById(R.id.SetDateTime))
				.setOnClickListener(new OnClickListener() {

					public void onClick(View v) {
						mDateTimePicker.clearFocus();

						task.setReminder(new GregorianCalendar(mDateTimePicker
								.get(Calendar.YEAR), mDateTimePicker
								.get(Calendar.MONTH), mDateTimePicker
								.get(Calendar.DAY_OF_MONTH), mDateTimePicker
								.get(Calendar.HOUR_OF_DAY), mDateTimePicker
								.get(Calendar.MINUTE)));
						task.save();
						onSuccess.exec();
						mDateTimeDialog.dismiss();
					}
				});

		// Cancel the dialog when the "Cancel" button is clicked
		((Button) mDateTimeDialogView.findViewById(R.id.CancelDialog))
				.setOnClickListener(new OnClickListener() {

					public void onClick(View v) {
						mDateTimePicker.clearFocus();
						task.setReminder(null);
						task.save();
						onSuccess.exec();
						mDateTimeDialog.dismiss();
					}
				});

		// Setup TimePicker
		// No title on the dialog window
		mDateTimeDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		// Set the dialog content view
		mDateTimeDialog.setContentView(mDateTimeDialogView);
		// Display the dialog
		mDateTimeDialog.show();

		/*
		 * 
		 * DatePickerDialog dialog = new DatePickerDialog( act, new
		 * OnDateSetListener() {
		 * 
		 * @Override public void onDateSet(DatePicker view, int year, int
		 * monthOfYear, int dayOfMonth) { if (mIgnoreTimeSet) return;
		 * 
		 * task.setReminder(new GregorianCalendar(year, monthOfYear,
		 * dayOfMonth)); task.save(); onSuccess.exec();
		 * 
		 * } }, reminder.get(Calendar.YEAR), reminder.get(Calendar.MONTH),
		 * reminder.get(Calendar.DAY_OF_MONTH));
		 * dialog.setButton(DialogInterface.BUTTON_NEGATIVE,
		 * act.getString(R.string.no_date), new
		 * DialogInterface.OnClickListener() { public void
		 * onClick(DialogInterface dialog, int which) { if (which ==
		 * DialogInterface.BUTTON_NEGATIVE) { mIgnoreTimeSet = true;
		 * task.setReminder(null); task.save(); onSuccess.exec(); } } });
		 * dialog.show();
		 */
	}

}
