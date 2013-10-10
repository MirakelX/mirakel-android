package de.azapps.mirakel.settings;

import de.azapps.mirakel.helper.Log;
import de.azapps.mirakelandroid.R;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Build;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.NumberPicker.OnValueChangeListener;
import android.widget.TextView;

public class NumPickerPref extends DialogPreference {
	private static final String TAG = "NumPickerPref";
	private Context ctx;
	private int MAX_VAL;
	private int MIN_VAL;
	private int SUMMARY_ID;
	private int VALUE;

	public NumPickerPref(Context context, AttributeSet attrs) {
		super(context, attrs);
		ctx = context;
		for (int i = 0; i < attrs.getAttributeCount(); i++) {
			String attr = attrs.getAttributeName(i);
			if (attr.equals("minimumValue")) {
				MIN_VAL = attrs.getAttributeIntValue(i, 0);
			} else if (attr.equals("maximumValue")) {
				MAX_VAL = attrs.getAttributeIntValue(i, 0);
			} else if (attr.equals("summaryString")) {
				SUMMARY_ID = attrs.getAttributeResourceValue(i, 0);
			}
		}
	}

	@SuppressLint("NewApi")
	@Override
	protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
		// super.onPrepareDialogBuilder(builder);
		final View dialog;
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			dialog = ((Activity) ctx).getLayoutInflater().inflate(
					R.layout.num_picker_pref_v10, null);
			final Button plus = (Button) dialog
					.findViewById(R.id.dialog_num_pick_plus);
			final Button minus = (Button) dialog
					.findViewById(R.id.dialog_num_pick_minus);
			final TextView tx = (TextView) dialog
					.findViewById(R.id.dialog_num_pick_val);
			plus.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (VALUE < MAX_VAL) {
						++VALUE;
						updateV10Value(tx);
						updateSummary(dialog);
					}
				}
			});
			minus.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (VALUE > MIN_VAL) {
						--VALUE;
						updateV10Value(tx);
						updateSummary(dialog);
					}
				}
			});
		} else {
			dialog = ((Activity) ctx).getLayoutInflater().inflate(
					R.layout.num_picker_pref, null);
			NumberPicker picker = (NumberPicker) dialog
					.findViewById(R.id.numberPicker);
			picker.setMaxValue(MAX_VAL);
			picker.setMinValue(MIN_VAL);
			picker.setWrapSelectorWheel(false);
			picker.setOnValueChangedListener(new OnValueChangeListener() {
				@Override
				public void onValueChange(NumberPicker picker, int oldVal,
						int newVal) {
					VALUE = newVal;
					updateSummary(dialog);
				}
			});
		}
		updateSummary(dialog);
		builder.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						callChangeListener(VALUE);

					}
				});
		builder.setNegativeButton(android.R.string.cancel,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// Nothing
					}
				});
		builder.setView(dialog);
	}

	protected void updateV10Value(TextView tx) {
		tx.setText("" + VALUE);

	}

	protected void updateSummary(View dialog) {
		if (SUMMARY_ID != 0) {
			((TextView) dialog.findViewById(R.id.num_picker_pref_summary))
					.setText(ctx.getResources().getQuantityString(SUMMARY_ID,
							VALUE));
		}

	}

	public int getValue() {
		return VALUE;
	}

	public void setValue(int newValue) {
		VALUE = newValue;
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		Log.d(TAG, a.getString(index));
		return super.onGetDefaultValue(a, index);
	}

	@Override
	public void onBindDialogView(View view) {
		Log.d(TAG, "bar");
		super.onBindDialogView(view);
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		if (positiveResult) {
			persistBoolean(!getPersistedBoolean(true));
		}
	}

}
