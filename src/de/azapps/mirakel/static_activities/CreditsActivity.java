package de.azapps.mirakel.static_activities;

import de.azapps.mirakel.R;
import de.azapps.mirakel.R.layout;
import de.azapps.mirakel.R.menu;
import android.os.Bundle;
import android.app.Activity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.widget.TextView;

public class CreditsActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_credits);
		TextView creditText=(TextView) findViewById(R.id.credit_text);
		creditText.setText(Html.fromHtml(getString(R.string.credit)));
		creditText.setMovementMethod(LinkMovementMethod.getInstance());
	}

}
