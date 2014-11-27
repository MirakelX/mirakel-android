package de.azapps.mirakel.settings.fragments;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.app.ActionBarActivity;

import de.azapps.mirakel.settings.Settings;
import de.azapps.mirakel.settings.model_settings.generic_list.IDetailFragment;


public abstract class MirakelPreferencesFragment extends PreferenceFragment implements
    IDetailFragment<Settings> {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (((ActionBarActivity)getActivity()).getSupportActionBar() != null) {
            ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle(getItem().getName());
        }
    }


}
