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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;

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

    public static boolean colorizeSubTasks() {
        return settings.getBoolean("colorize_subtasks", true);
    }

    public static boolean colorizeTasks() {
        return settings.getBoolean("colorize_tasks", true);
    }

    public static boolean colorizeTasksEverywhere() {
        return settings.getBoolean("colorize_tasks_everywhere", false);
    }

    public static boolean containsHighlightSelected() {
        return settings.contains("highlightSelected");
    }

    public static boolean containsStartupAllLists() {
        return settings.contains("startupAllLists");
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
                                        final String default_string) {
        Calendar ret;
        try {
            ret = DateTimeHelper.parseDate(settings.getString(name,
                                           default_string));
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
        getEditor().putString("notificationsList", listId).commit();
    }

    public static void setNotificationsListOpenId(final String listId) {
        getEditor().putString("notificationsListOpen", listId).commit();
    }

    public static int getNotificationsListOpenId() {
        int listId = getNotificationsListId();
        final String listOpen = settings.getString("notificationsListOpen",
                                "default");
        if (!listOpen.equals("default")) {
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

    public static boolean hideEmptyNotifications() {
        return settings.getBoolean("notificationsZeroHide", true);
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

    public static boolean isDateFormatRelative() {
        return settings.getBoolean("dateFormatRelative", true);
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

    public static boolean isSubtaskDefaultNew() {
        return settings.getBoolean("subtaskDefaultNew", true);
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
            final String[] string_items = serialized.split("_");
            for (final String item : string_items) {
                if (item.length() == 0) {
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

    public static boolean saveIntArray(final String preferenceName,
                                       final List<Integer> items) {
        final SharedPreferences.Editor editor = getEditor();
        String pref = "";
        for (final Integer item : items) {
            pref += String.valueOf(item) + "_";
        }
        editor.putString(preferenceName, pref);
        return editor.commit();
    }

    public static void setAutoBackupInterval(final int val) {
        final Editor ed = settings.edit();
        ed.putInt("autoBackupInterval", val);
        ed.commit();
    }

    public static void setNextBackup(final Calendar val) {
        final Editor ed = settings.edit();
        ed.putString("autoBackupNext", DateTimeHelper.formatDate(val));
        ed.commit();
    }

    public static void setShowAccountName(final boolean b) {
        settings.edit().putBoolean("show_account_name", b).commit();
    }

    public static void setTaskFragmentLayout(final List<Integer> newV) {
        MirakelCommonPreferences.saveIntArray("task_fragment_adapter_settings",
                                              newV);
    }

    public static boolean showDoneMain() {
        return settings.getBoolean("showDoneMain", true);
    }

    public static boolean showKillButton() {
        return settings.getBoolean("KillButton", false);
    }

    public static boolean swipeBehavior() {
        return settings.getBoolean("swipeBehavior", false);
    }

    public static void toogleDebugMenu() {
        settings.edit()
        .putBoolean("enableDebugMenu",
                    !MirakelCommonPreferences.isEnabledDebugMenu())
        .commit();
    }

    public static boolean useBigNotifications() {
        return settings.getBoolean("notificationsBig", true);
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

    public static boolean useSemanticNewTask() {
        return settings.getBoolean("semanticNewTask", true);
    }

    public static boolean isDemoMode() {
        if (settings == null) {
            return false;
        }
        return settings.getBoolean("demoMode", false);
    }

    public static void setDemoMode(final boolean val) {
        final Editor ed = settings.edit();
        ed.putBoolean("demoMode", val);
        ed.commit();
    }

    public static boolean writeLogsToFile() {
        if (settings == null) {
            return false;
        }
        return settings.getBoolean("writeLogsToFile", false);
    }

    public static boolean useNewUI() {
        if (settings == null) {
            return false;
        }
        return settings.getBoolean("newUI", false);
    }

    public static void setUseNewUI(final boolean val) {
        final Editor ed = settings.edit();
        ed.putBoolean("newUI", val);
        ed.commit();
    }

}
