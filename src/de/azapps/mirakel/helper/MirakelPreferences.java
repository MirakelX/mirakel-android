package de.azapps.mirakel.helper;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakelandroid.R;

/**
 * In this class we collect all functions to get the current settings. The
 * advantage is, that we can set the default value at a central place
 * 
 * @author az
 * 
 */
public class MirakelPreferences {
	private static SharedPreferences settings;
	private static Context context;

	public static void init(Context context) {
		MirakelPreferences.context = context;
		settings = PreferenceManager.getDefaultSharedPreferences(context);
	}

	public static boolean isDark() {
		return settings.getBoolean("DarkTheme", false);
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

	public static Calendar getNextAutoBackup() {
		return getCalendar("autoBackupNext", "");
	}

	public static void setNextBackup(Calendar val) {
		Editor ed = settings.edit();
		ed.putString("autoBackupNext", DateTimeHelper.formatDate(val));
		ed.commit();
	}

	public static int getAutoBackupIntervall() {
		return settings.getInt("autoBackupIntervall", 7);
	}

	public static void setAutoBackupIntervall(int val) {
		Editor ed = settings.edit();
		ed.putInt("autoBackupIntervall", val);
		ed.commit();
	}

	public static boolean useNotifications() {
		return settings.getBoolean("notificationsUse", false);
	}

	public static int getAlarmLater() {
		return settings.getInt("alarm_later", 15);
	}

	public static boolean isTablet() {
		return settings.getBoolean("useTabletLayout", context.getResources()
				.getBoolean(R.bool.isTablet));
	}

	public static ListMirakel getImportDefaultList() {
		if (settings.getBoolean("importDefaultList", false)) {
			int listId = settings.getInt("defaultImportList", 0);
			if (listId == 0)
				return null;
			return ListMirakel.getList(listId);
		}
		return null;
	}

	public static boolean isDateFormatRelative() {
		return settings.getBoolean("dateFormatRelative", true);
	}

	public static int getUndoNumber() {
		return settings.getInt("UndoNumber", 10);
	}

	public static String getFromLog(int id) {
		return settings.getString("OLD" + id, "");
	}

	public static Editor getEditor() {
		return settings.edit();
	}

	public static int getNotificationsListId() {
		return settings.getInt("notificationsList", -1);
	}

	public static ListMirakel getNotificationsList() {
		return getListFromIdString(getNotificationsListId());
	}

	private static ListMirakel getListFromIdString(int preference) {
		ListMirakel list;
		try {
			list = ListMirakel.getList(preference);
		} catch (NumberFormatException e) {
			list = SpecialList.firstSpecial();
		}
		if (list == null)
			list = ListMirakel.safeFirst(context);
		return list;
	}

	public static boolean isNotificationListOpenDefault() {

		String listOpen = settings
				.getString("notificationsListOpen", "default");
		return listOpen.equals("default");
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

	public static ListMirakel getNotificationsListOpen() {
		return ListMirakel.getList(getNotificationsListOpenId());
	}

	public static boolean isStartupAllLists() {
		return settings.getBoolean("startupAllLists", false);
	}

	public static ListMirakel getStartupList() {
		return ListMirakel.getList(settings.getInt("startupList", -1));
	}

	public static boolean useSync() {
		return settings.getBoolean("syncUse", false);
	}

	public static int getSyncFrequency() {
		return settings.getInt("syncFrequency", -1);
	}

	public static String getImportFileTitle() {
		return settings.getString("import_file_title",
				context.getString(R.string.file_default_title));
	}

	public static boolean addSubtaskToSameList() {
		return settings.getBoolean("subtaskAddToSameList", true);
	}

	public static ListMirakel subtaskAddToList() {
		return ListMirakel.getList(settings.getInt("subtaskAddToList", -1));
	}

	public static String getLanguage() {
		return settings.getString("language", "-1");
	}

	public static List<Integer> loadIntArray(String arrayName) {
		String serialized = settings.getString(arrayName, null);
		if (serialized == null)
			return null;
		List<Integer> items = new ArrayList<Integer>();
		String[] string_items = serialized.split("_");
		for (String item : string_items) {
			if (item.length() == 0)
				continue;
			items.add(Integer.valueOf(item));
		}
		return items;
	}

	public static boolean saveIntArray(String preferenceName,
			List<Integer> items) {
		SharedPreferences.Editor editor = getEditor();
		String pref = "";
		for (Integer item : items) {
			pref += String.valueOf(item) + "_";
		}
		editor.putString(preferenceName, pref);
		return editor.commit();
	}

	public static boolean isSubtaskDefaultNew() {
		return settings.getBoolean("subtaskDefaultNew", true);
	}
	public static boolean useSemanticNewTask() {
		return settings.getBoolean("semanticNewTask", true);
	}
}
