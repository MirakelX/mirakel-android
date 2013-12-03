/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calendar.recurrencepicker;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.DialogFragment;
import android.content.Context;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.os.Bundle;
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
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Switch;
import android.widget.ToggleButton;

import com.fourmob.datetimepicker.date.DatePicker;
import com.fourmob.datetimepicker.date.DatePicker.OnDateSetListener;

import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakelandroid.R;

@SuppressLint("NewApi")
public class RecurrencePickerDialog extends DialogFragment implements OnCheckedChangeListener, OnClickListener, OnDateSetListener {

	// in dp's
	private static final int MIN_SCREEN_WIDTH_FOR_SINGLE_ROW_WEEK = 450;
	protected static final String TAG = null;
	
	public static RecurrencePickerDialog newInstance(OnReccurenceSetListner r,
			Recurring recurring, boolean forDue,boolean dark) {
		RecurrencePickerDialog re = new RecurrencePickerDialog();
		re.initialize(r, recurring, forDue,dark);
		return re;
	}

	private OnReccurenceSetListner mCallback;
	private Recurring mRecurring;
	private boolean mForDue;
	private Spinner mRecurenceSelection;
	private int extraItems;
	private Switch mToggle;
	private Button mDoneButton;
	private int mPosition;
	private boolean mDark;
	private ToggleButton[] mWeekByDayButtons=new ToggleButton[7];
	private int numOfButtonsInRow1;
	private int numOfButtonsInRow2;

	public void initialize(OnReccurenceSetListner r, Recurring recurring,
			boolean forDue,boolean dark) {
		mRecurring = recurring;
		mCallback = r;
		mForDue = forDue;
		mDark=dark;
	}
	
    private final int[] TIME_DAY_TO_CALENDAR_DAY = new int[] {
            Calendar.SUNDAY,
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY,
    };
	private LinearLayout mWeekGroup;
	private LinearLayout mWeekGroup2;

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
		final List<Pair<Integer, String>> recurring = Recurring.getForDialog(
				mForDue);
		extraItems =1 ;
		CharSequence[] items = new String[recurring.size() + extraItems];

		//items[0] = ctx.getString(R.string.recurrence_no);//Dont need this, there is a button...
		items[0] = ctx.getString(R.string.recurrence_custom);
		mPosition = 0;
		for (int i = extraItems; i < recurring.size() + extraItems; i++) {
			items[i] = recurring.get(i - extraItems).second;
			if (mRecurring != null && items[i].equals(mRecurring.getLabel())) {
				mPosition = i;
			}
		}

		View view = inflater.inflate(R.layout.recurrencepicker, container);
		mRecurenceSelection = (Spinner) view.findViewById(R.id.freqSpinner);
		Resources res = ctx.getResources();
		final boolean isNotTwoRows=res.getConfiguration().screenWidthDp > MIN_SCREEN_WIDTH_FOR_SINGLE_ROW_WEEK;
		ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
				ctx, android.R.layout.simple_spinner_item, items);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mRecurenceSelection.setAdapter(adapter);
		mRecurenceSelection.setSelection(mPosition);
		mWeekGroup = (LinearLayout) view.findViewById(R.id.weekGroup);
        mWeekGroup2 = (LinearLayout) view.findViewById(R.id.weekGroup2);
		mRecurenceSelection.setOnItemSelectedListener(new OnItemSelectedListener() {
						@Override
						public void onItemSelected(AdapterView<?> arg0,
								View arg1, int pos, long id) {
							mPosition=pos;
							if(pos<extraItems){
								switch (pos) {
								case 0://CUSTOM
									mWeekGroup.setVisibility(View.VISIBLE);
									if(!isNotTwoRows)
										mWeekGroup2.setVisibility(View.VISIBLE);
									break;

								default:
									Log.wtf(TAG, "cannot be");
									break;
								}
							}else{
								mWeekGroup.setVisibility(View.GONE);
								mWeekGroup2.setVisibility(View.GONE);
								mRecurring=Recurring.get(recurring.get(pos-extraItems).first);
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
            mWeekByDayButtons[idx].setTextOff(dayOfWeekString[TIME_DAY_TO_CALENDAR_DAY[idx]]);
            mWeekByDayButtons[idx].setTextOn(dayOfWeekString[TIME_DAY_TO_CALENDAR_DAY[idx]]);
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
            mWeekByDayButtons[idx].setTextOff(dayOfWeekString[TIME_DAY_TO_CALENDAR_DAY[idx]]);
            mWeekByDayButtons[idx].setTextOn(dayOfWeekString[TIME_DAY_TO_CALENDAR_DAY[idx]]);
            mWeekByDayButtons[idx].setChecked(false);
            mWeekByDayButtons[idx].setOnCheckedChangeListener(this);
            if (++idx >= 7) {
                idx = 0;
            }
        }
		if(mPosition!=0){
			mWeekGroup.setVisibility(View.GONE);
			mWeekGroup2.setVisibility(View.GONE);
		}
		mToggle=(Switch)view.findViewById(R.id.repeat_switch);
		mToggle.setChecked(mRecurring!=null&&mRecurring.getId()!=-1);
		mRecurenceSelection.setEnabled(mRecurring!=null&&mRecurring.getId()!=-1);
		mToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mRecurenceSelection.setEnabled(isChecked);
				
			}
		});
	
		
		mDoneButton=(Button)view.findViewById(R.id.done);
		
		mDoneButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(mToggle.isChecked()){
					mCallback.OnReccurnceSet(mRecurring);
				}else{
					mCallback.OnReccurnceSet(null);
				}
				dismiss();
			}
		});
		if(mDark){
			view.findViewById(R.id.recurrence_picker_dialog).setBackgroundColor(res.getColor(R.color.dialog_gray));
			view.findViewById(R.id.recurrence_picker_head).setBackgroundColor(res.getColor(R.color.dialog_dark_gray));
			mDoneButton.setTextColor(res.getColor(R.color.White));
			mToggle.setThumbDrawable(res.getDrawable(R.drawable.switch_thumb_dark));
		}

		return view;
	}


	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

	}


	public interface OnReccurenceSetListner {
		void OnReccurnceSet(Recurring r);
	}

	@Override
	public void onDateSet(DatePicker datePickerDialog, int year, int month,
			int day) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onNoDateSet() {
		// TODO Auto-generated method stub

	}

}
