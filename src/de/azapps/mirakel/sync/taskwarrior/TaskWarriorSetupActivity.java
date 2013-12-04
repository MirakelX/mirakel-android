package de.azapps.mirakel.sync.taskwarrior;

import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakelandroid.R;

public class TaskWarriorSetupActivity extends Activity {
	private ProgressDialog progressDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
					startActivityForResult(intent, 0);
				} catch (Exception e) {
					Uri marketUri = Uri
							.parse("market://details?id=com.google.zxing.client.android");
					Intent marketIntent = new Intent(Intent.ACTION_VIEW,
							marketUri);
					startActivity(marketIntent);
				}

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
		} catch (MalformedURLException e) {
			progressDialog.dismiss();
			Log.v("myApp", "bad url entered");

			Toast.makeText(this, R.string.sync_taskwarrior_url_error,
					Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 0) {
			if (resultCode == RESULT_OK) {
				String inputUrl = data.getStringExtra("SCAN_RESULT");
				setupTaskwarriorFromURL(inputUrl);
			}
			if (resultCode == RESULT_CANCELED) {
				// handle cancel
			}
		}
	}
}
