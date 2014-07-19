package de.azapps.mirakel.settings.accounts;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.Gravity;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.DefinitionsHelper.NoSuchListException;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.settings.ListSettings;
import de.azapps.mirakel.settings.R;
import de.azapps.tools.Log;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class AccountSettingsFragment extends PreferenceFragment {
    private static final String TAG = "AccountSettingsFragment";

    public AccountSettingsFragment() {
        super();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_account);
        final Bundle b = getArguments();
        if (b != null) {
            final AccountMirakel account = AccountMirakel.get(b.getLong("id"));
            ((AccountSettingsActivity) getActivity()).setAccount(account);
            final ActionBar actionbar = getActivity().getActionBar();
            if (account == null) {
                actionbar.setTitle("No Account");
            } else {
                actionbar.setTitle(account.getName());
                // TODO: More meaningful title? (Including server)
            }
            if (!MirakelCommonPreferences.isTablet()) {
                final ImageButton delList = new ImageButton(getActivity());
                delList.setBackgroundResource(android.R.drawable.ic_menu_delete);
                actionbar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                                            ActionBar.DISPLAY_SHOW_CUSTOM);
                actionbar.setCustomView(delList, new ActionBar.LayoutParams(
                                            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                                            Gravity.CENTER_VERTICAL
                                            | DefinitionsHelper.GRAVITY_RIGHT));
                delList.setOnClickListener(((ListSettings) getActivity())
                                           .getDelOnClickListener());
            }
            try {
                new AccountSettings(this, account).setup();
            } catch (final NoSuchListException e) {
                getActivity().finish();
            }
        }
    }
}
