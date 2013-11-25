package de.azapps.mirakel.settings.accounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.Context;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import de.azapps.mirakel.Mirakel.NoSuchListException;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.sync.SyncAdapter;
import de.azapps.mirakelandroid.R;

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
		AccountManager am=AccountManager.get(ctx);
		Account a=getAccount();
		final Preference syncUsername =  findPreference("syncUsername");
		if(syncUsername!=null){
			syncUsername.setEnabled(false);
			syncUsername.setSummary(a==null?ctx.getString(R.string.local_account):a.name);
		}
		final Preference syncServer =  findPreference("syncServer");
		if(syncServer!=null){
			syncServer.setEnabled(false);
			if(a!=null&&a.type.equals(AccountMirakel.ACCOUNT_TYPE_MIRAKEL))
				syncServer.setSummary(am.getUserData(a, SyncAdapter.BUNDLE_SERVER_URL));
			else if(a!=null&&a.type.equals(AccountMirakel.ACCOUNT_TYPE_DAVDROID))
				syncServer.setSummary(a.name);
			else
				syncServer.setSummary("");
		}
		final Preference syncPassword =  findPreference("syncPassword");
		if(syncPassword!=null){
			syncPassword.setEnabled(false);
		}
		final Preference syncFrequency = findPreference("syncFrequency");
		if(syncFrequency!=null){
			syncFrequency.setEnabled(false);
		}
		
	}

	private Account getAccount() {
		AccountManager am=AccountManager.get(ctx);
		for(Account a:am.getAccounts()){
			if(a.name.equals(account.getName()))
				return a;
		}
		return null;
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
			return ((AccountSettingsFragment) settings)
					.findPreference(key);
		} else {
			return ((AccountSettingsActivity) settings)
					.findPreference(key);
		}
	}

}
