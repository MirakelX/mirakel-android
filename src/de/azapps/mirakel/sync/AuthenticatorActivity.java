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
/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * Modified by weiznich 2013
 */
//TODO Need to cleanup
package de.azapps.mirakel.sync;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.message.BasicNameValuePair;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.account.AccountMirakel.ACCOUNT_TYPES;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.sync.SyncAdapter.SYNC_STATE;
import de.azapps.mirakel.sync.caldav.CalDavSync;
import de.azapps.mirakel.sync.mirakel.MirakelSync;
import de.azapps.mirakel.sync.taskwarrior.TaskWarriorSync;
import de.azapps.mirakelandroid.R;
import de.azapps.tools.FileUtils;

/**
 * Activity which displays login screen to the user.
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity {
	/** The Intent flag to confirm credentials. */
	public static final String PARAM_CONFIRM_CREDENTIALS = "confirmCredentials";

	/** The Intent extra to store password. */
	public static final String PARAM_PASSWORD = "password";

	/** The Intent extra to store username. */
	public static final String PARAM_USERNAME = "username";

	/** The Intent extra to store username. */
	public static final String PARAM_AUTHTOKEN_TYPE = "authtokenType";

	/** The tag used to log to adb console. */
	private static final String TAG = "AuthenticatorActivity";
	private final int CONFIG_TASKWARRIOR = 1;
	private AccountManager mAccountManager;

	/** Keep track of the login task so can cancel it if requested */
	// private UserLoginTask mAuthTask = null;

	/** Keep track of the progress dialog so we can dismiss it */
	private ProgressDialog mProgressDialog = null;

	/** for posting authentication attempts back to UI thread */
	// private final Handler mHandler = new Handler();

	private TextView mMessage;

	private Spinner mType;

	private String mPassword;

	private EditText mPasswordEdit;

	/** Was the original caller asking for an entirely new account? */
	protected boolean mRequestNewAccount = false;

	private String mUsername;

	private EditText mUsernameEdit;

	private ACCOUNT_TYPES syncType;

	private String config_file;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		if (MirakelPreferences.isDark())
			setTheme(R.style.DialogDark);
		mAccountManager = AccountManager.get(this);
		final Intent intent = getIntent();

		mUsername = intent.getStringExtra(PARAM_USERNAME);
		mRequestNewAccount = mUsername == null;
		requestWindowFeature(Window.FEATURE_LEFT_ICON);
		setContentView(R.layout.login_activity);

		if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB)
			setTheme(R.style.Dialog);

		getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
				android.R.drawable.ic_dialog_alert);

		mMessage = (TextView) findViewById(R.id.message);
		mUsernameEdit = (EditText) findViewById(R.id.username_edit);
		mPasswordEdit = (EditText) findViewById(R.id.password_edit);
		config_file = null;

		if (!TextUtils.isEmpty(mUsername))
			mUsernameEdit.setText(mUsername);
		mType = (Spinner) findViewById(R.id.server_typ);

		mType.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				/**
				 * SYNC-Views Edit this if you want to implement a new Sync
				 */
				String syncTypeString = mType.getSelectedItem().toString();
				Log.d(TAG, syncTypeString);
				syncType = ACCOUNT_TYPES.getSyncType(syncTypeString);
				mMessage.setText(syncTypeString);
				ViewSwitcher switcher = (ViewSwitcher) findViewById(R.id.switcher_login);
				switch (syncType) {
				case TASKWARRIOR:
					if (switcher.getCurrentView().getId() != R.id.login_taskwarrior)
						switcher.showPrevious();
					break;
				case MIRAKEL:
					if (switcher.getCurrentView().getId() != R.id.login_mirakel)
						switcher.showPrevious();
					mUsernameEdit.setHint(R.string.Email);
					mUsernameEdit
							.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
					((EditText) findViewById(R.id.server_edit))
							.setText(R.string.offical_server_url);
					break;
				case CALDAV:
					if (switcher.getCurrentView().getId() != R.id.login_mirakel) {
						switcher.showPrevious();
					}
					mUsernameEdit
							.setHint(R.string.login_activity_username_label);
					mUsernameEdit.setInputType(InputType.TYPE_CLASS_TEXT);
					((EditText) findViewById(R.id.server_edit)).setText("");
					break;
				default:
					break;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub

			}
		});
		mMessage.setText(getMessage(getResources().getStringArray(
				R.array.server_typs)[0]));
		if (MirakelPreferences.isDark()
				&& VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB) {
			findViewById(R.id.login_button_frame).setBackgroundColor(
					getResources().getColor(android.R.color.transparent));
		}
		final Activity a = this;
		((Button) findViewById(R.id.load_configfile))
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						Helpers.showFileChooser(CONFIG_TASKWARRIOR,
								getString(R.string.select_config), a);

					}
				});

		((Button) findViewById(R.id.send_mail))
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						Intent browserIntent = new Intent(
								Intent.ACTION_VIEW,
								Uri.parse("http://mirakel.azapps.de/users/sign_up"));
						startActivity(browserIntent);
						/*
						 * http://mirakel.azapps.de/users/sign_up
						 * Helpers.contact(a,"[Mirakel] Registration TW-Server",
						 * "Hey Mirakel team,\n" +
						 * "Please setup an account for me.\n" +
						 * "My username: random (optional)\n" +
						 * "My company: random (optional)\n\n" +
						 * "Other things I want to say:\n\n" +
						 * "Thanks,\n[A Mirakel user]");
						 */

					}
				});
		((Button) findViewById(R.id.ok_button))
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						handleLogin(v);

					}
				});

		Button howToButton = (Button) findViewById(R.id.open_howto);
		howToButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri
						.parse("http://mirakel.azapps.de/taskwarrior.html"));
				startActivity(browserIntent);

			}
		});
	}

	/*
	 * {@inheritDoc}
	 */
	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		final ProgressDialog dialog = new ProgressDialog(this);
		dialog.setMessage(getText(R.string.ui_activity_authenticating));
		dialog.setIndeterminate(true);
		dialog.setCancelable(true);
		dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				Log.i(TAG, "user cancelling authentication");
			}
		});
		// We save off the progress dialog in a field so that we can dismiss
		// it later. We can't just call dismissDialog(0) because the system
		// can lose track of our dialog if there's an orientation change.
		mProgressDialog = dialog;
		return dialog;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK
				&& requestCode == CONFIG_TASKWARRIOR) {
			config_file = FileUtils.getPathFromUri(data.getData(), this);
			((TextView) findViewById(R.id.login_taskwarrior_path))
					.setText(config_file);
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * Handles onClick event on the Submit button. Sends username/password to
	 * the server for authentication. The button is configured to call
	 * handleLogin() in the layout XML.
	 * 
	 * @param view
	 *            The Submit button for which this method is invoked
	 */
	public void handleLogin(View view) {
		syncType = ACCOUNT_TYPES.getSyncType(mType.getSelectedItem().toString());
		if (mRequestNewAccount) {
			mUsername = mUsernameEdit.getText().toString();
		}
		mPassword = mPasswordEdit.getText().toString();
		if ((TextUtils.isEmpty(mUsername) || TextUtils.isEmpty(mPassword))
				&& syncType != ACCOUNT_TYPES.TASKWARRIOR) {
			mMessage.setText(getMessage(mType.getSelectedItem().toString()));
		} else {
			if (((CheckBox) findViewById(R.id.resync)).isChecked()) {
				Mirakel.getWritableDatabase().execSQL(
						"Delete from " + Task.TABLE + " where sync_state="
								+ SYNC_STATE.DELETE);
				Mirakel.getWritableDatabase().execSQL(
						"Delete from " + ListMirakel.TABLE
								+ " where sync_state=" + SYNC_STATE.DELETE);
				Mirakel.getWritableDatabase().execSQL(
						"Update " + Task.TABLE + " set sync_state="
								+ SYNC_STATE.ADD);
				Mirakel.getWritableDatabase().execSQL(
						"Update " + ListMirakel.TABLE + " set sync_state="
								+ SYNC_STATE.ADD);
			}

			/**
			 * SYNC Edit this if you want to implement a new Sync ––– Add your
			 * own handle*Login()
			 */
			boolean successfull = true;
			switch (syncType) {
			case TASKWARRIOR:
				Log.v(TAG, "Use Taskwarrior");
				try {
					finishTWLogin();
				} catch (FileNotFoundException e) {
					successfull = false;
				}
				break;
			case MIRAKEL:
				handleMirakelLogin();
				break;
			case CALDAV:
				handleCalDavLogin();
				break;
			default:
				Log.wtf(TAG, "Not supported sync-type.");
				Toast.makeText(getApplicationContext(),
						R.string.wrong_sync_type, Toast.LENGTH_LONG).show();
				return;
			}
			if (successfull) {
				final Intent intent = new Intent();
				intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mUsername);
				intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE,
						AccountMirakel.ACCOUNT_TYPE_MIRAKEL);
				setAccountAuthenticatorResult(intent.getExtras());
				setResult(RESULT_OK, intent);
				finish();
			} else {
				switch (syncType) {
				case TASKWARRIOR:
					((TextView) findViewById(R.id.message_bottom))
							.setText(R.string.wrong_config);
					break;
				default:
					Log.wtf(TAG, "unkown error");
					break;
				}
			}
		}
	}

	private void handleCalDavLogin() {
		Account account = new Account(mUsernameEdit.getText().toString(),
				AccountMirakel.ACCOUNT_TYPE_MIRAKEL);
		Bundle b = new Bundle();
		b.putString(SyncAdapter.BUNDLE_SERVER_TYPE, CalDavSync.TYPE);
		b.putString(SyncAdapter.BUNDLE_SERVER_URL,
				((EditText) findViewById(R.id.server_edit)).getText()
						.toString());
		String pwd = mPasswordEdit.getText().toString();
		mAccountManager.addAccountExplicitly(account, pwd, b);

	}

	private void finishTWLogin() throws FileNotFoundException {
		// TODO add errorhandling
		Log.d(TAG, config_file);
		if (config_file == null)
			return;
		File f = new File(config_file);
		if (f.exists() && f.canRead()) {
			byte[] buffer = new byte[(int) f.length()];// TODO remove cast
			try {
				new FileInputStream(f).read(buffer);
			} catch (FileNotFoundException e) {
				Log.e(TAG, "File not found");
				return;
			} catch (IOException e) {
				Log.e(TAG, "cannot read file");
				return;
			}
			try {
				Bundle b = new Bundle();
				b.putString(SyncAdapter.BUNDLE_SERVER_TYPE,
						TaskWarriorSync.TYPE);
				String content = new String(buffer);
				String[] t = content.split("org: ");
				Log.d(TAG, "user: " + t[0].replace("username: ", ""));
				final Account account = new Account(t[0].replace("username: ",
						"").replace("\n", ""), AccountMirakel.ACCOUNT_TYPE_MIRAKEL);
				t = t[1].split("user key: ");
				Log.d(TAG, "org: " + t[0].replace("\n", ""));
				b.putString(SyncAdapter.BUNDLE_ORG, t[0].replace("\n", ""));

				t = t[1].split("server: ");
				Log.d(TAG, "user key: " + t[0].replace("\n", ""));
				String pwd = t[0].replace("\n", "");

				t = t[1].split("Client.cert:\n");
				Log.d(TAG, "server: " + t[0].replace("\n", ""));
				b.putString(SyncAdapter.BUNDLE_SERVER_URL,
						t[0].replace("\n", ""));
				t = t[1].split("Client.key:\n");
				Log.d(TAG, "client cert: " + t[0].replace("\n", ""));

				FileUtils.writeToFile(
						new File(TaskWarriorSync.CLIENT_CERT_FILE), t[0]);

				t = t[1].split("ca.cert:\n");
				Log.d(TAG, "client key: " + t[0].replace("\n", ""));

				FileUtils.writeToFile(
						new File(TaskWarriorSync.CLIENT_KEY_FILE), t[0]);
				Log.d(TAG, "ca: " + t[1].replace("\n", ""));
				FileUtils.writeToFile(new File(TaskWarriorSync.CA_FILE), t[1]);
				mAccountManager.addAccountExplicitly(account, pwd, b);
			} catch (ArrayIndexOutOfBoundsException e) {
				Log.e(TAG, "wrong Configfile");
				throw new FileNotFoundException();
			}

		} else {
			Log.e(TAG, "File not found");
		}
		Log.i(TAG, "finishTWLogin()");
	}

	/**
	 * Called when response is received from the server for authentication
	 * request. See onAuthenticationResult(). Sets the
	 * AccountAuthenticatorResult which is sent back to the caller. We store the
	 * authToken that's returned from the server as the 'password' for this
	 * account - so we're never storing the user's actual password locally.
	 * 
	 * @param result
	 *            the confirmCredentials result.
	 */
	private void finishMirakelLogin(String url, String token) {
		Log.i(TAG, "finishLogin()");
		final Account account = new Account(mUsername, AccountMirakel.ACCOUNT_TYPE_MIRAKEL);
		if (mRequestNewAccount) {
			Bundle b = new Bundle();
			b.putString(SyncAdapter.BUNDLE_SERVER_URL, url);
			b.putString(SyncAdapter.BUNDLE_SERVER_TYPE, MirakelSync.TYPE);
			mAccountManager.addAccountExplicitly(account, mPassword, b);
			// Set contacts sync for this account.
		} else {
			mAccountManager.setPassword(account, mPassword);
		}
		new Network(new DataDownloadCommand() {
			@Override
			public void after_exec(String result) {
				ContentResolver.setIsSyncable(account, Mirakel.AUTHORITY_TYP, 1);
				ContentResolver.setSyncAutomatically(account,
						Mirakel.AUTHORITY_TYP, true);
			}
		}, Network.HttpMode.DELETE, this, null).execute(url + "/tokens/"
				+ token);
	}

	public void onAuthenticationCancel() {
		Log.i(TAG, "onAuthenticationCancel()");

		// Our task is complete, so clear it out
		// mAuthTask = null;

		// Hide the progress dialog
		hideProgress();
	}

	/**
	 * Returns the message to be displayed at the top of the login dialog box.
	 */
	private CharSequence getMessage(String accountTyp) {
		if (TextUtils.isEmpty(mUsername)) {
			// If no username, then we ask the user to log in using an
			// appropriate service.
			final CharSequence msg = getString(
					R.string.login_activity_newaccount_text, accountTyp);
			return msg;
		}
		if (TextUtils.isEmpty(mPassword)) {
			// We have an account but no password
			return getText(R.string.login_activity_loginfail_text_pwmissing);
		}
		return null;
	}

	/**
	 * Shows the progress UI for a lengthy operation.
	 */
	// TODO Fix This!!
	@SuppressWarnings("deprecation")
	private void showProgress() {
		showDialog(0);
	}

	/**
	 * Hides the progress UI for a lengthy operation.
	 */
	private void hideProgress() {
		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}
	}

	private void handleMirakelLogin() {
		Log.v(TAG, "Use Mirakel");
		// Show a progress dialog, and kick off a background task to
		// perform
		// the user login attempt.
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		List<BasicNameValuePair> data = new ArrayList<BasicNameValuePair>();
		data.add(new BasicNameValuePair("email", mUsername));
		data.add(new BasicNameValuePair("password", mPassword));
		if (networkInfo != null && networkInfo.isConnected()) {
			showProgress();
			final String url = ((EditText) findViewById(R.id.server_edit))
					.getText().toString();
			new Network(new DataDownloadCommand() {
				@Override
				public void after_exec(String result) {
					String token = Network.getToken(result);
					if (token == null) {
						Log.e(TAG, "Login failed");
						hideProgress();
					} else {
						Log.e(TAG, "Login sucess");
						finishMirakelLogin(url, token);
					}
				}
			}, Network.HttpMode.POST, data, this, null).execute(url
					+ "/tokens.json");
		} else {
			Log.e(TAG, "No network connection available.");
			Toast.makeText(getApplicationContext(), R.string.NoNetwork,
					Toast.LENGTH_LONG).show();
		}
	}

}
