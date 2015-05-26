package de.azapps.mirakel.static_activities;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakelandroid.R;
import android.widget.Toast;

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
        setTitle("Mirakel survey");
        
        Toast.makeText(this, "Please help us improving Mirakel by participating in this short survey", Toast.LENGTH_SHORT).show();
    }
}
