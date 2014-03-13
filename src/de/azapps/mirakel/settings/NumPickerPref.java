package de.azapps.mirakel.settings;

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
import de.azapps.tools.Log;

public class NumPickerPref extends DialogPreference {
	private static final String TAG = "NumPickerPref";
	private Context ctx;
	protected int MAX_VAL;
	protected int MIN_VAL;
	private int SUMMARY_ID;
	protected int VALUE;
	private NumberPicker picker;
	private TextView tx;
	private View dialog;

	public NumPickerPref(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.ctx = context;
		for (int i = 0; i < attrs.getAttributeCount(); i++) {
			String attr = attrs.getAttributeName(i);
			if (attr.equals("minimumValue")) {
				this.MIN_VAL = attrs.getAttributeIntValue(i, 0);
			} else if (attr.equals("maximumValue")) {
				this.MAX_VAL = attrs.getAttributeIntValue(i, 0);
			} else if (attr.equals("summaryString")) {
				this.SUMMARY_ID = attrs.getAttributeResourceValue(i, 0);
			}
		}
	}

	@SuppressLint("NewApi")
	@Override
	protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
		// super.onPrepareDialogBuilder(builder);
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			this.dialog = ((Activity) this.ctx).getLayoutInflater().inflate(
					R.layout.num_picker_pref_v10, null);
			final Button plus = (Button) this.dialog
					.findViewById(R.id.dialog_num_pick_plus);
			final Button minus = (Button) this.dialog
					.findViewById(R.id.dialog_num_pick_minus);
			this.tx = (TextView) this.dialog
					.findViewById(R.id.dialog_num_pick_val);
			updateV10Value();
			plus.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (NumPickerPref.this.VALUE < NumPickerPref.this.MAX_VAL) {
						++NumPickerPref.this.VALUE;
						updateV10Value();
						updateSummary();
					}
				}
			});
			minus.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (NumPickerPref.this.VALUE > NumPickerPref.this.MIN_VAL) {
						--NumPickerPref.this.VALUE;
						updateV10Value();
						updateSummary();
					}
				}
			});
		} else {
			this.dialog = ((Activity) this.ctx).getLayoutInflater().inflate(
					R.layout.num_picker_pref, null);
			this.picker = (NumberPicker) this.dialog
					.findViewById(R.id.numberPicker);
			this.picker.setMaxValue(this.MAX_VAL);
			this.picker.setMinValue(this.MIN_VAL);
			this.picker.setValue(this.VALUE);
			this.picker.setWrapSelectorWheel(false);
			this.picker.setOnValueChangedListener(new OnValueChangeListener() {
				@Override
				public void onValueChange(NumberPicker picker, int oldVal,
						int newVal) {
					NumPickerPref.this.VALUE = newVal;
					updateSummary();
				}
			});
		}
		updateSummary();
		builder.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						callChangeListener(NumPickerPref.this.VALUE);

					}
				});
		builder.setNegativeButton(android.R.string.cancel,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// Nothing
					}
				});
		builder.setView(this.dialog);
	}

	protected void updateV10Value() {
		if (this.tx != null)
			this.tx.setText("" + this.VALUE);

	}

	protected void updateSummary() {
		if (this.SUMMARY_ID != 0 && this.dialog != null) {
			((TextView) this.dialog.findViewById(R.id.num_picker_pref_summary))
					.setText(this.ctx.getResources().getQuantityString(
							this.SUMMARY_ID, this.VALUE));
		}

	}

	public int getValue() {
		return this.VALUE;
	}

	public void setValue(int newValue) {
		if (newValue <= this.MAX_VAL && newValue >= this.MIN_VAL) {
			this.VALUE = newValue;
		}
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
