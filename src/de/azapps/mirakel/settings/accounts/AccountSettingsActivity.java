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
import de.azapps.mirakel.DefinitionsHelper.NoSuchListException;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.settings.ListSettings;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.sync.taskwarrior.TaskWarriorSetupActivity;
import de.azapps.tools.Log;

public class AccountSettingsActivity extends ListSettings {

	private static final String TAG = "AccountSettingsActivity";
	protected AccountMirakel account;

	@Override
	protected OnClickListener getAddOnClickListener() {
		final Activity that = this;
		return new OnClickListener() {

			@Override
			public void onClick(final View v) {
				final CharSequence[] items = that.getResources().getTextArray(
						R.array.sync_types);
				new AlertDialog.Builder(that).setTitle(R.string.sync_add)
						.setItems(items, new DialogInterface.OnClickListener() {

							@Override
							public void onClick(final DialogInterface dialog,
									final int which) {
								switch (which) {
								case 0:
									showAlert(
											R.string.alert_caldav_title,
											R.string.alert_caldav,
											new DialogInterface.OnClickListener() {

												@Override
												public void onClick(
														final DialogInterface dialog,
														final int which) {
													handleCalDAV();
												}
											});
									break;
								case 1:
									showAlert(
											R.string.alert_taskwarrior_title,
											R.string.alert_taskwarrior,
											new DialogInterface.OnClickListener() {

												@Override
												public void onClick(
														final DialogInterface dialog,
														final int which) {
													startActivity(new Intent(
															that,
															TaskWarriorSetupActivity.class));
												}
											});
									break;
								default:
									break;
								}
								dialog.dismiss();
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
			public void onClick(final View v) {
				AccountSettingsActivity.this.account.destroy();
				if (Build.VERSION.SDK_INT < 11 || !onIsMultiPane()) {
					finish();
				} else {
					try {
						if (getHeader().size() > 0) {
							onHeaderClick(getHeader().get(0), 0);
						}
						invalidateHeaders();
					} catch (final Exception e) {
						finish();
					}
				}
			}
		};

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
	protected OnClickListener getHelpOnClickListener() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected List<Pair<Integer, String>> getItems() {
		return new ArrayList<Pair<Integer, String>>();
	}

	@Override
	protected int getSettingsRessource() {
		return R.xml.settings_account;
	}

	@Override
	protected int getTitleRessource() {
		return R.string.sync_title; // TODO: Look if the title is meaningful
	}

	protected void handleCalDAV() {
		new AlertDialog.Builder(this)
				.setTitle(R.string.sync_caldav)
				.setMessage(
						Html.fromHtml(this
								.getString(R.string.sync_caldav_howto_)))
				.setNegativeButton(R.string.download,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(final DialogInterface dialog,
									final int which) {
								Helpers.openURL(getBaseContext(),
										"http://mirakel.azapps.de/releases.html#davdroid");

							}
						})
				.setPositiveButton(R.string.sync_add_account,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(final DialogInterface dialog,
									final int which) {
								startActivity(new Intent(
										Settings.ACTION_ADD_ACCOUNT));

							}
						}).show();
	}

	@SuppressLint("NewApi")
	@Override
	public void onBuildHeaders(final List<Header> target) {
		final List<AccountMirakel> accounts = AccountMirakel.all();
		for (final AccountMirakel a : accounts) {
			final Bundle b = new Bundle();
			b.putInt("id", a.getId());
			final Header header = new Header();
			header.fragment = getDestFragmentClass().getCanonicalName();
			header.title = a.getName();
			header.summary = a.getType().typeName(this);
			header.fragmentArguments = b;
			header.extras = b;
			target.add(header);
			Log.d(TAG, "accountname: " + a.getName());
		}
		if (accounts.size() == 0) {
			final Header header = new Header();
			header.title = " ";
			header.fragment = getDestFragmentClass().getCanonicalName();
			target.add(header);
		}
		if (this.clickOnLast) {
			onHeaderClick(this.mTarget.get(this.mTarget.size() - 1),
					this.mTarget.size() - 1);
			this.clickOnLast = false;
		}
		this.mTarget = target;
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
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
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		default:
			if (item.getTitle().equals(getString(R.string.delete))) {
				if (this.account != null) {
					this.account.destroy();
				}
				finish();
				return true;
			} else if (item.getTitle().equals(getString(R.string.add))) {
				getAddOnClickListener().onClick(null);
			}
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	public void setAccount(final AccountMirakel account) {
		this.account = account;
	}

	@Override
	protected void setupSettings() {
		this.account = AccountMirakel.get(getIntent().getIntExtra("id", 0));
		try {
			new AccountSettings(this, this.account).setup();
		} catch (final NoSuchListException e) {
			Log.d(TAG, "no account attached");
		}
	}

	private void showAlert(final int titleId, final int messageId,
			final android.content.DialogInterface.OnClickListener listener) {
		new AlertDialog.Builder(this).setTitle(titleId).setMessage(messageId)
				.setPositiveButton(android.R.string.ok, listener).show();
	}

}
