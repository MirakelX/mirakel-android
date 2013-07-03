package de.azapps.mirakel.helper;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import de.azapps.mirakel.helper.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.TextView;
import de.azapps.mirakel.R;
import de.azapps.mirakel.helper.Helpers.ExecInterface;
import de.azapps.mirakel.model.task.Task;

public class TaskDialogHelpers {
	protected static final String TAG = "TaskDialogHelpers";
	/**
	 * Ugly helper variable
	 */
	private static View numberPicker;

	@SuppressLint("NewApi")
	public static void handlePriority(Context ctx, final Task task,
			final Helpers.ExecInterface onSuccess) {
		final String[] t = { "-2", "-1", "0", "1", "2" };
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			numberPicker = new NumberPicker(ctx);
			((NumberPicker) numberPicker).setMaxValue(4);
			((NumberPicker) numberPicker).setMinValue(0);
			((NumberPicker) numberPicker).setDisplayedValues(t);
			((NumberPicker) numberPicker).setWrapSelectorWheel(false);
			((NumberPicker) numberPicker).setValue(task.getPriority() + 2);
		} else {
			numberPicker = ((LayoutInflater) ctx
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
					.inflate(R.layout.dialog_num_picker_v10, null);
			((Button) numberPicker.findViewById(R.id.dialog_num_pick_plus))
					.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							String val = ((TextView) numberPicker
									.findViewById(R.id.dialog_num_pick_val))
									.getText().toString();
							int i = 0;
							while (!(t[i].contains(val) && t[i].length() == val
									.length())) {
								if (++i >= t.length) {
									Log.wtf(TAG,
											"unknown Value in NumericPicker");
									return;
								}
							}
							((TextView) numberPicker
									.findViewById(R.id.dialog_num_pick_val))
									.setText(t[i + 1 == t.length ? i : i + 1]);
						}
					});
			((Button) numberPicker.findViewById(R.id.dialog_num_pick_minus))
					.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							String val = ((TextView) numberPicker
									.findViewById(R.id.dialog_num_pick_val))
									.getText().toString();
							int i = 0;
							while (!(t[i].contains(val) && t[i].length() == val
									.length())) {
								if (++i >= t.length) {
									Log.wtf(TAG,
											"unknown Value in NumericPicker");
									return;
								}
							}
							((TextView) numberPicker
									.findViewById(R.id.dialog_num_pick_val))
									.setText(t[i - 1 >= 0 ? i - 1 : i]);
						}
					});
		}
		new AlertDialog.Builder(ctx)
				.setTitle(ctx.getString(R.string.task_change_prio_title))
				.setMessage(ctx.getString(R.string.task_change_prio_cont))
				.setView(numberPicker)
				.setPositiveButton(ctx.getString(R.string.OK),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
									task.setPriority((((NumberPicker) numberPicker)
											.getValue() - 2));
								} else {
									task.setPriority(Integer.parseInt(((TextView) numberPicker
											.findViewById(R.id.dialog_num_pick_val))
											.getText().toString()));
								}
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
