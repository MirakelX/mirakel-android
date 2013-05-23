package de.azapps.mirakel.special_lists_settings;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
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

		final EditText name = (EditText) findViewById(R.id.special_list_name);
		name.setText(specialList.getName());
		name.setOnEditorActionListener(new EditText.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					specialList.setName(v.getText().toString());
					specialList.save();

					InputMethodManager imm = (InputMethodManager) getApplication()
							.getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(name.getWindowToken(), 0);
					return true; // consume.
				}
				return false; // pass on to other listeners.
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

		Button sort_by = (Button) findViewById(R.id.special_list_sort_by);
		sort_by.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				specialList = (SpecialList) ListDialogHelpers.handleSortBy(ctx,
						specialList);
			}
		});
		Button def_list = (Button) findViewById(R.id.special_list_def_list);
		def_list.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				specialList = ListDialogHelpers.handleDefaultList(ctx, specialList,
						lists);
			}
		});
		
		Button def_date=(Button) findViewById(R.id.special_list_def_date);
		def_date.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				specialList=ListDialogHelpers.handleDefaultDate(ctx, specialList);
				
			}
		});

	}
}
