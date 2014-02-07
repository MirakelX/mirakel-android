package de.azapps.mirakel.sync.taskwarrior;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.sync.SyncAdapter;
import de.azapps.mirakelandroid.R;
import de.azapps.tools.FileUtils;
import de.azapps.tools.Log;

public class TaskWarriorSetupActivity extends Activity {
	private class DownloadTask extends AsyncTask<URL, Integer, Integer> {
		private final static String	TAG	= "DownloadTask";
		private final Exec				pre, progress, post;

		public DownloadTask(Exec pre, Exec progress, Exec post) {
			this.pre = pre;
			this.progress = progress;
			this.post = post;
		}

		@Override
		protected Integer doInBackground(URL... sUrl) {
			URL url = sUrl[0];
			HttpURLConnection connection = null;
			try {
				connection = (HttpURLConnection) url.openConnection();
				connection.setRequestMethod("GET");
				connection.setDoOutput(true);
				connection.connect();

				// expect HTTP 200 OK, so we don't mistakenly save error report
				// instead of the file
				if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
					return RESULT_ERROR;
				setupTaskWarrior(connection.getInputStream(), false);

			} catch (Exception e) {
				Log.e(TAG, Log.getStackTraceString(e));
				return RESULT_ERROR;
			}

			return RESULT_SUCCESS;
		}

		@Override
		protected void onPostExecute(Integer result) {
			this.post.execute(result);
		}

		@Override
		protected void onPreExecute() {
			this.pre.execute(null);
		}

		@Override
		protected void onProgressUpdate(Integer... progresses) {
			super.onProgressUpdate(progresses);
			this.progress.execute(progresses[0]);
		}

	}
	private interface Exec {
		void execute(Integer status);
	}
	private static final Integer	RESULT_ERROR	= 0;
	private static final Integer	RESULT_SUCCESS	= 1;

	private final static String	TAG	= "TaskWarriorSetupActivity";

	private final int			CONFIG_QR	= 0, CONFIG_TASKWARRIOR = 1;

	private AccountManager		mAccountManager;

