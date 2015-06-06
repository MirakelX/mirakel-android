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

package de.azapps.mirakel.helper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;

import com.google.common.base.Optional;

import java.util.List;

import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.settings.ColorPickerPref;
import de.azapps.mirakel.settings.custom_views.SliderPreference;
import de.azapps.mirakel.settings.custom_views.SwitchCompatPreference;
import de.azapps.mirakel.widget.R;

public class PreferencesWidgetHelper extends PreferencesHelper {

    public PreferencesWidgetHelper(final PreferenceFragment c) {
        super(c);
    }


    @SuppressLint("NewApi")
    public void setFunctionsWidget(final Context context, final int widgetId) {
        final List<ListMirakel> lists = ListMirakel.all();
        final CharSequence[] entryValues = new String[lists.size()];
        final CharSequence[] entries = new String[lists.size()];
        int i = 0;
        for (final ListMirakel list : lists) {
            entryValues[i] = String.valueOf(list.getId());
            entries[i] = list.getName();
            i++;
        }
        final ListMirakel list = WidgetHelper.getList(context, widgetId);
        final SwitchCompatPreference isDark = (SwitchCompatPreference) findPreference("isDark");
        final ListPreference widgetListPreference = (ListPreference) findPreference("widgetList");
        widgetListPreference.setEntries(entries);
        widgetListPreference.setEntryValues(entryValues);
        widgetListPreference.setSummary(this.activity.getString(
                                            R.string.widget_list_summary, list));
        widgetListPreference.setValue(String.valueOf(list.getId()));
        widgetListPreference
        .setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(
                final Preference preference, final Object newValue) {
                WidgetHelper.setList(context, widgetId,
                                     Integer.parseInt((String) newValue));
                final Optional<ListMirakel> listMirakelOptional = ListMirakel.get(Long.parseLong((
                            String) newValue));
                if (!listMirakelOptional.isPresent()) {
                    return true;
                }
                final String list = listMirakelOptional.get().getName();
                widgetListPreference
                .setSummary(PreferencesWidgetHelper.this.activity
                            .getString(
                                R.string.notifications_list_summary,
                                list));
                return true;
            }
        });
        isDark.setChecked(WidgetHelper.isDark(context, widgetId));
        isDark.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference,
                                              final Object newValue) {
                WidgetHelper.setDark(context, widgetId, (Boolean) newValue);
                return true;
            }
        });
        final SwitchCompatPreference noGradient = (SwitchCompatPreference)
                findPreference("widgetUseGradient");
        if (noGradient != null) {
            noGradient.setChecked(WidgetHelper
                                  .gethasGradient(context, widgetId));
            noGradient
            .setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(
                    final Preference preference,
                    final Object newValue) {
                    WidgetHelper.setHasGradient(context, widgetId,
                                                (Boolean) newValue);
                    return true;
                }
            });
        }
        final SwitchCompatPreference showDone = (SwitchCompatPreference) findPreference("showDone");
        showDone.setChecked(WidgetHelper.showDone(context, widgetId));
        showDone.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference,
                                              final Object newValue) {
                WidgetHelper.setDone(context, widgetId, (Boolean) newValue);
                return true;
            }
        });
        final SwitchCompatPreference dueColors = (SwitchCompatPreference) findPreference("widgetDueColors");
        dueColors.setChecked(WidgetHelper.dueColors(context, widgetId));
        dueColors
        .setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(
                final Preference preference, final Object newValue) {
                WidgetHelper.setDueColors(context, widgetId,
                                          (Boolean) newValue);
                return true;
            }
        });
        final SliderPreference widgetTransparency = (SliderPreference) findPreference("widgetTransparency");
        final ColorPickerPref widgetFontColor = (ColorPickerPref) findPreference("widgetFontColor");
        widgetFontColor.setColor(WidgetHelper.getFontColor(context,
                                 widgetId));
        widgetFontColor.setOldColor(WidgetHelper.getFontColor(context,
                                    widgetId));
        widgetFontColor
        .setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(
                final Preference preference,
                final Object newValue) {
                WidgetHelper.setFontColor(context, widgetId,
                                          widgetFontColor.getColor());
                return false;
            }
        });
        final int value = (int) (100.0F - (WidgetHelper.getTransparency(
                                               context, widgetId) / 2.55F));
        widgetTransparency.setSummary(this.activity.getString(
                                          R.string.widget_transparency_summary, value));
        widgetTransparency.setMax(100);
        widgetTransparency.setProgress(value);
        widgetTransparency.setPositiveButtonText(android.R.string.ok);
        widgetTransparency.setNegativeButtonText(android.R.string.cancel);
        widgetTransparency.setDialogTitle(R.string.widget_transparency);
        widgetTransparency.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                WidgetHelper
                .setTransparency(
                    context,
                    widgetId,
                    (int) (255.0F - ((Integer) newValue * 2.55F)));
                widgetTransparency
                .setSummary(PreferencesWidgetHelper.this.activity
                            .getString(
                                R.string.widget_transparency_summary,
                                (Integer) newValue));
                return true;
            }
        });
    }

}
