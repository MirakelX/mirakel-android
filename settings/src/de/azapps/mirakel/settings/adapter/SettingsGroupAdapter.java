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

import android.graphics.Typeface;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.TwoStatePreference;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.common.base.Optional;

import java.util.HashMap;
import java.util.Map;

import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.custom_views.ExpandablePreference;
import de.azapps.mirakel.settings.custom_views.SwipeLinearLayout;
import de.azapps.mirakel.settings.helper.PreferencesHelper;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;


public class SettingsGroupAdapter extends RecyclerView.Adapter<SettingsGroupAdapter.ViewHolder>
    implements Preference.OnPreferenceChangeListener {
    @NonNull
    private PreferenceScreen screen;
    private final LayoutInflater inflater;

    private Map<String, Integer> dependencies = new HashMap<>();
    @NonNull
    private Optional<SwipeLinearLayout.OnItemRemoveListener> onRemove = absent();

    public SettingsGroupAdapter(final @NonNull PreferenceScreen preferenceScreen) {
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
        return new ViewHolder((LinearLayout) inflater.inflate(R.layout.preferences_group_card, null));
    }

    public void setRemoveListener(final @Nullable SwipeLinearLayout.OnItemRemoveListener onRemove) {
        this.onRemove = fromNullable(onRemove);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final Preference preference = screen.getPreference(position);
        final CardView card = holder.card;
        card.removeAllViews();
        card.setRadius(0.0F);
        if (preference instanceof PreferenceGroup) {
            final SwipeLinearLayout ll = new SwipeLinearLayout(holder.itemView.getContext());
            if (onRemove.isPresent()) {
                ll.setOnItemRemovedListener(new SwipeLinearLayout.OnItemRemoveListener() {
                    @Override
                    public void onRemove(final int position, final int index) {
                        onRemove.get().onRemove(position, index);
                    }
                });
            }
            ll.setOrientation(LinearLayout.VERTICAL);
            final View header = preference.getView(null, null);
            final TextView title = (TextView) header.findViewById(android.R.id.title);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.0F);
            title.setTextColor(ThemeManager.getAccentThemeColor());

            if (preference.getKey() != null &&
                preference.getKey().startsWith(String.valueOf(SwipeLinearLayout.SWIPEABLE_VIEW))) {
                header.setTag(SwipeLinearLayout.SWIPEABLE_VIEW, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        header.performClick();
                    }
                });
            }
            ll.addView(header);
            if (!(preference instanceof ExpandablePreference)) {
                final PreferenceGroup group = (PreferenceGroup) preference;
                for (int i = 0; i < group.getPreferenceCount(); i++) {
                    ll.addView(getView(position, group.getPreference(i)));
                }
            }
            card.addView(ll);
        } else {
            card.addView(getView(position, preference));
        }
        ((View) card.getParent()).invalidate();
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

        @NonNull
        public final CardView card;

        public ViewHolder(final @NonNull LinearLayout itemView) {
            super(itemView);
            itemView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                     ViewGroup.LayoutParams.WRAP_CONTENT));
            card = (CardView) itemView.findViewById(R.id.card_wrapper);
        }

    }
}
