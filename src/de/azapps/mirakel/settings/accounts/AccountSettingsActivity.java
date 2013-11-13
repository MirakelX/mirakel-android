package de.azapps.mirakel.settings.accounts;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.settings.ListSettings;
import de.azapps.mirakel.settings.special_list.SpecialListsSettingsActivity;
import de.azapps.mirakel.sync.SyncAdapter.SYNC_TYPES;
import de.azapps.mirakelandroid.R;

public class AccountSettingsActivity extends ListSettings {

	@SuppressWarnings("unused")
	private static final String TAG = "AccountSettingsActivity";
	private AccountMirakel account;
	
	private AccountMirakel newAccount() {
		return AccountMirakel.newAccount(getString(R.string.sync_new),
				SYNC_TYPES.CALDAV, true);
	}

	
	@SuppressLint("NewApi")

	@Override
	protected OnClickListener getAddOnClickListener() {
		// TODO Auto-generated method stub
		return new OnClickListener() {

			@Override
			public void onClick(View v) {
				newAccount();
				clickOnLast();
				invalidateHeaders();
			}

		};
	}

	@SuppressLint("NewApi") // TODO: Is not needed in SpecialListSettingsActivity: Why?
	@Override
	public OnClickListener getDelOnClickListener() {
		return new OnClickListener() {

			@Override
			public void onClick(View v) {
				account.destroy();
				if (!onIsMultiPane()) {
					finish();
				} else {
					try {
						if (getHeader().size() > 0) {
							onHeaderClick(getHeader().get(0), 0);
							invalidateHeaders();
						}
					} catch (Exception e) {
						finish();
					}
				}
			}
		};
	}

	@Override
	protected OnClickListener getHelpOnClickListener() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected int getSettingsRessource() {
		return R.xml.settings_account;
	}

	@Override
	protected void setupSettings() {
		// TODO
	}

	@Override
	protected List<Pair<Integer, String>> getItems() {

		List<AccountMirakel> accounts = AccountMirakel.getAll();

		List<Pair<Integer, String>> items = new ArrayList<Pair<Integer, String>>();
		for (AccountMirakel account : accounts) {
			items.add(new Pair<Integer, String>(account.getId(), account
					.getName())); // TODO: Add more info to the title
		}
		return items;
	}

	@Override
	protected Class<?> getDestClass() {
		return AccountSettingsActivity.class;
	}

	@Override
	protected Class<?> getDestFragmentClass() {
		return AccountSettingsFragment.class;
	}

	@Override
	protected int getTitleRessource() {
		return R.string.sync_title; // TODO: Look if the title is meaningful
	}

	public void setAccount(AccountMirakel account) {
		this.account = account;
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		if (Build.VERSION_CODES.ICE_CREAM_SANDWICH > Build.VERSION.SDK_INT) {
			if (getIntent().hasExtra("id")) {
				menu.add(R.string.delete);
			} else {
				menu.add(R.string.add);
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		default:
			if (item.getTitle().equals(getString(R.string.delete))) {
				if (account != null) {
					account.destroy();
				}
				finish();
				return true;
			} else if (item.getTitle().equals(getString(R.string.add))) {
				AccountMirakel s = newAccount();
				Intent intent = new Intent(this,
						AccountSettingsActivity.class);
				intent.putExtra("id", s.getId());
				startActivity(intent);
				return true;
			}
			break;
		}
		return super.onOptionsItemSelected(item);
	}

}
