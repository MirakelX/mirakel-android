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
package de.azapps.mirakel.static_activities;

import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.R;
import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.sync.AuthenticatorActivity;

public class SettingsFragment extends PreferenceFragment {
	private static final String TAG="SettingsFragment";
	private ListPreference startupListPreference;
	//private MainActivity main;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Initialize needed Arrays
		List<ListMirakel> lists = ListMirakel.all();
		CharSequence entryValues[] = new String[lists.size()];
		CharSequence entries[] = new String[lists.size()];
		int i = 0;
		for (ListMirakel list : lists) {
			entryValues[i] = String.valueOf(list.getId());
			entries[i] = list.getName();
			i++;
		}

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);

		// Notifications List
		ListPreference notificationsListPreference = (ListPreference) findPreference("notificationsList");
		notificationsListPreference.setEntries(entries);
		notificationsListPreference.setEntryValues(entryValues);

		// Startup
		CheckBoxPreference startupAllListPreference = (CheckBoxPreference) findPreference("startupAllLists");
		startupListPreference = (ListPreference) findPreference("startupList");
		if (startupAllListPreference.isChecked()) {
			startupListPreference.setEnabled(false);
		}
		startupAllListPreference
				.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						if((Boolean) newValue) {
							startupListPreference.setEnabled(false);
						} else {
							startupListPreference.setEnabled(true);
						}
						return true;
					}

				});
		startupListPreference.setEntries(entries);
		startupListPreference.setEntryValues(entryValues);
		
		CheckBoxPreference sync=(CheckBoxPreference)findPreference("syncUse");
		sync.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Log.e(TAG,""+((Boolean)newValue).toString());
				AccountManager am = AccountManager.get(getActivity());
				Account[] accounts = am.getAccountsByType(Mirakel.ACCOUNT_TYP);
				if((Boolean)newValue){
					if(accounts.length==0){
						Intent intent = new Intent(getActivity(),
								AuthenticatorActivity.class);
						intent.setAction(MainActivity.SHOW_LISTS);
						startActivity(intent);
					}else{
					//	account=accounts[0];
					}
				}else{
					try{
						am.removeAccount(accounts[0], null, null);
					}catch(Exception e){
						Log.e(TAG,"Cannot remove Account");
					}
				}
				return true;
			}
		});
		/*
		EditTextPreference email=(EditTextPreference)findPreference("syncEmail");
		email.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				AccountManager am = AccountManager.get(getActivity());
				String pwd=am.getPassword(account);
				am.removeAccount(account, null, null);
				account=new Account((String)newValue, Mirakel.ACCOUNT_TYP);
				am.addAccountExplicitly(account, pwd, null);
				return true;
			}
		});*/
		
		final AccountManager am = AccountManager.get(getActivity());
		final Account account=getAccount(am);		
		if (account == null) {
			SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(getActivity()
							.getApplicationContext());
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("syncUse", false);
			editor.commit();
			sync.setChecked(false);
		}else{
			sync.setChecked(true);
		}

		EditTextPreference password= (EditTextPreference)findPreference("syncPassword");
		password.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object password) {
				//TODO CHECK NEW PASSWORD???
				if(account!=null){
					am.setPassword(account, (String)password);
				}
				return true;
			}
		});
		if(account!=null){
			password.setText(am.getPassword(account));
		}
		//TODO Add Option To Resync all
		EditTextPreference url=(EditTextPreference)findPreference("syncServer");
		if(account!=null){
			url.setText(am.getUserData(account, "url"));
		}
		url.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object url) {
				//TODO CHECK URL??
				if(account!=null){
					am.setUserData(account, "url", (String) url);
				}
				return true;
			}
		});
		
		ListPreference syncIntervall=(ListPreference)findPreference("syncFrequency");
		syncIntervall.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if(account!=null){
					ContentResolver.addPeriodicSync(account, Mirakel.AUTHORITY_TYP, null, ((Long)newValue)*60);
				}
				return true;
			}
		});
			
		
	}
	
	private Account getAccount(AccountManager am) {
		try {
			return am.getAccountsByType(Mirakel.ACCOUNT_TYP)[0];
		} catch (ArrayIndexOutOfBoundsException f) {
			Log.e(TAG, "No Account found");
			return null;
		}
	}

}
