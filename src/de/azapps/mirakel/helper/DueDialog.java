package de.azapps.mirakel.helper;

import de.azapps.mirakelandroid.R;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;

public class DueDialog extends AlertDialog {
	private Context ctx;
	private VALUE dayYear=VALUE.DAY;
	private int count;
	private View dialogView;
	private String[] s;

	public enum VALUE {
		DAY, MONTH, YEAR;
	}
	public void setNegativeButton(int textId,OnClickListener onCancel){
		setButton(BUTTON_NEGATIVE, ctx.getString(textId), onCancel);
		
	}
	public void setPositiveButton(int textId,OnClickListener onCancel){
		setButton(BUTTON_POSITIVE, ctx.getString(textId), onCancel);
		
	}
	public void setNeutralButton(int textId,OnClickListener onCancel){
		setButton(BUTTON_NEUTRAL, ctx.getString(textId), onCancel);
		
	}

	@SuppressLint("NewApi")
	public DueDialog(Context context) {
		super(context);
		ctx = context;
		s = new String[100];
		for (int i = 0; i < s.length; i++) {
			s[i] = (i > 10 ? "+" : "") + (i - 10) + "";
		}
		
		dialogView = getNumericPicker();
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			final NumberPicker pickerDay = ((NumberPicker) dialogView
					.findViewById(R.id.due_day_year));
			NumberPicker pickerValue = ((NumberPicker) dialogView
					.findViewById(R.id.due_val));

			pickerDay.setDisplayedValues(getDayYearValues(0));
			pickerDay.setMaxValue(getDayYearValues(0).length - 1);
			pickerValue.setMaxValue(s.length - 1);
			pickerValue.setValue(10);
			pickerValue.setMinValue(0);
			pickerValue.setDisplayedValues(s);
			pickerValue
					.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
			pickerDay
					.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
			pickerValue.setWrapSelectorWheel(false);
			pickerValue.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {

						@Override
						public void onValueChange(NumberPicker picker,
								int oldVal, int newVal) {
							pickerDay.setDisplayedValues(getDayYearValues(newVal-10));
							count=newVal-10;
						}
					});
			pickerDay.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
				
				@Override
				public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
					switch (newVal){
					case 0:
						dayYear=VALUE.DAY;
						break;
					case 1:
						dayYear=VALUE.MONTH;
						break;
					case 2:
						dayYear=VALUE.YEAR;
						break;
					}
					
				}
			});

		} else {
			final TextView pickerValue = ((TextView) dialogView
					.findViewById(R.id.dialog_due_pick_val));
			pickerValue.setText(s[10]);
			count = 0;
			final TextView pickerDay = ((TextView) dialogView
					.findViewById(R.id.dialog_due_pick_val_day));
			pickerDay.setText(ctx.getResources().getQuantityString(
					R.plurals.due_day, 0));
			dayYear = VALUE.DAY;

			((Button) dialogView.findViewById(R.id.dialog_due_pick_plus_val))
					.setOnClickListener(new View.OnClickListener() {

						@Override
						public void onClick(View v) {
							int val = Integer.parseInt(pickerValue.getText()
									.toString().replace("+", "")) + 10;
							if (val + 1 < s.length) {
								pickerValue.setText(s[val + 1]);
								count = val - 10;
							}
							pickerDay.setText(updateDayYear());

						}
					});
			((Button) dialogView.findViewById(R.id.dialog_due_pick_minus_val))
					.setOnClickListener(new View.OnClickListener() {

						@Override
						public void onClick(View v) {
							int val = Integer.parseInt(pickerValue.getText()
									.toString().replace("+", "")) + 10;
							if (val - 1 > 0) {
								pickerValue.setText(s[val - 1]);
								count = val - 10;
							}
							pickerDay.setText(updateDayYear());
						}
					});
			((Button) dialogView.findViewById(R.id.dialog_due_pick_plus_day))
					.setOnClickListener(new View.OnClickListener() {

						@Override
						public void onClick(View v) {
							if (dayYear == VALUE.DAY) {
								dayYear = VALUE.MONTH;
							} else if (dayYear == VALUE.MONTH) {
								dayYear = VALUE.YEAR;
							}
							pickerDay.setText(updateDayYear());
						}
					});
			((Button) dialogView.findViewById(R.id.dialog_due_pick_minus_day))
					.setOnClickListener(new View.OnClickListener() {

						@Override
						public void onClick(View v) {
							if (dayYear == VALUE.MONTH) {
								dayYear = VALUE.DAY;
							} else if (dayYear == VALUE.YEAR) {
								dayYear = VALUE.MONTH;
							}
							pickerDay.setText(updateDayYear());
						}
					});
		}
		setView(dialogView);

	}

	protected String updateDayYear() {
		switch (dayYear) {
		case DAY:
			return ctx.getResources().getQuantityString(R.plurals.due_day,
					count);
		case MONTH:
			return ctx.getResources().getQuantityString(R.plurals.due_month,
					count);
		case YEAR:
			return ctx.getResources().getQuantityString(R.plurals.due_year,
					count);
		}
		return "";

	}

	protected String[] getDayYearValues(int newVal) {
		String[] ret = new String[3];
		ret[0] = ctx.getResources()
				.getQuantityString(R.plurals.due_day, newVal);
		ret[1] = ctx.getResources().getQuantityString(R.plurals.due_month,
				newVal);
		ret[2] = ctx.getResources().getQuantityString(R.plurals.due_year,
				newVal);
		return ret;
	}

	protected View getNumericPicker() {
		if (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB)
			return getLayoutInflater().inflate(R.layout.due_dialog, null);
		else
			return getLayoutInflater().inflate(R.layout.due_dialog_v10, null);
	}

	@SuppressLint("NewApi")
	public void setValue(int val, VALUE day) {	
		if(VERSION.SDK_INT>VERSION_CODES.HONEYCOMB){
			int _day=0;
			switch(day){
			case DAY:
				_day=0;
				break;
			case MONTH:
				_day=1;
				break;
			case YEAR:
				_day=2;
				break;		
			}
			((NumberPicker) dialogView
					.findViewById(R.id.due_day_year)).setValue(_day);;
			((NumberPicker) dialogView
					.findViewById(R.id.due_val)).setValue(val+10);;
		}else{
			count=val;
			dayYear=day;
			((TextView) dialogView
					.findViewById(R.id.dialog_due_pick_val)).setText(""+(val+10));
			((TextView) dialogView
					.findViewById(R.id.dialog_due_pick_val_day)).setText(updateDayYear());
		}
		
	}
	
	public int getValue(){
		return count;
	}
	
	public VALUE getDayYear(){
		return dayYear;
	}

}
