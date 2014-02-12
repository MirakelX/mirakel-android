/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists Copyright (c) 2013-2014 Anatolij Zelenin, Georg
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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import de.azapps.mirakel.custom_views.TaskDetailView.TYPE;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.account.AccountMirakel.ACCOUNT_TYPES;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakelandroid.BuildConfig;
import de.azapps.mirakelandroid.R;

/**
 * In this class we collect all functions to get the current settings. The advantage is, that we can
 * set the default value at a central place
 * 
 * @author az
 */
public class MirakelPreferences {
	private static Context				context;
	private static SharedPreferences	settings;

	public static boolean addSubtaskToSameList() {
		return settings.getBoolean("subtaskAddToSameList", true);
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

	public static int getAutoBackupIntervall() {
		return settings.getInt("autoBackupIntervall", 7);
	}

	private static Calendar getCalendar(String name, String default_string) {
		Calendar ret;
		try {
			ret = DateTimeHelper.parseDate(settings.getString(name,
					default_string));
		} catch (ParseException e) {
			return null;
		}
		return ret;
	}

	public static AccountMirakel getDefaultAccount() {
		int id = settings.getInt("defaultAccountID", AccountMirakel.getLocal()
				.getId());
		AccountMirakel a = AccountMirakel.get(id);
		if (a != null) return a;
		return AccountMirakel.getLocal();
	}

	public static Editor getEditor() {
		return settings.edit();
	}

	public static String getFromLog(int id) {
		return settings.getString("OLD" + id, "");
	}

	public static ListMirakel getImportDefaultList(boolean safe) {
		if (settings.getBoolean("importDefaultList", false)) {
			int listId = settings.getInt("defaultImportList", 0);
			if (listId == 0) return null;
			return ListMirakel.getList(listId);
		}
		if (!safe) return null;
		return ListMirakel.safeFirst(context);
	}

	public static String getImportFileTitle() {
		return settings.getString("import_file_title",
				context.getString(R.string.file_default_title));
	}

	public static String getLanguage() {
		return settings.getString("language", "-1");
	}

	public static ListMirakel getListForSubtask(Task parent) {
		if (settings.contains("subtaskAddToSameList")) {
			if (addSubtaskToSameList()) return parent.getList();
			return subtaskAddToList();
		}
		// Create a new list and set this list as the default list for future subtasks
		final String listname=context
				.getString(R.string.subtask_list_name);
		final AccountMirakel a = parent.getList().getAccount();
		ListMirakel l = ListMirakel.findByName(listname, a);
		if (l == null) {
			l = ListMirakel.newList(listname);
			l.setAccount(a);
			l.save(false);
		}
		return l;
	}

	private static ListMirakel getListFromIdString(int preference) {
		ListMirakel list;
		try {
			list = ListMirakel.getList(preference);
		} catch (NumberFormatException e) {
			list = SpecialList.firstSpecial();
		}
		if (list == null) {
			list = ListMirakel.safeFirst(context);
		}
		return list;
	}

	public static Calendar getNextAutoBackup() {
		return getCalendar("autoBackupNext", "");
	}

	public static ListMirakel getNotificationsList() {
		return getListFromIdString(getNotificationsListId());
	}

	public static int getNotificationsListId() {
		try {
			return Integer.parseInt(settings.getString("notificationsList",
					"-1"));
		} catch (NumberFormatException E) {
			return -1;
		}
	}

	public static ListMirakel getNotificationsListOpen() {
		return ListMirakel.getList(getNotificationsListOpenId());
	}

	public static int getNotificationsListOpenId() {
		int listId = getNotificationsListId();
		String listOpen = settings
				.getString("notificationsListOpen", "default");
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

	public static ListMirakel getStartupList() {
		try {
			return ListMirakel.safeGetList(Integer.parseInt(settings.getString(
					"startupList", "-1")));
		} catch (NumberFormatException E) {
			return ListMirakel.safeFirst(context);
		}
	}

	public static int getSyncFrequency(AccountMirakel account) {
		try {
			return Integer.parseInt(settings.getString("syncFrequency"
					+ account.getName(), "-1"));
		} catch (NumberFormatException E) {
			return -1;
		}
	}

	@SuppressWarnings("boxing")
	public static List<Integer> getTaskFragmentLayout() {
		List<Integer> items = MirakelPreferences
				.loadIntArray("task_fragment_adapter_settings");
		if (items.size() == 0) {// should not be, add all
			items.add(TYPE.HEADER);
			items.add(TYPE.DUE);
			items.add(TYPE.REMINDER);
			items.add(TYPE.CONTENT);
			items.add(TYPE.PROGRESS);
			items.add(TYPE.SUBTASK);
			items.add(TYPE.FILE);
			setTaskFragmentLayout(items);
		}
		return items;
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

	public static void init(Context ctx) {
		if (settings == null || MirakelPreferences.context == null) {
			MirakelPreferences.context = ctx;
			settings = PreferenceManager.getDefaultSharedPreferences(ctx);
		}
	}

	public static boolean isDark() {
		return settings.getBoolean("DarkTheme", false);
	}

	public static boolean isDateFormatRelative() {
		return settings.getBoolean("dateFormatRelative", true);
	}

	public static boolean isDebug() {
		if(settings!=null&&MirakelPreferences.isEnabledDebugMenu()) return settings.getBoolean("enabledDebug", BuildConfig.DEBUG);
		return BuildConfig.DEBUG;
	}

	public static boolean isDumpTw() {
		return settings.getBoolean("dump_tw_sync_to_sdcard", false);
	}

	public static boolean isEnabledDebugMenu() {
		return settings.getBoolean("enableDebugMenu", false);
	}

	public static boolean isNotificationListOpenDefault() {

		String listOpen = settings
				.getString("notificationsListOpen", "default");
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
		String value=settings.getString("useTabletLayoutNew", null);
		if(value!=null){
			int orientation = context.getResources().getConfiguration().orientation;
			int v = Integer.parseInt(value);
			if (v == 0) return false;
			else if (v == 1) return orientation == Configuration.ORIENTATION_LANDSCAPE;
			else if (v == 2) return orientation == Configuration.ORIENTATION_PORTRAIT;
			else if (v == 3) return true;
		}

		return settings.getBoolean("useTabletLayout", context.getResources()
				.getBoolean(R.bool.isTablet));

	}

	public static List<Integer> loadIntArray(String arrayName) {
		String serialized = settings.getString(arrayName, null);
		List<Integer> items = new ArrayList<Integer>();
		if (serialized != null) {
			String[] string_items = serialized.split("_");
			for (String item : string_items) {
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

	public static boolean saveIntArray(String preferenceName, List<Integer> items) {
		SharedPreferences.Editor editor = getEditor();
		String pref = "";
		for (Integer item : items) {
			pref += String.valueOf(item) + "_";
		}
		editor.putString(preferenceName, pref);
		return editor.commit();
	}

	public static void setAutoBackupIntervall(int val) {
		Editor ed = settings.edit();
		ed.putInt("autoBackupIntervall", val);
		ed.commit();
	}

	public static void setDefaultAccount(AccountMirakel a) {
		settings.edit().putInt("defaultAccountID", a.getId()).commit();
	}

	public static void setNextBackup(Calendar val) {
		Editor ed = settings.edit();
		ed.putString("autoBackupNext", DateTimeHelper.formatDate(val));
		ed.commit();
	}

	public static void setShowAccountName(boolean b) {
		settings.edit().putBoolean("show_account_name", b).commit();

	}

	public static void setTaskFragmentLayout(List<Integer> newV) {
		MirakelPreferences.saveIntArray("task_fragment_adapter_settings", newV);
	}

	public static boolean showDoneMain() {
		return settings.getBoolean("showDoneMain", true);
	}

	public static boolean showKillButton() {
		return settings.getBoolean("KillButton", false);
	}

	public static ListMirakel subtaskAddToList() {
		try {
			return ListMirakel.getList(Integer.parseInt(settings.getString(
					"subtaskAddToList", "-1")));
		} catch (NumberFormatException E) {
			return null;
		}
	}

	public static boolean swipeBehavior() {
		return settings.getBoolean("swipeBehavior", false);
	}

	public static void toogleDebugMenu() {
		settings.edit().putBoolean("enableDebugMenu", !MirakelPreferences.isEnabledDebugMenu()).commit();

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

	public static boolean useSync() {
		List<AccountMirakel>all=AccountMirakel.getAll();
		for (AccountMirakel a : all) {
			if (a.getType() != ACCOUNT_TYPES.LOCAL && a.isEnabeld()) return true;
		}
		return false;
	}
}
