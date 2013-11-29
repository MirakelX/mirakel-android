package de.azapps.mirakel.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class MirakelPreferences {
	private static SharedPreferences settings;

	public static void init(Context ctx) {
		settings = PreferenceManager.getDefaultSharedPreferences(ctx);
	}

	public static boolean isDark() {
		return settings.getBoolean("DarkTheme", false);
	}
}
