package de.azapps.mirakel.main_activity;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakelandroid.R;

/**
 * Created by gsemmler1 on 25.05.15.
 */
public class SurveyActivity extends FragmentActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (MirakelCommonPreferences.isDark()) {
            setTheme(R.style.AppBaseThemeDARK);
        } else {
            setTheme(R.style.AppBaseTheme);
        }
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, new SurveyFragment(), "foo")
                .commit();
    }
}
