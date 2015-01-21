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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.support.annotation.NonNull;

import com.google.common.base.Optional;

import java.util.List;

import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.MirakelModelPreferences;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.reminders.ReminderAlarm;
import de.azapps.mirakel.services.NotificationService;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.custom_views.Settings;
import de.azapps.tools.OptionalUtils;

public class NotificationSettingsFragment extends MirakelPreferencesFragment<Settings> {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settings_notifications);


        // Initialize needed Arrays
        final List<ListMirakel> lists = ListMirakel.all();
        final CharSequence entryValues[] = new String[lists.size()];
        final CharSequence entries[] = new String[lists.size()];
        final CharSequence entryValuesWithDefault[] = new String[lists.size() + 1];
        final CharSequence entriesWithDefault[] = new String[lists.size() + 1];
        int i = 0;
        for (final ListMirakel list : lists) {
            final String id = String.valueOf(list.getId());
            final String name = list.getName();
            entryValues[i] = id;
            entries[i] = name;
            entryValuesWithDefault[i + 1] = id;
            entriesWithDefault[i + 1] = name;
            i++;
        }
        entriesWithDefault[0] = getString(R.string.default_list);
        entryValuesWithDefault[0] = "default";


        final CheckBoxPreference notificationsUse = (CheckBoxPreference) findPreference("notificationsUse");
        notificationsUse
        .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(
                final Preference preference,
                final Object newValue) {
                if ((Boolean) newValue) {
                    getActivity()
                    .startService(new Intent(
                                      getActivity(),
                                      NotificationService.class));
                } else {
                    if (getActivity()
                        .startService(new Intent(
                                          getActivity(),
                                          NotificationService.class)) != null) {
                        getActivity()
                        .stopService(new Intent(
                                         getActivity(),
                                         NotificationService.class));
                    }
                }
                final SharedPreferences.Editor e = preference.getEditor();
                e.putBoolean("notificationsUse", (Boolean) newValue);
                e.commit();
                NotificationService.updateServices(getActivity());
                return true;
            }
        });

        final CheckBoxPreference notifSetting = (CheckBoxPreference)
                                                findPreference("notificationsPersistent");
        notifSetting
        .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(
                final Preference preference,
                final Object newValue) {
                final SharedPreferences.Editor e = preference.getEditor();
                e.putBoolean("notificationsPersistent", (Boolean) newValue);
                e.commit();
                NotificationService.updateServices(getActivity());
                return true;
            }
        });
        // Load the preferences from an XML resource
        // Notifications List
        final ListPreference notificationsListPreference = (ListPreference)
                findPreference("notificationsList");
        final ListPreference notificationsListOpenPreference = (ListPreference)
                findPreference("notificationsListOpen");
        notificationsListPreference.setEntries(entries);
        notificationsListPreference.setEntryValues(entryValues);
        notificationsListPreference.setValue(MirakelCommonPreferences
                                             .getNotificationsListId() + "");
        final ListMirakel notificationsList = MirakelModelPreferences
                                              .getNotificationsList();
        notificationsListPreference.setSummary(getString(R.string.notifications_list_summary,
                                               notificationsList.getName()));
        notificationsListPreference
        .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(
                final Preference preference,
                final Object newValue) {
                Optional<ListMirakel> listMirakelOptional = ListMirakel.get(Integer.parseInt((String) newValue));
                if (!listMirakelOptional.isPresent()) {
                    return false; // Do not update summary
                }
                final String list = listMirakelOptional.get().getName();
                notificationsListPreference
                .setSummary(getActivity()
                            .getString(
                                R.string.notifications_list_summary,
                                list));
                if (MirakelCommonPreferences
                    .isNotificationListOpenDefault()) {
                    notificationsListOpenPreference
                    .setSummary(getActivity()
                                .getString(
                                    R.string.notifications_list_summary,
                                    list));
                }
                MirakelCommonPreferences
                .setNotificationsListId((String) newValue);
                notificationsListPreference
                .setValue((String) newValue);
                NotificationService.updateServices(getActivity());
                return false;
            }
        });
        notificationsListOpenPreference.setEntries(entriesWithDefault);
        notificationsListOpenPreference
        .setEntryValues(entryValuesWithDefault);
        notificationsListOpenPreference.setValue(MirakelCommonPreferences
                .getNotificationsListOpenId() + "");
        final Optional<ListMirakel> notificationsListOpen = MirakelModelPreferences
                .getNotificationsListOpen();
        OptionalUtils.withOptional(notificationsListOpen, new OptionalUtils.Procedure<ListMirakel>() {
            @Override
            public void apply(ListMirakel input) {
                notificationsListOpenPreference.setSummary(getString(
                            R.string.notifications_list_open_summary,
                            input.getName()));
            }
        });
        notificationsListOpenPreference
        .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(
                final Preference preference,
                final Object newValue) {
                String list;
                if (!"default".equals(newValue.toString())) {
                    list = ListMirakel.get(
                               Integer.parseInt((String) newValue)).get()
                           .getName();
                } else {
                    list = MirakelModelPreferences
                           .getNotificationsList().getName();
                }
                notificationsListOpenPreference
                .setSummary(getActivity()
                            .getString(
                                R.string.notifications_list_summary,
                                list));
                MirakelCommonPreferences
                .setNotificationsListOpenId((String) newValue);
                notificationsListOpenPreference
                .setValue((String) newValue);
                NotificationService.updateServices(getActivity());
                return false;
            }
        });


        final CheckBoxPreference remindersPersistent = (CheckBoxPreference)
                findPreference("remindersPersistent");
        remindersPersistent
        .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(
                final Preference preference,
                final Object newValue) {
                final SharedPreferences.Editor e = preference.getEditor();
                e.putBoolean("remindersPersistent",
                             (Boolean) newValue);
                e.commit();
                ReminderAlarm.restart(getActivity());
                return true;
            }
        });
    }

    @NonNull
    @Override
    public Settings getItem() {
        return Settings.NOTIFICATION;
    }
}
