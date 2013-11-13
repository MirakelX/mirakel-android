package de.azapps.mirakel.settings.accounts;

import de.azapps.mirakel.Mirakel.NoSuchListException;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.settings.special_list.SpecialListsSettingsActivity;
import de.azapps.mirakel.settings.special_list.SpecialListsSettingsFragment;
import android.annotation.SuppressLint;
import android.content.Context;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;

public class AccountSettings implements OnPreferenceChangeListener {

	private AccountMirakel account;
	private boolean v4_0;
	private Object settings;
	private Context ctx;

	@SuppressLint("NewApi")
	public AccountSettings(AccountSettingsFragment accountSettingsFragment,
			AccountMirakel account) {
		this.account = account;
		v4_0 = true;
		settings = accountSettingsFragment;
		ctx = accountSettingsFragment.getActivity();
	}

	public AccountSettings(AccountSettingsActivity accountSettingsFragment,
			AccountMirakel account) {
		this.account = account;
		v4_0 = false;
		settings = accountSettingsFragment;
		ctx = accountSettingsFragment;
	}

	public void setup() throws NoSuchListException {
		if (account == null) {
			throw new NoSuchListException();
		}

		final EditTextPreference user = (EditTextPreference) findPreference("account_user");

		// TODO: Glue, glue, glue...
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		// TODO: Nothing? Then why does this class implement
		// OnPreferenceChangeListener?
		return false;
	}

	// TODO: This repeats in SpecialListSettings. Maybe extract into a
	// superclass?
	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	private Preference findPreference(String key) {
		if (v4_0) {
			return ((SpecialListsSettingsFragment) settings)
					.findPreference(key);
		} else {
			return ((SpecialListsSettingsActivity) settings)
					.findPreference(key);
		}
	}

}
