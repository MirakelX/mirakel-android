package de.azapps.mirakel.settings.fragments;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.shamanland.fab.FloatingActionButton;

import de.azapps.mirakel.ThemeManager;
import de.azapps.mirakel.model.IGenericElementInterface;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.adapter.SettingsGroupAdapter;
import de.azapps.mirakel.settings.model_settings.generic_list.IDetailFragment;


public abstract class MirakelPreferencesFragment<T extends IGenericElementInterface> extends
    PreferenceFragment implements
    IDetailFragment<T>, View.OnClickListener {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (((ActionBarActivity)getActivity()).getSupportActionBar() != null) {
            ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle(getItem().getName());
        }
    }

    protected void onFABClicked(){
    //nothing
     }

    @Override
    public void onClick(final View v) {
        onFABClicked();
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.generic_list_fragment, null);
        final SettingsGroupAdapter a = new SettingsGroupAdapter(getPreferenceScreen());
        final RecyclerView recyclerView = (RecyclerView)rootView.findViewById(R.id.generic_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(container.getContext()));
        recyclerView.setAdapter(a);
        fab = (FloatingActionButton) rootView.findViewById(R.id.fabbutton);
        if (isFabVisible()) {
            fab.setColorStateList(ColorStateList.valueOf(ThemeManager.getAccentThemeColor()));
            fab.setColorFilter(Color.WHITE);
            fab.setImageResource(android.R.drawable.ic_menu_delete);
            fab.setOnClickListener(this);
        } else {
            fab.setVisibility(View.GONE);
        }
        return rootView;
    }


    protected boolean isFabVisible() {
        return false;
    }



    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        try {
            super.onActivityCreated(savedInstanceState);
        } catch (final RuntimeException ignored) {
            //we must call this
        }
    }
}
