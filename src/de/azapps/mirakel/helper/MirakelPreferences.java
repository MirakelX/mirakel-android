package de.azapps.mirakel.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class MirakelPreferences {
	
	protected static Context				context;
	protected static SharedPreferences	settings;

	public static void init(Context ctx) {
		if (settings == null || MirakelPreferences.context == null) {
			MirakelPreferences.context = ctx;
			settings = PreferenceManager.getDefaultSharedPreferences(ctx);
		}
	}

	public static Editor getEditor() {
		return settings.edit();
	}

}
