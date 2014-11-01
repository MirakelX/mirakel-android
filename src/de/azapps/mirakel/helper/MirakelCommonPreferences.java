/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists Copyright (c) 2013 Anatolij Zelenin, Georg
 * Semmler. This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either version 3
 * of the License, or any later version. This program is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.azapps.mirakel.helper;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * In this class we collect all functions to get the current settings. The
 * advantage is, that we can set the default value at a central place
 *
 * @author az
 */
public class MirakelCommonPreferences extends MirakelPreferences {

    public static boolean addSubtaskToSameList() {
        return settings.getBoolean("subtaskAddToSameList", false);
    }


    public static boolean containsHighlightSelected() {
        return settings.contains("highlightSelected");
    }

    public static boolean containsStartupList() {
        return settings.contains("startupList");
    }

    public static int getAlarmLater() {
        return settings.getInt("alarm_later", 15);
    }

    public static String getAudioDefaultTitle() {
        return settings.getString("audioDefaultTitle",
                                  context.getString(R.string.audio_default_title));
    }

    public static int getAutoBackupInterval() {
        return settings.getInt("autoBackupInterval", 7);
    }

    private static Calendar getCalendar(final String name,
                                        final String defaultString) {
        final Calendar ret;
        try {
            ret = DateTimeHelper.parseDate(settings.getString(name,
                                           defaultString));
        } catch (final ParseException e) {
            return null;
        }
        return ret;
    }

    public static String getFromLog(final int id) {
        return settings.getString("OLD" + id, "");
    }

    public static String getImportFileTitle() {
        return settings.getString("import_file_title",
                                  context.getString(R.string.file_default_title));
    }

    public static String getLanguage() {
        return settings.getString("language", "-1");
    }

    public static Calendar getNextAutoBackup() {
        return getCalendar("autoBackupNext", "");
    }

    public static int getNotificationsListId() {
        try {
            return Integer.parseInt(settings.getString("notificationsList",
                                    "-1"));
        } catch (final NumberFormatException E) {
            return -1;
        }
    }

    public static void setNotificationsListId(final String listId) {
        getEditor().putString("notificationsList", listId).apply();
    }

    public static void setNotificationsListOpenId(final String listId) {
        getEditor().putString("notificationsListOpen", listId).apply();
    }

    public static int getNotificationsListOpenId() {
        int listId = getNotificationsListId();
        final String listOpen = settings.getString("notificationsListOpen",
                                "default");
        if (!"default".equals(listOpen)) {
            listId = Integer.parseInt(listOpen);
        }
        return listId;
    }

    public static int getOldVersion() {
        return settings.getInt("mirakel_old_version", -1);
    }

    public static String getPhotoDefaultTitle() {
        return settings.getString("photoDefaultTitle",
                                  context.getString(R.string.photo_default_title));
    }

    public static int getUndoNumber() {
        return settings.getInt("UndoNumber", 10);
    }

    public static String getVersionKey() {
        return settings.getString("PREFS_VERSION_KEY", "");
    }

    public static boolean hideKeyboard() {
        return settings.getBoolean("hideKeyboard", true);
    }

    public static boolean highlightSelected() {
        return settings.getBoolean("highlightSelected", isTablet());
    }

    public static boolean isDark() {
        return settings.getBoolean("DarkTheme", false);
    }

    public static void setIsDark(boolean isDark) {
        final Editor editor = getEditor();
        editor.putBoolean("DarkTheme", isDark);
        editor.apply();
    }

    public static boolean isDebug() {
        if (settings != null && MirakelCommonPreferences.isEnabledDebugMenu()) {
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

    public static boolean isNotificationListOpenDefault() {
        final String listOpen = settings.getString("notificationsListOpen",
                                "default");
        return listOpen.equals("default");
    }

    public static boolean isShowAccountName() {
        return settings.getBoolean("show_account_name", false);
    }

    public static boolean isStartupAllLists() {
        return settings.getBoolean("startupAllLists", false);
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
        final List<Integer> items = new ArrayList<Integer>();
        if (serialized != null) {
            final String[] stringItems = serialized.split("_");
            for (final String item : stringItems) {
                if (item.isEmpty()) {
                    continue;
                }
                items.add(Integer.valueOf(item));
            }
        }
        return items;
    }

    public static boolean lockDrawerInTaskFragment() {
        return settings.getBoolean("lockDrawerInTaskFragment", false);
    }

    public static void saveIntArray(final String preferenceName,
                                    final List<Integer> items) {
        final SharedPreferences.Editor editor = getEditor();
        final StringBuilder pref = new StringBuilder(items.size() * 3);
        for (final Integer item : items) {
            pref.append(String.valueOf(item) + '_');
        }
        editor.putString(preferenceName, pref.toString());
        editor.apply();
    }

    public static void setAutoBackupInterval(final int val) {
        final Editor ed = settings.edit();
        ed.putInt("autoBackupInterval", val);
        ed.apply();
    }

    public static void setNextBackup(final Calendar val) {
        final Editor ed = settings.edit();
        ed.putString("autoBackupNext", DateTimeHelper.formatDate(val));
        ed.apply();
    }

    public static void setShowAccountName(final boolean showAccountName) {
        settings.edit().putBoolean("show_account_name", showAccountName).apply();
    }

    public static void setTaskFragmentLayout(final List<Integer> newV) {
        MirakelCommonPreferences.saveIntArray("task_fragment_adapter_settings",
                                              newV);
    }

    public static boolean showDoneMain() {
        return settings.getBoolean("showDoneMain", false);
    }

    public static void toogleDebugMenu() {
        settings.edit()
        .putBoolean("enableDebugMenu",
                    !MirakelCommonPreferences.isEnabledDebugMenu())
        .apply();
    }

    public static boolean useBtnAudioRecord() {
        return settings.getBoolean("useBtnAudioRecord", true);
    }

    public static boolean useBtnCamera() {
        return settings.getBoolean("useBtnCamera", true);
    }

    public static boolean useBtnSpeak() {
        return settings.getBoolean("useBtnSpeak", false);
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
        return settings != null && settings.getBoolean("demoMode", false);
    }

    public static void setDemoMode(final boolean val) {
        final Editor ed = settings.edit();
        ed.putBoolean("demoMode", val);
        ed.apply();
    }

    public static boolean writeLogsToFile() {
        return settings != null && settings.getBoolean("writeLogsToFile", false);
    }

    public static boolean useNewUI() {
        return settings != null && settings.getBoolean("newUI", false);
    }

    public static void setUseNewUI(final boolean val) {
        final Editor ed = settings.edit();
        ed.putBoolean("newUI", val);
        ed.commit(); // Use commit here because we are restarting the app afterwards
    }

}
