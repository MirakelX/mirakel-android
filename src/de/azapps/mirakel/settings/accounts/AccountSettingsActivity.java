package de.azapps.mirakel.settings.accounts;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import de.azapps.mirakel.Mirakel.NoSuchListException;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.account.AccountMirakel.ACCOUNT_TYPES;
import de.azapps.mirakel.settings.ListSettings;
import de.azapps.mirakel.sync.taskwarrior.TaskWarriorSetupActivity;
import de.azapps.mirakelandroid.R;

public class AccountSettingsActivity extends ListSettings {

	private static final String	TAG	= "AccountSettingsActivity";
	private AccountMirakel		account;

	private AccountMirakel newAccount() {
		return AccountMirakel.newAccount(getString(R.string.sync_new),
				ACCOUNT_TYPES.CALDAV, true);
	}

	private void handleCalDAV() {
		new AlertDialog.Builder(this)
				.setTitle(R.string.sync_caldav)
				.setMessage(
						Html.fromHtml(this
								.getString(R.string.sync_caldav_howto_)))
								.setNegativeButton(R.string.download, new DialogInterface.OnClickListener() {
									
									@Override
									public void onClick(DialogInterface dialog, int which) {
										Helpers.openURL(getBaseContext(), "http://mirakel.azapps.de/releases.html#davdroid");
										
									}
								})
				.setPositiveButton(R.string.sync_add_account,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								startActivity(new Intent(
										Settings.ACTION_ADD_ACCOUNT));

							}
						}).show();
	}

	@Override
	protected OnClickListener getAddOnClickListener() {
		final Activity that = this;
		return new OnClickListener() {

			@Override
			public void onClick(View v) {
				CharSequence[] items = that.getResources().getTextArray(
						R.array.sync_types);
				new AlertDialog.Builder(that).setTitle(R.string.sync_add)
						.setItems(items, new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								switch (which) {
									case 0:
										handleCalDAV();
										break;
									case 1:
										startActivity(new Intent(that,
												TaskWarriorSetupActivity.class));
										break;
									default:
										break;
								}
							}
						}).show();
			}
		};
	}

	@SuppressLint("NewApi")
	@Override
	public OnClickListener getDelOnClickListener() {
		return new OnClickListener() {

			@Override
			public void onClick(View v) {
				account.destroy();
				if (Build.VERSION.SDK_INT < 11 || !onIsMultiPane()) finish();
				else {
					try {
						if (getHeader().size() > 0)
							onHeaderClick(getHeader().get(0), 0);
						invalidateHeaders();
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
		account = AccountMirakel.get(getIntent().getIntExtra("id", 0));
		try {
			new AccountSettings(this, account).setup();
		} catch (NoSuchListException e) {
			Log.d(TAG, "no account attached");
		}
	}

	@Override
	protected List<Pair<Integer, String>> getItems() {
		return new ArrayList<Pair<Integer, String>>();
	}

	@SuppressLint("NewApi")
	@Override
	public void onBuildHeaders(List<Header> target) {
		List<AccountMirakel> accounts = AccountMirakel.getAll();
		for (AccountMirakel a : accounts) {
			Bundle b = new Bundle();
			b.putInt("id", a.getId());
			Header header = new Header();
			header.fragment = getDestFragmentClass().getCanonicalName();
			header.title = a.getName();
			header.summary = a.getType().typeName(this);
			header.fragmentArguments = b;
			header.extras = b;
			target.add(header);
			Log.d(TAG, "accountname: " + a.getName());
		}
		if (accounts.size() == 0) {
			Header header = new Header();
			header.title = " ";
			header.fragment = getDestFragmentClass().getCanonicalName();
			target.add(header);
		}
		if (clickOnLast) {
			onHeaderClick(mTarget.get(mTarget.size() - 1), mTarget.size() - 1);
			clickOnLast = false;
		}
		mTarget = target;
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
