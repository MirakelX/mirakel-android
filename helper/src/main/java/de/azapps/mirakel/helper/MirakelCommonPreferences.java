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

import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import de.azapps.mirakel.helper.R;

/**
 * In this class we collect all functions to get the current settings. The
 * advantage is, that we can set the default value at a central place
 *
 * @author az
 */
public class MirakelCommonPreferences extends MirakelPreferences {

    private static final Pattern ARRAY_SPLIT = Pattern.compile("_");

    public static boolean addSubtaskToSameList() {
        return settings.getBoolean("subtaskAddToSameList", false);
    }

    public static int getAlarmLater() {
        return settings.getInt("alarm_later", 15);
    }

    public static String getImportFileTitle() {
        return settings.getString("import_file_title",
                                  context.getString(R.string.file_default_title));
    }

    public static String getLanguage() {
        return settings.getString("language", "-1");
    }

    public static int getNotificationsListId() {
        try {
            return Integer.parseInt(settings.getString("notificationsList",
                                    "-1"));
        } catch (final NumberFormatException ignored) {
            return -1;
        }
    }

    public static void setNotificationsListId(final String listId) {
        getEditor().putString("notificationsList", listId).apply();
    }


    public static boolean isDark() {
        return false;
    }

    public static boolean isDebug() {
        if ((settings != null) && MirakelCommonPreferences.isEnabledDebugMenu()) {
            return settings.getBoolean("enabledDebug", BuildConfig.DEBUG);
        }
        return BuildConfig.DEBUG;
    }

    public static boolean isDumpTw() {
        return settings.getBoolean("dump_tw_sync_to_sdcard", false);
    }

    public static boolean isEnabledDebugMenu() {
        return settings.getBoolean("enableDebugMenu", false);
    }

    public static boolean isTablet() {
        final String value = settings.getString("useTabletLayoutNew", null);
        if (value != null) {
            final int orientation = context.getResources().getConfiguration().orientation;
            final int v = Integer.parseInt(value);
            if (v == 0) {
                return false;
            } else if (v == 1) {
                return orientation == Configuration.ORIENTATION_LANDSCAPE;
            } else if (v == 2) {
                return orientation == Configuration.ORIENTATION_PORTRAIT;
            } else if (v == 3) {
                return true;
            }
        }
        return settings.getBoolean("useTabletLayout", context.getResources()
                                   .getBoolean(R.bool.isTablet));
    }

    public static List<Integer> loadIntArray(final String arrayName) {
        final String serialized = settings.getString(arrayName, null);
        if (serialized != null) {
            final String[] stringItems = ARRAY_SPLIT.split(serialized);
            final List<Integer> items = new ArrayList<>(stringItems.length);
            for (final String item : stringItems) {
                if (item.isEmpty()) {
                    continue;
                }
                items.add(Integer.valueOf(item));
            }
            return items;
        }
        return new ArrayList<>(0);
    }

    public static void saveIntArray(final String preferenceName,
                                    final List<Integer> items) {
        final Editor editor = getEditor();
        final StringBuilder pref = new StringBuilder(items.size() * 3);
        for (final Integer item : items) {
            pref.append(String.valueOf(item)).append('_');
        }
        editor.putString(preferenceName, pref.toString());
        editor.apply();
    }

    public static void toogleDebugMenu() {
        settings.edit()
        .putBoolean("enableDebugMenu",
                    !MirakelCommonPreferences.isEnabledDebugMenu())
        .apply();
    }

    public static boolean useNotifications() {
        return settings.getBoolean("notificationsUse", false);
    }

    public static boolean usePersistentNotifications() {
        return settings.getBoolean("notificationsPersistent", true);
    }

    public static boolean usePersistentReminders() {
        return settings.getBoolean("remindersPersistent", true);
    }

    public static boolean isDemoMode() {
        return (settings != null) && settings.getBoolean("demoMode", false);
    }

    public static void setDemoMode(final boolean val) {
        final Editor ed = settings.edit();
        ed.putBoolean("demoMode", val);
        ed.commit(); // Use commit here because we are restarting the app afterwards
    }

    public static boolean writeLogsToFile() {
        return (settings != null) && settings.getBoolean("writeLogsToFile", false);
    }

    public static boolean useAnalytics() {
        return (settings == null) || settings.getBoolean("useAnalytics", true);
    }

    public static void setUseAnalytics(final boolean val) {
        final Editor ed = settings.edit();
        ed.putBoolean("useAnalytics", val);
        ed.apply();
    }
}
