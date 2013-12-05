package de.azapps.mirakel.sync.taskwarrior;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import de.azapps.mirakel.helper.DownloadTask;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.sync.SyncAdapter;
import de.azapps.mirakelandroid.R;
import de.azapps.tools.FileUtils;

public class TaskWarriorSetupActivity extends Activity {
	private final static String TAG = "TaskWarriorSetupActivity";
	private ProgressDialog progressDialog;
	private final int CONFIG_QR = 0, CONFIG_TASKWARRIOR = 1;
	private AccountManager mAccountManager;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAccountManager = AccountManager.get(this);
		if (MirakelPreferences.isDark())
			setTheme(R.style.AppBaseThemeDARK);
		setContentView(R.layout.activity_sync_taskwarrior);
		Button scanQR = (Button) findViewById(R.id.sync_taskwarrior_scan_qr);
		scanQR.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					Intent intent = new Intent(
							"com.google.zxing.client.android.SCAN");
					intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
					startActivityForResult(intent, CONFIG_QR);
				} catch (Exception e) {
					new AlertDialog.Builder(getApplicationContext())
							.setTitle(R.string.no_barcode_app)
							.setMessage(R.string.no_barcode_app_message)
							.setPositiveButton(R.string.no_barcode_app_install,
									new DialogInterface.OnClickListener() {

										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {
											Uri marketUri = Uri
													.parse("market://details?id=com.google.zxing.client.android");
											Intent marketIntent = new Intent(
													Intent.ACTION_VIEW,
													marketUri);
											startActivity(marketIntent);
										}
									}).show();
				}

			}
		});
		final Activity that = this;
		Button select_config_file = (Button) findViewById(R.id.sync_taskwarrior_select_file);
		select_config_file.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Helpers.showFileChooser(CONFIG_TASKWARRIOR,
						getString(R.string.select_config), that);

			}
		});
		Button register = (Button) findViewById(R.id.sync_taskwarrior_register);
		register.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri
						.parse("http://mirakel.azapps.de/users/sign_up"));
				startActivity(browserIntent);
			}
		});
		Button howto = (Button) findViewById(R.id.sync_taskwarrior_how_to);
		howto.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri
						.parse("http://mirakel.azapps.de/taskwarrior.html"));
				startActivity(browserIntent);
			}
		});
	}

	private void setupTaskwarriorFromURL(String inputUrl) {
		progressDialog = new ProgressDialog(this);
		progressDialog
				.setMessage(getString(R.string.sync_taskwarrior_configuring));
		progressDialog.setIndeterminate(true);
		progressDialog.show();

		if (!inputUrl.contains("http://"))
			inputUrl = "http://" + inputUrl;

		try {
			URL url = new URL(inputUrl);
			final File dest = new File(getCacheDir(), "taskd.config");
			final Activity that = this;
			DownloadTask dlTask = new DownloadTask(new DownloadTask.Exec() {

				@Override
				public void execute(Integer status) {
				}
			}, new DownloadTask.Exec() {

				@Override
				public void execute(Integer status) {
				}
			}, new DownloadTask.Exec() {

				@Override
				public void execute(Integer result) {
					if (result != DownloadTask.RESULT_SUCCESS) {
						Toast.makeText(
								that,
								getString(R.string.sync_taskwarrior_error_download),
								Toast.LENGTH_LONG).show();
						progressDialog.dismiss();
					} else {
						try {
							setupTaskwarrior(dest, true);
						} catch (IOException e) {
							Log.e(TAG, Log.getStackTraceString(e));
							Toast.makeText(that,
									getString(R.string.wrong_config),
									Toast.LENGTH_LONG).show();
						} finally {
							progressDialog.dismiss();
						}
					}
				}
			});
			Pair<URL, File> sUrl = new Pair<URL, File>(url, dest);
			dlTask.execute(sUrl);

			Log.e("Blubb", dest.getAbsolutePath());
		} catch (MalformedURLException e) {
			progressDialog.dismiss();
			Log.v(TAG, "bad url entered");

			Toast.makeText(this, R.string.sync_taskwarrior_url_error,
					Toast.LENGTH_SHORT).show();
			progressDialog.dismiss();
		}
	}

	private void setupTaskwarrior(File configFile, boolean deleteAfter)
			throws IOException {
		boolean success = false;
		String error = "";
		if (configFile.exists() && configFile.canRead()) {
			try {

				FileInputStream fis = new FileInputStream(configFile);
				byte[] buffer = new byte[(int) configFile.length()];
				fis.read(buffer);
				fis.close();
				Bundle b = new Bundle();
				b.putString(SyncAdapter.BUNDLE_SERVER_TYPE,
						TaskWarriorSync.TYPE);
				String content = new String(buffer);
				String[] t = content.split("org: ");
				Log.d(TAG, "user: " + t[0].replace("username: ", ""));
				final Account account = new Account(t[0].replace("username: ",
						"").replace("\n", ""),
						AccountMirakel.ACCOUNT_TYPE_MIRAKEL);
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
				success = true;
			} catch (ArrayIndexOutOfBoundsException e) {
				Log.e(TAG, "wrong Configfile");
				error = "Wrong config file";
				success = false;
			} catch (IOException e) {
				success = false;
				error = "Cannot open file";
			}
		} else {
			Log.e(TAG, "File not found");
		}
		if (deleteAfter)
			configFile.delete();

		if (success) {
			Toast.makeText(this,
					getString(R.string.sync_taskwarrior_setup_success),
					Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(this, error, Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode != RESULT_OK) {
			return;
		}
		switch (requestCode) {
		case CONFIG_QR:
			String inputUrl = data.getStringExtra("SCAN_RESULT");
			setupTaskwarriorFromURL(inputUrl);
			break;
		case CONFIG_TASKWARRIOR:
			try {
				setupTaskwarrior(
						new File(FileUtils.getPathFromUri(data.getData(), this)),
						false);
			} catch (IOException e) {
				Log.e(TAG, Log.getStackTraceString(e));
				Toast.makeText(this, getString(R.string.wrong_config),
						Toast.LENGTH_LONG).show();
			}
			break;
		}
	}
}
