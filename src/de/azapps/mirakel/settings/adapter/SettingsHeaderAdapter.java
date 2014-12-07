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

import android.preference.Preference;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import de.azapps.mirakel.ThemeManager;
import de.azapps.mirakel.adapter.OnItemClickedListener;
import de.azapps.mirakel.settings.Settings;


public class SettingsHeaderAdapter extends
    RecyclerView.Adapter<SettingsHeaderAdapter.SettingsViewHolder> {
    @NonNull
    private List<Settings> settings;
    @NonNull
    private final OnItemClickedListener<Settings> onItemClickedListener;


    public SettingsHeaderAdapter(final @NonNull List<Settings> settings,
                                 final @NonNull OnItemClickedListener<Settings> onClick) {
        this.settings = settings;
        this.onItemClickedListener = onClick;
    }



    public void setData(final @NonNull List<Settings> header) {
        settings = header;
        notifyDataSetChanged();
    }


    @Override
    public SettingsViewHolder onCreateViewHolder(final ViewGroup viewGroup, final int viewType) {
        final Preference header = new Preference(viewGroup.getContext());
        header.setIcon(android.R.drawable.ic_btn_speak_now);//dummy
        return new SettingsViewHolder(header.getView(null, viewGroup));
    }

    @Override
    public void onBindViewHolder(final SettingsViewHolder settingsViewHolder, int position) {
        final Settings setting = settings.get(position);
        if (setting.getIconResId().isPresent()) {
            settingsViewHolder.icon.setImageResource(setting.getIconResId().get());
            settingsViewHolder.icon.setVisibility(View.VISIBLE);
        } else {
            settingsViewHolder.icon.setVisibility(View.GONE);
        }
        settingsViewHolder.title.setText(setting.getName());
        settingsViewHolder.setSetting(setting);
        ((View)settingsViewHolder.icon.getParent()).invalidate();
    }

    @Override
    public int getItemCount() {
        return settings.size();
    }


    public class SettingsViewHolder extends RecyclerView.ViewHolder {
        @NonNull
        final TextView title;
        @NonNull
        final ImageView icon;
        @NonNull
        final TextView summary;

        private Settings setting;

        public SettingsViewHolder(final @NonNull View itemView) {
            super(itemView);
            icon = (ImageView) itemView.findViewById(android.R.id.icon);
            icon.setColorFilter(ThemeManager.getAccentThemeColor());
            title = (TextView) itemView.findViewById(android.R.id.title);
            title.setVisibility(View.VISIBLE);
            summary = (TextView) itemView.findViewById(android.R.id.summary);
            itemView.findViewById(android.R.id.widget_frame).setVisibility(View.GONE);
            summary.setVisibility(View.GONE);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    onItemClickedListener.onItemSelected(setting);
                }
            });
        }

        public synchronized void setSetting(final Settings setting) {
            this.setting = setting;
        }
    }
}
