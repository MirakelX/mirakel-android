package de.azapps.mirakel.helper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

public class PreferencesHelper {
    protected final Activity activity;
    protected final Object ctx;
    protected boolean v4_0;

    public PreferencesHelper(final PreferenceActivity c) {
        this.ctx = c;
        this.v4_0 = false;
        this.activity = c;
    }

    @SuppressLint("NewApi")
    public PreferencesHelper(final PreferenceFragment c) {
        this.ctx = c;
        this.v4_0 = true;
        this.activity = c.getActivity();
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    protected Preference findPreference(final String key) {
        if (this.v4_0) {
            return ((PreferenceFragment) this.ctx).findPreference(key);
        }
        return ((PreferenceActivity) this.ctx).findPreference(key);
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    protected void removePreference(final String which) {
        final Preference pref = findPreference(which);
        if (pref != null) {
            if (this.v4_0) {
                ((PreferenceFragment) this.ctx).getPreferenceScreen()
                .removePreference(pref);
            } else {
                ((PreferenceActivity) this.activity).getPreferenceScreen()
                .removePreference(pref);
            }
        }
    }

}
