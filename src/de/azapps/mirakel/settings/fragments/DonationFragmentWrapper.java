package de.azapps.mirakel.settings.fragments;


import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBarActivity;

import org.sufficientlysecure.donations.DonationsFragment;

import de.azapps.mirakel.settings.Settings;
import de.azapps.mirakel.settings.model_settings.generic_list.IDetailFragment;

public class DonationFragmentWrapper extends DonationsFragment implements
    IDetailFragment<Settings> {

    @NonNull
    @Override
    public Settings getItem() {
        return Settings.DONATE;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (((ActionBarActivity)getActivity()).getSupportActionBar() != null) {
            ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle(getItem().getName());
        }
    }

    @NonNull
    public static Fragment newInstance() {
        return new DonationFragmentWrapper();
    }
}
