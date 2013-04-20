package de.azapps.mirakel;

import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.media.audiofx.AcousticEchoCanceler;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.util.Log;

public class SettingsFragment extends PreferenceFragment {
	private static final String TAG="SettingsFragment";
	private ListPreference startupListPreference;
	private Account account;
	//private MainActivity main;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Initialize needed Arrays
		//main=(MainActivity) getActivity();
		ListsDataSource listsDataSource = new ListsDataSource(getActivity());
		List<List_mirakle> lists = listsDataSource.getAllLists();
		CharSequence entryValues[] = new String[lists.size()];
		CharSequence entries[] = new String[lists.size()];
		int i = 0;
		for (List_mirakle list : lists) {
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
						//account = new Account("foobar", Mirakel.ACCOUNT_TYP);
						//Authenticator t=new Authenticator(getActivity());
						//t.addAccount(response, Mirakel.ACCOUNT_TYP, null, requiredFeatures, null)
						//am.addAccountExplicitly(account, "abc", null);
					}else{
						account=accounts[0];
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
		});
		
		EditTextPreference password= (EditTextPreference)findPreference("syncPassword");
		password.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				AccountManager am = AccountManager.get(getActivity());
				am.setPassword(account, (String)newValue);
				return true;
			}
		});*/
		
	}

}
