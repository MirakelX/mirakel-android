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

import java.util.List;

import android.annotation.SuppressLint;
import android.app.DialogFragment;
import android.content.Context;
import android.database.DataSetObserver;
import android.os.Bundle;
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
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Switch;

import com.fourmob.datetimepicker.date.DatePicker;
import com.fourmob.datetimepicker.date.DatePicker.OnDateSetListener;

import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakelandroid.R;

@SuppressLint("NewApi")
public class RecurrencePickerDialog extends DialogFragment implements OnCheckedChangeListener, OnClickListener,
		android.widget.RadioGroup.OnCheckedChangeListener, OnDateSetListener {

	public static RecurrencePickerDialog newInstance(OnReccurenceSetListner r,
			Recurring recurring, boolean forDue) {
		RecurrencePickerDialog re = new RecurrencePickerDialog();
		re.initialize(r, recurring, forDue);
		return re;
	}

	private OnReccurenceSetListner mCallback;
	private Recurring mRecurring;
	private boolean mForDue;
	private Spinner mReccurenceSelection;
	private int extraItems;
	private Switch mToggle;
	private Button mDoneButton;

	public void initialize(OnReccurenceSetListner r, Recurring recurring,
			boolean forDue) {
		mRecurring = recurring;
		mCallback = r;
		mForDue = forDue;
	}

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
		int pos = 0;
		for (int i = extraItems; i < recurring.size() + extraItems; i++) {
			items[i] = recurring.get(i - extraItems).second;
			if (mRecurring != null && items[i].equals(mRecurring.getLabel())) {
				pos = i;
			}
		}

		View view = inflater.inflate(R.layout.recurrencepicker, container);
		mReccurenceSelection = (Spinner) view.findViewById(R.id.freqSpinner);

		ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
				ctx, android.R.layout.simple_spinner_item, items);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mReccurenceSelection.setAdapter(adapter);
		mReccurenceSelection.setSelection(pos);
		mReccurenceSelection.setOnItemSelectedListener(new OnItemSelectedListener() {
						@Override
						public void onItemSelected(AdapterView<?> arg0,
								View arg1, int pos, long id) {
							if(pos<extraItems){
								//handle this
							}else{
								mRecurring=Recurring.get(recurring.get(pos-extraItems).first);
							}
							
						}

						@Override
						public void onNothingSelected(AdapterView<?> arg0) {
							// TODO Auto-generated method stub
							
						}
		});
		
		mToggle=(Switch)view.findViewById(R.id.repeat_switch);
		
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

		return view;
	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		// TODO Auto-generated method stub

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
