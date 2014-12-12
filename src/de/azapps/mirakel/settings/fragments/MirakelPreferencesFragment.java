package de.azapps.mirakel.settings.fragments;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.azapps.mirakel.model.IGenericElementInterface;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.adapter.SettingsGroupAdapter;
import de.azapps.mirakel.settings.model_settings.generic_list.IDetailFragment;


public abstract class MirakelPreferencesFragment<T extends IGenericElementInterface> extends
    PreferenceFragment implements
    IDetailFragment<T> {


    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (((ActionBarActivity)getActivity()).getSupportActionBar() != null) {
            ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle(getItem().getName());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.generic_list_fragment, null);
        SettingsGroupAdapter a = new SettingsGroupAdapter(getPreferenceScreen());
        RecyclerView l = (RecyclerView)rootView.findViewById(R.id.generic_list);
        l.setLayoutManager(new LinearLayoutManager(container.getContext()));
        l.setAdapter(a);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        try {
            super.onActivityCreated(savedInstanceState);
        } catch (RuntimeException e) {
            //we must call this
        }
    }
}
