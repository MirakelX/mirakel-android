package de.azapps.mirakel.helper;

import java.text.ParseException;
import java.util.Calendar;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

/**
 * In this class we collect all functions to get the current settings. The
 * advantage is, that we can set the default value at a central place
 * 
 * @author az
 * 
 */
public class MirakelPreferences {
	private static SharedPreferences settings;

	public static void init(Context ctx) {
		settings = PreferenceManager.getDefaultSharedPreferences(ctx);
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
}
