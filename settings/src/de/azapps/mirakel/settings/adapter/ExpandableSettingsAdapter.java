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

package de.azapps.mirakel.settings.adapter;

import android.content.Context;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.TwoStatePreference;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.google.common.base.Optional;

import java.util.HashMap;
import java.util.Map;

import de.azapps.mirakel.settings.custom_views.SwipeLinearLayout;
import de.azapps.mirakel.settings.helper.PreferencesHelper;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;


public class ExpandableSettingsAdapter extends
    RecyclerView.Adapter<ExpandableSettingsAdapter.ViewHolder>
    implements Preference.OnPreferenceChangeListener {
    @NonNull
    private PreferenceScreen screen;
    private final LayoutInflater inflater;

    private Map<String, Integer> dependencies = new HashMap<>();
    @NonNull
    private Optional<SwipeLinearLayout.OnItemRemoveListener> onRemove = absent();

    public ExpandableSettingsAdapter(final @NonNull PreferenceScreen preferenceScreen) {
        screen = preferenceScreen;
        screen.setOnPreferenceChangeListener(this);

        inflater = LayoutInflater.from(preferenceScreen.getContext());

    }

    public void updateScreen(final @NonNull PreferenceScreen newScreen) {
        screen = newScreen;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        return new ViewHolder(parent.getContext());
    }

    public void setRemoveListener(final @Nullable SwipeLinearLayout.OnItemRemoveListener onRemove) {
        this.onRemove = fromNullable(onRemove);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final Preference preference = screen.getPreference(position);
        ((SwipeLinearLayout)holder.itemView).removeAllViews();
        ((SwipeLinearLayout)holder.itemView).addView(getView(position, preference));
        if (onRemove.isPresent()) {
            ((SwipeLinearLayout)holder.itemView).setOnItemRemovedListener(new
            SwipeLinearLayout.OnItemRemoveListener() {
                @Override
                public void onRemove(final int pos, final int index) {
                    onRemove.get().onRemove(position, index);
                }
            });
        }
    }

    private View getView(final int position, final @NonNull Preference preference) {
        if (preference.getDependency() != null) {
            dependencies.put(preference.getKey(), position);
            preference.setEnabled(((TwoStatePreference)screen.findPreference(
                                       preference.getDependency())).isChecked());
        }
        final Preference.OnPreferenceChangeListener changed = preference.getOnPreferenceChangeListener();
        preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference p, Object newValue) {
                notifyItemChanged(position);
                if (dependencies.containsKey(p.getKey()) && (dependencies.get(p.getKey()) != position)) {
                    notifyItemChanged(dependencies.get(p.getKey()));
                }
                return (changed == null) || changed.onPreferenceChange(p, newValue);
            }
        });
        final View v = preference.getView(null, null);
        final View.OnClickListener onClick = new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (preference.hasKey()) {
                    final Integer pos = PreferencesHelper.findPreferenceScreenForPreference(preference.getKey(),
                                        screen);
                    if (pos != null) {
                        screen.onItemClick(null, null, pos, 0L);
                    }
                    if (!(preference instanceof DialogPreference)) {
                        notifyItemChanged(position);
                    }
                }
            }
        };
        if ((preference.getKey() != null) &&
            preference.getKey().startsWith(String.valueOf(SwipeLinearLayout.SWIPEABLE_VIEW))) {
            v.setTag(SwipeLinearLayout.SWIPEABLE_VIEW, onClick);
        }
        v.setOnClickListener(onClick);
        return v;
    }




    @Override
    public int getItemCount() {
        return screen.getPreferenceCount();
    }

    @Override
    public boolean onPreferenceChange(final Preference preference, final Object newValue) {
        notifyDataSetChanged();
        return true;
    }

    @NonNull
    public PreferenceScreen getScreen() {
        return screen;
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(final Context ctx) {
            super(new SwipeLinearLayout(ctx));
            final SwipeLinearLayout frame = (SwipeLinearLayout) itemView;
            frame.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                  ViewGroup.LayoutParams.WRAP_CONTENT));
        }

    }
}
