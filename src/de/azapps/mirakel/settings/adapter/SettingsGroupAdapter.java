/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 *   Copyright (c) 2013-2014 Anatolij Zelenin, Georg Semmler.
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

package de.azapps.mirakel.settings.adapter;

import android.app.ActionBar;
import android.os.Build;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.TwoStatePreference;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.HashMap;
import java.util.Map;

import de.azapps.mirakel.settings.R;


public class SettingsGroupAdapter extends RecyclerView.Adapter<SettingsGroupAdapter.ViewHolder>
    implements Preference.OnPreferenceChangeListener {
    @NonNull
    private final PreferenceScreen screen;
    private final static LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT);
    private Map<String, Integer> dependencis = new HashMap<>();

    public SettingsGroupAdapter(final @NonNull PreferenceScreen preferenceScreen) {
        screen = preferenceScreen;
        screen.setOnPreferenceChangeListener(this);
        params.setMargins(0, (int) (screen.getContext().getResources().getDimension(
                                        R.dimen.padding_list_item) * 0.5), 0, 0);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final LinearLayout wrapper = new LinearLayout(parent.getContext());
        wrapper.setLayoutParams(params);
        return new ViewHolder(wrapper);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final Preference preference = screen.getPreference(position);
        ((LinearLayout)holder.itemView).removeAllViews();
        final View v;
        if (preference instanceof PreferenceGroup) {
            v = new CardView(holder.itemView.getContext());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                v.setElevation(5);
            }
            v.setLayoutParams(params);
            final LinearLayout ll = new LinearLayout(holder.itemView.getContext());
            ll.setOrientation(LinearLayout.VERTICAL);
            ll.addView(preference.getView(null, null));
            final PreferenceGroup group = (PreferenceGroup)preference;
            for (int i = 0; i < group.getPreferenceCount(); i++) {
                ll.addView(getView(position, group.getPreference(i)));
            }
            ((CardView)v).addView(ll);
        } else {
            v = getView(position, preference);

        }
        ((LinearLayout) holder.itemView).addView(v);
    }

    private View getView(final int position, final @NonNull Preference preference) {
        if (preference.getDependency() != null) {
            dependencis.put(preference.getKey(), position);
            preference.setEnabled(((TwoStatePreference)screen.findPreference(
                                       preference.getDependency())).isChecked());
        }
        final Preference.OnPreferenceChangeListener changed = preference.getOnPreferenceChangeListener();
        preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference p, Object newValue) {
                notifyItemChanged(position);
                if (dependencis.containsKey(p.getKey()) && (dependencis.get(p.getKey()) != position)) {
                    notifyItemChanged(dependencis.get(p.getKey()));
                }
                return changed == null || changed.onPreferenceChange(p, newValue);
            }
        });
        View v = preference.getView(null, null);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                screen.onItemClick(null, null, findPreferenceScreenForPreference(preference.getKey(), null), 0);
                if (!(preference instanceof DialogPreference)) {
                    notifyItemChanged(position);
                }
            }
        });
        return v;
    }

    @Nullable
    private Integer findPreferenceScreenForPreference( @NonNull final String key,
            PreferenceScreen screen ) {
        if ( screen == null ) {
            screen = this.screen;
        }

        final android.widget.Adapter ada = screen.getRootAdapter();
        for ( int i = 0; i < ada.getCount(); i++ ) {
            final String prefKey = ((Preference)ada.getItem(i)).getKey();
            if (key.equals( prefKey ) ) {
                return i;
            }
            if ( ada.getItem(i) instanceof PreferenceScreen ) {
                final Integer result = findPreferenceScreenForPreference(key, (PreferenceScreen) ada.getItem(i));
                if ( result != null ) {
                    return result;
                }
            }
        }
        return null;
    }


    @Override
    public int getItemCount() {
        return screen.getPreferenceCount();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        notifyDataSetChanged();
        return true;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(View itemView) {
            super(itemView);
        }

    }
}
