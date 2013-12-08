/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013 Anatolij Zelenin, Georg Semmler.
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package com.android.calendar.recurrencepicker;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import android.annotation.SuppressLint;
import android.support.v4.app.DialogFragment;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.fourmob.datetimepicker.date.DatePicker;
import com.fourmob.datetimepicker.date.DatePicker.OnDateSetListener;
import com.fourmob.datetimepicker.date.DatePickerDialog;

import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakelandroid.R;

@SuppressLint("NewApi")
public class RecurrencePickerDialog extends DialogFragment implements
		OnCheckedChangeListener {

	// in dp's
	private static final int MIN_SCREEN_WIDTH_FOR_SINGLE_ROW_WEEK = 450;
	protected static final String TAG = null;

	public static RecurrencePickerDialog newInstance(OnRecurenceSetListner r,
			Recurring recurring, boolean forDue, boolean dark, boolean exact) {
		RecurrencePickerDialog re = new RecurrencePickerDialog();
		re.initialize(r, recurring, forDue, dark,exact);
		return re;
	}

	private OnRecurenceSetListner mCallback;
	private Recurring mRecurring;
	private boolean mForDue;
	private Spinner mRecurenceSelection;
	private int extraItems;
	private Switch mToggle;
	private Button mDoneButton;
	private int mPosition;
	private boolean mDark;
	private ToggleButton[] mWeekByDayButtons = new ToggleButton[7];
	private int numOfButtonsInRow1;
	private int numOfButtonsInRow2;

	public void initialize(OnRecurenceSetListner r, Recurring recurring,
			boolean forDue, boolean dark,boolean exact) {
		mRecurring = recurring;
		mCallback = r;
		mForDue = forDue;
		mDark = dark;
		mInitialExact=exact;
	}

	private final int[] TIME_DAY_TO_CALENDAR_DAY = new int[] { Calendar.SUNDAY,
			Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
			Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, };
	private LinearLayout mWeekGroup;
	private LinearLayout mWeekGroup2;
	private LinearLayout mOptions;
	private Spinner mIntervalType;
	private EditText mIntervalCount;
	protected int mIntervalValue;
	private RadioGroup mRadioGroup;
	private Spinner mEndSpinner;
	private GregorianCalendar mEndDate;
	private TextView mEndDateView;
	protected boolean mIsCustom = false;
	private CheckBox mUseExact;
	private boolean mInitialExact;
	private Spinner mStartSpinner;
	private GregorianCalendar mStartDate;
	private TextView mStartDateView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Context ctx = getDialog().getContext();
		try {
			getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		final List<Pair<Integer, String>> recurring = Recurring
				.getForDialog(mForDue);
		extraItems = 1;
		CharSequence[] items = new String[recurring.size() + extraItems];

		// items[0] = ctx.getString(R.string.recurrence_no);//Dont need this,
		// there is a button...
		items[0] = ctx.getString(R.string.recurrence_custom);
		mPosition = 0;
		for (int i = extraItems; i < recurring.size() + extraItems; i++) {
			items[i] = recurring.get(i - extraItems).second;
			if (mRecurring != null && items[i].equals(mRecurring.getLabel())) {
				mPosition = i;
			}
		}

		final View view = inflater
				.inflate(R.layout.recurrencepicker, container);
		mRecurenceSelection = (Spinner) view.findViewById(R.id.freqSpinner);
		Resources res = ctx.getResources();
		final boolean isNotTwoRows = res.getConfiguration().screenWidthDp > MIN_SCREEN_WIDTH_FOR_SINGLE_ROW_WEEK;
		ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
				ctx, android.R.layout.simple_spinner_item, items);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mOptions = (LinearLayout) view.findViewById(R.id.options);
		mRecurenceSelection.setAdapter(adapter);
		mRecurenceSelection.setSelection(mPosition);
		mWeekGroup = (LinearLayout) view.findViewById(R.id.weekGroup);
		mWeekGroup2 = (LinearLayout) view.findViewById(R.id.weekGroup2);
		mRecurenceSelection
				.setOnItemSelectedListener(new OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> arg0, View arg1,
							int pos, long id) {
						mPosition = pos;
						if (pos < extraItems) {
							switch (pos) {
							case 0:// CUSTOM
								mOptions.setVisibility(View.VISIBLE);
								mIsCustom = true;
								break;

							default:
								Log.wtf(TAG, "cannot be");
								break;
							}
						} else {
							mIsCustom = false;
							mOptions.setVisibility(View.GONE);
							mRecurring = Recurring.get(recurring.get(pos
									- extraItems).first);
						}

					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
						// TODO Auto-generated method stub

					}
				});
		String[] dayOfWeekString = new DateFormatSymbols().getShortWeekdays();
		int idx = DateTimeHelper.getFirstDayOfWeek();
		if (isNotTwoRows) {
			numOfButtonsInRow1 = 7;
			numOfButtonsInRow2 = 0;
			mWeekGroup2.setVisibility(View.GONE);
			mWeekGroup2.getChildAt(3).setVisibility(View.GONE);
		} else {
			numOfButtonsInRow1 = 4;
			numOfButtonsInRow2 = 3;

			mWeekGroup2.setVisibility(View.VISIBLE);
			// Set rightmost button on the second row invisible so it takes up
			// space and everything centers properly
			mWeekGroup2.getChildAt(3).setVisibility(View.INVISIBLE);
		}

		/* First row */
		for (int i = 0; i < 7; i++) {
			if (i >= numOfButtonsInRow1) {
				mWeekGroup.getChildAt(i).setVisibility(View.GONE);
				continue;
			}
			mWeekByDayButtons[idx] = (ToggleButton) mWeekGroup.getChildAt(i);
			mWeekByDayButtons[idx]
					.setTextOff(dayOfWeekString[TIME_DAY_TO_CALENDAR_DAY[idx]]);
			mWeekByDayButtons[idx]
					.setTextOn(dayOfWeekString[TIME_DAY_TO_CALENDAR_DAY[idx]]);
			mWeekByDayButtons[idx].setChecked(false);
			mWeekByDayButtons[idx].setOnCheckedChangeListener(this);
			if (++idx >= 7) {
				idx = 0;
			}
		}

		/* 2nd Row */
		for (int i = 0; i < 3; i++) {
			if (i >= numOfButtonsInRow2) {
				mWeekGroup2.getChildAt(i).setVisibility(View.GONE);
				continue;
			}
			mWeekByDayButtons[idx] = (ToggleButton) mWeekGroup2.getChildAt(i);
			mWeekByDayButtons[idx]
					.setTextOff(dayOfWeekString[TIME_DAY_TO_CALENDAR_DAY[idx]]);
			mWeekByDayButtons[idx]
					.setTextOn(dayOfWeekString[TIME_DAY_TO_CALENDAR_DAY[idx]]);
			mWeekByDayButtons[idx].setChecked(false);
			mWeekByDayButtons[idx].setOnCheckedChangeListener(this);
			if (++idx >= 7) {
				idx = 0;
			}
		}
		if (mPosition != 0) {
			mOptions.setVisibility(View.GONE);
		}
		mUseExact=(CheckBox)view.findViewById(R.id.recurrence_is_exact);
		Log.w(TAG,"exact: "+mInitialExact);
		mUseExact.setChecked(mInitialExact);
		mToggle = (Switch) view.findViewById(R.id.repeat_switch);
		mToggle.setChecked(mRecurring != null && mRecurring.getId() != -1);
		mToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				setEnabledComponents(isChecked);

			}
		});

		mDoneButton = (Button) view.findViewById(R.id.done);

		mDoneButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mToggle.isChecked()) {
					if (mIsCustom) {
						ArrayList<Integer> checked = new ArrayList<Integer>();
						boolean isOneChecked = false;
						for (int i = 0; i < mWeekByDayButtons.length; i++) {
							if (mWeekByDayButtons[i].isChecked()) {
								isOneChecked = true;
								checked.add(TIME_DAY_TO_CALENDAR_DAY[i]);
							}
						}
						if (isOneChecked) {
							mCallback.OnCustomRecurnceSetWeekdays(mForDue,
									checked,mStartDate, mEndDate,mUseExact.isChecked());
						} else {
							int intervalMonths = 0;
							int intervalYears = 0;
							int intervalDays = 0;
							int intervalMinutes = 0;
							int intervalHours = 0;
							int type = mIntervalType.getSelectedItemPosition();
							if (type == 0) {
								if (mForDue) {
									intervalDays = mIntervalValue;
								} else {
									intervalMinutes = mIntervalValue;
								}
							} else if (type == 1) {
								if (mForDue) {
									intervalMonths = mIntervalValue;
								} else {
									intervalHours = mIntervalValue;
								}
							} else if (type == 2) {
								if (mForDue) {
									intervalYears = mIntervalValue;
								} else {
									intervalDays = mIntervalValue;
								}
							} else if (type == 3) {
								intervalMonths = mIntervalValue;
							} else if (type == 4) {
								intervalYears = mIntervalValue;
							}
							mCallback.OnCustomRecurnceSetIntervall(mForDue,
									intervalYears, intervalMonths,
									intervalDays, intervalHours,
									intervalMinutes,mStartDate, mEndDate,mUseExact.isChecked());
						}
					} else {
						Recurring r = Recurring.createTemporayCopy(mRecurring);
						r.setExact(mUseExact.isChecked());
						Log.d(TAG,"exact: "+r.isExact() );
						r.save();
						mCallback.OnRecurrenceSet(r);
					}
				} else {
					mCallback.onNoRecurrenceSet();
				}
				dismiss();
			}
		});
		mIntervalType = (Spinner) view.findViewById(R.id.interval_type);
		mIntervalCount = (EditText) view.findViewById(R.id.interval_count);
		mIntervalCount.setText("1");// TODO set value, if rule is custom...
		updateIntervallType();
		mIntervalCount.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				int newValue = mIntervalValue;
				try {
					newValue = Integer.parseInt(s.toString());
				} catch (NumberFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (newValue == 0)
					mIntervalCount.setText("" + mIntervalValue);
				updateIntervallType();
				mIntervalValue = newValue;
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});
		final int dayPosition = mForDue ? 0 : 2;
		mIntervalType.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int pos,
					long arg3) {
				if (pos == dayPosition) {
					view.findViewById(R.id.weekGroup).setVisibility(
							View.VISIBLE);
					view.findViewById(R.id.weekGroup2).setVisibility(
							View.VISIBLE);
				} else {
					view.findViewById(R.id.weekGroup).setVisibility(View.GONE);
					view.findViewById(R.id.weekGroup2).setVisibility(View.GONE);
				}

			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub

			}
		});
		mRadioGroup = (RadioGroup) view.findViewById(R.id.monthGroup);
		mRadioGroup.setVisibility(View.GONE);// Don't support this for now...

		String[] end = { res.getString(R.string.recurrence_end_continously),
				res.getString(R.string.recurrence_end_date_label) // ,res.getString(R.string.recurrence_end_count_label)
																	// Dont
																	// support
																	// this
																	// now...*/
																	// };
		};

		mEndSpinner = (Spinner) view.findViewById(R.id.endSpinner);
		mEndSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int pos,
					long arg3) {
				switch (pos) {
				case 1:
					mEndDateView.setVisibility(View.VISIBLE);
					break;
				default:// FOREVER
					mEndDateView.setVisibility(View.GONE);
					break;
				}

			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub

			}
		});
		ArrayAdapter<CharSequence> endSpinnerAdapter = new ArrayAdapter<CharSequence>(
				ctx, android.R.layout.simple_spinner_item, end);
		endSpinnerAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mEndDateView = (TextView) view.findViewById(mDark ? R.id.endDate_dark
				: R.id.endDate_light);
		mEndDate = new GregorianCalendar();
		mEndDate.add(Calendar.MONTH, 1);
		mEndDateView.setText(DateTimeHelper.formatDate(mEndDate));
		mEndDateView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				DatePickerDialog dp = DatePickerDialog.newInstance(
						new OnDateSetListener() {

							@Override
							public void onNoDateSet() {
								// Nothing
							}

							@Override
							public void onDateSet(DatePicker datePickerDialog,
									int year, int month, int day) {
								mEndDate.set(Calendar.YEAR, year);
								mEndDate.set(Calendar.MONTH, month);
								mEndDate.set(Calendar.DAY_OF_WEEK, day);

							}
						}, mEndDate.get(Calendar.YEAR), mEndDate
								.get(Calendar.MONTH), mEndDate
								.get(Calendar.DAY_OF_MONTH), mDark, false);
				dp.show(getFragmentManager(), "endDate");

			}
		});
		mEndSpinner.setAdapter(endSpinnerAdapter);
		
		String[] start = { res.getString(R.string.recurrence_from_now),
				res.getString(R.string.recurrence_from) // ,res.getString(R.string.recurrence_end_count_label)
																	// Dont
																	// support
																	// this
																	// now...*/
																	// };
		};

		mStartSpinner = (Spinner) view.findViewById(R.id.startSpinner);
		mStartSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int pos,
					long arg3) {
				switch (pos) {
				case 1:
					mStartDateView.setVisibility(View.VISIBLE);
					break;
				default:// FOREVER
					mStartDateView.setVisibility(View.GONE);
					break;
				}

			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub

			}
		});
		ArrayAdapter<CharSequence> startSpinnerAdapter = new ArrayAdapter<CharSequence>(
				ctx, android.R.layout.simple_spinner_item, start);
		startSpinnerAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mStartDateView = (TextView) view.findViewById(mDark ? R.id.startDate_dark
				: R.id.startDate_light);
		mStartDate = new GregorianCalendar();
		mStartDateView.setText(DateTimeHelper.formatDate(mStartDate));
		mStartDateView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				DatePickerDialog dp = DatePickerDialog.newInstance(
						new OnDateSetListener() {

							@Override
							public void onNoDateSet() {
								// Nothing
							}

							@Override
							public void onDateSet(DatePicker datePickerDialog,
									int year, int month, int day) {
								mStartDate.set(Calendar.YEAR, year);
								mStartDate.set(Calendar.MONTH, month);
								mStartDate.set(Calendar.DAY_OF_WEEK, day);

							}
						}, mStartDate.get(Calendar.YEAR), mStartDate
								.get(Calendar.MONTH), mStartDate
								.get(Calendar.DAY_OF_MONTH), mDark, false);
				dp.show(getFragmentManager(), "startDate");

			}
		});
		mStartSpinner.setAdapter(startSpinnerAdapter);
		setEnabledComponents(mRecurring != null
				&& mRecurring.getId() != -1);
		if (mDark) {
			view.findViewById(R.id.recurrence_picker_dialog)
					.setBackgroundColor(res.getColor(R.color.dialog_gray));
			view.findViewById(R.id.recurrence_picker_head).setBackgroundColor(
					res.getColor(R.color.dialog_dark_gray));
			mDoneButton.setTextColor(res.getColor(R.color.White));
			mToggle.setThumbDrawable(res
					.getDrawable(R.drawable.switch_thumb_dark));
			mEndDateView.setTextColor(res.getColor(R.color.White));
			mStartDateView.setTextColor(res.getColor(R.color.White));
			mUseExact.setButtonDrawable(R.drawable.btn_check_holo_dark_red);
		}
		if(!mForDue){
			mUseExact.setVisibility(View.GONE);
		}

		return view;
	}

	private void setEnabledComponents(boolean b) {
		mRecurenceSelection.setEnabled(b);
		mEndSpinner.setEnabled(b);
		mEndDateView.setEnabled(b);
		mStartSpinner.setEnabled(b);
		mStartDateView.setEnabled(b);
		for(ToggleButton t: mWeekByDayButtons){
			t.setEnabled(b);
		}
		mIntervalCount.setEnabled(b);
		mIntervalType.setEnabled(b);
		mUseExact.setEnabled(b);
		if(mDark){
			if(b){
				mStartDateView.setTextColor(getResources().getColor(R.color.White));
				mEndDateView.setTextColor(getResources().getColor(R.color.White));
			}else{
				mStartDateView.setTextColor(getResources().getColor(R.color.grey));
				mEndDateView.setTextColor(getResources().getColor(R.color.grey));
			}
		}
	}

	private void updateIntervallType() {
		ArrayAdapter<CharSequence> adapterInterval = new ArrayAdapter<CharSequence>(
				getDialog().getContext(), android.R.layout.simple_spinner_item,
				getDayYearValues());
		adapterInterval
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mIntervalType.setAdapter(adapterInterval);
	}

	protected String[] getDayYearValues() {
		Context ctx = getDialog().getContext();
		int size = mForDue ? 3 : 5;
		int i = 0;
		String[] ret = new String[size];
		if (!mForDue) {
			ret[i++] = ctx.getResources().getQuantityString(
					R.plurals.due_minute, mIntervalValue);
			ret[i++] = ctx.getResources().getQuantityString(R.plurals.due_hour,
					mIntervalValue);
		}
		ret[i++] = ctx.getResources().getQuantityString(R.plurals.due_day,
				mIntervalValue);
		ret[i++] = ctx.getResources().getQuantityString(R.plurals.due_month,
				mIntervalValue);
		ret[i] = ctx.getResources().getQuantityString(R.plurals.due_year,
				mIntervalValue);

		return ret;
	}

	public interface OnRecurenceSetListner {
		void OnCustomRecurnceSetIntervall(boolean isDue, int intervalYears,
				int intervalMonths, int intervalDays, int intervalHours,
				int intervalMinutes,Calendar startDate, Calendar endDate,boolean isExact);

		void OnCustomRecurnceSetWeekdays(boolean isDue, List<Integer> weekdays,Calendar startDate,
				Calendar endDate,boolean isExact);

		void OnRecurrenceSet(Recurring r);

		void onNoRecurrenceSet();
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		// TODO Auto-generated method stub

	}

}
