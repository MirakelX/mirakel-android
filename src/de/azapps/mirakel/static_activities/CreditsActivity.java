package de.azapps.mirakel.static_activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.widget.TextView;
import de.azapps.mirakelandroid.R;

public class CreditsActivity extends Activity {

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		if (preferences.getBoolean("DarkTheme", false))
			setTheme(R.style.AppBaseThemeDARK);
		setContentView(R.layout.activity_credits);
		TextView creditText = (TextView) findViewById(R.id.credit_text);
		creditText.setText(Html.fromHtml(getString(R.string.credit)));
		creditText.setMovementMethod(LinkMovementMethod.getInstance());
		if (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB)
			getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
