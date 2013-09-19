package de.azapps.mirakel.helper;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import de.azapps.mirakel.Mirakel.NoSuchListException;
import de.azapps.mirakel.helper.Helpers.ExecInterface;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakelandroid.R;

public class TaskDialogHelpers {
	protected static final String TAG = "TaskDialogHelpers";
	/**
	 * Ugly helper variable
	 */
	private static View numberPicker;

	@SuppressLint("NewApi")
	public static void handlePriority(final Context ctx, final Task task,
			final Helpers.ExecInterface onSuccess) {
		final String[] t = { "-2", "-1", "0", "1", "2" };
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			numberPicker = new NumberPicker(ctx);
			NumberPicker np=(NumberPicker) numberPicker;
			np.setMaxValue(4);
			np.setMinValue(0);
			np.setDisplayedValues(t);
			np.setWrapSelectorWheel(false);
			np.setValue(task.getPriority() + 2);
			np.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
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
								safeSafeTask(ctx, task);
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

	private static void safeSafeTask(Context context, Task task) {
		try {
			task.save();
		} catch (NoSuchListException e) {
			Toast.makeText(context, R.string.list_vanished, Toast.LENGTH_LONG)
					.show();
		}
	}

	public static void handleReminder(final Activity act, final Task task,
			final ExecInterface onSuccess) {
		final Context ctx = (Context) act;
		Calendar reminder = (task.getReminder() == null ? new GregorianCalendar()
				: task.getReminder());
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
		new AlertDialog.Builder(ctx)
				.setTitle(R.string.task_set_reminder)
				.setView(mDateTimeDialogView)
				.setPositiveButton(R.string.OK,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								mDateTimePicker.clearFocus();

								task.setReminder(new GregorianCalendar(
										mDateTimePicker.get(Calendar.YEAR),
										mDateTimePicker.get(Calendar.MONTH),
										mDateTimePicker
												.get(Calendar.DAY_OF_MONTH),
										mDateTimePicker
												.get(Calendar.HOUR_OF_DAY),
										mDateTimePicker.get(Calendar.MINUTE)));
								safeSafeTask(ctx, task);
								onSuccess.exec();

							}
						})
				.setNegativeButton(R.string.no_date,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								mDateTimePicker.clearFocus();
								task.setReminder(null);
								safeSafeTask(ctx, task);
								onSuccess.exec();

							}
						}).show();
	}

}
