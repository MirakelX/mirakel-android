package de.azapps.mirakel.settings.accounts;

import de.azapps.mirakel.Mirakel.NoSuchListException;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.settings.ListSettings;
import de.azapps.mirakelandroid.R;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.Gravity;
import android.widget.ImageButton;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class AccountSettingsFragment extends PreferenceFragment {
	private static final String TAG = "AccountSettingsFragment";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings_account);
		Bundle b = getArguments();
		if (b != null) {
			Log.d(TAG, "id= " + b.getInt("id"));
			final AccountMirakel account = AccountMirakel.get(b.getInt("id")); 
			((AccountSettingsActivity) getActivity()).setAccount(account);

			ActionBar actionbar = getActivity().getActionBar();
			if (account == null) {
				actionbar.setTitle("No Account");
			} else {
				actionbar.setTitle(account.getName()); // TODO: More meaningful
														// title? (Including
														// server)
			}
			//TODO implement this
			/*if (!Helpers.isTablet(getActivity())) {
				ImageButton delList = new ImageButton(getActivity());
				delList.setBackgroundResource(android.R.drawable.ic_menu_delete);
				actionbar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
						ActionBar.DISPLAY_SHOW_CUSTOM);
				actionbar.setCustomView(delList, new ActionBar.LayoutParams(
						ActionBar.LayoutParams.WRAP_CONTENT,
						ActionBar.LayoutParams.WRAP_CONTENT,
						Gravity.CENTER_VERTICAL | Gravity.RIGHT));
				delList.setOnClickListener(((ListSettings) getActivity())
						.getDelOnClickListener());
			}*/
			
			try {
				new AccountSettings(this, account).setup();
				
			}
			catch (NoSuchListException e) {
				getActivity().finish();
			}
		}
	}
}
