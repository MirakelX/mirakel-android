package de.azapps.mirakel.special_lists_settings;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;

public class SpecialListSettingsActivity extends Activity {
	public static final String SLIST_ID = "de.azapps.mirakel.SpecialListSettings/list_id";
	private List<ListMirakel> lists;
	private SpecialList specialList;
	private AlertDialog setDefaultListDialog;

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

		Button def_list = (Button) findViewById(R.id.special_list_def_list);
		def_list.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				handleDefaultList();
			}
		});

	}

	/**
	 * Handle the actions after clicking on a move task button
	 * 
	 * @param task
	 */
	public void handleDefaultList() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.special_list_def_list);
		List<CharSequence> items = new ArrayList<CharSequence>();
		final List<Integer> list_ids = new ArrayList<Integer>();
		int currentItem = 0, i = 0;
		items.add(getString(R.string.special_list_first));
		list_ids.add(null);
		for (ListMirakel list : lists) {
			if (list.getId() > 0) {
				items.add(list.getName());
				if (specialList.getDefaultList() == null) {
					currentItem = 0;
				} else {
					if (specialList.getDefaultList().getId() == list.getId())
						currentItem = i;
				}
				list_ids.add(list.getId());
				++i;
			}
		}

		builder.setSingleChoiceItems(
				items.toArray(new CharSequence[items.size()]), currentItem,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						Integer lid = list_ids.get(item);
						if (lid == null) {
							specialList.setDefaultList(null);
						} else {
							specialList.setDefaultList(ListMirakel.getList(lid));
						}
						specialList.save();
						setDefaultListDialog.dismiss();
					}
				});

		setDefaultListDialog = builder.create();
		setDefaultListDialog.show();
	}

}
