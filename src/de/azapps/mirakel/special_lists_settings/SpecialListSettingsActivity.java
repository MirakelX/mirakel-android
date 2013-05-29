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
package de.azapps.mirakel.special_lists_settings;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ViewSwitcher;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView.OnEditorActionListener;
import android.widget.EditText;
import android.widget.TextView;
import de.azapps.mirakel.R;
import de.azapps.mirakel.helper.ListDialogHelpers;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;

public class SpecialListSettingsActivity extends Activity {
	public static final String SLIST_ID = "de.azapps.mirakel.SpecialListSettings/list_id";
	private List<ListMirakel> lists;
	private SpecialList specialList;
	Context ctx = this;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent i = getIntent();
		specialList = SpecialList.getSpecialList(i.getIntExtra(SLIST_ID, 1));
		setContentView(R.layout.special_list_preferences);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setTitle(specialList.getName());
		
		ViewSwitcher s = (ViewSwitcher) findViewById(R.id.switch_special_list_name);
		if (s.getNextView().getId() != R.id.special_list_name_edit) {
			s.showPrevious();
		}

		final TextView name = (TextView) findViewById(R.id.special_list_name);
		name.setText(specialList.getName());
		name.setOnClickListener(new OnClickListener() {	
			
			@Override
			public void onClick(View v) {
				final ViewSwitcher switcher = (ViewSwitcher)findViewById(R.id.switch_special_list_name);
				switcher.showNext(); 
				EditText txt = (EditText) findViewById(R.id.special_list_name_edit);
				txt.setText(name.getText());
				txt.requestFocus();
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(txt, InputMethodManager.SHOW_IMPLICIT);
				txt.setOnEditorActionListener(new OnEditorActionListener() {
					
					@Override
					public boolean onEditorAction(TextView v, int actionId,
							KeyEvent event) {
						if (actionId == EditorInfo.IME_ACTION_DONE) {
							specialList.setName(v.getText().toString());
							specialList.save();
							InputMethodManager imm = (InputMethodManager) getApplication()
									.getSystemService(Context.INPUT_METHOD_SERVICE);
							imm.hideSoftInputFromWindow(name.getWindowToken(), 0);
							switcher.showPrevious();
							return true; // consume.
						}
						return false; // pass on to other listeners.
					}
				});				
			}
		});

		CheckBox active = (CheckBox) findViewById(R.id.special_list_active);
		active.setChecked(specialList.isActive());
		active.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				specialList.setActive(isChecked);
				specialList.save();
			}
		});

		final EditText where = (EditText) findViewById(R.id.special_list_where);
		where.setText(specialList.getWhereQuery());
		where.setOnEditorActionListener(new EditText.OnEditorActionListener() {
			
			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					specialList.setWhereQuery(v.getText().toString());
					specialList.save();

					InputMethodManager imm = (InputMethodManager) getApplication()
							.getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(where.getWindowToken(), 0);
					return true; // consume.

				}
				return false; // pass on to other listeners.
			}
		});

		lists = ListMirakel.all(false);
		LinearLayout sortBy=(LinearLayout)findViewById(R.id.special_list_sort_by_view);
		final TextView sortByShow=(TextView)findViewById(R.id.special_list_sort_by_pref);
		sortByShow.setText(getResources().getStringArray(R.array.task_sorting_items)[specialList.getSortBy()]);
		sortBy.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				specialList = (SpecialList) ListDialogHelpers.handleSortBy(ctx,
						specialList,sortByShow);
			}
		});
		LinearLayout defList = (LinearLayout) findViewById(R.id.special_list_def_list_view);
		final TextView defListShow=(TextView)findViewById(R.id.special_list_def_list_pref);
		defListShow.setText(specialList.getDefaultList().getName());
		defList.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				specialList = ListDialogHelpers.handleDefaultList(ctx, specialList,
						lists,defListShow);
			}
		});
		
		LinearLayout defDate=(LinearLayout) findViewById(R.id.special_list_def_date_view);
		final TextView defDateShow=(TextView) findViewById(R.id.special_list_def_date_pref);
		int[] values=getResources().getIntArray(R.array.special_list_def_date_picker_val);
		for(int j=0;j<values.length;j++){
			if(specialList.getDefaultDate()==null)
				defDateShow.setText(getResources().getStringArray(R.array.special_list_def_date_picker)[0]);
			else if(values[j]==specialList.getDefaultDate()){
				defDateShow.setText(getResources().getStringArray(R.array.special_list_def_date_picker)[j]);
			}
		}
		defDate.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				specialList=ListDialogHelpers.handleDefaultDate(ctx, specialList,defDateShow);
			}
		});

	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.special_list_settingsactivity, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		case R.id.menu_delete:
			specialList.destroy();
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
