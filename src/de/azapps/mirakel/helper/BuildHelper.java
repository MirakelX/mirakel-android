package de.azapps.mirakel.helper;

import de.azapps.mirakel.DefinitionsHelper;



public class BuildHelper {

	public static boolean isBeta() {
		return MirakelCommonPreferences.isDebug();
	}

	public static boolean isForFDroid() {
		return !DefinitionsHelper.IS_PLAYSTORE;
	}

	public static boolean isForPlayStore() {
		return DefinitionsHelper.IS_PLAYSTORE;
	}

	public static boolean isNightly() {
		return MirakelCommonPreferences.isDebug();
	}

	public static boolean isRelease() {
		return !MirakelCommonPreferences.isDebug();
	}

	public static boolean useAutoUpdater() {
		return !(isForPlayStore() || isForFDroid());
	}
}
