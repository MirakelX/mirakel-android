package de.azapps.mirakel.static_activities;

import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Configuration;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakelandroid.R;

public class CreditsActivity extends Activity {
	private final String[][]	libraries		= {
			{ "Gson", "Apache 2.0", "https://code.google.com/p/google-gson/" },
			{ "Joda-Time", "Apache 2.0", "http://joda-time.sourceforge.net" },
			{ "Android Change Log", "Apache 2.0",
			"https://code.google.com/p/android-change-log/" },
			{ "ACRA", "Apache 2.0", "http://acra.ch" },
			{ "HoloColorPicker", "Apache 2.0",
			"https://github.com/LarsWerkman/HoloColorPicker" },
			{ "Progress Wheel", "",
			"https://github.com/Todd-Davies/ProgressWheel" },
			{ "DateTimePicker Compatibility Library", "Apache 2.0",
			"https://github.com/flavienlaurent/datetimepicker" },
			{ "Webicons", "CC-Attrib", "http://fairheadcreative.com/" },
			{ "Android Donations Lib", "Apache 2.0",
			"https://github.com/dschuermann/android-donations-lib" } };
	private final String[][]	translations	= {
			{ "Spanish", "macebal" },
			{ "French", "Ghost of Kendo, waghanza, npettiaux, benasse" },
			{ "German", "Anatolij Zelenin, Georg Semmler, Patrik Kernstock" },
			{ "Portuguese", "Sérgio Marques" },
			{ "Russian", "Katy, Dmitry Derjavin" },
			{ "Spanisch", "macebal, RaindropR", "Pablo Corbalán (@monofluor)" },
			{ "Norwegian", "Jim-Stefhan Johansen" }, { "Slovenian", "mateju" },
			{ "Arabic", "Rajaa Gutknecht" }, { "Czech", "sarimak" },
			{ "Dutch", "Toon van Gerwen" },
			{ "Italian", "Rajaa Gutknecht, fazen, Claudio Arseni" },
			{ "Bulgarian", "Boriana Tcholakova"}};

	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
		Locale.setDefault(Helpers.getLocal(this));
		super.onConfigurationChanged(newConfig);
	}

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (MirakelPreferences.isDark()) {
			setTheme(R.style.AppBaseThemeDARK);
		}
		Locale.setDefault(Helpers.getLocal(this));
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_credits);
		TextView creditTextHead = (TextView) findViewById(R.id.credits_text_head);
		creditTextHead.setText(Html
				.fromHtml(getString(R.string.credits_text_head)));
		creditTextHead.setMovementMethod(LinkMovementMethod.getInstance());
		if (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}

		// Set Libraries
		String libs = "";
		for (String[] library : this.libraries) {
			libs += "<a href=\"" + library[2] + "\"><b>" + library[0]
					+ "</b></a> (" + library[1] + ")<br />";
		}
		TextView creditTextLibs = (TextView) findViewById(R.id.credits_libraries_text);
		creditTextLibs.setText(Html.fromHtml(libs));
		creditTextLibs.setMovementMethod(LinkMovementMethod.getInstance());
		// Set translations
		String trans = "";
		for (String[] translation : this.translations) {
			trans += "<b>" + translation[0] + ": </b>" + translation[1]
					+ "<br/>";
		}
		TextView creditTextTrans = (TextView) findViewById(R.id.credits_translations_text);
		creditTextTrans.setText(Html.fromHtml(trans));
		creditTextTrans.setMovementMethod(LinkMovementMethod.getInstance());
		TextView creditTextLicense = (TextView) findViewById(R.id.credits_license_text);
		creditTextLicense.setText(Html
				.fromHtml(getString(R.string.credits_license)));
		creditTextLicense.setMovementMethod(LinkMovementMethod.getInstance());
	}

	public void onOpenGithubClick(View v) {
		Helpers.openURL(this, "https://github.com/azapps/mirakel-android");
	}

	public void onOpenGooglePlusClick(View v) {
		Helpers.openURL(this,
				"https://plus.google.com/u/0/communities/110640831388790835840");
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

	public void sendFeedback(View v) {
		Helpers.contact(this);
	}

}
