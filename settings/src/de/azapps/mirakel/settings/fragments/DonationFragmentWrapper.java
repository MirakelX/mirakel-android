/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 *   Copyright (c) 2013-2015 Anatolij Zelenin, Georg Semmler.
 *
 *       This program is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       any later version.
 *
 *       This program is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU General Public License for more details.
 *
 *       You should have received a copy of the GNU General Public License
 *       along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package de.azapps.mirakel.settings.fragments;


import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import org.sufficientlysecure.donations.DonationsFragment;

import de.azapps.mirakel.helper.AnalyticsWrapperBase;
import de.azapps.mirakel.settings.custom_views.Settings;
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
        if (((AppCompatActivity)getActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(getItem().getName());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        AnalyticsWrapperBase.setScreen(this);
    }

    @NonNull
    public static Fragment newInstance() {
        return new DonationFragmentWrapper();
    }
}
