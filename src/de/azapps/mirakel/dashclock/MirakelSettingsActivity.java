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
package de.azapps.mirakel.dashclock;

import java.util.ArrayList;
import java.util.List;

import org.dmfs.provider.tasks.TaskContract.TaskLists;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.widget.NumberPicker;
import android.widget.Toast;

public class MirakelSettingsActivity extends PreferenceActivity {
	private static final String TAG = "MirakelSettingsActivity";
	private NumberPicker numberPicker;

	@SuppressWarnings("deprecation")
	// TODO Why?
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		Drawable d = getResources().getDrawable(R.drawable.bw_mirakel);
		getActionBar().setIcon(d);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getListView().setDividerHeight(0);
		addPreferencesFromResource(R.xml.pref_xml);
		final ListPreference startupListPreference = (ListPreference) findPreference("startupList");
		String[] s = { TaskLists._ID, TaskLists.LIST_NAME };
		// Get Lists from Mirakel-Contentresolver
		Cursor c = null;
		try {
			c = getContentResolver().query(TaskLists.CONTENT_URI, s, "1=1",
					null, null);
		} catch (Exception e) {
			Log.e(TAG, "Cannot communicate to Mirakel");
			return;
		}
		if (c == null) {
			Log.wtf(TAG, "Mirakel-Contentprovider not Found");
			Toast.makeText(this, getString(R.string.installMirakel),
					Toast.LENGTH_SHORT).show();
			return;
		}
		int list_id = Integer.parseInt(prefs.getString("startupList", "-1"));
		final List<CharSequence> values = new ArrayList<CharSequence>();
		final List<CharSequence> entries = new ArrayList<CharSequence>();
		c.moveToFirst();
		while (!c.isAfterLast()) {
			values.add("" + (c.getInt(0)));
			entries.add(c.getString(1));
			c.moveToNext();
		}
		setListSummary(startupListPreference, list_id, values, entries);
		startupListPreference.setEntries(entries.toArray(new String[entries
				.size()]));
		startupListPreference.setEntryValues(values.toArray(new String[values
				.size()]));
		final Preference p = findPreference("showTasks");
		int maxTasks = prefs.getInt("showTaskNumber", 1);
		startupListPreference
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference, Object newValue) {
						int listId = Integer.parseInt(newValue.toString());
						setListSummary(startupListPreference, listId, values,
								entries);
						return true;
					}
				});
		p.setSummary(getResources().getQuantityString(R.plurals.how_many,
				maxTasks, maxTasks));
		final Context ctx = this;
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);
		p.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				numberPicker = new NumberPicker(ctx);
				numberPicker.setMaxValue(5);
				numberPicker.setMinValue(0);
				numberPicker.setWrapSelectorWheel(false);
				numberPicker.setValue(settings.getInt("showTaskNumber", 0));
				numberPicker
						.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
				new AlertDialog.Builder(ctx)
						.setTitle(getString(R.string.number_of))
						.setMessage(getString(R.string.how_many))
						.setView(numberPicker)
						.setPositiveButton(getString(android.R.string.ok),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
										int count = numberPicker.getValue();
										SharedPreferences.Editor editor = settings
												.edit();
										editor.putInt("showTaskNumber", count);
										editor.commit();
										p.setSummary(getResources()
												.getQuantityString(
														R.plurals.how_many,
														count, count));
									}

								})
						.setNegativeButton(getString(android.R.string.cancel),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
										// Do nothing.
									}
								}).show();
				return true;
			}
		});

	}

	private void setListSummary(final ListPreference startupListPreference, int list_id, List<CharSequence> values, List<CharSequence> entries) {
		for (int i = 0; i < entries.size(); i++) {
			if (values.get(i).equals(list_id + "")) {
				startupListPreference.setSummary(entries.get(i));
				break;
			}
		}
		Log.d(TAG, "fuck " + list_id);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