	private ProgressDialog		progressDialog;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode != RESULT_OK) return;
		switch (requestCode) {
			case CONFIG_QR:
				String inputUrl = data.getStringExtra("SCAN_RESULT");
				setupTaskwarriorFromURL(inputUrl);
				break;
			case CONFIG_TASKWARRIOR:
				String path = FileUtils.getPathFromUri(data.getData(), this);
				if (path == null
						&& Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
					try {
						setupTaskWarrior(
								getContentResolver().openInputStream(
										data.getData()), true);
					} catch (FileNotFoundException e) {
						Toast.makeText(
								this,
								getString(R.string.sync_taskwarrior_select_file_not_exists),
								Toast.LENGTH_LONG).show();
					}
				} else if (path != null) {
					Log.w(TAG, "path: " + path);
					Log.w(TAG, "uri: " + data.getData().toString());
					setupTaskwarrior(new File(path), false);
				}
				break;
			default:
				break;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.mAccountManager = AccountManager.get(this);
		if (MirakelPreferences.isDark()) {
			setTheme(R.style.AppBaseThemeDARK);
		}
		setContentView(R.layout.activity_sync_taskwarrior);
		Button scanQR = (Button) findViewById(R.id.sync_taskwarrior_scan_qr);
		scanQR.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					Intent intent = new Intent(
							"com.google.zxing.client.android.SCAN");
					intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
					startActivityForResult(intent, TaskWarriorSetupActivity.this.CONFIG_QR);
				} catch (Exception e) {
					new AlertDialog.Builder(TaskWarriorSetupActivity.this)
					.setTitle(R.string.no_barcode_app)
					.setMessage(R.string.no_barcode_app_message)
					.setPositiveButton(R.string.no_barcode_app_install,
							new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
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
				Helpers.showFileChooser(TaskWarriorSetupActivity.this.CONFIG_TASKWARRIOR,
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

	private void setupTaskwarrior(File configFile, boolean deleteAfter) {
		if (configFile.exists() && configFile.canRead()) {
			try {
				setupTaskWarrior(new FileInputStream(configFile), true);
			} catch (FileNotFoundException e) {
				Log.wtf(TAG, "file vanish");
			}
		} else {
			Log.d(TAG, "file not found");
		}
		if (deleteAfter) {
			configFile.delete();
		}
	}

	private void setupTaskWarrior(InputStream stream, boolean showToasts) {
		boolean success = false;
		final int nothing = -1;
		final int wrong_config = 0;
		final int ioError = 1;
		int error = nothing;
		try {
			String content = new String();
			BufferedReader r = new BufferedReader(new InputStreamReader(stream));
			String line;
			while ((line = r.readLine()) != null) {
				content += line + "\n";
			}
			Bundle b = new Bundle();
			b.putString(SyncAdapter.BUNDLE_SERVER_TYPE, TaskWarriorSync.TYPE);
			// String content = new String(buffer);
			String[] t = content.split("(?i)org: ");
			// Log.d(TAG, "user: " + t[0].replace("username: ", ""));
			final Account account = new Account(t[0].replaceAll(
					"(?i)username: ",
					"")
					.replace("\n", ""), AccountMirakel.ACCOUNT_TYPE_MIRAKEL);
			t = t[1].split("(?i)user key: ");
			// Log.d(TAG, "org: " + t[0].replace("\n", ""));
			b.putString(SyncAdapter.BUNDLE_ORG, t[0].replace("\n", ""));

			t = t[1].split("(?i)server: ");
			// Log.d(TAG, "user key: " + t[0].replace("\n", ""));
			String pwd = t[0].replace("\n", "");

			t = t[1].split("(?i)Client.cert:\n");
			// Log.d(TAG, "server: " + t[0].replace("\n", ""));
			b.putString(SyncAdapter.BUNDLE_SERVER_URL, t[0].replace("\n", ""));
			t = t[1].split("(?i)Client.key:\n");
			// Log.d(TAG, "client cert: " + t[0].replace("\n", ""));

			FileUtils.writeToFile(new File(TaskWarriorSync.CLIENT_CERT_FILE),
					t[0]);

			t = t[1].split("(?i)ca.cert:\n");
			// Log.d(TAG, "client key: " + t[0].replace("\n", ""));

			FileUtils.writeToFile(new File(TaskWarriorSync.CLIENT_KEY_FILE),
					t[0]);
			// Log.d(TAG, "ca: " + t[1].replace("\n", ""));
			FileUtils.writeToFile(new File(TaskWarriorSync.CA_FILE), t[1]);
			this.mAccountManager.addAccountExplicitly(account, pwd, b);
			success = true;
		} catch (ArrayIndexOutOfBoundsException e) {
			Log.e(TAG, "wrong Configfile");
			error = wrong_config;
			success = false;
		} catch (IOException e) {
			success = false;
			error = ioError;
		}
		if (showToasts) {
			if (success) {
				Toast.makeText(this,
						getString(R.string.sync_taskwarrior_setup_success),
						Toast.LENGTH_LONG).show();
				finish();
			} else {
				// maybe look here which error was reported
				Toast.makeText(
						this,
						getString(error == ioError ? R.string.sync_taskwarrior_select_file_not_exists
								: R.string.wrong_config), Toast.LENGTH_LONG)
								.show();
			}
		} else if (!success) throw new RuntimeException();
	}

	public void setupTaskwarriorFromURL(String inputUrl) {
		this.progressDialog = new ProgressDialog(this);
		this.progressDialog
		.setMessage(getString(R.string.sync_taskwarrior_configuring));
		this.progressDialog.setIndeterminate(true);
		this.progressDialog.show();

		if (!inputUrl.startsWith("http")) {
			inputUrl = "http://" + inputUrl;
		}

		try {
			URL url = new URL(inputUrl);
			final Activity that = this;
			DownloadTask dlTask = new DownloadTask(new Exec() {

				@Override
				public void execute(Integer status) {}
			}, new Exec() {

				@Override
				public void execute(Integer status) {}
			}, new Exec() {

				@Override
				public void execute(Integer result) {
					Toast.makeText(
							that,
							getString(result == RESULT_SUCCESS ? R.string.sync_taskwarrior_setup_success
									: R.string.sync_taskwarrior_error_download),
									Toast.LENGTH_LONG).show();

					TaskWarriorSetupActivity.this.progressDialog.dismiss();
					finish();
				}
			});
			dlTask.execute(url);
		} catch (MalformedURLException e) {
			this.progressDialog.dismiss();
			Log.v(TAG, "bad url entered");

			Toast.makeText(this, R.string.sync_taskwarrior_url_error,
					Toast.LENGTH_SHORT).show();
			this.progressDialog.dismiss();
		}
	}

}
